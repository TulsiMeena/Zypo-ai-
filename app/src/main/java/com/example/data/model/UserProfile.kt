package com.example.data.model

data class UserProfile(
    val uid: String = "",
    val displayName: String = "Zypo Explorer",
    val email: String = "user@zypo.ai",
    val photoUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val lastLoginAt: Long = System.currentTimeMillis(),
    val plan: String = "Free Tier",
    val language: String = "English (US)",
    val isEmailVerified: Boolean = false,
    val preferences: Map<String, String> = emptyMap()
)

enum class AuthState {
    IDLE,
    LOADING,
    SUCCESS,
    ERROR,
    VERIFICATION_REQUIRED,
    UNCONFIGURED
}
