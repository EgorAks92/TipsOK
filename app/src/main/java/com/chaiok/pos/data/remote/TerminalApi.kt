package com.chaiok.pos.data.remote

import com.chaiok.pos.data.remote.dto.ApiResponseObjectDto
import com.chaiok.pos.data.remote.dto.ApiResponseTransactionsDto
import com.chaiok.pos.data.remote.dto.GetTransactionsRequestDto
import com.chaiok.pos.data.remote.dto.ApiResponseTransactionRangeDto
import com.chaiok.pos.data.remote.dto.TerminalAuthRequestDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface TerminalApi {
    @POST("login")
    suspend fun terminalLogin(@Body request: TerminalAuthRequestDto): Response<ApiResponseObjectDto>

    @POST("getTransactions")
    suspend fun getTransactions(
        @Header("Authorization") authorization: String,
        @Body request: GetTransactionsRequestDto
    ): Response<ApiResponseTransactionsDto>

    @GET("getTransactionRange")
    suspend fun getTransactionRange(
        @Header("Authorization") authorization: String
    ): Response<ApiResponseTransactionRangeDto>
}
