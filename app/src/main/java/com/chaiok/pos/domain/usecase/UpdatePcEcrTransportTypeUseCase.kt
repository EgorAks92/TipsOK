package com.chaiok.pos.domain.usecase

import com.chaiok.pos.domain.model.PcEcrTransportType
import com.chaiok.pos.domain.repository.SettingsRepository

class UpdatePcEcrTransportTypeUseCase(
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(type: PcEcrTransportType) {
        settingsRepository.setPcEcrTransportType(type)
    }
}
