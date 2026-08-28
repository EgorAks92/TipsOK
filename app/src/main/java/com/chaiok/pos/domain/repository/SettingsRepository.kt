package com.chaiok.pos.domain.repository

import com.chaiok.pos.domain.model.AppSettings
import com.chaiok.pos.domain.model.Arcus2NewWaySettings
import com.chaiok.pos.domain.model.PcCompactPaymentDesignStyle
import com.chaiok.pos.domain.model.PcEcrTransportType
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun observeSettings(): Flow<AppSettings>
    suspend fun setIntegrationMode(enabled: Boolean)
    suspend fun setTableMode(enabled: Boolean)
    suspend fun setTileBackground(background: String)
    suspend fun setPcUsbMode(enabled: Boolean)
    suspend fun setPcIdleImages(images: List<String>)
    suspend fun setPcCompactServiceFeeEnabled(enabled: Boolean)
    suspend fun setShowCustomTipButton(enabled: Boolean)
    suspend fun setPcCompactPaymentDesignStyle(style: PcCompactPaymentDesignStyle)
    suspend fun setPcEcrTransportType(type: PcEcrTransportType)
    suspend fun setArcus2NewWaySettings(settings: Arcus2NewWaySettings)
}
