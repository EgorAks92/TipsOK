package com.chaiok.pos.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.compose.rememberNavController
import com.chaiok.pos.data.di.AppContainer
import com.chaiok.pos.presentation.background.ProfileBackgroundScreen
import com.chaiok.pos.presentation.background.ProfileBackgroundViewModel
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
import com.chaiok.pos.presentation.settings.SettingsRoute
import com.chaiok.pos.presentation.settings.SettingsViewModel
import com.chaiok.pos.presentation.status.StatusScreen
import com.chaiok.pos.presentation.status.StatusViewModel
import com.chaiok.pos.presentation.tips.TipsScreen
import com.chaiok.pos.presentation.tips.TipsViewModel
import com.chaiok.pos.presentation.tipselection.TipSelectionScreen
import com.chaiok.pos.presentation.tipselection.TipSelectionViewModel
import androidx.navigation.NavHostController

@Composable
fun ChaiOkNavHost(container: AppContainer) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.Login
    ) {
        composable(Routes.Login) {
            val vm: LoginViewModel = viewModel(
                factory = SimpleFactory {
                    LoginViewModel(
                        container.loginWithPinUseCase,
                        container::refreshTipRangeAfterLogin
                    )
                }
            )

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

            LoginScreen(
                state = state,
                onDigit = vm::onDigitPressed,
                onDelete = vm::onDeletePressed,
                onLogin = vm::onLoginPressed
            )
        }

        composable(Routes.Home) {
            val vm: HomeViewModel = viewModel(
                factory = SimpleFactory {
                    HomeViewModel(
                        container.observeProfileUseCase,
                        container.observeSettingsUseCase,
                        container.logoutUseCase
                    )
                }
            )

            val state by vm.uiState.collectAsStateWithLifecycle()

            LaunchedEffect(Unit) {
                vm.oneTimeEvents.collect { event ->
                    when (event) {
                        is HomeEvent.NavigateToLogin -> navController.navigate(Routes.Login) {
                            popUpTo(Routes.Home) { inclusive = true }
                        }
                        is HomeEvent.NavigateToTipSelection -> navController.navigate(Routes.tipSelection(event.billAmount))
                    }
                }
            }

            HomeScreen(
                state = state,
                onLogout = vm::logout,
                onOpenSettings = { navController.navigateSingleTopTo(Routes.Settings) },
                onDigit = vm::onAmountDigitPressed,
                onBackspace = vm::onAmountDeletePressed,
                onConfirm = vm::onConfirmAmount,
                onSnackbarShown = vm::onSnackbarShown,
                onBindCard = {
                    vm.onCardBindingStarted()
                    navController.navigate(Routes.CardBinding)
                },
                onDismissBindDialog = vm::dismissLinkCardDialog
            )
        }

        composable(Routes.Settings) {
            val vm: SettingsViewModel = viewModel(
                factory = SimpleFactory {
                    SettingsViewModel(container.observeProfileUseCase)
                }
            )

            SettingsRoute(
                viewModel = vm,
                onBack = {
                    val popped = navController.popBackStack(
                        route = Routes.Home,
                        inclusive = false
                    )

                    if (!popped) {
                        navController.navigateSingleTopTo(Routes.Home)
                    }
                },
                onCardBinding = { navController.navigate(Routes.CardBinding) },
                onStatus = { navController.navigate(Routes.Status) },
                onTips = { navController.navigate(Routes.Tips) },
                onIntegration = { navController.navigate(Routes.Integration) },
                onBackground = { navController.navigate(Routes.Background) }
            )
        }

        composable(Routes.CardBinding) {
            val vm: CardBindingViewModel = viewModel(
                factory = SimpleFactory {
                    CardBindingViewModel(
                        container.readCardUseCase,
                        container.linkCardUseCase
                    )
                }
            )

            val state by vm.uiState.collectAsStateWithLifecycle()

            CardBindingScreen(
                state = state,
                onBack = { navController.popBackStack() },
                onReadCard = vm::readCard
            )
        }

        composable(Routes.Status) {
            val vm: StatusViewModel = viewModel(
                factory = SimpleFactory {
                    StatusViewModel(
                        container.updateStatusUseCase,
                        container.observeCurrentStatusUseCase
                    )
                }
            )

            val state by vm.uiState.collectAsStateWithLifecycle()

            StatusScreen(
                state,
                onBack = { navController.popBackStack() },
                onStatusChanged = vm::onStatusChanged,
                onSave = vm::saveStatus
            )
        }

        composable(Routes.Tips) {
            val vm: TipsViewModel = viewModel(
                factory = SimpleFactory {
                    TipsViewModel(container.getTipsUseCase)
                }
            )

            val state by vm.uiState.collectAsStateWithLifecycle()

            TipsScreen(
                state = state,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.Background) {
            val vm: ProfileBackgroundViewModel = viewModel(
                factory = SimpleFactory {
                    ProfileBackgroundViewModel(
                        container.observeSettingsUseCase,
                        container.updateTileBackgroundUseCase
                    )
                }
            )

            val state by vm.uiState.collectAsStateWithLifecycle()

            ProfileBackgroundScreen(
                state = state,
                onBack = { navController.popBackStack() },
                onSelect = vm::setBackground
            )
        }



        composable(
            route = Routes.TipSelectionWithArg,
            arguments = listOf(navArgument("billAmountRub") { type = NavType.IntType })
        ) { backStack ->
            val billAmount = backStack.arguments?.getInt("billAmountRub")?.toDouble() ?: 0.0
            val vm: TipSelectionViewModel = viewModel(factory = SimpleFactory {
                TipSelectionViewModel(
                    billAmount = billAmount,
                    getTransactionRangeUseCase = container.getTransactionRangeUseCase,
                    observeProfileUseCase = container.observeProfileUseCase
                )
            })
            val state by vm.uiState.collectAsStateWithLifecycle()
            TipSelectionScreen(
                state = state,
                onBack = { navController.popBackStack() },
                onPreset = vm::selectPreset,
                onCustomStart = vm::openCustomDialog,
                onCustomSet = vm::applyCustom,
                onDismissCustom = vm::dismissCustomDialog,
                onPay = { },
                onSnackbarShown = vm::onMessageShown
            )
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

private fun NavHostController.navigateSingleTopTo(route: String) {
    navigate(route) {
        launchSingleTop = true
    }
}
