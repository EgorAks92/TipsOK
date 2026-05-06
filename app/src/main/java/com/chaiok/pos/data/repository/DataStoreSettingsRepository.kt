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
        dataStore.tileBackgroundFlow,
        dataStore.pcUsbModeFlow,
        dataStore.pcIdleImagesFlow
    ) { integration, table, background, pcUsb, pcIdleImages ->
        AppSettings(
            integrationModeEnabled = integration,
            tableModeEnabled = table,
            tileBackground = background,
            pcUsbModeEnabled = pcUsb,
            pcIdleImages = pcIdleImages
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

    override suspend fun setPcUsbMode(enabled: Boolean) {
        runCatching {
            dataStore.setPcUsbMode(enabled)
        }
    }

    override suspend fun setPcIdleImages(images: List<String>) {
        runCatching {
            dataStore.setPcIdleImages(images)
        }
    }
}