package com.chaiok.pos.data.remote.dto

import com.google.gson.JsonElement

data class ApiResponseErrorDto(
    val status: String?,
    val statusCode: String?,
    val data: JsonElement?
)
