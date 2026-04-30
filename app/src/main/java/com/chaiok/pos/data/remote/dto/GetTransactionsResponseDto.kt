package com.chaiok.pos.data.remote.dto

data class ApiResponseTransactionsDto(
    val status: String?,
    val statusCode: String?,
    val data: List<TransactionDto>?
)

data class TransactionDto(
    val id: Long?,
    val date: String?,
    val amount: Double?,
    val billPercentage: Double?,
    val compensation: Boolean?
)
