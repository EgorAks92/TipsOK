package com.chaiok.pos.domain.model

data class AuthSession(
    val waiterId: String,
    val profileId: Long,
    val accessToken: String
)
