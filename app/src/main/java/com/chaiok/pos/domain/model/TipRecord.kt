package com.chaiok.pos.domain.model

import java.time.LocalDateTime

data class TipRecord(
    val id: String,
    val dateTime: LocalDateTime,
    val billAmount: Double,
    val tipPercent: Int,
    val tipAmount: Double
)
