package com.chaiok.pos.data.repository

import android.util.Log
import com.chaiok.pos.data.remote.TerminalApi
import com.chaiok.pos.data.remote.dto.TerminalAuthRequestDto
import com.chaiok.pos.domain.error.DomainError
import com.chaiok.pos.domain.model.TerminalInfo
import com.chaiok.pos.domain.repository.AuthRepository
import com.google.gson.JsonElement
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

            Log.e(
                "LoginFlow",
                "terminalLogin request started serial=***${terminalInfo.serialNumber.takeLast(4)} tid=***${terminalInfo.tid.takeLast(4)}"
            )
            val response = api.terminalLogin(request)
            Log.e("LoginFlow", "terminalLogin response httpCode=${response.code()} isSuccessful=${response.isSuccessful}")
            if (!response.isSuccessful) {
                throw when (response.code()) {
                    400 -> DomainError.InvalidPin
                    else -> DomainError.LoginFailed
                }
            }

            val body = response.body() ?: run {
                Log.e("LoginFlow", "terminalLogin body is null")
                throw DomainError.LoginFailed
            }
            if (!body.status.isSuccessStatus()) {
                Log.e("LoginFlow", "terminalLogin status is not success: ${body.status}")
                throw DomainError.LoginFailed
            }

            val payload = body.data ?: throw DomainError.LoginFailed
            val waiterId = payload.extractWaiterId()
                ?: run {
                    Log.e("LoginFlow", "terminalLogin waiterId not found in data")
                    throw DomainError.LoginFailed
                } // TODO: replace flexible parsing after backend provides exact TerminalLogin response schema.
            Log.e("LoginFlow", "terminalLogin waiterId extracted=$waiterId")

            val token = payload.extractAuthToken()
            if (token != null) {
                Log.e("LoginFlow", "terminalLogin accessToken found=true")
                // TODO: store auth token in SessionRepository when protected endpoints are connected.
            } else {
                Log.e("LoginFlow", "terminalLogin accessToken found=false")
            }

            waiterId
        }.recoverCatching { error ->
            throw when (error) {
                is DomainError -> error
                is IOException -> {
                    Log.e("LoginFlow", "terminalLogin IOException: ${error.message}", error)
                    DomainError.LoginFailed
                }
                else -> {
                    Log.e("LoginFlow", "terminalLogin failed: ${error.message}", error)
                    DomainError.LoginFailed
                }
            }
        }
    }

    override suspend fun logout() = Unit
}

private fun String?.isSuccessStatus(): Boolean {
    return equals("OK", ignoreCase = true) ||
        equals("SUCCESS", ignoreCase = true)
}

private fun JsonElement.asStringOrNumberString(): String? {
    return when {
        isJsonPrimitive && asJsonPrimitive.isString -> asString
        isJsonPrimitive && asJsonPrimitive.isNumber -> asNumber.toString()
        else -> null
    }
}

private fun JsonObject.extractWaiterId(): String? {
    val directFields = listOf("waiterId", "profileId", "id", "userId")

    directFields.asSequence()
        .mapNotNull { key -> get(key) }
        .firstNotNullOfOrNull { element -> element.asStringOrNumberString() }
        ?.let { return it }

    val profile = getAsJsonObject("profile")

    if (profile != null) {
        listOf("waiterId", "id", "profileId", "userId")
            .asSequence()
            .mapNotNull { key -> profile.get(key) }
            .firstNotNullOfOrNull { element -> element.asStringOrNumberString() }
            ?.let { return it }
    }

    return null
}

private fun JsonObject.extractAuthToken(): String? {
    val fields = listOf("token", "accessToken", "authToken", "authorization")
    return fields.asSequence()
        .mapNotNull { key -> this.get(key) }
        .firstNotNullOfOrNull { element -> element.asStringOrNumberString() }
}
