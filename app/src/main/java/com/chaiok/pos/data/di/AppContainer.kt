package com.chaiok.pos.data.di

import android.content.Context
import com.chaiok.pos.data.remote.TerminalNetworkFactory
import com.chaiok.pos.data.repository.BackendAuthRepository
import com.chaiok.pos.data.repository.DataStoreSettingsRepository
import com.chaiok.pos.data.repository.InMemorySessionRepository
import com.chaiok.pos.data.repository.MockAuthRepository
import com.chaiok.pos.data.repository.MockCardReaderRepository
import com.chaiok.pos.data.repository.MockTerminalDataProvider
import com.chaiok.pos.data.repository.MockTipsRepository
import com.chaiok.pos.data.repository.MockWaiterRepository
import com.chaiok.pos.data.storage.AppDataStore
import com.chaiok.pos.data.storage.EncryptedPrefsSensitiveStorage
import com.chaiok.pos.domain.repository.AuthRepository
import com.chaiok.pos.domain.repository.CardReaderRepository
import com.chaiok.pos.domain.repository.SessionRepository
import com.chaiok.pos.domain.repository.SettingsRepository
import com.chaiok.pos.domain.repository.TipsRepository
import com.chaiok.pos.domain.repository.WaiterRepository
import com.chaiok.pos.domain.usecase.GetTipsUseCase
import com.chaiok.pos.domain.usecase.LinkCardUseCase
import com.chaiok.pos.domain.usecase.LoginWithPinUseCase
import com.chaiok.pos.domain.usecase.LogoutUseCase
import com.chaiok.pos.domain.usecase.ObserveProfileUseCase
import com.chaiok.pos.domain.usecase.ObserveSettingsUseCase
import com.chaiok.pos.domain.usecase.ObserveCurrentStatusUseCase
import com.chaiok.pos.domain.usecase.ReadCardUseCase
import com.chaiok.pos.domain.usecase.UpdateIntegrationModeUseCase
import com.chaiok.pos.domain.usecase.UpdateStatusUseCase
import com.chaiok.pos.domain.usecase.UpdateTableModeUseCase
import com.chaiok.pos.domain.usecase.UpdateTileBackgroundUseCase

class AppContainer(context: Context) {
    private val appDataStore = AppDataStore(context)
    private val sensitiveStorage = EncryptedPrefsSensitiveStorage(context)

    private val terminalApi = TerminalNetworkFactory.createTerminalApi()
    private val terminalDataProvider = MockTerminalDataProvider()

    private companion object {
        const val USE_MOCK_AUTH = false
    }

    val authRepository: AuthRepository = if (USE_MOCK_AUTH) MockAuthRepository() else BackendAuthRepository(terminalApi)
    val sessionRepository: SessionRepository = InMemorySessionRepository()
    val waiterRepository: WaiterRepository = MockWaiterRepository(appDataStore, sensitiveStorage)
    val tipsRepository: TipsRepository = MockTipsRepository()
    val settingsRepository: SettingsRepository = DataStoreSettingsRepository(appDataStore)
    val cardReaderRepository: CardReaderRepository = MockCardReaderRepository(MockCardReaderRepository.Mode.AlwaysSuccess)

    val loginWithPinUseCase = LoginWithPinUseCase(authRepository, waiterRepository, sessionRepository, terminalDataProvider)
    val logoutUseCase = LogoutUseCase(authRepository, sessionRepository)
    val observeProfileUseCase = ObserveProfileUseCase(waiterRepository)
    val updateStatusUseCase = UpdateStatusUseCase(waiterRepository)
    val observeCurrentStatusUseCase = ObserveCurrentStatusUseCase(observeProfileUseCase)
    val linkCardUseCase = LinkCardUseCase(waiterRepository)
    val readCardUseCase = ReadCardUseCase(cardReaderRepository)
    val getTipsUseCase = GetTipsUseCase(tipsRepository)
    val observeSettingsUseCase = ObserveSettingsUseCase(settingsRepository)
    val updateIntegrationModeUseCase = UpdateIntegrationModeUseCase(settingsRepository)
    val updateTableModeUseCase = UpdateTableModeUseCase(settingsRepository)
    val updateTileBackgroundUseCase = UpdateTileBackgroundUseCase(settingsRepository)
}
