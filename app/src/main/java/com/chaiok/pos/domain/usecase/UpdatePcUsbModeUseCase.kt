package com.chaiok.pos.domain.usecase

import com.chaiok.pos.domain.repository.SettingsRepository

class UpdatePcUsbModeUseCase(private val settingsRepository: SettingsRepository) {
    suspend operator fun invoke(enabled: Boolean) = settingsRepository.setPcUsbMode(enabled)
}
