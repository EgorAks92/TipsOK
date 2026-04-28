package com.chaiok.pos.domain.repository

import com.chaiok.pos.domain.model.CardReadResult

interface CardReaderRepository {
    suspend fun readCard(): Result<CardReadResult>
}
