package com.chaiok.pos.data.remote.dto

import com.google.gson.annotations.SerializedName

data class AddReviewRequestDto(
    @SerializedName("id")
    val id: Long = 0L,

    @SerializedName("profileId")
    val profileId: Long,

    @SerializedName("clientId")
    val clientId: Long = 0L,

    @SerializedName("kitchenEvaluation")
    val kitchenEvaluation: Int,

    @SerializedName("serviceEvaluation")
    val serviceEvaluation: Int,

    @SerializedName("comment")
    val comment: String = ""
)