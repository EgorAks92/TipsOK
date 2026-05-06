package com.chaiok.pos.domain.usecase

import com.chaiok.pos.domain.repository.SettingsRepository

class UpdatePcIdleImagesUseCase(
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(images: List<String>) =
        settingsRepository.setPcIdleImages(images)
}
