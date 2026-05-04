package com.chaiok.pos.presentation.navigation

import android.content.ActivityNotFoundException
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.chaiok.pos.data.di.AppContainer
import com.chaiok.pos.presentation.background.ProfileBackgroundScreen
import com.chaiok.pos.presentation.background.ProfileBackgroundViewModel
import com.chaiok.pos.presentation.home.HomeEvent
import com.chaiok.pos.presentation.home.HomeScreen
import com.chaiok.pos.presentation.home.HomeViewModel
import com.chaiok.pos.presentation.login.LoginEvent
import com.chaiok.pos.presentation.login.LoginScreen
import com.chaiok.pos.presentation.login.LoginViewModel
import com.chaiok.pos.presentation.payment.SmartSkyPaymentIntentFactory
import com.chaiok.pos.presentation.settings.SettingsRoute
import com.chaiok.pos.presentation.settings.SettingsViewModel
import com.chaiok.pos.presentation.status.StatusScreen
import com.chaiok.pos.presentation.status.StatusViewModel
import com.chaiok.pos.presentation.tips.TipsScreen
import com.chaiok.pos.presentation.tips.TipsViewModel
import com.chaiok.pos.presentation.tipselection.TipSelectionScreen
import com.chaiok.pos.presentation.tipselection.TipSelectionViewModel
import java.math.BigDecimal
import java.math.RoundingMode

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
                        is HomeEvent.NavigateToLogin -> {
                            navController.navigate(Routes.Login) {
                                popUpTo(Routes.Home) { inclusive = true }
                            }
                        }

                        is HomeEvent.NavigateToTipSelection -> {
                            navController.navigate(Routes.tipSelection(event.billAmount))
                        }
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
                onSnackbarShown = vm::onSnackbarShown
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
                onStatus = { navController.navigate(Routes.Status) },
                onTips = { navController.navigate(Routes.Tips) },
                onBackground = { navController.navigate(Routes.Background) }
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
                state = state,
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
            val billAmount = backStack.arguments
                ?.getInt("billAmountRub")
                ?.toDouble()
                ?: 0.0

            val vm: TipSelectionViewModel = viewModel(
                factory = SimpleFactory {
                    TipSelectionViewModel(
                        billAmount = billAmount,
                        getTransactionRangeUseCase = container.getTransactionRangeUseCase,
                        observeProfileUseCase = container.observeProfileUseCase,
                        addReviewUseCase = container.addReviewUseCase,
                        sessionRepository = container.sessionRepository
                    )
                }
            )

            val state by vm.uiState.collectAsStateWithLifecycle()

            val paymentLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartActivityForResult()
            ) { result ->
                Log.i(
                    PAYMENT_TAG,
                    "payment activity resultCode=${result.resultCode}"
                )

                Log.i(
                    PAYMENT_TAG,
                    "payment activity data extras keys=${result.data?.extras?.keySet()?.joinToString()}"
                )

                val paymentResult = SmartSkyPaymentIntentFactory.mapPaymentActivityResult(
                    resultCode = result.resultCode,
                    data = result.data
                )

                vm.handlePaymentResult(paymentResult)
            }

            TipSelectionScreen(
                state = state,
                onBack = { navController.popBackStack() },
                onPreset = vm::selectPreset,
                onCustomStart = vm::openCustomDialog,
                onCustomSet = vm::applyCustom,
                onDismissCustom = vm::dismissCustomDialog,
                onPay = {
                    if (!vm.startExternalPayment()) {
                        return@TipSelectionScreen
                    }

                    try {
                        val amount = BigDecimal
                            .valueOf(state.totalAmount)
                            .setScale(2, RoundingMode.HALF_UP)

                        val intent = SmartSkyPaymentIntentFactory.createPaymentIntent(
                            amount = amount,
                            waiterId = state.waiterId,
                            terminalId = state.terminalId,
                            tipAmount = state.selectedTipAmount,
                            serviceFee = state.serviceFeeAmount,
                            feesCovered = state.isServiceFeeEnabled
                        )

                        Log.i(PAYMENT_TAG, "launching SmartSky payment activity")

                        paymentLauncher.launch(intent)
                    } catch (error: ActivityNotFoundException) {
                        Log.e(PAYMENT_TAG, "launch error ActivityNotFound", error)
                        vm.handlePaymentLaunchError("Платежное приложение не найдено")
                    } catch (error: SecurityException) {
                        Log.e(PAYMENT_TAG, "launch error Security", error)
                        vm.handlePaymentLaunchError("Нет доступа к платежному приложению")
                    } catch (error: Exception) {
                        Log.e(PAYMENT_TAG, "launch error Exception", error)
                        vm.handlePaymentLaunchError(
                            error.message ?: "Не удалось запустить оплату"
                        )
                    }
                },
                onSnackbarShown = vm::onMessageShown,
                onDone = {
                    val popped = navController.popBackStack(
                        route = Routes.Home,
                        inclusive = false
                    )

                    if (!popped) {
                        navController.navigateSingleTopTo(Routes.Home)
                    }
                },
                onRetry = vm::resetPaymentState,
                onServiceFeeToggle = vm::toggleServiceFee,
                onKitchenEvaluation = vm::selectKitchenEvaluation,
                onServiceEvaluation = vm::selectServiceEvaluation
            )
        }
    }
}

private fun NavHostController.navigateSingleTopTo(route: String) {
    navigate(route) {
        launchSingleTop = true
    }
}

private const val PAYMENT_TAG = "TipsPaymentFlow"