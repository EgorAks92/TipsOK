package com.chaiok.pos.data.repository

import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LoginServerErrorLogTest {
    @Test
    fun `server error details are preserved`() {
        val body = """{"status":"ERROR","statusCode":"TERMINAL_NOT_FOUND","data":{"message":"Терминал не найден","tid":"12345678"}}"""
        val logged = loginServerErrorForLog(body, "9999")
        assertTrue(logged.contains("TERMINAL_NOT_FOUND"))
        assertTrue(logged.contains("Терминал не найден"))
        assertTrue(logged.contains("12345678"))
    }

    @Test
    fun `nested credentials and echoed pin are masked`() {
        val body = """{"message":"Rejected PIN 1234","data":[{"waiterCode":"1234","access_token":"secret-access","refreshToken":"secret-refresh","Authorization":"Bearer secret-auth"}]}"""
        val logged = loginServerErrorForLog(body, "1234")
        assertFalse(logged.contains("1234"))
        assertFalse(logged.contains("secret-"))
        assertTrue(logged.contains("Rejected PIN"))
        assertEquals("[REDACTED]", JsonParser.parseString(logged).asJsonObject["data"].asJsonArray[0].asJsonObject["access_token"].asString)
    }

    @Test
    fun `plain text errors are preserved with credentials masked`() {
        val logged = loginServerErrorForLog("Terminal not found; token=secret-value; PIN 1234", "1234")
        assertTrue(logged.contains("Terminal not found"))
        assertFalse(logged.contains("secret-value"))
        assertFalse(logged.contains("1234"))
    }

    @Test
    fun `empty body has an explicit marker`() {
        assertEquals("<empty error body>", loginServerErrorForLog(null, "1234"))
        assertEquals("<empty error body>", loginServerErrorForLog(" ", "1234"))
    }
}
