package com.policyregistration.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.policyregistration.app.ui.navigation.Screen
import com.policyregistration.app.ui.screens.*
import com.policyregistration.app.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PolicyRegistrationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(viewModel)
                }
            }
        }
    }
}

private val RoyalBlue = Color(0xFF1E40AF)
private val LightBlue = Color(0xFF3B82F6)
private val DarkBlue = Color(0xFF1E3A8A)

@Composable
fun PolicyRegistrationTheme(content: @Composable () -> Unit) {
    val colorScheme = lightColorScheme(
        primary = RoyalBlue,
        secondary = LightBlue,
        tertiary = DarkBlue,
        background = Color(0xFFF8FAFC),
        surface = Color.White
    )
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

@Composable
fun AppNavigation(viewModel: MainViewModel) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route
    ) {
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                viewModel = viewModel,
                onNavigateToManual = {
                    viewModel.loadNextSerialNumber()
                    navController.navigate(Screen.RegistrationForm.route)
                },
                onNavigateToRecords = {
                    navController.navigate(Screen.RecordsList.route)
                },
                onNavigateToExport = {
                    navController.navigate(Screen.ExportExcel.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.CampaignSettings.route)
                },
                onNavigateToBackup = {
                    navController.navigate(Screen.BackupRestore.route)
                }
            )
        }

        composable(Screen.RegistrationForm.route) {
            RegistrationFormScreen(
                viewModel = viewModel,
                onNavigateToScanner = {
                    navController.navigate(Screen.CameraScan.route)
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.CameraScan.route) {
            CameraScanScreen(
                viewModel = viewModel,
                onNavigateToOcrReview = {
                    navController.navigate(Screen.OcrReview.route)
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.OcrReview.route) {
            OcrReviewScreen(
                viewModel = viewModel,
                onAcceptAndContinue = {
                    navController.popBackStack(Screen.RegistrationForm.route, false)
                },
                onRetake = {
                    navController.popBackStack(Screen.CameraScan.route, false)
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.RecordsList.route) {
            RecordsListScreen(
                viewModel = viewModel,
                onNavigateToEdit = {
                    navController.navigate(Screen.RegistrationForm.route)
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.ExportExcel.route) {
            ExportExcelScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.CampaignSettings.route) {
            CampaignSettingsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.BackupRestore.route) {
            BackupRestoreScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
