package com.chaiok.pos.data.repository

import com.chaiok.pos.data.storage.AppDataStore
import com.chaiok.pos.domain.model.AppSettings
import com.chaiok.pos.domain.model.PcEcrProtocol
import com.chaiok.pos.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class DataStoreSettingsRepository(
    private val dataStore: AppDataStore
) : SettingsRepository {

    override fun observeSettings(): Flow<AppSettings> {
        val baseSettingsFlow = combine(
            dataStore.integrationModeFlow,
            dataStore.tableModeFlow,
            dataStore.tileBackgroundFlow,
            dataStore.pcUsbModeFlow,
            dataStore.pcIdleImagesFlow
        ) { integration, table, background, pcUsb, pcIdleImages ->
            PartialSettings(
                integration = integration,
                table = table,
                background = background,
                pcUsb = pcUsb,
                pcIdleImages = pcIdleImages
            )
        }

        return combine(
            baseSettingsFlow,
            dataStore.pcCompactServiceFeeEnabledFlow,
            dataStore.showCustomTipButtonFlow,
            dataStore.pcEcrProtocolFlow,
            dataStore.arcus2NewWaySettingsFlow
        ) { base, pcCompactServiceFeeEnabled, showCustomTipButton, pcEcrProtocol, arcus2NewWaySettings ->
            AppSettings(
                integrationModeEnabled = base.integration,
                tableModeEnabled = base.table,
                tileBackground = base.background,
                pcUsbModeEnabled = base.pcUsb,
                pcIdleImages = base.pcIdleImages,
                pcCompactServiceFeeEnabled = pcCompactServiceFeeEnabled,
                showCustomTipButton = showCustomTipButton,
                pcEcrProtocol = pcEcrProtocol,
                arcus2NewWaySettings = arcus2NewWaySettings
            )
        }
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

    override suspend fun setPcCompactServiceFeeEnabled(enabled: Boolean) {
        runCatching {
            dataStore.setPcCompactServiceFeeEnabled(enabled)
        }
    }

    override suspend fun setShowCustomTipButton(enabled: Boolean) {
        runCatching {
            dataStore.setShowCustomTipButton(enabled)
        }
    }


    override suspend fun setPcEcrProtocol(protocol: PcEcrProtocol) {
        runCatching {
            dataStore.setPcEcrProtocol(protocol)
        }
    }

    private data class PartialSettings(
        val integration: Boolean,
        val table: Boolean,
        val background: String,
        val pcUsb: Boolean,
        val pcIdleImages: List<String>
    )
}
