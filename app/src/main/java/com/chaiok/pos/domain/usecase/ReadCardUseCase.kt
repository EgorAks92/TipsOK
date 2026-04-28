package com.chaiok.pos.domain.usecase

import com.chaiok.pos.domain.repository.CardReaderRepository

class ReadCardUseCase(
    private val cardReaderRepository: CardReaderRepository
) {
    suspend operator fun invoke() = cardReaderRepository.readCard()
}
