package com.chaiok.pos.domain.model

data class WaiterProfile(
    val id: String,
    val firstName: String,
    val lastName: String,
    val status: String,
    val hasLinkedCard: Boolean,
    val cardSha256: String?
)
