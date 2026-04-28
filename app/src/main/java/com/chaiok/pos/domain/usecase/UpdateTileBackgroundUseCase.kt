package com.chaiok.pos.domain.usecase

import com.chaiok.pos.domain.repository.SettingsRepository

class UpdateTileBackgroundUseCase(private val settingsRepository: SettingsRepository) {
    suspend operator fun invoke(background: String) = settingsRepository.setTileBackground(background)
}
