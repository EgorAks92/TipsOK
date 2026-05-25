package com.chaiok.pos.data.ecr

import com.chaiok.pos.domain.model.Arcus2NewWaySettings
import com.chaiok.pos.domain.model.PcEcrFinalPaymentResult
import com.chaiok.pos.domain.model.PcPaymentCommand
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class Arcus2NewWayResultSequenceBuilderTest {
    private val cmd = PcPaymentCommand(amount = BigDecimal("100.00"))

    @Test fun minimalApprovedSequenceWithoutReceipt() {
        val seq = Arcus2NewWayResultSequenceBuilder.buildPaymentResultSequence(cmd, PcEcrFinalPaymentResult.Approved(), null, Arcus2NewWaySettings(minimalResultMode = true))
        assertEquals(listOf("STORERC:00", "SETTAGS:", "ENDTR"), seq.map { decodeWin1251(it.data) })
    }

    @Test fun minimalApprovedSequenceWithReceipt_defaultNoMarkers() {
        val texts = Arcus2NewWayResultSequenceBuilder.buildPaymentResultSequence(cmd, PcEcrFinalPaymentResult.Approved(), "hello", Arcus2NewWaySettings(minimalResultMode = true)).map { decodeWin1251(it.data) }
        assertTrue(texts.any { it.startsWith("PRINT:") })
        assertFalse(texts.any { it.startsWith("STARTPRINT") })
        assertFalse(texts.any { it.startsWith("ENDPRINT") })
        assertEquals(listOf("STORERC:00", "SETTAGS:", "ENDTR"), texts.takeLast(3))
    }

    @Test fun minimalApprovedSequenceWithReceipt_markersEnabled() {
        val texts = Arcus2NewWayResultSequenceBuilder.buildPaymentResultSequence(cmd, PcEcrFinalPaymentResult.Approved(), "hello", Arcus2NewWaySettings(minimalResultMode = true, usePrintSessionMarkersInMinimalMode = true)).map { decodeWin1251(it.data) }
        assertTrue(texts.any { it.startsWith("STARTPRINT") })
        assertTrue(texts.any { it.startsWith("PRINT:") })
        assertTrue(texts.any { it.startsWith("ENDPRINT") })
    }

    @Test fun approvedLongReceiptMultiplePrint() {
        val longReceipt = (1..30).joinToString("\n") { "LINE-$it-ABCDEFGHIJ" }
        val seq = Arcus2NewWayResultSequenceBuilder.buildPaymentResultSequence(cmd, PcEcrFinalPaymentResult.Approved(), longReceipt, Arcus2NewWaySettings(minimalResultMode = false, maxReceiptPrintBlockBytes = 40))
        assertTrue(seq.count { decodeWin1251(it.data).startsWith("PRINT:") } > 1)
    }

    @Test fun approvedWithoutPrint() {
        val seq = Arcus2NewWayResultSequenceBuilder.buildPaymentResultSequence(cmd, PcEcrFinalPaymentResult.Approved(), "hello", Arcus2NewWaySettings(minimalResultMode = false, sendPrintCommands = false))
        assertFalse(seq.any { decodeWin1251(it.data).startsWith("PRINT:") })
    }

    @Test fun minimalModeReceiptDisabled() {
        val seq = Arcus2NewWayResultSequenceBuilder.buildPaymentResultSequence(cmd, PcEcrFinalPaymentResult.Approved(), "hello", Arcus2NewWaySettings(minimalResultMode = true, sendReceiptInMinimalMode = false))
        assertFalse(seq.any { decodeWin1251(it.data).startsWith("PRINT:") })
    }

    @Test fun cancelledErrorFullMode() {
        val settings = Arcus2NewWaySettings(minimalResultMode = false, sendSetTags = true)
        val cancelled = Arcus2NewWayResultSequenceBuilder.buildPaymentResultSequence(cmd, PcEcrFinalPaymentResult.Cancelled(), null, settings).map { decodeWin1251(it.data) }
        val error = Arcus2NewWayResultSequenceBuilder.buildPaymentResultSequence(cmd, PcEcrFinalPaymentResult.Error("e"), null, settings).map { decodeWin1251(it.data) }
        assertTrue(cancelled.any { it.startsWith("STORERC:999") })
        assertTrue(cancelled.any { it == "SETTAGS:" })
        assertTrue(cancelled.last() == "ENDTR")
        assertTrue(error.any { it.startsWith("STORERC:999") })
        assertTrue(error.any { it == "SETTAGS:" })
        assertTrue(error.last() == "ENDTR")
    }
}
