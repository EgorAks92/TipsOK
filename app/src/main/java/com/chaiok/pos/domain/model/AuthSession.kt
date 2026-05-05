package com.chaiok.pos.domain.model

data class AuthSession(
    val waiterId: String,
    val profileId: Long,
    val accessToken: String,
    val isCardConnected: Boolean,
    val serviceFeePercent: Double = 0.0,
    val nickname: String? = null,
    val personalAppeal: String? = null
)
