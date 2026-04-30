package com.chaiok.pos.data.repository

import android.util.Log
import com.chaiok.pos.data.remote.TerminalApi
import com.chaiok.pos.data.remote.dto.TerminalAuthRequestDto
import com.chaiok.pos.domain.error.DomainError
import com.chaiok.pos.domain.model.AuthSession
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

    override suspend fun login(pin: String, terminalInfo: TerminalInfo): Result<AuthSession> = withContext(Dispatchers.IO) {
        runCatching {
            val request = TerminalAuthRequestDto(pin, terminalInfo.serialNumber, terminalInfo.tid)
            Log.e("LoginFlow", "terminalLogin request started serial=***${terminalInfo.serialNumber.takeLast(4)} tid=***${terminalInfo.tid.takeLast(4)}")
            val response = api.terminalLogin(request)
            Log.e("LoginFlow", "terminalLogin response httpCode=${response.code()} isSuccessful=${response.isSuccessful}")
            if (!response.isSuccessful) throw if (response.code() == 400) DomainError.InvalidPin else DomainError.LoginFailed

            val body = response.body() ?: throw DomainError.LoginFailed
            if (!body.status.isSuccessStatus()) throw DomainError.LoginFailed

            val payload = body.data ?: throw DomainError.LoginFailed
            val waiterId = payload.extractWaiterId() ?: throw DomainError.LoginFailed
            val profileId = payload.extractProfileId() ?: throw DomainError.LoginFailed
            val token = payload.extractAuthToken()?.takeIf { it.isNotBlank() } ?: throw DomainError.LoginFailed
            val isCardConnected = payload.extractIsCardConnected()

            Log.e("LoginFlow", "terminalLogin waiterId extracted=$waiterId")
            Log.e("LoginFlow", "terminalLogin profileId extracted=$profileId")
            Log.e("LoginFlow", "terminalLogin accessToken found=true")
            Log.e("LoginFlow", "terminalLogin isCardConnected=$isCardConnected")

            AuthSession(waiterId = waiterId, profileId = profileId, accessToken = token, isCardConnected = isCardConnected)
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

private fun String?.isSuccessStatus(): Boolean = equals("OK", true) || equals("SUCCESS", true)

private fun JsonElement.asStringOrNumberString(): String? = when {
    isJsonPrimitive && asJsonPrimitive.isString -> asString
    isJsonPrimitive && asJsonPrimitive.isNumber -> asNumber.toString()
    else -> null
}

private fun JsonObject.extractWaiterId(): String? {
    getAsJsonObject("profile")?.get("waiterId")?.asStringOrNumberString()?.let { return it }
    return listOf("waiterId", "profileId", "id", "userId")
        .asSequence()
        .mapNotNull { get(it) }
        .firstNotNullOfOrNull { it.asStringOrNumberString() }
}

private fun JsonObject.extractProfileId(): Long? {
    val profile = getAsJsonObject("profile")
    val value = profile?.get("id") ?: get("profileId") ?: get("id")
    return value?.let {
        if (it.isJsonPrimitive && it.asJsonPrimitive.isNumber) it.asLong
        else it.asStringOrNumberString()?.toLongOrNull()
    }
}

private fun JsonObject.extractAuthToken(): String? {
    val fields = listOf("accessToken", "token", "authToken", "authorization")
    return fields.asSequence().mapNotNull { get(it) }.firstNotNullOfOrNull { it.asStringOrNumberString() }
}

private fun JsonObject.extractIsCardConnected(): Boolean {
    val profile = get("profile")?.takeIf { it.isJsonObject }?.asJsonObject

    val value = profile?.get("isCardConnected")
        ?: profile?.get("cardConnected")
        ?: profile?.get("isCardLinked")
        ?: get("isCardConnected")
        ?: get("cardConnected")
        ?: get("isCardLinked")

    return when {
        value == null || value.isJsonNull -> false
        value.isJsonPrimitive && value.asJsonPrimitive.isBoolean -> value.asBoolean
        value.isJsonPrimitive && value.asJsonPrimitive.isString -> value.asString.equals("true", ignoreCase = true)
        value.isJsonPrimitive && value.asJsonPrimitive.isNumber -> value.asInt == 1
        else -> false
    }
}
