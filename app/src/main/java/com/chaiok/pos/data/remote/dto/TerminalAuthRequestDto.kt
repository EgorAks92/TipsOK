package com.chaiok.pos.data.remote.dto

data class TerminalAuthRequestDto(
    val waiterCode: String,
    val serialNumber: String,
    val tid: String
)
