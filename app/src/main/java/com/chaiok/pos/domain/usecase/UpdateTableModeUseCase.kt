package com.chaiok.pos.domain.usecase

import com.chaiok.pos.domain.repository.SettingsRepository

class UpdateTableModeUseCase(private val settingsRepository: SettingsRepository) {
    suspend operator fun invoke(enabled: Boolean) = settingsRepository.setTableMode(enabled)
}
