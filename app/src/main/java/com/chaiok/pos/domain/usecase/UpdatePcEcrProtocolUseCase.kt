package com.chaiok.pos.domain.usecase

import com.chaiok.pos.domain.model.PcEcrProtocol
import com.chaiok.pos.domain.repository.SettingsRepository

class UpdatePcEcrProtocolUseCase(private val settingsRepository: SettingsRepository) {
    suspend operator fun invoke(protocol: PcEcrProtocol) = settingsRepository.setPcEcrProtocol(protocol)
}
