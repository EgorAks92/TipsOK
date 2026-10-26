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
import com.google.gson.Gson
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

class BackendAuthRepository(
    private val api: TerminalApi
) : AuthRepository {

    private companion object {
        private const val LOGIN_TAG = "LoginFlow"
    }

    override suspend fun login(
        pin: String,
        terminalInfo: TerminalInfo
    ): Result<AuthSession> = withContext(Dispatchers.IO) {
        runCatching {
            val request = TerminalAuthRequestDto(
                pin,
                terminalInfo.serialNumber,
                terminalInfo.tid
            )

            Log.i(
                LOGIN_TAG,
                "terminalLogin request started serial=***${terminalInfo.serialNumber.takeLast(4)} tid=***${terminalInfo.tid.takeLast(4)}"
            )

            val response = api.terminalLogin(request)

            Log.i(
                LOGIN_TAG,
                "terminalLogin response httpCode=${response.code()} isSuccessful=${response.isSuccessful}"
            )

            if (!response.isSuccessful) {
                val serverError = runCatching { response.errorBody()?.string() }
                    .getOrElse { "<error body read failed: ${it.javaClass.simpleName}>" }
                logServerError(response.code(), serverError, pin)
                throw if (response.code() == 400) {
                    DomainError.InvalidPin
                } else {
                    DomainError.LoginFailed
                }
            }

            val body = response.body() ?: throw DomainError.LoginFailed

            if (!body.status.isSuccessStatus()) {
                logServerError(response.code(), Gson().toJson(body), pin)
                throw DomainError.LoginFailed
            }

            val payload = body.data ?: throw DomainError.LoginFailed

            val waiterId = payload.extractWaiterId() ?: throw DomainError.LoginFailed
            val profileId = payload.extractProfileId() ?: throw DomainError.LoginFailed
            val token = payload.extractAuthToken()
                ?.takeIf { it.isNotBlank() }
                ?: throw DomainError.LoginFailed

            val isCardConnected = payload.extractIsCardConnected()
            val serviceFeePercent = payload.extractServiceFeePercent()
            val nickname = payload.extractNickname()
            val personalAppeal = payload.extractPersonalAppeal()

            Log.i(
                LOGIN_TAG,
                "terminalLogin profile parsed nicknamePresent=${nickname != null} personalAppealPresent=${personalAppeal != null}"
            )

            AuthSession(
                waiterId = waiterId,
                profileId = profileId,
                accessToken = token,
                isCardConnected = isCardConnected,
                serviceFeePercent = serviceFeePercent,
                nickname = nickname,
                personalAppeal = personalAppeal
            )
        }.recoverCatching { error ->
            throw when (error) {
                is DomainError -> error
                is IOException -> DomainError.LoginFailed
                else -> DomainError.LoginFailed
            }
        }
    }

    override suspend fun logout() = Unit

    private fun logServerError(httpCode: Int, body: String?, pin: String) {
        loginServerErrorForLog(body, pin).chunked(3_000).forEachIndexed { index, part ->
            Log.e(LOGIN_TAG, "terminalLogin server error httpCode=$httpCode part=${index + 1} body=$part")
        }
    }
}

internal fun loginServerErrorForLog(body: String?, pin: String): String {
    if (body.isNullOrBlank()) return "<empty error body>"

    val secretFields = setOf("pin", "waitercode", "password", "token", "accesstoken", "refreshtoken", "authtoken", "authorization", "cardtoken")
    fun redact(element: JsonElement) {
        when {
            element.isJsonObject -> element.asJsonObject.entrySet().forEach { (key, value) ->
                if (key.lowercase().replace("_", "").replace("-", "") in secretFields) {
                    element.asJsonObject.addProperty(key, "[REDACTED]")
                } else {
                    redact(value)
                }
            }
            element.isJsonArray -> element.asJsonArray.forEach(::redact)
        }
    }

    val safeBody = runCatching {
        JsonParser.parseString(body).also(::redact).toString()
    }.getOrDefault(body)
    val secretAssignment = Regex(
        """(?i)((?:["']?)(?:pin|waiterCode|password|(?:access[_-]?|refresh[_-]?|auth[_-]?|card[_-]?)?token|authorization)["']?\s*[:=]\s*)("(?:\\.|[^"\\])*"|'[^']*'|[^\s,;}\]]+)"""
    )
    val maskedBody = safeBody.replace(secretAssignment) { "${it.groupValues[1]}\"[REDACTED]\"" }
        .replace(Regex("""(?i)\bBearer\s+[^\s"',;}\]]+"""), "Bearer [REDACTED]")
    return if (pin.isNotEmpty()) maskedBody.replace(pin, "[REDACTED]") else maskedBody
}

private fun String?.isSuccessStatus(): Boolean =
    equals("OK", true) || equals("SUCCESS", true)

private fun JsonElement.asStringOrNumberString(): String? = when {
    isJsonPrimitive && asJsonPrimitive.isString -> asString
    isJsonPrimitive && asJsonPrimitive.isNumber -> asNumber.toString()
    else -> null
}

private fun JsonElement.asDoubleOrNull(): Double? = when {
    isJsonPrimitive && asJsonPrimitive.isNumber -> asDouble
    isJsonPrimitive && asJsonPrimitive.isString -> asString.replace(",", ".").toDoubleOrNull()
    else -> null
}

private fun JsonObject.extractWaiterId(): String? {
    getAsJsonObject("profile")
        ?.get("waiterId")
        ?.asStringOrNumberString()
        ?.let { return it }

    return listOf("waiterId", "profileId", "id", "userId")
        .asSequence()
        .mapNotNull { get(it) }
        .firstNotNullOfOrNull { it.asStringOrNumberString() }
}

private fun JsonObject.extractProfileId(): Long? {
    val profile = getAsJsonObject("profile")
    val value = profile?.get("id") ?: get("profileId") ?: get("id")

    return value?.let {
        if (it.isJsonPrimitive && it.asJsonPrimitive.isNumber) {
            it.asLong
        } else {
            it.asStringOrNumberString()?.toLongOrNull()
        }
    }
}

private fun JsonObject.extractAuthToken(): String? {
    val fields = listOf("accessToken", "token", "authToken", "authorization")

    return fields
        .asSequence()
        .mapNotNull { get(it) }
        .firstNotNullOfOrNull { it.asStringOrNumberString() }
}

private fun JsonObject.extractIsCardConnected(): Boolean {
    val profile = get("profile")
        ?.takeIf { it.isJsonObject }
        ?.asJsonObject

    val value = profile?.get("isCardConnected")
        ?: profile?.get("cardConnected")
        ?: profile?.get("isCardLinked")
        ?: get("isCardConnected")
        ?: get("cardConnected")
        ?: get("isCardLinked")

    return when {
        value == null || value.isJsonNull -> false
        value.isJsonPrimitive && value.asJsonPrimitive.isBoolean -> value.asBoolean
        value.isJsonPrimitive && value.asJsonPrimitive.isString -> {
            value.asString.equals("true", ignoreCase = true)
        }
        value.isJsonPrimitive && value.asJsonPrimitive.isNumber -> value.asInt == 1
        else -> false
    }
}

private fun JsonObject.extractServiceFeePercent(): Double {
    val profile = get("profile")
        ?.takeIf { it.isJsonObject }
        ?.asJsonObject

    val value = profile?.get("serviceFeePercent")
        ?: profile?.get("serviceFeePrecent")
        ?: get("serviceFeePercent")
        ?: get("serviceFeePrecent")

    return value
        ?.takeIf { !it.isJsonNull }
        ?.asDoubleOrNull()
        ?.coerceAtLeast(0.0)
        ?: 0.0
}

private fun JsonObject.extractNickname(): String? =
    getAsJsonObject("profile")
        ?.get("nickname")
        ?.asStringOrNumberString()
        ?.trim()
        ?.takeIf { it.isNotBlank() }

private fun JsonObject.extractPersonalAppeal(): String? =
    getAsJsonObject("profile")
        ?.get("personalAppeal")
        ?.asStringOrNumberString()
        ?.trim()
        ?.takeIf { it.isNotBlank() }
