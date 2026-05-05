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
    val compensation: Boolean?,
    val kitchenEvaluation: Any? = null,
    val serviceEvaluation: Any? = null,
    val kitchenRating: Any? = null,
    val serviceRating: Any? = null,
    val kitchenAssessment: Any? = null,
    val serviceAssessment: Any? = null,
    val kitchenAssessmentValue: Any? = null,
    val serviceAssessmentValue: Any? = null,
    val kitchenScore: Any? = null,
    val serviceScore: Any? = null
)
