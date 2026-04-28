package com.chaiok.pos.domain.repository

import com.chaiok.pos.domain.model.TipRecord

interface TipsRepository {
    suspend fun getTips(): Result<List<TipRecord>>
}
