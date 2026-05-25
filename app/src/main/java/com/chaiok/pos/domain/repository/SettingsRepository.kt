package com.chaiok.pos.domain.repository

import com.chaiok.pos.domain.model.AppSettings
import com.chaiok.pos.domain.model.PcEcrProtocol
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
    suspend fun setPcEcrProtocol(protocol: PcEcrProtocol)
}
