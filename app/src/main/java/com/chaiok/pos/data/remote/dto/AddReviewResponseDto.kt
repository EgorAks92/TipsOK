package com.chaiok.pos.data.remote.dto

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName

data class AddReviewResponseDto(
    @SerializedName("status")
    val status: String? = null,

    @SerializedName("statusCode")
    val statusCode: String? = null,

    @SerializedName("data")
    val data: JsonElement? = null
)