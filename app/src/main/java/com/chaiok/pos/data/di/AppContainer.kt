package com.chaiok.pos.data.di

import android.content.Context
import android.util.Log
import com.chaiok.pos.data.remote.TerminalNetworkFactory
import com.chaiok.pos.data.repository.BackendAuthRepository
import com.chaiok.pos.data.repository.BackendTipsRepository
import com.chaiok.pos.data.repository.BackendTipRangeRepository
import com.chaiok.pos.data.repository.DataStoreSettingsRepository
import com.chaiok.pos.data.repository.InMemorySessionRepository
import com.chaiok.pos.data.repository.MockAuthRepository
import com.chaiok.pos.data.repository.MockCardReaderRepository
import com.chaiok.pos.data.repository.MockTerminalDataProvider
import com.chaiok.pos.data.repository.MockTipsRepository
import com.chaiok.pos.data.repository.MockWaiterRepository
import com.chaiok.pos.data.repository.PaymentTerminalApi
import com.chaiok.pos.data.repository.PaymentTerminalDataProvider
import com.chaiok.pos.data.repository.SmartSkyPosTerminalApi
import com.chaiok.pos.data.storage.AppDataStore
import com.chaiok.pos.data.storage.EncryptedPrefsSensitiveStorage
import com.chaiok.pos.domain.repository.AuthRepository
import com.chaiok.pos.domain.repository.CardReaderRepository
import com.chaiok.pos.domain.repository.SessionRepository
import com.chaiok.pos.domain.repository.SettingsRepository
import com.chaiok.pos.domain.repository.TerminalDataProvider
import com.chaiok.pos.domain.repository.TipsRepository
import com.chaiok.pos.domain.repository.TipRangeRepository
import com.chaiok.pos.domain.repository.WaiterRepository
import com.chaiok.pos.domain.usecase.GetTipsUseCase
import com.chaiok.pos.domain.usecase.LinkCardUseCase
import com.chaiok.pos.domain.usecase.LoginWithPinUseCase
import com.chaiok.pos.domain.usecase.LogoutUseCase
import com.chaiok.pos.domain.usecase.ObserveCurrentStatusUseCase
import com.chaiok.pos.domain.usecase.ObserveProfileUseCase
import com.chaiok.pos.domain.usecase.ObserveSettingsUseCase
import com.chaiok.pos.domain.usecase.GetTransactionRangeUseCase
import com.chaiok.pos.domain.usecase.ReadCardUseCase
import com.chaiok.pos.domain.usecase.UpdateIntegrationModeUseCase
import com.chaiok.pos.domain.usecase.UpdateStatusUseCase
import com.chaiok.pos.domain.usecase.UpdateTableModeUseCase
import com.chaiok.pos.domain.usecase.UpdateTileBackgroundUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AppContainer(context: Context) {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val appDataStore = AppDataStore(context)
    private val sensitiveStorage = EncryptedPrefsSensitiveStorage(context)
    private val terminalApi = TerminalNetworkFactory.createTerminalApi()

    private val paymentTerminalApi: PaymentTerminalApi =
        SmartSkyPosTerminalApi(context.applicationContext)

    private val terminalDataProvider: TerminalDataProvider = if (USE_MOCK_TERMINAL_DATA) {
        MockTerminalDataProvider()
    } else {
        PaymentTerminalDataProvider(paymentTerminalApi)
    }

    val authRepository: AuthRepository =
        if (USE_MOCK_AUTH) MockAuthRepository() else BackendAuthRepository(terminalApi)

    val sessionRepository: SessionRepository = InMemorySessionRepository()
    val waiterRepository: WaiterRepository = MockWaiterRepository(appDataStore, sensitiveStorage)
    val tipsRepository: TipsRepository =
        if (USE_MOCK_TIPS) MockTipsRepository() else BackendTipsRepository(terminalApi, sessionRepository)
    val tipRangeRepository: TipRangeRepository = BackendTipRangeRepository(terminalApi, sessionRepository, appDataStore)
    val settingsRepository: SettingsRepository = DataStoreSettingsRepository(appDataStore)
    val cardReaderRepository: CardReaderRepository = MockCardReaderRepository(MockCardReaderRepository.Mode.AlwaysSuccess)



    init {
        Log.e("LoginFlow", "USE_MOCK_AUTH=$USE_MOCK_AUTH USE_MOCK_TERMINAL_DATA=$USE_MOCK_TERMINAL_DATA")
        Log.e("LoginFlow", "authRepository=${authRepository::class.java.simpleName}")
        Log.e("LoginFlow", "terminalDataProvider=${terminalDataProvider::class.java.simpleName}")
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
    val linkCardUseCase = LinkCardUseCase(waiterRepository)
    val readCardUseCase = ReadCardUseCase(cardReaderRepository)
    val getTipsUseCase = GetTipsUseCase(tipsRepository)
    val getTransactionRangeUseCase = GetTransactionRangeUseCase(tipRangeRepository)
    val observeSettingsUseCase = ObserveSettingsUseCase(settingsRepository)
    val updateIntegrationModeUseCase = UpdateIntegrationModeUseCase(settingsRepository)
    val updateTableModeUseCase = UpdateTableModeUseCase(settingsRepository)
    val updateTileBackgroundUseCase = UpdateTileBackgroundUseCase(settingsRepository)

    fun refreshTipRangeAfterLogin() {
        appScope.launch {
            getTransactionRangeUseCase.refresh()
                .onFailure { Log.e("LoginFlow", "refreshTransactionRange failed after login", it) }
        }
    }

    private companion object {
        private const val USE_MOCK_AUTH = false
        private const val USE_MOCK_TERMINAL_DATA = false
        private const val USE_MOCK_TIPS = false
    }
}
