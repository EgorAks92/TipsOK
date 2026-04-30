package com.chaiok.pos.data.remote.dto

import com.google.gson.JsonObject

data class ApiResponseObjectDto(
    val status: String?,
    val data: JsonObject?
)
