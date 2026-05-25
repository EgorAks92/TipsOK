package com.chaiok.pos.data.ecr

import com.chaiok.pos.domain.model.Arcus2NewWaySettings
import com.chaiok.pos.domain.model.PcEcrFinalPaymentResult
import com.chaiok.pos.domain.model.PcPaymentCommand
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class Arcus2NewWayResultSequenceBuilderTest {
    private val cmd = PcPaymentCommand(amount = BigDecimal("100.00"))


    @Test fun minimalApprovedSequence() {
        val seq = Arcus2NewWayResultSequenceBuilder.buildPaymentResultSequence(cmd, PcEcrFinalPaymentResult.Approved(), "hello", Arcus2NewWaySettings(minimalResultMode = true))
        val texts = seq.map { decodeWin1251(it.data) }
        assertTrue(texts == listOf("STORERC:00", "SETTAGS:", "ENDTR"))
    }

    @Test fun minimalDeclinedSequence() {
        val seq = Arcus2NewWayResultSequenceBuilder.buildPaymentResultSequence(cmd, PcEcrFinalPaymentResult.Declined(), null, Arcus2NewWaySettings(minimalResultMode = true))
        val texts = seq.map { decodeWin1251(it.data) }
        assertTrue(texts == listOf("STORERC:05", "SETTAGS:", "ENDTR"))
    }

    @Test fun approvedWithReceipt() {
        val seq = Arcus2NewWayResultSequenceBuilder.buildPaymentResultSequence(cmd, PcEcrFinalPaymentResult.Approved(), "hello", Arcus2NewWaySettings(minimalResultMode = false))
        val texts = seq.map { decodeWin1251(it.data) }
        assertTrue(texts.any { it.startsWith("STATUS:Одобрено") })
        assertTrue(texts.any { it.startsWith("STARTPRINT:CUSTOMER") })
        assertTrue(texts.any { it.startsWith("PRINT:") })
        assertTrue(texts.any { it.startsWith("ENDPRINT:CUSTOMER") })
        assertTrue(texts.any { it.startsWith("STORERC:00") })
        assertTrue(texts.any { it.startsWith("SETTAGS:") })
        assertTrue(texts.last().startsWith("ENDTR"))
    }

    @Test fun approvedLongReceiptMultiplePrint() {
        val longReceipt = (1..30).joinToString("\n") { "LINE-$it-ABCDEFGHIJ" }
        val seq = Arcus2NewWayResultSequenceBuilder.buildPaymentResultSequence(cmd, PcEcrFinalPaymentResult.Approved(), longReceipt, Arcus2NewWaySettings(minimalResultMode = false, maxReceiptPrintBlockBytes = 40))
        val printCount = seq.map { decodeWin1251(it.data) }.count { it.startsWith("PRINT:") }
        assertTrue(printCount > 1)
    }

    @Test fun approvedWithoutPrint() {
        val seq = Arcus2NewWayResultSequenceBuilder.buildPaymentResultSequence(cmd, PcEcrFinalPaymentResult.Approved(), "hello", Arcus2NewWaySettings(minimalResultMode = false, sendPrintCommands = false))
        assertTrue(seq.map { decodeWin1251(it.data) }.none { it.startsWith("PRINT:") })
    }

    @Test fun declinedWithReceiptPrints() {
        val seq = Arcus2NewWayResultSequenceBuilder.buildPaymentResultSequence(cmd, PcEcrFinalPaymentResult.Declined(), "line1\nline2", Arcus2NewWaySettings(minimalResultMode = false))
        val texts = seq.map { decodeWin1251(it.data) }
        assertTrue(texts.any { it.startsWith("PRINT:") })
    }

    @Test fun declined() {
        val seq = Arcus2NewWayResultSequenceBuilder.buildPaymentResultSequence(cmd, PcEcrFinalPaymentResult.Declined(), null, Arcus2NewWaySettings(minimalResultMode = false))
        val texts = seq.map { decodeWin1251(it.data) }
        assertTrue(texts.any { it.startsWith("STORERC:05") })
        assertTrue(texts.last().startsWith("ENDTR"))
    }

    @Test fun cancelled() {
        val seq = Arcus2NewWayResultSequenceBuilder.buildPaymentResultSequence(cmd, PcEcrFinalPaymentResult.Cancelled(), null, Arcus2NewWaySettings(minimalResultMode = false))
        val texts = seq.map { decodeWin1251(it.data) }
        assertTrue(texts.any { it.startsWith("STORERC:999") })
        assertTrue(texts.last().startsWith("ENDTR"))
    }

    @Test fun error() {
        val seq = Arcus2NewWayResultSequenceBuilder.buildPaymentResultSequence(cmd, PcEcrFinalPaymentResult.Error("e"), null, Arcus2NewWaySettings(minimalResultMode = false))
        val texts = seq.map { decodeWin1251(it.data) }
        assertTrue(texts.any { it.startsWith("STORERC:999") })
        assertTrue(texts.last().startsWith("ENDTR"))
    }


    @Test fun settingsDefaults() {
        val s = Arcus2NewWaySettings()
        assertTrue(s.sendBeginTrOnPaymentStart)
        assertTrue(s.sendStatusOnPaymentStart)
        assertTrue(s.paymentStartStatusText == "Ожидание карты")
        assertTrue(s.minimalResultMode)
        assertTrue(!s.waitOkAfterEachCommand)
    }
}
