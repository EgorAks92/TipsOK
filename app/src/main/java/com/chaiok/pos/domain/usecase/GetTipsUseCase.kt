package com.chaiok.pos.domain.usecase

import com.chaiok.pos.domain.repository.TipsRepository

class GetTipsUseCase(private val tipsRepository: TipsRepository) {
    suspend operator fun invoke() = tipsRepository.getTips()
}
