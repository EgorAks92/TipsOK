package com.chaiok.pos.data.remote

import com.chaiok.pos.data.remote.dto.ApiResponseObjectDto
import com.chaiok.pos.data.remote.dto.TerminalAuthRequestDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface TerminalApi {
    @POST("login")
    suspend fun terminalLogin(@Body request: TerminalAuthRequestDto): Response<ApiResponseObjectDto>
}
