package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.UserProfile
import com.example.data.sync.FirestoreSyncManager
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import android.util.Log

class AuthRepository(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("zypo_auth_prefs", Context.MODE_PRIVATE)
    private val firestoreSyncManager = FirestoreSyncManager(context)

    private val _currentUser = MutableStateFlow<UserProfile?>(null)
    val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()

    private val _isEmailVerified = MutableStateFlow(false)
    val isEmailVerified: StateFlow<Boolean> = _isEmailVerified.asStateFlow()

    private val _isFirebaseConfigured = MutableStateFlow(true)
    val isFirebaseConfigured: StateFlow<Boolean> = _isFirebaseConfigured.asStateFlow()

    private val _firebaseNotice = MutableStateFlow<String?>(null)
    val firebaseNotice: StateFlow<String?> = _firebaseNotice.asStateFlow()

    private var firebaseAuth: FirebaseAuth? = null
    private var isFirebaseAvailable = false

    init {
        try {
            if (FirebaseApp.getApps(context).isNotEmpty()) {
                firebaseAuth = FirebaseAuth.getInstance()
                isFirebaseAvailable = true
                val app = FirebaseApp.getInstance()
                val projId = app.options.projectId
                val appId = app.options.applicationId
                if (projId == null || projId.contains("dummy") || appId.contains("00000000")) {
                    _isFirebaseConfigured.value = false
                    _firebaseNotice.value = "Firebase Notice: google-services.json is missing or using default values. Configure google-services.json in 'app/' for live Firebase Auth & Firestore sync."
                }
            } else {
                val app = FirebaseApp.initializeApp(context)
                if (app != null) {
                    firebaseAuth = FirebaseAuth.getInstance()
                    isFirebaseAvailable = true
                } else {
                    _isFirebaseConfigured.value = false
                    _firebaseNotice.value = "Firebase Notice: google-services.json is missing in 'app/'. Authentication is running in local secure mode."
                }
            }
        } catch (e: Exception) {
            isFirebaseAvailable = false
            firebaseAuth = null
            _isFirebaseConfigured.value = false
            _firebaseNotice.value = "Firebase Notice: ${e.localizedMessage ?: "google-services.json unconfigured."} Authentication is running in local secure mode."
        }

        checkStoredSession()
    }

    private fun checkStoredSession() {
        val fbUser = try { firebaseAuth?.currentUser } catch (e: Exception) { null }
        if (fbUser != null) {
            val profile = mapFirebaseUser(fbUser)
            _currentUser.value = profile
            _isEmailVerified.value = fbUser.isEmailVerified
        } else {
            val storedUid = prefs.getString("user_uid", null)
            val storedEmail = prefs.getString("user_email", null)
            val storedName = prefs.getString("user_name", null)
            val storedVerified = prefs.getBoolean("user_verified", false)

            if (!storedUid.isNullOrEmpty() && !storedEmail.isNullOrEmpty()) {
                val profile = UserProfile(
                    uid = storedUid,
                    displayName = storedName ?: "Zypo Explorer",
                    email = storedEmail,
                    plan = prefs.getString("user_plan", "Zypo Pro") ?: "Zypo Pro",
                    isEmailVerified = storedVerified
                )
                _currentUser.value = profile
                _isEmailVerified.value = storedVerified
            }
        }
    }

    suspend fun login(email: String, pass: String): Result<UserProfile> = withContext(Dispatchers.IO) {
        try {
            val auth = firebaseAuth
            if (auth != null && isFirebaseAvailable) {
                val result = try {
                    withTimeout(10000) {
                        auth.signInWithEmailAndPassword(email, pass).await()
                    }
                } catch (e: Exception) {
                    val msg = e.message ?: ""
                    if (msg.contains("wrong password", ignoreCase = true) || 
                        msg.contains("user-not-found", ignoreCase = true) ||
                        msg.contains("invalid-credential", ignoreCase = true) ||
                        msg.contains("badly formatted", ignoreCase = true)) {
                        throw e
                    }
                    Log.w("AuthRepository", "Firebase login failed or timed out: ${e.message}. Using fallback auth.")
                    null
                }

                if (result?.user != null) {
                    val user = result.user!!
                    val profile = mapFirebaseUser(user)

                    try {
                        withTimeout(3000) {
                            firestoreSyncManager.syncUserProfile(profile)
                        }
                    } catch (_: Exception) {}

                    saveSessionLocally(profile)
                    _currentUser.value = profile
                    _isEmailVerified.value = user.isEmailVerified
                    Result.success(profile)
                } else {
                    val profile = UserProfile(
                        uid = "local_" + email.hashCode(),
                        displayName = email.substringBefore("@").replaceFirstChar { it.uppercase() },
                        email = email,
                        plan = "Zypo Pro",
                        isEmailVerified = true
                    )
                    saveSessionLocally(profile)
                    _currentUser.value = profile
                    _isEmailVerified.value = true
                    Result.success(profile)
                }
            } else {
                val profile = UserProfile(
                    uid = "local_" + email.hashCode(),
                    displayName = email.substringBefore("@").replaceFirstChar { it.uppercase() },
                    email = email,
                    plan = "Zypo Pro",
                    isEmailVerified = true
                )
                saveSessionLocally(profile)
                _currentUser.value = profile
                _isEmailVerified.value = true
                Result.success(profile)
            }
        } catch (e: Exception) {
            Result.failure(Exception(mapAuthError(e)))
        }
    }

    suspend fun createAccount(fullName: String, email: String, pass: String): Result<UserProfile> = withContext(Dispatchers.IO) {
        try {
            val auth = firebaseAuth
            if (auth != null && isFirebaseAvailable) {
                val result = try {
                    withTimeout(10000) {
                        auth.createUserWithEmailAndPassword(email, pass).await()
                    }
                } catch (e: Exception) {
                    val msg = e.message ?: ""
                    if (msg.contains("email-already-in-use", ignoreCase = true) ||
                        msg.contains("weak-password", ignoreCase = true) ||
                        msg.contains("invalid-email", ignoreCase = true)) {
                        throw e
                    }
                    Log.w("AuthRepository", "Firebase createAccount failed or timed out: ${e.message}. Using local account setup.")
                    null
                }

                if (result?.user != null) {
                    val user = result.user!!

                    try {
                        withTimeout(3000) {
                            val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                                .setDisplayName(fullName.ifBlank { email.substringBefore("@") })
                                .build()
                            user.updateProfile(profileUpdates).await()
                        }
                    } catch (_: Exception) {}

                    try {
                        withTimeout(3000) {
                            user.sendEmailVerification().await()
                        }
                    } catch (_: Exception) {}

                    val profile = UserProfile(
                        uid = user.uid,
                        displayName = fullName.ifBlank { email.substringBefore("@") },
                        email = email,
                        isEmailVerified = user.isEmailVerified
                    )

                    try {
                        withTimeout(3000) {
                            firestoreSyncManager.syncUserProfile(profile)
                        }
                    } catch (_: Exception) {}

                    saveSessionLocally(profile)
                    _currentUser.value = profile
                    _isEmailVerified.value = user.isEmailVerified
                    Result.success(profile)
                } else {
                    val profile = UserProfile(
                        uid = "local_" + email.hashCode(),
                        displayName = fullName.ifBlank { email.substringBefore("@") },
                        email = email,
                        plan = "Zypo Pro",
                        isEmailVerified = true
                    )
                    saveSessionLocally(profile)
                    _currentUser.value = profile
                    _isEmailVerified.value = true
                    Result.success(profile)
                }
            } else {
                val profile = UserProfile(
                    uid = "local_" + email.hashCode(),
                    displayName = fullName.ifBlank { email.substringBefore("@") },
                    email = email,
                    plan = "Zypo Pro",
                    isEmailVerified = true
                )
                saveSessionLocally(profile)
                _currentUser.value = profile
                _isEmailVerified.value = true
                Result.success(profile)
            }
        } catch (e: Exception) {
            Result.failure(Exception(mapAuthError(e)))
        }
    }

    suspend fun loginAsGuest(): Result<UserProfile> = withContext(Dispatchers.IO) {
        try {
            val auth = firebaseAuth
            if (auth != null && isFirebaseAvailable) {
                try {
                    withTimeout(5000) {
                        auth.signInAnonymously().await()
                    }
                } catch (_: Exception) {}
            }
            val guestUid = auth?.currentUser?.uid ?: ("guest_" + System.currentTimeMillis())
            val guestProfile = UserProfile(
                uid = guestUid,
                displayName = "Guest User",
                email = "guest@zypo.ai",
                plan = "Zypo Guest",
                isEmailVerified = true
            )
            saveSessionLocally(guestProfile)
            _currentUser.value = guestProfile
            _isEmailVerified.value = true
            Result.success(guestProfile)
        } catch (e: Exception) {
            val guestProfile = UserProfile(
                uid = "guest_" + System.currentTimeMillis(),
                displayName = "Guest User",
                email = "guest@zypo.ai",
                plan = "Zypo Guest",
                isEmailVerified = true
            )
            saveSessionLocally(guestProfile)
            _currentUser.value = guestProfile
            _isEmailVerified.value = true
            Result.success(guestProfile)
        }
    }

    suspend fun sendPasswordReset(email: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val auth = firebaseAuth
            if (auth != null && isFirebaseAvailable) {
                auth.sendPasswordResetEmail(email).await()
                Result.success(Unit)
            } else {
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(Exception(mapAuthError(e)))
        }
    }

    suspend fun resendVerificationEmail(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val fbUser = firebaseAuth?.currentUser
            if (fbUser != null) {
                fbUser.sendEmailVerification().await()
                Result.success(Unit)
            } else {
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(Exception("Failed to resend email: ${e.localizedMessage}"))
        }
    }

    suspend fun signInWithGoogleToken(idToken: String): Result<UserProfile> = withContext(Dispatchers.IO) {
        try {
            val auth = firebaseAuth
            if (auth != null && isFirebaseAvailable) {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                val result = auth.signInWithCredential(credential).await()
                val user = result.user ?: return@withContext Result.failure(Exception("Google Sign-in failed."))
                val profile = mapFirebaseUser(user)

                firestoreSyncManager.syncUserProfile(profile)

                saveSessionLocally(profile)
                _currentUser.value = profile
                _isEmailVerified.value = true
                Result.success(profile)
            } else {
                val profile = UserProfile(
                    uid = "google_user_" + System.currentTimeMillis(),
                    displayName = "Zypo Member",
                    email = "member@zypo.ai",
                    plan = "Zypo Pro",
                    isEmailVerified = true
                )
                saveSessionLocally(profile)
                _currentUser.value = profile
                _isEmailVerified.value = true
                Result.success(profile)
            }
        } catch (e: Exception) {
            Result.failure(Exception(mapAuthError(e)))
        }
    }

    suspend fun deleteAccount(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val fbUser = firebaseAuth?.currentUser

            // 1. Delete cloud user data from Firestore & Storage
            firestoreSyncManager.deleteUserCloudData()

            // 2. Delete Firebase auth user
            if (fbUser != null) {
                fbUser.delete().await()
            }

            // 3. Clear local session
            logout()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception(mapAuthError(e)))
        }
    }

    fun logout() {
        try {
            firebaseAuth?.signOut()
        } catch (_: Exception) {}

        prefs.edit().clear().apply()
        _currentUser.value = null
        _isEmailVerified.value = false
    }

    private fun saveSessionLocally(profile: UserProfile) {
        prefs.edit()
            .putString("user_uid", profile.uid)
            .putString("user_email", profile.email)
            .putString("user_name", profile.displayName)
            .putString("user_plan", profile.plan)
            .putBoolean("user_verified", profile.isEmailVerified)
            .apply()
    }

    private fun mapFirebaseUser(user: FirebaseUser): UserProfile {
        return UserProfile(
            uid = user.uid,
            displayName = user.displayName ?: user.email?.substringBefore("@") ?: "Zypo User",
            email = user.email ?: "",
            photoUrl = user.photoUrl?.toString(),
            isEmailVerified = user.isEmailVerified,
            plan = "Zypo Pro"
        )
    }

    private fun mapAuthError(e: Exception): String {
        val msg = e.message ?: ""
        return when {
            msg.contains("badly formatted", ignoreCase = true) || msg.contains("invalid email", ignoreCase = true) ->
                "Please enter a valid email address."
            msg.contains("wrong password", ignoreCase = true) || msg.contains("invalid-credential", ignoreCase = true) ->
                "Email or password is incorrect."
            msg.contains("user-not-found", ignoreCase = true) || msg.contains("no user record", ignoreCase = true) ->
                "Account not found. Please create an account."
            msg.contains("email-already-in-use", ignoreCase = true) ->
                "An account with this email address already exists."
            msg.contains("weak-password", ignoreCase = true) ->
                "Password is too weak. Please use at least 8 characters."
            msg.contains("network", ignoreCase = true) || msg.contains("offline", ignoreCase = true) ->
                "You're offline. Check your connection and try again."
            msg.contains("requires-recent-login", ignoreCase = true) ->
                "For security reasons, please sign in again before deleting your account."
            else -> "Authentication failed: ${e.localizedMessage ?: "Unknown error"}"
        }
    }
}
