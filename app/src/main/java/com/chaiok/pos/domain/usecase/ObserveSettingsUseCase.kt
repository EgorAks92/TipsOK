package com.chaiok.pos.domain.usecase

import com.chaiok.pos.domain.repository.SettingsRepository

class ObserveSettingsUseCase(private val settingsRepository: SettingsRepository) {
    operator fun invoke() = settingsRepository.observeSettings()
}
