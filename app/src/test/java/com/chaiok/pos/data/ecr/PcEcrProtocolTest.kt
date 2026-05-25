package com.chaiok.pos.data.ecr

import com.chaiok.pos.domain.model.ChaiOkEcrPaymentResultFrame
import com.chaiok.pos.domain.model.ChaiOkEcrReceiptFrame
import com.chaiok.pos.domain.model.PcEcrFinalPaymentResult
import java.math.BigDecimal
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PcEcrProtocolTest {
    @Test
    fun dto_serializes_payment_result_type() {
        val json = Json { encodeDefaults = true; explicitNulls = false }
        val frame = ChaiOkEcrPaymentResultFrame(commandId = "CHECK-1", status = "approved", success = true, currency = "RUB", createdAt = "2026-01-01T00:00:00Z", receipt = ChaiOkEcrReceiptFrame(text = "line1\nline2"))
        val line = json.encodeToString(frame)
        assertTrue(line.contains("\"type\":\"payment_result\""))
        assertTrue((line + "\n").endsWith("\n"))
    }

    @Test
    fun mapper_statuses_and_receipt() {
        val mapper = PcEcrPaymentResultMapper()
        val approved = mapper.map(PcEcrFinalPaymentResult.Approved(receiptText = "a\nb"), "cmd", null, "RUB", BigDecimal("1500.00"), BigDecimal("150.00"), BigDecimal("1650.00"), "t1")
        assertEquals("approved", approved.status)
        assertEquals(true, approved.success)
        assertTrue(approved.receipt?.text?.contains("a\nb") == true)
        assertEquals("declined", mapper.map(PcEcrFinalPaymentResult.Declined(message = "No"), "cmd", null, "RUB", BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ONE, "t1").status)
        assertEquals("cancelled", mapper.map(PcEcrFinalPaymentResult.Cancelled(), "cmd", null, "RUB", BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ONE, "t1").status)
        assertEquals("error", mapper.map(PcEcrFinalPaymentResult.Error("Err"), "cmd", null, "RUB", BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ONE, "t1").status)
    }

    @Test
    fun sanitizer_masks_pan_cvv_track2() {
        val src = "card 4276123412341234 cvv=123 ;4276123412341234=2512?"
        val sanitized = PcEcrSafeLogSanitizer.sanitize(src).orEmpty()
        assertTrue(sanitized.contains("4276"))
        assertTrue(!sanitized.contains("cvv=123", ignoreCase = true))
        assertTrue(!sanitized.contains("=2512"))
    }
}
