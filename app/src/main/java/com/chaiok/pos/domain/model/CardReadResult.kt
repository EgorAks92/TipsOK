package com.chaiok.pos.domain.model

data class CardReadResult(
    val cardSha256: String,
    val encryptedCardToken: String
)
