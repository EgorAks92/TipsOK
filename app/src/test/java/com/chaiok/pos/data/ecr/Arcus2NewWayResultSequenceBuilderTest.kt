package com.chaiok.pos.data.ecr

import com.chaiok.pos.domain.model.Arcus2NewWaySettings
import com.chaiok.pos.domain.model.PcEcrFinalPaymentResult
import com.chaiok.pos.domain.model.PcPaymentCommand
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class Arcus2NewWayResultSequenceBuilderTest {
    private val cmd = PcPaymentCommand(amount = BigDecimal("100.00"))

    @Test fun minimalApprovedSequenceWithoutReceipt() {
        val seq = Arcus2NewWayResultSequenceBuilder.buildPaymentResultSequence(cmd, PcEcrFinalPaymentResult.Approved(), null, Arcus2NewWaySettings(minimalResultMode = true))
        assertTrue(seq.map { decodeWin1251(it.data) } == listOf("STORERC:00", "SETTAGS:", "ENDTR"))
    }

    @Test fun minimalApprovedSequenceWithReceipt() {
        val seq = Arcus2NewWayResultSequenceBuilder.buildPaymentResultSequence(cmd, PcEcrFinalPaymentResult.Approved(), "hello", Arcus2NewWaySettings(minimalResultMode = true))
        val texts = seq.map { decodeWin1251(it.data) }
        assertTrue(texts.first() == "STARTPRINT:CUSTOMER")
        assertTrue(texts.any { it.startsWith("PRINT:") })
        assertTrue(texts.contains("ENDPRINT:CUSTOMER"))
        assertTrue(texts.takeLast(3) == listOf("STORERC:00", "SETTAGS:", "ENDTR"))
    }

    @Test fun minimalDeclinedSequence() {
        val seq = Arcus2NewWayResultSequenceBuilder.buildPaymentResultSequence(cmd, PcEcrFinalPaymentResult.Declined(), "line1", Arcus2NewWaySettings(minimalResultMode = true))
        assertTrue(seq.map { decodeWin1251(it.data) }.takeLast(3) == listOf("STORERC:05", "SETTAGS:", "ENDTR"))
    }

    @Test fun minimalModeReceiptDisabled() {
        val seq = Arcus2NewWayResultSequenceBuilder.buildPaymentResultSequence(cmd, PcEcrFinalPaymentResult.Approved(), "hello", Arcus2NewWaySettings(minimalResultMode = true, sendReceiptInMinimalMode = false))
        assertTrue(seq.map { decodeWin1251(it.data) }.none { it.startsWith("PRINT:") })
    }

    @Test fun approvedWithReceipt() {
        val seq = Arcus2NewWayResultSequenceBuilder.buildPaymentResultSequence(cmd, PcEcrFinalPaymentResult.Approved(), "hello", Arcus2NewWaySettings(minimalResultMode = false))
        val texts = seq.map { decodeWin1251(it.data) }
        assertTrue(texts.any { it.startsWith("STATUS:Одобрено") })
        assertTrue(texts.any { it.startsWith("PRINT:") })
        assertTrue(texts.any { it.startsWith("STORERC:00") })
        assertTrue(texts.any { it.startsWith("SETTAGS:") })
        assertTrue(texts.last().startsWith("ENDTR"))
    }

    @Test fun settingsDefaults() {
        val s = Arcus2NewWaySettings()
        assertTrue(s.minimalResultMode)
        assertTrue(!s.waitOkAfterEachCommand)
        assertTrue(s.sendReceiptInMinimalMode)
        assertTrue(s.drainOkAfterCommandMs == 200L)
    }
}
