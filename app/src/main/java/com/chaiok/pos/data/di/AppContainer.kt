package com.chaiok.pos.data.di

import android.content.Context
import android.util.Log
import com.chaiok.pos.data.remote.TerminalNetworkFactory
import com.chaiok.pos.data.repository.BackendAuthRepository
import com.chaiok.pos.data.repository.BackendReviewRepository
import com.chaiok.pos.data.repository.BackendTipRangeRepository
import com.chaiok.pos.data.repository.BackendTipsRepository
import com.chaiok.pos.data.repository.DataStoreSettingsRepository
import com.chaiok.pos.data.repository.InMemorySessionRepository
import com.chaiok.pos.data.repository.MockAuthRepository
import com.chaiok.pos.data.repository.MockTerminalDataProvider
import com.chaiok.pos.data.repository.MockTipsRepository
import com.chaiok.pos.data.repository.MockWaiterRepository
import com.chaiok.pos.data.repository.PaymentTerminalApi
import com.chaiok.pos.data.repository.PaymentTerminalDataProvider
import com.chaiok.pos.data.repository.SmartSkyHeadlessPosPaymentRepository
import com.chaiok.pos.data.repository.SmartSkyPosTerminalApi
import com.chaiok.pos.data.storage.AppDataStore
import com.chaiok.pos.data.storage.EncryptedPrefsSensitiveStorage
import com.chaiok.pos.domain.repository.AuthRepository
import com.chaiok.pos.domain.repository.PosPaymentRepository
import com.chaiok.pos.domain.repository.ReviewRepository
import com.chaiok.pos.domain.repository.SessionRepository
import com.chaiok.pos.domain.repository.SettingsRepository
import com.chaiok.pos.domain.repository.TerminalDataProvider
import com.chaiok.pos.domain.repository.TipRangeRepository
import com.chaiok.pos.domain.repository.TipsRepository
import com.chaiok.pos.domain.repository.WaiterRepository
import com.chaiok.pos.domain.usecase.AddReviewUseCase
import com.chaiok.pos.domain.usecase.CancelPosPaymentUseCase
import com.chaiok.pos.domain.usecase.GetTipsUseCase
import com.chaiok.pos.domain.usecase.GetTransactionRangeUseCase
import com.chaiok.pos.domain.usecase.LoginWithPinUseCase
import com.chaiok.pos.domain.usecase.LogoutUseCase
import com.chaiok.pos.domain.usecase.ObserveCurrentStatusUseCase
import com.chaiok.pos.domain.usecase.ObserveProfileUseCase
import com.chaiok.pos.domain.usecase.ObserveSettingsUseCase
import com.chaiok.pos.domain.usecase.StartPosPaymentUseCase
import com.chaiok.pos.domain.usecase.UpdateStatusUseCase
import com.chaiok.pos.domain.usecase.UpdateTileBackgroundUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val appDataStore = AppDataStore(appContext)
    private val sensitiveStorage = EncryptedPrefsSensitiveStorage(appContext)
    private val terminalApi = TerminalNetworkFactory.createTerminalApi()

    private val paymentTerminalApi: PaymentTerminalApi =
        SmartSkyPosTerminalApi(appContext)

    private val terminalDataProvider: TerminalDataProvider =
        if (USE_MOCK_TERMINAL_DATA) {
            MockTerminalDataProvider()
        } else {
            PaymentTerminalDataProvider(paymentTerminalApi)
        }

    val authRepository: AuthRepository =
        if (USE_MOCK_AUTH) {
            MockAuthRepository()
        } else {
            BackendAuthRepository(terminalApi)
        }

    val sessionRepository: SessionRepository =
        InMemorySessionRepository()

    val waiterRepository: WaiterRepository =
        MockWaiterRepository(appDataStore, sensitiveStorage)

    val tipsRepository: TipsRepository =
        if (USE_MOCK_TIPS) {
            MockTipsRepository()
        } else {
            BackendTipsRepository(terminalApi, sessionRepository)
        }

    val tipRangeRepository: TipRangeRepository =
        BackendTipRangeRepository(
            api = terminalApi,
            sessionRepository = sessionRepository,
            appDataStore = appDataStore
        )

    val settingsRepository: SettingsRepository =
        DataStoreSettingsRepository(appDataStore)

    val reviewRepository: ReviewRepository =
        BackendReviewRepository(
            api = terminalApi,
            sessionRepository = sessionRepository
        )

    val posPaymentRepository: PosPaymentRepository =
        SmartSkyHeadlessPosPaymentRepository(appContext)

    init {
        Log.i(
            LOGIN_TAG,
            "USE_MOCK_AUTH=$USE_MOCK_AUTH USE_MOCK_TERMINAL_DATA=$USE_MOCK_TERMINAL_DATA"
        )
        Log.i(LOGIN_TAG, "authRepository=${authRepository::class.java.simpleName}")
        Log.i(LOGIN_TAG, "terminalDataProvider=${terminalDataProvider::class.java.simpleName}")
    }

    val loginWithPinUseCase = LoginWithPinUseCase(
        authRepository,
        terminalDataProvider,
        waiterRepository,
        sessionRepository
    )

    val logoutUseCase = LogoutUseCase(authRepository, sessionRepository)
    val observeProfileUseCase = ObserveProfileUseCase(waiterRepository)
    val updateStatusUseCase = UpdateStatusUseCase(waiterRepository)
    val observeCurrentStatusUseCase = ObserveCurrentStatusUseCase(observeProfileUseCase)
    val getTipsUseCase = GetTipsUseCase(tipsRepository)
    val getTransactionRangeUseCase = GetTransactionRangeUseCase(tipRangeRepository)
    val observeSettingsUseCase = ObserveSettingsUseCase(settingsRepository)
    val updateTileBackgroundUseCase = UpdateTileBackgroundUseCase(settingsRepository)
    val addReviewUseCase = AddReviewUseCase(reviewRepository)
    val startPosPaymentUseCase = StartPosPaymentUseCase(posPaymentRepository)
    val cancelPosPaymentUseCase = CancelPosPaymentUseCase(posPaymentRepository)

    fun refreshTipRangeAfterLogin() {
        appScope.launch {
            getTransactionRangeUseCase.refresh()
                .onFailure {
                    Log.w(LOGIN_TAG, "refreshTransactionRange failed after login", it)
                }
        }
    }

    private companion object {
        private const val LOGIN_TAG = "LoginFlow"

        // Mock flags are for local development/debug only and must stay false for production builds.
        private const val USE_MOCK_AUTH = false
        private const val USE_MOCK_TERMINAL_DATA = false
        private const val USE_MOCK_TIPS = false
    }
}