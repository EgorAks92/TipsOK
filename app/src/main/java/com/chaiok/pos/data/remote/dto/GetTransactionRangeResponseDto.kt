package com.chaiok.pos.data.remote.dto

data class ApiResponseTransactionRangeDto(
    val status: String?,
    val statusCode: String?,
    val data: TransactionRangeDto?
)

data class TransactionRangeDto(
    val allTransactionRange: List<Double>?,
    val startRange: Int?,
    val finishRange: Int?,
    val defaultIndex: Int?
)
