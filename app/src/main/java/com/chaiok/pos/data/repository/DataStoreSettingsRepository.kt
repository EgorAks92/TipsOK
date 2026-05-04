package com.chaiok.pos.data.repository

import com.chaiok.pos.data.storage.AppDataStore
import com.chaiok.pos.domain.model.AppSettings
import com.chaiok.pos.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class DataStoreSettingsRepository(
    private val dataStore: AppDataStore
) : SettingsRepository {

    override fun observeSettings(): Flow<AppSettings> = combine(
        dataStore.integrationModeFlow,
        dataStore.tableModeFlow,
        dataStore.tileBackgroundFlow
    ) { integration, table, background ->
        AppSettings(
            integrationModeEnabled = integration,
            tableModeEnabled = table,
            tileBackground = background
        )
    }

    override suspend fun setIntegrationMode(enabled: Boolean) {
        runCatching {
            dataStore.setIntegrationMode(enabled)
        }
    }

    override suspend fun setTableMode(enabled: Boolean) {
        runCatching {
            dataStore.setTableMode(enabled)
        }
    }

    override suspend fun setTileBackground(background: String) {
        runCatching {
            dataStore.setTileBackground(background)
        }
    }
}