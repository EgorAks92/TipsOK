package com.chaiok.pos.presentation.navigation

import android.os.SystemClock
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.chaiok.pos.data.di.AppContainer
import com.chaiok.pos.domain.model.PaymentResult
import com.chaiok.pos.domain.model.PosPaymentRequest
import com.chaiok.pos.presentation.background.ProfileBackgroundScreen
import com.chaiok.pos.presentation.background.ProfileBackgroundViewModel
import com.chaiok.pos.presentation.adaptive.ChaiOkDeviceClass
import com.chaiok.pos.presentation.adaptive.rememberChaiOkDeviceClass
import com.chaiok.pos.presentation.cardpresenting.CardPresentingOneTimeEvent
import com.chaiok.pos.presentation.cardpresenting.CardPresentingScreen
import com.chaiok.pos.presentation.cardpresenting.CardPresentingViewModel
import com.chaiok.pos.presentation.home.HomeEvent
import com.chaiok.pos.presentation.home.HomeScreen
import com.chaiok.pos.presentation.home.HomeViewModel
import com.chaiok.pos.presentation.login.LoginEvent
import com.chaiok.pos.presentation.login.LoginScreen
import com.chaiok.pos.presentation.login.LoginViewModel
import com.chaiok.pos.presentation.pc.PcCommandIdleEvent
import com.chaiok.pos.presentation.pc.PcCommandIdleScreen
import com.chaiok.pos.presentation.pc.PcCommandIdleViewModel
import com.chaiok.pos.presentation.pc.PcIdleImagesRoute
import com.chaiok.pos.presentation.pc.PcIdleImagesViewModel
import com.chaiok.pos.presentation.settings.SettingsRoute
import com.chaiok.pos.presentation.settings.SettingsViewModel
import com.chaiok.pos.presentation.status.StatusScreen
import com.chaiok.pos.presentation.status.StatusViewModel
import com.chaiok.pos.presentation.tips.TipsScreen
import com.chaiok.pos.presentation.tips.TipsViewModel
import com.chaiok.pos.presentation.tipselection.TipSelectionScreen
import com.chaiok.pos.presentation.tipselection.TipSelectionUiState
import com.chaiok.pos.presentation.tipselection.TipSelectionViewModel
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.abs
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

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
            val loginEvents: Flow<LoginEvent> = vm.oneTimeEvents

            LaunchedEffect(loginEvents) {
                loginEvents.collect(
                    object : FlowCollector<LoginEvent> {
                        override suspend fun emit(value: LoginEvent) {
                            when (value) {
                                is LoginEvent.NavigateToHome -> {
                                    val goPc = container.observeSettingsUseCase()
                                        .first()
                                        .pcUsbModeEnabled

                                    navController.navigate(
                                        if (goPc) Routes.PcCommandIdle else Routes.Home
                                    ) {
                                        popUpTo(Routes.Login) {
                                            inclusive = true
                                        }
                                    }
                                }
                            }
                        }
                    }
                )
            }

            LoginScreen(
                state = state,
                onDigit = vm::onDigitPressed,
                onDelete = vm::onDeletePressed,
                onLogin = vm::onLoginPressed
            )
        }

        composable(Routes.Home) { backStack ->
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
            val homeEvents: Flow<HomeEvent> = vm.oneTimeEvents

            LaunchedEffect(backStack) {
                val shouldClearAmount = backStack.savedStateHandle.remove<Boolean>(
                    CLEAR_HOME_AMOUNT_KEY
                ) == true

                if (shouldClearAmount) {
                    Log.i(PAYMENT_TAG, "Home amount reset consumed")
                    vm.clearAmountInput()
                }
            }

            LaunchedEffect(homeEvents) {
                homeEvents.collect(
                    object : FlowCollector<HomeEvent> {
                        override suspend fun emit(value: HomeEvent) {
                            when (value) {
                                is HomeEvent.NavigateToLogin -> {
                                    navController.navigate(Routes.Login) {
                                        popUpTo(Routes.Home) {
                                            inclusive = true
                                        }
                                    }
                                }

                                is HomeEvent.NavigateToTipSelection -> {
                                    navController.navigate(
                                        Routes.tipSelectionFromNormal(value.billAmount)
                                    )
                                }
                            }
                        }
                    }
                )
            }

            HomeScreen(
                state = state,
                onLogout = vm::logout,
                onOpenSettings = {
                    navController.navigateSingleTopTo(Routes.Settings)
                },
                onDigit = vm::onAmountDigitPressed,
                onBackspace = vm::onAmountDeletePressed,
                onConfirm = vm::onConfirmAmount,
                onSnackbarShown = vm::onSnackbarShown
            )
        }

        composable(Routes.Settings) {
            val vm: SettingsViewModel = viewModel(
                factory = SimpleFactory {
                    SettingsViewModel(
                        observeProfileUseCase = container.observeProfileUseCase,
                        observeSettingsUseCase = container.observeSettingsUseCase,
                        updatePcUsbModeUseCase = container.updatePcUsbModeUseCase
                    )
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
                onStatus = {
                    navController.navigate(Routes.Status)
                },
                onTips = {
                    navController.navigate(Routes.Tips)
                },
                onBackground = {
                    navController.navigate(Routes.Background)
                },
                onPcIdleImages = {
                    navController.navigate(Routes.PcIdleImages)
                }
            )
        }

        composable(Routes.SettingsFromPc) {
            val vm: SettingsViewModel = viewModel(
                factory = SimpleFactory {
                    SettingsViewModel(
                        observeProfileUseCase = container.observeProfileUseCase,
                        observeSettingsUseCase = container.observeSettingsUseCase,
                        updatePcUsbModeUseCase = container.updatePcUsbModeUseCase
                    )
                }
            )

            val settingsState by vm.uiState.collectAsStateWithLifecycle()

            SettingsRoute(
                viewModel = vm,
                onBack = {
                    if (settingsState.pcUsbModeEnabled) {
                        val popped = navController.popBackStack(
                            route = Routes.PcCommandIdle,
                            inclusive = false
                        )

                        if (!popped) {
                            navController.navigateSingleTopTo(Routes.PcCommandIdle)
                        }
                    } else {
                        navController.navigate(Routes.Home) {
                            popUpTo(Routes.PcCommandIdle) {
                                inclusive = true
                            }
                            launchSingleTop = true
                        }
                    }
                },
                onStatus = {
                    navController.navigate(Routes.Status)
                },
                onTips = {
                    navController.navigate(Routes.Tips)
                },
                onBackground = {
                    navController.navigate(Routes.Background)
                },
                onPcIdleImages = {
                    navController.navigate(Routes.PcIdleImages)
                }
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
                onBack = {
                    navController.popBackStack()
                },
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
                onBack = {
                    navController.popBackStack()
                }
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
                onBack = {
                    navController.popBackStack()
                },
                onSelect = vm::setBackground
            )
        }

        composable(
            route = Routes.TipSelectionWithArg,
            arguments = listOf(
                navArgument("billAmountKopecks") {
                    type = NavType.LongType
                },
                navArgument("source") {
                    type = NavType.StringType
                    defaultValue = "normal"
                },
                navArgument("commandId") {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument("orderId") {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { backStack ->
            val billAmount = backStack.arguments
                ?.getLong("billAmountKopecks")
                ?.toDouble()
                ?.div(100.0)
                ?: 0.0

            val isPcUsbSource = backStack.arguments?.getString("source") == "pc_usb"
            val deviceClass = rememberChaiOkDeviceClass()
            val paymentScope = rememberCoroutineScope()

            val vm: TipSelectionViewModel = viewModel(
                factory = SimpleFactory {
                    TipSelectionViewModel(
                        billAmount = billAmount,
                        getTransactionRangeUseCase = container.getTransactionRangeUseCase,
                        observeProfileUseCase = container.observeProfileUseCase,
                        addReviewUseCase = container.addReviewUseCase,
                        sessionRepository = container.sessionRepository,
                        observeSettingsUseCase = container.observeSettingsUseCase
                    )
                }
            )

            val state by vm.uiState.collectAsStateWithLifecycle()

            LaunchedEffect(backStack) {
                when (val consumedResult = backStack.savedStateHandle.consumePaymentResult()) {
                    is ConsumedPaymentResult.Finished -> {
                        Log.i(
                            PAYMENT_TAG,
                            "TipSelection consumed payment result=${consumedResult.result.javaClass.simpleName}"
                        )

                        when (val result = consumedResult.result) {
                            is PaymentResult.Approved -> {
                                val shouldNavigateNow = vm.handleApprovedPayment(
                                    result = result,
                                    requirePostPaymentReview = deviceClass == ChaiOkDeviceClass.SquareCompact
                                )

                                backStack.savedStateHandle.remove<PosPaymentRequest>(
                                    PENDING_PAYMENT_REQUEST_KEY
                                )

                                if (!shouldNavigateNow) return@LaunchedEffect

                                navController.navigateAfterTipPayment(isPcUsbSource)
                            }

                            is PaymentResult.Declined,
                            is PaymentResult.Error -> {
                                vm.handleFailedPaymentResult(result)
                            }
                        }
                    }

                    ConsumedPaymentResult.Cancelled -> {
                        Log.i(PAYMENT_TAG, "TipSelection consumed payment cancelled")

                        backStack.savedStateHandle.remove<PosPaymentRequest>(
                            PENDING_PAYMENT_REQUEST_KEY
                        )

                        vm.resetPaymentState()

                        if (isPcUsbSource) {
                            val popped = navController.popBackStack(
                                route = Routes.PcCommandIdle,
                                inclusive = false
                            )

                            if (!popped) {
                                navController.navigateSingleTopTo(Routes.PcCommandIdle)
                            }
                        }
                    }

                    null -> Unit
                }
            }

            TipSelectionScreen(
                state = state,
                onBack = {
                    if (isPcUsbSource) {
                        val popped = navController.popBackStack(
                            route = Routes.PcCommandIdle,
                            inclusive = false
                        )

                        if (!popped) {
                            navController.navigateSingleTopTo(Routes.PcCommandIdle)
                        }
                    } else {
                        navController.popBackStack()
                    }
                },
                onPreset = vm::selectPreset,
                onCustomStart = vm::openCustomDialog,
                onCustomSet = vm::applyCustom,
                onDismissCustom = vm::dismissCustomDialog,
                onPay = {
                    val paymentRequest = buildPaymentRequest(state)

                    if (isPcUsbSource) {
                        paymentScope.launch {
                            startPcUsbPaymentFlow(
                                container = container,
                                navController = navController,
                                viewModel = vm,
                                paymentRequest = paymentRequest,
                                requireReviewBeforePayment = deviceClass != ChaiOkDeviceClass.SquareCompact
                            )
                        }
                    } else {
                        startPaymentFlow(
                            navController = navController,
                            viewModel = vm,
                            paymentRequest = paymentRequest,
                            requireReviewBeforePayment = deviceClass != ChaiOkDeviceClass.SquareCompact
                        )
                    }
                },
                onSnackbarShown = vm::onMessageShown,
                onDone = {
                    navController.navigateAfterTipPayment(isPcUsbSource)
                },
                onCompactReviewSubmit = {
                    paymentScope.launch {
                        if (vm.finishPostPaymentReview(submit = true)) {
                            navController.navigateAfterTipPayment(isPcUsbSource)
                        }
                    }
                },
                onCompactReviewSkip = {
                    paymentScope.launch {
                        if (vm.finishPostPaymentReview(submit = false)) {
                            navController.navigateAfterTipPayment(isPcUsbSource)
                        }
                    }
                },
                onRetry = {
                    val handle = navController.currentBackStackEntry?.savedStateHandle
                    val reusedRequest = handle?.get<PosPaymentRequest>(
                        PENDING_PAYMENT_REQUEST_KEY
                    )

                    val paymentRequest = reusedRequest ?: buildPaymentRequest(state)

                    if (reusedRequest == null) {
                        Log.w(
                            PAYMENT_TAG,
                            "Retry fallback: pending payment request is missing, rebuilding from current state"
                        )
                    } else {
                        Log.i(
                            PAYMENT_TAG,
                            "Retry reuses pending payment request terminalId=***${paymentRequest.terminalId.takeLast(4)}"
                        )
                    }

                    if (isPcUsbSource) {
                        paymentScope.launch {
                            startPcUsbPaymentFlow(
                                container = container,
                                navController = navController,
                                viewModel = vm,
                                paymentRequest = paymentRequest,
                                requireReviewBeforePayment = deviceClass != ChaiOkDeviceClass.SquareCompact
                            )
                        }
                    } else {
                        startPaymentFlow(
                            navController = navController,
                            viewModel = vm,
                            paymentRequest = paymentRequest,
                            requireReviewBeforePayment = deviceClass != ChaiOkDeviceClass.SquareCompact
                        )
                    }
                },
                onServiceFeeToggle = vm::toggleServiceFee,
                onKitchenEvaluation = vm::selectKitchenEvaluation,
                onServiceEvaluation = vm::selectServiceEvaluation
            )
        }


        composable(Routes.PcIdleImages) {
            val vm: PcIdleImagesViewModel = viewModel(
                factory = SimpleFactory {
                    PcIdleImagesViewModel(
                        observeSettingsUseCase = container.observeSettingsUseCase,
                        updatePcIdleImagesUseCase = container.updatePcIdleImagesUseCase
                    )
                }
            )

            PcIdleImagesRoute(
                viewModel = vm,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.PcCommandIdle) {
            val vm: PcCommandIdleViewModel = viewModel(
                factory = SimpleFactory {
                    PcCommandIdleViewModel(
                        repository = container.pcPaymentCommandRepository,
                        observeSettingsUseCase = container.observeSettingsUseCase
                    )
                }
            )

            val state by vm.uiState.collectAsStateWithLifecycle()
            val pcEvents: Flow<PcCommandIdleEvent> = vm.events

            val lifecycleOwner = LocalLifecycleOwner.current
            val scope = rememberCoroutineScope()

            DisposableEffect(lifecycleOwner) {
                val observer = object : LifecycleEventObserver {
                    override fun onStateChanged(
                        source: LifecycleOwner,
                        event: Lifecycle.Event
                    ) {
                        when (event) {
                            Lifecycle.Event.ON_RESUME -> {
                                scope.launch {
                                    val enabled = container.observeSettingsUseCase()
                                        .first()
                                        .pcUsbModeEnabled

                                    if (enabled) {
                                        vm.resumeListening()
                                    } else {
                                        vm.pauseListening()

                                        navController.navigate(Routes.Home) {
                                            popUpTo(Routes.PcCommandIdle) {
                                                inclusive = true
                                            }
                                            launchSingleTop = true
                                        }
                                    }
                                }
                            }

                            Lifecycle.Event.ON_PAUSE,
                            Lifecycle.Event.ON_STOP -> {
                                vm.pauseListening()
                            }

                            else -> Unit
                        }
                    }
                }

                lifecycleOwner.lifecycle.addObserver(observer)

                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                    vm.pauseListening()
                }
            }

            LaunchedEffect(pcEvents) {
                pcEvents.collect(
                    object : FlowCollector<PcCommandIdleEvent> {
                        override suspend fun emit(value: PcCommandIdleEvent) {
                            when (value) {
                                is PcCommandIdleEvent.OpenTipSelection -> {
                                    navController.navigateSingleTopTo(
                                        Routes.tipSelectionFromPc(
                                            value.amount,
                                            value.commandId,
                                            value.orderId
                                        )
                                    )
                                }
                            }
                        }
                    }
                )
            }

            PcCommandIdleScreen(
                state = state,
                onOpenSettings = {
                    navController.navigateSingleTopTo(Routes.SettingsFromPc)
                }
            )
        }

        composable(Routes.CardPresenting) {
            val previousHandle = remember {
                navController.previousBackStackEntry?.savedStateHandle
            }

            val request = remember {
                previousHandle?.get<PosPaymentRequest>(
                    PENDING_PAYMENT_REQUEST_KEY
                )
            }

            if (previousHandle == null || request == null) {
                Log.e(
                    PAYMENT_TAG,
                    "CardPresenting opened without payment request"
                )

                previousHandle?.putPaymentCancelled()

                LaunchedEffect(Unit) {
                    navController.popBackStack()
                }

                return@composable
            }

            val paymentHandle: SavedStateHandle = previousHandle
            val paymentRequest: PosPaymentRequest = request

            val vm: CardPresentingViewModel = viewModel(
                factory = SimpleFactory {
                    CardPresentingViewModel(
                        startPosPaymentUseCase = container.startPosPaymentUseCase,
                        cancelPosPaymentUseCase = container.cancelPosPaymentUseCase
                    )
                }
            )

            val state by vm.uiState.collectAsStateWithLifecycle()
            val cardEvents: Flow<CardPresentingOneTimeEvent> = vm.oneTimeEvents

            BackHandler(enabled = state.canCancel) {
                vm.cancelPayment()
            }

            LaunchedEffect(paymentRequest) {
                vm.startPayment(
                    request = paymentRequest,
                    amountText = formatPaymentAmount(paymentRequest.amount)
                )
            }

            LaunchedEffect(cardEvents) {
                cardEvents.collect(
                    object : FlowCollector<CardPresentingOneTimeEvent> {
                        override suspend fun emit(value: CardPresentingOneTimeEvent) {
                            when (value) {
                                is CardPresentingOneTimeEvent.PaymentFinished -> {
                                    Log.i(
                                        PAYMENT_TAG,
                                        "CardPresenting oneTimeEvent PaymentFinished result=${value.result.javaClass.simpleName}"
                                    )

                                    paymentHandle.putPaymentResult(value.result)
                                    navController.popBackStack()
                                }

                                CardPresentingOneTimeEvent.Cancelled -> {
                                    Log.i(
                                        PAYMENT_TAG,
                                        "CardPresenting oneTimeEvent Cancelled"
                                    )

                                    paymentHandle.putPaymentCancelled()
                                    navController.popBackStack()
                                }
                            }
                        }
                    }
                )
            }

            CardPresentingScreen(
                state = state,
                onCancel = vm::cancelPayment
            )
        }
    }
}

private fun NavHostController.navigateSingleTopTo(route: String) {
    navigate(route) {
        launchSingleTop = true
    }
}

private fun NavHostController.requestHomeAmountResetSafely() {
    runCatching {
        getBackStackEntry(Routes.Home)
            .savedStateHandle
            .set(CLEAR_HOME_AMOUNT_KEY, true)
    }
        .onSuccess {
            Log.i(
                PAYMENT_TAG,
                "Approved payment completed, requesting Home amount reset"
            )
        }
        .onFailure {
            Log.w(
                PAYMENT_TAG,
                "Home back stack entry missing while requesting amount reset"
            )

            currentBackStackEntry
                ?.savedStateHandle
                ?.set(CLEAR_HOME_AMOUNT_KEY, true)
        }
}

private fun NavHostController.navigateAfterTipPayment(isPcUsbSource: Boolean) {
    if (isPcUsbSource) {
        val popped = popBackStack(route = Routes.PcCommandIdle, inclusive = false)
        if (!popped) navigateSingleTopTo(Routes.PcCommandIdle)
    } else {
        requestHomeAmountResetSafely()
        val popped = popBackStack(route = Routes.Home, inclusive = false)
        if (!popped) navigateSingleTopTo(Routes.Home)
    }
}

private fun buildPaymentRequest(state: TipSelectionUiState): PosPaymentRequest {
    return PosPaymentRequest(
        amount = BigDecimal
            .valueOf(state.totalAmount)
            .setScale(2, RoundingMode.HALF_UP),
        waiterId = state.waiterId,
        terminalId = state.terminalId,
        tipAmount = state.selectedTipAmount,
        serviceFee = state.serviceFeeAmount,
        feesCovered = state.isServiceFeeEnabled
    )
}

private fun startPaymentFlow(
    navController: NavHostController,
    viewModel: TipSelectionViewModel,
    paymentRequest: PosPaymentRequest,
    requireReviewBeforePayment: Boolean
) {
    Log.i(
        PAYMENT_TAG,
        "startPaymentFlow requested terminalId=***${paymentRequest.terminalId.takeLast(4)}"
    )

    if (!viewModel.startExternalPayment(requireReviewBeforePayment)) {
        Log.w(
            PAYMENT_TAG,
            "startPaymentFlow aborted: startExternalPayment returned false"
        )
        return
    }

    Log.i(
        PAYMENT_TAG,
        "Navigate to CardPresenting terminalId=***${paymentRequest.terminalId.takeLast(4)}"
    )

    navController.currentBackStackEntry
        ?.savedStateHandle
        ?.set(PENDING_PAYMENT_REQUEST_KEY, paymentRequest)

    navController.navigate(Routes.CardPresenting)
}

private suspend fun startPcUsbPaymentFlow(
    container: AppContainer,
    navController: NavHostController,
    viewModel: TipSelectionViewModel,
    paymentRequest: PosPaymentRequest,
    requireReviewBeforePayment: Boolean
) {
    val flowStartMs = SystemClock.elapsedRealtime()

    Log.i(
        PAYMENT_TAG,
        "PC USB payment flow requested terminalId=***${paymentRequest.terminalId.takeLast(4)}"
    )
    Log.i(PAYMENT_TAG, "PC USB payment flow release start")

    runCatching {
        container.pcPaymentCommandRepository.stop()
    }.onSuccess {
        Log.i(
            PAYMENT_TAG,
            "PC USB payment flow release done elapsedMs=${SystemClock.elapsedRealtime() - flowStartMs}"
        )
    }.onFailure { throwable ->
        Log.e(PAYMENT_TAG, "PC USB ECR release before POS flow failed", throwable)
    }

    delay(PC_USB_SAFETY_SETTLE_DELAY_MS)

    Log.i(
        PAYMENT_TAG,
        "PC USB payment flow navigating to CardPresenting elapsedMs=${SystemClock.elapsedRealtime() - flowStartMs}"
    )

    startPaymentFlow(
        navController = navController,
        viewModel = viewModel,
        paymentRequest = paymentRequest,
        requireReviewBeforePayment = requireReviewBeforePayment
    )
}

private sealed interface ConsumedPaymentResult {
    data class Finished(
        val result: PaymentResult
    ) : ConsumedPaymentResult

    data object Cancelled : ConsumedPaymentResult
}

private fun SavedStateHandle.putPaymentResult(result: PaymentResult) {
    when (result) {
        is PaymentResult.Approved -> {
            Log.i(
                PAYMENT_TAG,
                "putPaymentResult Approved rawMessagePreview=${result.rawMessage.toPaymentMessagePreview()}"
            )

            set(PAYMENT_RESULT_TYPE_KEY, PAYMENT_RESULT_APPROVED)
            set(PAYMENT_RESULT_TRANSACTION_ID_KEY, result.transactionId)
            set(PAYMENT_RESULT_RRN_KEY, result.rrn)
            set(PAYMENT_RESULT_AUTH_CODE_KEY, result.authCode)
            set(PAYMENT_RESULT_MESSAGE_KEY, result.rawMessage)
        }

        is PaymentResult.Declined -> {
            Log.i(
                PAYMENT_TAG,
                "putPaymentResult Declined reasonPreview=${result.reason.toPaymentMessagePreview()}"
            )

            set(PAYMENT_RESULT_TYPE_KEY, PAYMENT_RESULT_DECLINED)
            set(PAYMENT_RESULT_MESSAGE_KEY, result.reason)
            set(PAYMENT_RESULT_CODE_KEY, result.code)
            set(PAYMENT_RESULT_RAW_MESSAGE_KEY, result.rawMessage)
        }

        is PaymentResult.Error -> {
            Log.i(
                PAYMENT_TAG,
                "putPaymentResult Error messagePreview=${result.message.toPaymentMessagePreview()}"
            )

            set(PAYMENT_RESULT_TYPE_KEY, PAYMENT_RESULT_ERROR)
            set(PAYMENT_RESULT_MESSAGE_KEY, result.message)
        }
    }
}

private fun SavedStateHandle.putPaymentCancelled() {
    set(PAYMENT_RESULT_TYPE_KEY, PAYMENT_RESULT_CANCELLED)
}

private fun SavedStateHandle.consumePaymentResult(): ConsumedPaymentResult? {
    return when (val type = get<String>(PAYMENT_RESULT_TYPE_KEY)) {
        PAYMENT_RESULT_APPROVED -> {
            Log.i(
                PAYMENT_TAG,
                "consumePaymentResult Approved messagePreview=${get<String>(PAYMENT_RESULT_MESSAGE_KEY).toPaymentMessagePreview()}"
            )

            val result = PaymentResult.Approved(
                transactionId = get(PAYMENT_RESULT_TRANSACTION_ID_KEY),
                rrn = get(PAYMENT_RESULT_RRN_KEY),
                authCode = get(PAYMENT_RESULT_AUTH_CODE_KEY),
                rawMessage = get(PAYMENT_RESULT_MESSAGE_KEY)
            )

            clearPaymentResult()

            ConsumedPaymentResult.Finished(result)
        }

        PAYMENT_RESULT_DECLINED -> {
            Log.i(
                PAYMENT_TAG,
                "consumePaymentResult Declined messagePreview=${get<String>(PAYMENT_RESULT_MESSAGE_KEY).toPaymentMessagePreview()}"
            )

            val result = PaymentResult.Declined(
                reason = get(PAYMENT_RESULT_MESSAGE_KEY),
                code = get(PAYMENT_RESULT_CODE_KEY),
                rawMessage = get(PAYMENT_RESULT_RAW_MESSAGE_KEY)
            )

            clearPaymentResult()

            ConsumedPaymentResult.Finished(result)
        }

        PAYMENT_RESULT_ERROR -> {
            Log.i(
                PAYMENT_TAG,
                "consumePaymentResult Error messagePreview=${get<String>(PAYMENT_RESULT_MESSAGE_KEY).toPaymentMessagePreview()}"
            )

            val result = PaymentResult.Error(
                message = get<String>(PAYMENT_RESULT_MESSAGE_KEY)
                    ?: "Ошибка оплаты"
            )

            clearPaymentResult()

            ConsumedPaymentResult.Finished(result)
        }

        PAYMENT_RESULT_CANCELLED -> {
            Log.i(PAYMENT_TAG, "consumePaymentResult Cancelled")

            clearPaymentResult()

            ConsumedPaymentResult.Cancelled
        }

        null -> null

        else -> {
            Log.w(PAYMENT_TAG, "Unknown payment result type=$type")

            clearPaymentResult()

            null
        }
    }
}

private fun SavedStateHandle.clearPaymentResult() {
    remove<String>(PAYMENT_RESULT_TYPE_KEY)
    remove<String>(PAYMENT_RESULT_TRANSACTION_ID_KEY)
    remove<String>(PAYMENT_RESULT_RRN_KEY)
    remove<String>(PAYMENT_RESULT_AUTH_CODE_KEY)
    remove<String>(PAYMENT_RESULT_MESSAGE_KEY)
    remove<String>(PAYMENT_RESULT_CODE_KEY)
    remove<String>(PAYMENT_RESULT_RAW_MESSAGE_KEY)
}

private fun formatPaymentAmount(amount: BigDecimal): String {
    val normalized = amount.setScale(2, RoundingMode.HALF_UP)

    val kopecks = normalized
        .movePointRight(2)
        .setScale(0, RoundingMode.HALF_UP)
        .toLong()

    val rubles = kopecks / 100
    val cents = abs(kopecks % 100)

    val groupedRubles = rubles
        .toString()
        .reversed()
        .chunked(3)
        .joinToString(" ")
        .reversed()

    return if (cents == 0L) {
        "$groupedRubles ₽"
    } else {
        "$groupedRubles,${cents.toString().padStart(2, '0')} ₽"
    }
}

private fun String?.toPaymentMessagePreview(): String {
    val normalized = this
        ?.replace("\n", " ")
        ?.replace("\r", " ")
        ?.trim()
        ?.take(160)

    return if (normalized.isNullOrBlank()) {
        "<blank>"
    } else {
        "\"$normalized\""
    }
}

private const val PAYMENT_TAG = "TipsPaymentFlow"

private const val PENDING_PAYMENT_REQUEST_KEY = "pending_payment_request"
private const val CLEAR_HOME_AMOUNT_KEY = "clear_home_amount"

private const val PAYMENT_RESULT_TYPE_KEY = "payment_result_type"
private const val PAYMENT_RESULT_TRANSACTION_ID_KEY = "payment_result_transaction_id"
private const val PAYMENT_RESULT_RRN_KEY = "payment_result_rrn"
private const val PAYMENT_RESULT_AUTH_CODE_KEY = "payment_result_auth_code"
private const val PAYMENT_RESULT_MESSAGE_KEY = "payment_result_message"
private const val PAYMENT_RESULT_CODE_KEY = "payment_result_code"
private const val PAYMENT_RESULT_RAW_MESSAGE_KEY = "payment_result_raw_message"

private const val PAYMENT_RESULT_APPROVED = "approved"
private const val PAYMENT_RESULT_DECLINED = "declined"
private const val PAYMENT_RESULT_ERROR = "error"
private const val PAYMENT_RESULT_CANCELLED = "cancelled"

// Full ECR release is performed when PC command is accepted; this is a final settle delay.
private const val PC_USB_SAFETY_SETTLE_DELAY_MS = 150L
