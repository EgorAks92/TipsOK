package com.chaiok.pos.data.ecr

import com.chaiok.pos.domain.model.Arcus2NewWaySettings
import com.chaiok.pos.domain.model.PcEcrFinalPaymentResult
import com.chaiok.pos.domain.model.PcPaymentCommand
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class Arcus2NewWayResultSequenceBuilderTest {
    private val cmd = PcPaymentCommand(amount = BigDecimal("100.00"))

    @Test fun approvedWithReceipt() {
        val seq = Arcus2NewWayResultSequenceBuilder.buildPaymentResultSequence(cmd, PcEcrFinalPaymentResult.Approved(), "hello", Arcus2NewWaySettings())
        val texts = seq.map { decodeWin1251(it.data) }
        assertTrue(texts.any { it.startsWith("STATUS:Одобрено") })
        assertTrue(texts.any { it.startsWith("STARTPRINT:CUSTOMER") })
        assertTrue(texts.any { it.startsWith("PRINT:") })
        assertTrue(texts.any { it.startsWith("ENDPRINT:CUSTOMER") })
        assertTrue(texts.any { it.startsWith("STORERC:00") })
        assertTrue(texts.any { it.startsWith("SETTAGS:") })
        assertTrue(texts.last().startsWith("ENDTR"))
    }

    @Test fun approvedWithoutPrint() {
        val seq = Arcus2NewWayResultSequenceBuilder.buildPaymentResultSequence(cmd, PcEcrFinalPaymentResult.Approved(), "hello", Arcus2NewWaySettings(sendPrintCommands = false))
        assertTrue(seq.map { decodeWin1251(it.data) }.none { it.startsWith("PRINT:") })
    }

    @Test fun declined() {
        val seq = Arcus2NewWayResultSequenceBuilder.buildPaymentResultSequence(cmd, PcEcrFinalPaymentResult.Declined(), null, Arcus2NewWaySettings())
        val texts = seq.map { decodeWin1251(it.data) }
        assertTrue(texts.any { it.startsWith("STORERC:05") })
        assertTrue(texts.last().startsWith("ENDTR"))
    }

    @Test fun cancelled() {
        val seq = Arcus2NewWayResultSequenceBuilder.buildPaymentResultSequence(cmd, PcEcrFinalPaymentResult.Cancelled(), null, Arcus2NewWaySettings())
        val texts = seq.map { decodeWin1251(it.data) }
        assertTrue(texts.any { it.startsWith("STORERC:999") })
        assertTrue(texts.last().startsWith("ENDTR"))
    }

    @Test fun error() {
        val seq = Arcus2NewWayResultSequenceBuilder.buildPaymentResultSequence(cmd, PcEcrFinalPaymentResult.Error("e"), null, Arcus2NewWaySettings())
        val texts = seq.map { decodeWin1251(it.data) }
        assertTrue(texts.any { it.startsWith("STORERC:999") })
        assertTrue(texts.last().startsWith("ENDTR"))
    }
}
