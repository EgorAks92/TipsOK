package com.chaiok.pos.data.repository

import com.chaiok.pos.data.remote.TerminalApi
import com.chaiok.pos.data.remote.dto.TerminalAuthRequestDto
import com.chaiok.pos.domain.error.DomainError
import com.chaiok.pos.domain.model.TerminalInfo
import com.chaiok.pos.domain.repository.AuthRepository
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

class BackendAuthRepository(
    private val api: TerminalApi
) : AuthRepository {

    override suspend fun login(pin: String, terminalInfo: TerminalInfo): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val request = TerminalAuthRequestDto(
                waiterCode = pin,
                serialNumber = terminalInfo.serialNumber,
                tid = terminalInfo.tid
            )

            val response = api.terminalLogin(request)
            if (!response.isSuccessful) {
                throw when (response.code()) {
                    400 -> DomainError.InvalidPin
                    else -> DomainError.LoginFailed
                }
            }

            val body = response.body() ?: throw DomainError.LoginFailed
            if (body.status != "OK") throw DomainError.LoginFailed

            val payload = body.data ?: throw DomainError.LoginFailed
            val waiterId = payload.extractWaiterId()
                ?: throw DomainError.LoginFailed // TODO: replace flexible parsing after backend provides exact TerminalLogin response schema.

            payload.extractAuthToken()?.let {
                // TODO: store auth token in SessionRepository when protected endpoints are connected.
            }

            waiterId
        }.recoverCatching { error ->
            throw when (error) {
                is DomainError -> error
                is IOException -> DomainError.LoginFailed
                else -> DomainError.LoginFailed
            }
        }
    }

    override suspend fun logout() = Unit
}

private fun JsonObject.extractWaiterId(): String? {
    val fields = listOf("waiterId", "profileId", "id", "userId")
    return fields.asSequence()
        .mapNotNull { key -> this.get(key) }
        .firstNotNullOfOrNull { element ->
            when {
                element.isJsonPrimitive && element.asJsonPrimitive.isString -> element.asString
                element.isJsonPrimitive && element.asJsonPrimitive.isNumber -> element.asNumber.toString()
                else -> null
            }
        }
}

private fun JsonObject.extractAuthToken(): String? {
    val fields = listOf("token", "accessToken", "authToken", "authorization")
    return fields.asSequence()
        .mapNotNull { key -> this.get(key) }
        .firstNotNullOfOrNull { element ->
            when {
                element.isJsonPrimitive && element.asJsonPrimitive.isString -> element.asString
                element.isJsonPrimitive && element.asJsonPrimitive.isNumber -> element.asNumber.toString()
                else -> null
            }
        }
}
