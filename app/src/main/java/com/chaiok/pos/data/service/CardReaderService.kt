package com.chaiok.pos.data.service

interface CardReaderService {
    suspend fun readCard(): Result<CardReadResult>
}

data class CardReadResult(
    val cardSha256: String,
    val encryptedCardToken: String
)
