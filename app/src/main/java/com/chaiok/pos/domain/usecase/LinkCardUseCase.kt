package com.chaiok.pos.domain.usecase

import com.chaiok.pos.domain.repository.WaiterRepository

class LinkCardUseCase(private val waiterRepository: WaiterRepository) {
    suspend operator fun invoke(cardSha256: String, encryptedCardToken: String) =
        waiterRepository.linkCard(cardSha256, encryptedCardToken)
}
