package com.example.ui.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.ConversationHistoryScreen
import com.example.ui.screens.DiagnosticsScreen
import com.example.ui.screens.PermissionStatusScreen
import com.example.ui.screens.PrivacyScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SetupWizardScreen
import com.example.ui.viewmodel.AssistantViewModel

object Destinations {
    const val CHAT = "chat"
    const val HISTORY = "history"
    const val SETUP = "setup"
    const val SETTINGS = "settings"
    const val PERMISSIONS = "permissions"
    const val PRIVACY = "privacy"
    const val DIAGNOSTICS = "diagnostics"
}

@Composable
fun SalimAppNavHost(
    viewModel: AssistantViewModel,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    val startDestination = if (viewModel.prefsManager.isSetupCompleted || viewModel.prefsManager.hasGroqApiKey()) {
        Destinations.CHAT
    } else {
        Destinations.SETUP
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        enterTransition = { fadeIn() },
        exitTransition = { fadeOut() }
    ) {
        composable(Destinations.CHAT) {
            ChatScreen(
                viewModel = viewModel,
                onNavigateToSettings = { navController.navigate(Destinations.SETTINGS) },
                onNavigateToHistory = { navController.navigate(Destinations.HISTORY) },
                onNavigateToSetup = { navController.navigate(Destinations.SETUP) },
                onNavigateToDiagnostics = { navController.navigate(Destinations.DIAGNOSTICS) }
            )
        }

        composable(Destinations.HISTORY) {
            ConversationHistoryScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Destinations.SETUP) {
            SetupWizardScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onFinishSetup = {
                    navController.navigate(Destinations.CHAT) {
                        popUpTo(Destinations.SETUP) { inclusive = true }
                    }
                }
            )
        }

        composable(Destinations.SETTINGS) {
            SettingsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPermissions = { navController.navigate(Destinations.PERMISSIONS) },
                onNavigateToSetup = { navController.navigate(Destinations.SETUP) },
                onNavigateToPrivacy = { navController.navigate(Destinations.PRIVACY) },
                onNavigateToDiagnostics = { navController.navigate(Destinations.DIAGNOSTICS) }
            )
        }

        composable(Destinations.PERMISSIONS) {
            PermissionStatusScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Destinations.PRIVACY) {
            PrivacyScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Destinations.DIAGNOSTICS) {
            DiagnosticsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
