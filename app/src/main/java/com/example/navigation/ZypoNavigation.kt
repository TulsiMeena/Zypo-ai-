package com.example.navigation

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.model.AuthState
import com.example.ui.screens.AboutScreen
import com.example.ui.screens.AuthLandingScreen
import com.example.ui.screens.BookmarksScreen
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.ChatStatisticsScreen
import com.example.ui.screens.CreateAccountScreen
import com.example.ui.screens.CustomInstructionsScreen
import com.example.ui.screens.ApiSettingsScreen
import com.example.ui.screens.DeveloperDiagnosticsScreen
import com.example.ui.screens.EmailVerificationScreen
import com.example.ui.screens.ForgotPasswordScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.MemoryScreen
import com.example.ui.screens.PremiumScreen
import com.example.ui.screens.PrivacyDashboardScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.VoiceScreen
import com.example.ui.viewmodel.ApiSettingsViewModel
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.ChatViewModel
import com.example.ui.viewmodel.SettingsViewModel

object ZypoRoutes {
    const val SPLASH = "splash"
    const val AUTH_LANDING = "auth_landing"
    const val CREATE_ACCOUNT = "create_account"
    const val LOGIN = "login"
    const val FORGOT_PASSWORD = "forgot_password"
    const val VERIFY_EMAIL = "verify_email"

    const val CHAT = "chat"
    const val CHAT_DETAIL = "chat/{chatId}"
    const val SEARCH = "search"
    const val SETTINGS = "settings"
    const val PROFILE = "profile"
    const val PREMIUM = "premium"
    const val ABOUT = "about"
    const val VOICE = "voice"
    const val MEMORY = "memory"
    const val CUSTOM_INSTRUCTIONS = "custom_instructions"
    const val BOOKMARKS = "bookmarks"
    const val PRIVACY = "privacy"
    const val STATISTICS = "statistics"
    const val API_SETTINGS = "api_settings"
    const val DEVELOPER_DIAGNOSTICS = "developer_diagnostics"

    fun chatDetail(chatId: String) = "chat/$chatId"
}

// Backwards compatibility alias
typealias NovaRoutes = ZypoRoutes

@Composable
fun ZypoNavigation(
    chatViewModel: ChatViewModel,
    settingsViewModel: SettingsViewModel,
    authViewModel: AuthViewModel,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current
    val currentUser by authViewModel.currentUser.collectAsState()
    val authState by authViewModel.authState.collectAsState()

    // Listen for toast messages from auth viewmodel
    LaunchedEffect(Unit) {
        authViewModel.toastEvent.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        chatViewModel.toastEvent.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        settingsViewModel.toastEvent.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    val apiSettingsViewModel: ApiSettingsViewModel = androidx.lifecycle.viewmodel.compose.viewModel()

    // Auto-navigate when auth state changes
    LaunchedEffect(authState, currentUser) {
        if (authState == AuthState.SUCCESS && currentUser != null) {
            navController.navigate(ZypoRoutes.CHAT) {
                popUpTo(ZypoRoutes.SPLASH) { inclusive = true }
                popUpTo(ZypoRoutes.AUTH_LANDING) { inclusive = true }
            }
        } else if (authState == AuthState.VERIFICATION_REQUIRED) {
            navController.navigate(ZypoRoutes.VERIFY_EMAIL)
        }
    }

    NavHost(
        navController = navController,
        startDestination = ZypoRoutes.SPLASH,
        modifier = modifier
    ) {
        // Splash Screen
        composable(ZypoRoutes.SPLASH) {
            SplashScreen(
                onSplashComplete = {
                    if (currentUser != null) {
                        navController.navigate(ZypoRoutes.CHAT) {
                            popUpTo(ZypoRoutes.SPLASH) { inclusive = true }
                        }
                    } else {
                        navController.navigate(ZypoRoutes.AUTH_LANDING) {
                            popUpTo(ZypoRoutes.SPLASH) { inclusive = true }
                        }
                    }
                }
            )
        }

        // Auth Landing Screen
        composable(ZypoRoutes.AUTH_LANDING) {
            AuthLandingScreen(
                onContinueWithGoogle = {
                    // Trigger Google Auth fallback or credential flow
                    authViewModel.signInWithGoogleToken("google_id_token_demo")
                },
                onContinueWithEmail = { navController.navigate(ZypoRoutes.CREATE_ACCOUNT) },
                onLoginClick = { navController.navigate(ZypoRoutes.LOGIN) },
                onContinueAsGuest = { authViewModel.loginAsGuest() }
            )
        }

        // Create Account Screen
        composable(ZypoRoutes.CREATE_ACCOUNT) {
            CreateAccountScreen(
                viewModel = authViewModel,
                onBackClick = { navController.popBackStack() },
                onLoginClick = { navController.navigate(ZypoRoutes.LOGIN) },
                onContinueWithGoogle = { authViewModel.signInWithGoogleToken("google_id_token_demo") },
                onContinueAsGuest = { authViewModel.loginAsGuest() }
            )
        }

        // Login Screen
        composable(ZypoRoutes.LOGIN) {
            LoginScreen(
                viewModel = authViewModel,
                onBackClick = { navController.popBackStack() },
                onCreateAccountClick = { navController.navigate(ZypoRoutes.CREATE_ACCOUNT) },
                onForgotPasswordClick = { navController.navigate(ZypoRoutes.FORGOT_PASSWORD) },
                onContinueWithGoogle = { authViewModel.signInWithGoogleToken("google_id_token_demo") },
                onContinueAsGuest = { authViewModel.loginAsGuest() }
            )
        }

        // Forgot Password Screen
        composable(ZypoRoutes.FORGOT_PASSWORD) {
            ForgotPasswordScreen(
                viewModel = authViewModel,
                onBackClick = { navController.popBackStack() }
            )
        }

        // Email Verification Screen
        composable(ZypoRoutes.VERIFY_EMAIL) {
            EmailVerificationScreen(
                viewModel = authViewModel,
                onChangeEmailClick = {
                    authViewModel.logout()
                    navController.navigate(ZypoRoutes.LOGIN) {
                        popUpTo(ZypoRoutes.VERIFY_EMAIL) { inclusive = true }
                    }
                }
            )
        }

        // Main Chat Screen
        composable(ZypoRoutes.CHAT) {
            ChatScreen(
                viewModel = chatViewModel,
                onNavigateToSearch = { navController.navigate(ZypoRoutes.SEARCH) },
                onNavigateToProfile = { navController.navigate(ZypoRoutes.PROFILE) },
                onNavigateToSettings = { navController.navigate(ZypoRoutes.SETTINGS) },
                onNavigateToPremium = { navController.navigate(ZypoRoutes.PREMIUM) },
                onNavigateToVoice = { navController.navigate(ZypoRoutes.VOICE) }
            )
        }

        // Chat Detail Screen
        composable(
            route = ZypoRoutes.CHAT_DETAIL,
            arguments = listOf(navArgument("chatId") { type = NavType.StringType })
        ) { backStackEntry ->
            val chatId = backStackEntry.arguments?.getString("chatId")
            if (chatId != null) {
                chatViewModel.selectChat(chatId)
            }
            ChatScreen(
                viewModel = chatViewModel,
                onNavigateToSearch = { navController.navigate(ZypoRoutes.SEARCH) },
                onNavigateToProfile = { navController.navigate(ZypoRoutes.PROFILE) },
                onNavigateToSettings = { navController.navigate(ZypoRoutes.SETTINGS) },
                onNavigateToPremium = { navController.navigate(ZypoRoutes.PREMIUM) },
                onNavigateToVoice = { navController.navigate(ZypoRoutes.VOICE) }
            )
        }

        // Search Screen
        composable(ZypoRoutes.SEARCH) {
            SearchScreen(
                viewModel = chatViewModel,
                onBackClick = { navController.popBackStack() },
                onSelectChat = { id ->
                    chatViewModel.selectChat(id)
                    navController.popBackStack()
                }
            )
        }

        // Settings Screen
        composable(ZypoRoutes.SETTINGS) {
            SettingsScreen(
                viewModel = settingsViewModel,
                onBackClick = { navController.popBackStack() },
                onAboutClick = { navController.navigate(ZypoRoutes.ABOUT) },
                onNavigateToMemory = { navController.navigate(ZypoRoutes.MEMORY) },
                onNavigateToCustomInstructions = { navController.navigate(ZypoRoutes.CUSTOM_INSTRUCTIONS) },
                onNavigateToBookmarks = { navController.navigate(ZypoRoutes.BOOKMARKS) },
                onNavigateToPrivacy = { navController.navigate(ZypoRoutes.PRIVACY) },
                onNavigateToStatistics = { navController.navigate(ZypoRoutes.STATISTICS) },
                onNavigateToApiSettings = { navController.navigate(ZypoRoutes.API_SETTINGS) },
                onNavigateToDiagnostics = { navController.navigate(ZypoRoutes.DEVELOPER_DIAGNOSTICS) }
            )
        }

        // API Settings Screen
        composable(ZypoRoutes.API_SETTINGS) {
            ApiSettingsScreen(
                viewModel = apiSettingsViewModel,
                onBackClick = { navController.popBackStack() }
            )
        }

        // Developer Diagnostics Screen
        composable(ZypoRoutes.DEVELOPER_DIAGNOSTICS) {
            DeveloperDiagnosticsScreen(
                apiViewModel = apiSettingsViewModel,
                settingsViewModel = settingsViewModel,
                onBackClick = { navController.popBackStack() }
            )
        }

        // Memory Screen
        composable(ZypoRoutes.MEMORY) {
            MemoryScreen(
                viewModel = settingsViewModel,
                onBackClick = { navController.popBackStack() }
            )
        }

        // Custom Instructions Screen
        composable(ZypoRoutes.CUSTOM_INSTRUCTIONS) {
            CustomInstructionsScreen(
                viewModel = settingsViewModel,
                onBackClick = { navController.popBackStack() }
            )
        }

        // Bookmarks Screen
        composable(ZypoRoutes.BOOKMARKS) {
            BookmarksScreen(
                viewModel = settingsViewModel,
                onBackClick = { navController.popBackStack() }
            )
        }

        // Privacy Dashboard Screen
        composable(ZypoRoutes.PRIVACY) {
            PrivacyDashboardScreen(
                viewModel = settingsViewModel,
                onBackClick = { navController.popBackStack() }
            )
        }

        // Chat Statistics Screen
        composable(ZypoRoutes.STATISTICS) {
            ChatStatisticsScreen(
                viewModel = settingsViewModel,
                onBackClick = { navController.popBackStack() }
            )
        }

        // Profile Screen
        composable(ZypoRoutes.PROFILE) {
            ProfileScreen(
                viewModel = chatViewModel,
                authViewModel = authViewModel,
                onBackClick = { navController.popBackStack() },
                onNavigateToSettings = { navController.navigate(ZypoRoutes.SETTINGS) },
                onNavigateToPremium = { navController.navigate(ZypoRoutes.PREMIUM) },
                onLogoutClick = {
                    authViewModel.logout()
                    navController.navigate(ZypoRoutes.AUTH_LANDING) {
                        popUpTo(ZypoRoutes.CHAT) { inclusive = true }
                    }
                }
            )
        }

        // Premium Screen
        composable(ZypoRoutes.PREMIUM) {
            PremiumScreen(
                viewModel = chatViewModel,
                onBackClick = { navController.popBackStack() }
            )
        }

        // About Screen
        composable(ZypoRoutes.ABOUT) {
            AboutScreen(
                viewModel = chatViewModel,
                onBackClick = { navController.popBackStack() }
            )
        }

        // Voice Assistant Screen
        composable(ZypoRoutes.VOICE) {
            VoiceScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

@Composable
fun NovaNavigation(
    chatViewModel: ChatViewModel,
    settingsViewModel: SettingsViewModel,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current
    val authRepository = remember(context) { com.example.data.repository.AuthRepository(context) }
    val authViewModel = remember(authRepository) { AuthViewModel(authRepository) }

    ZypoNavigation(
        chatViewModel = chatViewModel,
        settingsViewModel = settingsViewModel,
        authViewModel = authViewModel,
        modifier = modifier,
        navController = navController
    )
}
