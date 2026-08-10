package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AuthState
import com.example.data.model.UserProfile
import com.example.data.repository.AuthRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class PasswordStrength(val label: String, val score: Float) {
    NONE("", 0f),
    WEAK("Weak", 0.33f),
    MEDIUM("Medium", 0.66f),
    STRONG("Strong", 1.0f)
}

class AuthViewModel(val authRepository: AuthRepository) : ViewModel() {

    val currentUser: StateFlow<UserProfile?> = authRepository.currentUser
    val isEmailVerified: StateFlow<Boolean> = authRepository.isEmailVerified
    val isFirebaseConfigured: StateFlow<Boolean> = authRepository.isFirebaseConfigured
    val firebaseNotice: StateFlow<String?> = authRepository.firebaseNotice

    private val _authState = MutableStateFlow(AuthState.IDLE)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _infoMessage = MutableStateFlow<String?>(null)
    val infoMessage: StateFlow<String?> = _infoMessage.asStateFlow()

    private val _resendCooldown = MutableStateFlow(0)
    val resendCooldown: StateFlow<Int> = _resendCooldown.asStateFlow()

    private val _toastEvent = MutableSharedFlow<String>()
    val toastEvent: SharedFlow<String> = _toastEvent.asSharedFlow()

    private var cooldownJob: Job? = null

    // Form Fields
    var fullName = MutableStateFlow("")
    var email = MutableStateFlow("")
    var password = MutableStateFlow("")
    var confirmPassword = MutableStateFlow("")
    var rememberMe = MutableStateFlow(true)
    var showPassword = MutableStateFlow(false)
    var showConfirmPassword = MutableStateFlow(false)

    fun calculatePasswordStrength(pass: String): PasswordStrength {
        if (pass.isBlank()) return PasswordStrength.NONE
        if (pass.length < 8) return PasswordStrength.WEAK

        var score = 0
        if (pass.length >= 8) score++
        if (pass.any { it.isDigit() }) score++
        if (pass.any { it.isUpperCase() }) score++
        if (pass.any { !it.isLetterOrDigit() }) score++

        return when {
            score <= 1 -> PasswordStrength.WEAK
            score in 2..3 -> PasswordStrength.MEDIUM
            else -> PasswordStrength.STRONG
        }
    }

    fun login() {
        val em = email.value.trim()
        val pass = password.value.trim()

        if (em.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(em).matches()) {
            _errorMessage.value = "Please enter a valid email address."
            return
        }

        if (pass.isBlank()) {
            _errorMessage.value = "Please enter your password."
            return
        }

        _errorMessage.value = null
        _authState.value = AuthState.LOADING

        viewModelScope.launch {
            val result = authRepository.login(em, pass)
            result.onSuccess { profile ->
                _authState.value = AuthState.SUCCESS
                _toastEvent.emit("Welcome back to Zypo AI, ${profile.displayName}!")
            }.onFailure { err ->
                _authState.value = AuthState.ERROR
                _errorMessage.value = err.message ?: "Login failed. Please check your credentials."
            }
        }
    }

    fun createAccount() {
        val name = fullName.value.trim()
        val em = email.value.trim()
        val pass = password.value.trim()
        val confirm = confirmPassword.value.trim()

        if (name.isBlank()) {
            _errorMessage.value = "Please enter your full name."
            return
        }

        if (em.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(em).matches()) {
            _errorMessage.value = "Please enter a valid email address."
            return
        }

        if (pass.length < 8) {
            _errorMessage.value = "Use at least 8 characters for your password."
            return
        }

        if (pass != confirm) {
            _errorMessage.value = "Passwords do not match."
            return
        }

        _errorMessage.value = null
        _authState.value = AuthState.LOADING

        viewModelScope.launch {
            val result = authRepository.createAccount(name, em, pass)
            result.onSuccess { profile ->
                if (!profile.isEmailVerified) {
                    _authState.value = AuthState.VERIFICATION_REQUIRED
                    startCooldownTimer()
                } else {
                    _authState.value = AuthState.SUCCESS
                }
                _toastEvent.emit("Welcome to Zypo AI, ${profile.displayName}!")
            }.onFailure { err ->
                _authState.value = AuthState.ERROR
                _errorMessage.value = err.message ?: "Account creation failed."
            }
        }
    }

    fun sendPasswordReset() {
        val em = email.value.trim()
        if (em.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(em).matches()) {
            _errorMessage.value = "Please enter a valid email address."
            return
        }

        _errorMessage.value = null
        _authState.value = AuthState.LOADING

        viewModelScope.launch {
            val result = authRepository.sendPasswordReset(em)
            result.onSuccess {
                _authState.value = AuthState.IDLE
                _infoMessage.value = "Check your email for the password reset link."
            }.onFailure { err ->
                _authState.value = AuthState.ERROR
                _errorMessage.value = err.message ?: "Failed to send reset email."
            }
        }
    }

    fun resendVerificationEmail() {
        if (_resendCooldown.value > 0) return

        viewModelScope.launch {
            val result = authRepository.resendVerificationEmail()
            result.onSuccess {
                _toastEvent.emit("Verification email sent!")
                startCooldownTimer()
            }.onFailure { err ->
                _errorMessage.value = err.message
            }
        }
    }

    fun signInWithGoogleToken(idToken: String) {
        _authState.value = AuthState.LOADING
        viewModelScope.launch {
            val result = authRepository.signInWithGoogleToken(idToken)
            result.onSuccess { profile ->
                _authState.value = AuthState.SUCCESS
                _toastEvent.emit("Signed in with Google as ${profile.displayName}")
            }.onFailure { err ->
                _authState.value = AuthState.ERROR
                _errorMessage.value = err.message ?: "Google sign-in failed."
            }
        }
    }

    fun loginAsGuest() {
        _errorMessage.value = null
        _authState.value = AuthState.LOADING
        viewModelScope.launch {
            val result = authRepository.loginAsGuest()
            result.onSuccess { profile ->
                _authState.value = AuthState.SUCCESS
                _toastEvent.emit("Welcome to Zypo AI, Guest!")
            }.onFailure { err ->
                _authState.value = AuthState.ERROR
                _errorMessage.value = err.message ?: "Guest login failed."
            }
        }
    }

    fun startCooldownTimer() {
        cooldownJob?.cancel()
        _resendCooldown.value = 30
        cooldownJob = viewModelScope.launch {
            while (_resendCooldown.value > 0) {
                delay(1000)
                _resendCooldown.value -= 1
            }
        }
    }

    fun logout() {
        authRepository.logout()
        _authState.value = AuthState.IDLE
        _errorMessage.value = null
        _infoMessage.value = null
        fullName.value = ""
        email.value = ""
        password.value = ""
        confirmPassword.value = ""
    }

    fun deleteAccount(onSuccess: () -> Unit) {
        _authState.value = AuthState.LOADING
        viewModelScope.launch {
            val result = authRepository.deleteAccount()
            result.onSuccess {
                _authState.value = AuthState.IDLE
                _toastEvent.emit("Your Zypo AI account and cloud data have been deleted.")
                logout()
                onSuccess()
            }.onFailure { err ->
                _authState.value = AuthState.ERROR
                _errorMessage.value = err.message ?: "Account deletion failed."
            }
        }
    }

    fun clearMessages() {
        _errorMessage.value = null
        _infoMessage.value = null
    }
}
