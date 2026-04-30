package com.chaiok.pos.data.remote.dto

data class GetTransactionsRequestDto(
    val profileId: Long,
    val gift: Int = 1
)
