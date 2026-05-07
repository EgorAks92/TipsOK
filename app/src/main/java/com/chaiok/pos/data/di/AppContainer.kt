package com.chaiok.pos.data.di

import android.content.Context
import android.util.Log
import com.chaiok.pos.data.ecr.XchengPcPaymentCommandRepository
import com.chaiok.pos.data.ecr.XchengWireEcrPortClient
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
import com.chaiok.pos.domain.usecase.UpdatePcIdleImagesUseCase
import com.chaiok.pos.domain.usecase.UpdatePcUsbModeUseCase
import com.chaiok.pos.domain.usecase.UpdateStatusUseCase
import com.chaiok.pos.domain.usecase.UpdateTileBackgroundUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        Log.i("StartupTrace", "AppContainer init start")
        Log.i("StartupTrace", "AppContainer init end")
    }

    private val appDataStore by lazy { AppDataStore(appContext) }
    private val sensitiveStorage by lazy { EncryptedPrefsSensitiveStorage(appContext) }
    private val terminalApi by lazy { TerminalNetworkFactory.createTerminalApi() }
    private val paymentTerminalApi: PaymentTerminalApi by lazy { SmartSkyPosTerminalApi(appContext) }

    private val terminalDataProvider: TerminalDataProvider by lazy {
        if (USE_MOCK_TERMINAL_DATA) MockTerminalDataProvider() else PaymentTerminalDataProvider(paymentTerminalApi)
    }

    val authRepository: AuthRepository by lazy {
        if (USE_MOCK_AUTH) MockAuthRepository() else BackendAuthRepository(terminalApi)
    }
    val sessionRepository: SessionRepository by lazy { InMemorySessionRepository() }
    val waiterRepository: WaiterRepository by lazy { MockWaiterRepository(appDataStore, sensitiveStorage) }
    val tipsRepository: TipsRepository by lazy {
        if (USE_MOCK_TIPS) MockTipsRepository() else BackendTipsRepository(terminalApi, sessionRepository)
    }
    val tipRangeRepository: TipRangeRepository by lazy {
        BackendTipRangeRepository(terminalApi, sessionRepository, appDataStore)
    }
    val settingsRepository: SettingsRepository by lazy { DataStoreSettingsRepository(appDataStore) }
    val pcPaymentCommandRepository by lazy { XchengPcPaymentCommandRepository(XchengWireEcrPortClient(appContext)) }
    val reviewRepository: ReviewRepository by lazy { BackendReviewRepository(terminalApi, sessionRepository) }
    val posPaymentRepository: PosPaymentRepository by lazy { SmartSkyHeadlessPosPaymentRepository(appContext) }

    val loginWithPinUseCase by lazy {
        LoginWithPinUseCase(authRepository, terminalDataProvider, waiterRepository, sessionRepository)
    }
    val logoutUseCase by lazy { LogoutUseCase(authRepository, sessionRepository) }
    val observeProfileUseCase by lazy { ObserveProfileUseCase(waiterRepository) }
    val updateStatusUseCase by lazy { UpdateStatusUseCase(waiterRepository) }
    val observeCurrentStatusUseCase by lazy { ObserveCurrentStatusUseCase(observeProfileUseCase) }
    val getTipsUseCase by lazy { GetTipsUseCase(tipsRepository) }
    val getTransactionRangeUseCase by lazy { GetTransactionRangeUseCase(tipRangeRepository) }
    val observeSettingsUseCase by lazy { ObserveSettingsUseCase(settingsRepository) }
    val updateTileBackgroundUseCase by lazy { UpdateTileBackgroundUseCase(settingsRepository) }
    val updatePcUsbModeUseCase by lazy { UpdatePcUsbModeUseCase(settingsRepository) }
    val updatePcIdleImagesUseCase by lazy { UpdatePcIdleImagesUseCase(settingsRepository) }
    val addReviewUseCase by lazy { AddReviewUseCase(reviewRepository) }
    val startPosPaymentUseCase by lazy { StartPosPaymentUseCase(posPaymentRepository) }
    val cancelPosPaymentUseCase by lazy { CancelPosPaymentUseCase(posPaymentRepository) }

    fun refreshTipRangeAfterLogin() {
        appScope.launch {
            getTransactionRangeUseCase.refresh().onFailure {
                Log.w(LOGIN_TAG, "refreshTransactionRange failed after login", it)
            }
        }
    }

    private companion object {
        private const val LOGIN_TAG = "LoginFlow"
        private const val USE_MOCK_AUTH = false
        private const val USE_MOCK_TERMINAL_DATA = false
        private const val USE_MOCK_TIPS = false
    }
}
