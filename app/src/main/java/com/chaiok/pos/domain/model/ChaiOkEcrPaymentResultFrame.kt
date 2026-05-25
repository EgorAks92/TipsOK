package com.chaiok.pos.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class ChaiOkEcrPaymentResultFrame(
    val proto: String = "chaiok-ecr",
    val version: Int = 1,
    val type: String = "payment_result",
    val commandId: String,
    val orderId: String? = null,
    val status: String,
    val success: Boolean,
    val resultCode: String? = null,
    val message: String? = null,
    val currency: String,
    val billAmount: String? = null,
    val tipAmount: String? = null,
    val totalAmount: String? = null,
    val externalTransactionId: String? = null,
    val rrn: String? = null,
    val authCode: String? = null,
    val terminalId: String? = null,
    val createdAt: String,
    val receipt: ChaiOkEcrReceiptFrame? = null
)

@Serializable
data class ChaiOkEcrReceiptFrame(
    val format: String = "text",
    val encoding: String = "utf-8",
    val text: String
)
