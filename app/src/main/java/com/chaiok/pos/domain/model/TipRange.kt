package com.chaiok.pos.domain.model

data class TipRange(
    val percents: List<Double>,
    val startRange: Int,
    val finishRange: Int,
    val defaultIndex: Int
)
