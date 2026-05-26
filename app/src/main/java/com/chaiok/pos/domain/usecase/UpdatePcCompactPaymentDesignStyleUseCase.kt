package com.chaiok.pos.domain.usecase

import com.chaiok.pos.domain.model.PcCompactPaymentDesignStyle
import com.chaiok.pos.domain.repository.SettingsRepository

class UpdatePcCompactPaymentDesignStyleUseCase(
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(style: PcCompactPaymentDesignStyle) =
        settingsRepository.setPcCompactPaymentDesignStyle(style)
}
