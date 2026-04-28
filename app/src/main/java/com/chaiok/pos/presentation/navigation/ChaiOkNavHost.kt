package com.chaiok.pos.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.chaiok.pos.app.ChaiOkApp
import com.chaiok.pos.data.di.AppContainer
import com.chaiok.pos.presentation.cardbinding.CardBindingScreen
import com.chaiok.pos.presentation.cardbinding.CardBindingViewModel
import com.chaiok.pos.presentation.home.HomeEvent
import com.chaiok.pos.presentation.home.HomeScreen
import com.chaiok.pos.presentation.home.HomeViewModel
import com.chaiok.pos.presentation.integration.IntegrationScreen
import com.chaiok.pos.presentation.integration.IntegrationViewModel
import com.chaiok.pos.presentation.login.LoginEvent
import com.chaiok.pos.presentation.login.LoginScreen
import com.chaiok.pos.presentation.login.LoginViewModel
import com.chaiok.pos.presentation.settings.SettingsScreen
import com.chaiok.pos.presentation.status.StatusScreen
import com.chaiok.pos.presentation.status.StatusViewModel
import com.chaiok.pos.presentation.tips.TipsScreen
import com.chaiok.pos.presentation.tips.TipsViewModel

@Composable
fun ChaiOkNavHost(container: AppContainer) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Routes.Login) {
        composable(Routes.Login) {
            val vm: LoginViewModel = viewModel(factory = SimpleFactory { LoginViewModel(container.loginWithPinUseCase) })
            val state by vm.uiState.collectAsStateWithLifecycle()
            LaunchedEffect(Unit) {
                vm.oneTimeEvents.collect { event ->
                    if (event is LoginEvent.NavigateToHome) {
                        navController.navigate(Routes.Home) {
                            popUpTo(Routes.Login) { inclusive = true }
                        }
                    }
                }
            }
            LoginScreen(state, vm::onDigitPressed, vm::onDeletePressed, vm::onLoginPressed)
        }

        composable(Routes.Home) {
            val vm: HomeViewModel = viewModel(
                factory = SimpleFactory {
                    HomeViewModel(container.observeProfileUseCase, container.observeSettingsUseCase, container.logoutUseCase)
                }
            )
            val state by vm.uiState.collectAsStateWithLifecycle()
            LaunchedEffect(Unit) {
                vm.oneTimeEvents.collect { event ->
                    if (event is HomeEvent.NavigateToLogin) {
                        navController.navigate(Routes.Login) {
                            popUpTo(Routes.Home) { inclusive = true }
                        }
                    }
                }
            }
            HomeScreen(
                state = state,
                onLogout = vm::logout,
                onOpenSettings = { navController.navigate(Routes.Settings) },
                onDigit = vm::onAmountDigitPressed,
                onBackspace = vm::onAmountDeletePressed,
                onConfirm = vm::onConfirmAmount,
                onSnackbarShown = vm::onSnackbarShown,
                onBindCard = {
                    vm.dismissLinkCardDialog()
                    navController.navigate(Routes.CardBinding)
                },
                onDismissBindDialog = vm::dismissLinkCardDialog
            )
        }

        composable(Routes.Settings) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onCardBinding = { navController.navigate(Routes.CardBinding) },
                onStatus = { navController.navigate(Routes.Status) },
                onTips = { navController.navigate(Routes.Tips) },
                onIntegration = { navController.navigate(Routes.Integration) }
            )
        }

        composable(Routes.CardBinding) {
            val vm: CardBindingViewModel = viewModel(
                factory = SimpleFactory { CardBindingViewModel(container.cardReaderService, container.linkCardUseCase) }
            )
            val state by vm.uiState.collectAsStateWithLifecycle()
            CardBindingScreen(state = state, onBack = { navController.popBackStack() }, onReadCard = vm::readCard)
        }

        composable(Routes.Status) {
            val vm: StatusViewModel = viewModel(factory = SimpleFactory { StatusViewModel(container.updateStatusUseCase) })
            val state by vm.uiState.collectAsStateWithLifecycle()
            StatusScreen(state, onBack = { navController.popBackStack() }, onStatusChanged = vm::onStatusChanged, onSave = vm::saveStatus)
        }

        composable(Routes.Tips) {
            val vm: TipsViewModel = viewModel(factory = SimpleFactory { TipsViewModel(container.getTipsUseCase) })
            val state by vm.uiState.collectAsStateWithLifecycle()
            TipsScreen(state = state, onBack = { navController.popBackStack() })
        }

        composable(Routes.Integration) {
            val vm: IntegrationViewModel = viewModel(
                factory = SimpleFactory {
                    IntegrationViewModel(
                        container.observeSettingsUseCase,
                        container.updateIntegrationModeUseCase,
                        container.updateTableModeUseCase
                    )
                }
            )
            val state by vm.uiState.collectAsStateWithLifecycle()
            IntegrationScreen(
                state = state,
                onBack = { navController.popBackStack() },
                onToggleIntegration = vm::toggleIntegration,
                onToggleTableMode = vm::toggleTableMode
            )
        }
    }
}
