package com.example

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.example.data.local.AppDatabase
import com.example.data.model.ThemeMode
import com.example.data.repository.AuthRepository
import com.example.data.repository.RoomChatRepositoryImpl
import com.example.navigation.ZypoNavigation
import com.example.ui.theme.ZypoAiTheme
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.ChatViewModel
import com.example.ui.viewmodel.SettingsViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val permissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { }
                LaunchedEffect(Unit) {
                    permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                }
            }

            // Instantiate Database, Repositories & ViewModels
            val database = remember { AppDatabase.getDatabase(applicationContext) }
            val repository = remember { RoomChatRepositoryImpl(applicationContext, database) }
            val authRepository = remember { AuthRepository(applicationContext) }

            val chatViewModel = remember { ChatViewModel(repository) }
            val settingsViewModel = remember { SettingsViewModel(repository) }
            val authViewModel = remember { AuthViewModel(authRepository) }

            val currentUser by authRepository.currentUser.collectAsState()
            androidx.compose.runtime.LaunchedEffect(currentUser) {
                val uid = currentUser?.uid ?: "default_user"
                repository.setCurrentUserId(uid)
            }

            val settings by settingsViewModel.settings.collectAsState()

            val isDark = when (settings.themeMode) {
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            ZypoAiTheme(darkTheme = isDark) {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    ZypoNavigation(
                        chatViewModel = chatViewModel,
                        settingsViewModel = settingsViewModel,
                        authViewModel = authViewModel
                    )
                }
            }
        }
    }
}
