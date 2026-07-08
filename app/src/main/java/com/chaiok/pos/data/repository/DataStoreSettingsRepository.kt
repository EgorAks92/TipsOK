package com.chaiok.pos.data.repository

import com.chaiok.pos.data.storage.AppDataStore
import com.chaiok.pos.domain.model.AppSettings
import com.chaiok.pos.domain.model.Arcus2NewWaySettings
import com.chaiok.pos.domain.model.PcCompactPaymentDesignStyle
import com.chaiok.pos.domain.model.PcEcrTransportType
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

        val pcPaymentUiSettingsFlow = combine(
            dataStore.pcCompactServiceFeeEnabledFlow,
            dataStore.showCustomTipButtonFlow,
            dataStore.pcCompactPaymentDesignStyleFlow,
            dataStore.pcEcrTransportTypeFlow
        ) { pcCompactServiceFeeEnabled, showCustomTipButton, pcCompactPaymentDesignStyle, pcEcrTransportType ->
            PcPaymentUiSettings(
                pcCompactServiceFeeEnabled = pcCompactServiceFeeEnabled,
                showCustomTipButton = showCustomTipButton,
                pcCompactPaymentDesignStyle = pcCompactPaymentDesignStyle,
                pcEcrTransportType = pcEcrTransportType
            )
        }

        return combine(
            baseSettingsFlow,
            pcPaymentUiSettingsFlow,
            dataStore.arcus2NewWaySettingsFlow
        ) { base, pcPaymentUiSettings, arcus2NewWaySettings ->
            AppSettings(
                integrationModeEnabled = base.integration,
                tableModeEnabled = base.table,
                tileBackground = base.background,
                pcUsbModeEnabled = base.pcUsb,
                pcIdleImages = base.pcIdleImages,
                pcCompactServiceFeeEnabled = pcPaymentUiSettings.pcCompactServiceFeeEnabled,
                showCustomTipButton = pcPaymentUiSettings.showCustomTipButton,
                pcCompactPaymentDesignStyle = pcPaymentUiSettings.pcCompactPaymentDesignStyle,
                pcEcrTransportType = pcPaymentUiSettings.pcEcrTransportType,
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

    override suspend fun setPcCompactPaymentDesignStyle(style: PcCompactPaymentDesignStyle) {
        runCatching {
            dataStore.setPcCompactPaymentDesignStyle(style)
        }
    }

    override suspend fun setPcEcrTransportType(type: PcEcrTransportType) {
        runCatching {
            dataStore.setPcEcrTransportType(type)
        }
    }

    override suspend fun setArcus2NewWaySettings(settings: Arcus2NewWaySettings) {
        runCatching {
            dataStore.setArcus2NewWaySettings(settings)
        }
    }

    private data class PartialSettings(
        val integration: Boolean,
        val table: Boolean,
        val background: String,
        val pcUsb: Boolean,
        val pcIdleImages: List<String>
    )

    private data class PcPaymentUiSettings(
        val pcCompactServiceFeeEnabled: Boolean,
        val showCustomTipButton: Boolean,
        val pcCompactPaymentDesignStyle: PcCompactPaymentDesignStyle,
        val pcEcrTransportType: PcEcrTransportType
    )
}
