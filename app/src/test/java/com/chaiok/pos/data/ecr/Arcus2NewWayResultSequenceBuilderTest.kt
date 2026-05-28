package com.chaiok.pos.data.ecr

import com.chaiok.pos.domain.model.Arcus2NewWaySettings
import com.chaiok.pos.domain.model.PcEcrFinalPaymentResult
import com.chaiok.pos.domain.model.PcEcrOperationType
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
        val texts = seq.map { decodeWin1251(it.data) }
        assertEquals("STORERC:00", texts[texts.size - 3])
        assertTrue(texts[texts.size - 2].startsWith("SETTAGS:"))
        assertEquals("ENDTR", texts.last())
    }

    @Test fun minimalApprovedSequenceWithReceipt_defaultNoMarkers() {
        val texts = Arcus2NewWayResultSequenceBuilder.buildPaymentResultSequence(cmd, PcEcrFinalPaymentResult.Approved(), "hello", Arcus2NewWaySettings(minimalResultMode = true)).map { decodeWin1251(it.data) }
        assertTrue(texts.any { it.startsWith("PRINT:") })
        assertFalse(texts.any { it.startsWith("STARTPRINT") })
        assertFalse(texts.any { it.startsWith("ENDPRINT") })
        val tail = texts.takeLast(3)
        assertEquals("STORERC:00", tail[0])
        assertTrue(tail[1].startsWith("SETTAGS:"))
        assertEquals("ENDTR", tail[2])
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
        assertTrue(cancelled.any { it.startsWith("SETTAGS:") })
        assertTrue(cancelled.last() == "ENDTR")
        assertTrue(error.any { it.startsWith("STORERC:999") })
        assertTrue(error.any { it.startsWith("SETTAGS:") })
        assertTrue(error.last() == "ENDTR")
    }

    @Test fun minimalApprovedSetTagsContainsRcAmountCurrency() {
        val seq = Arcus2NewWayResultSequenceBuilder.buildPaymentResultSequence(cmd, PcEcrFinalPaymentResult.Approved(), null, Arcus2NewWaySettings(minimalResultMode = true))
        val setTags = seq.first { it.label == "SETTAGS" }
        val text = decodeWin1251(setTags.data)
        assertTrue(text.startsWith("SETTAGS:"))
        assertTrue(text.contains("RC=00"))
        assertTrue(text.contains("AMOUNT=100.00"))
        assertTrue(text.contains("CURRENCY=RUB"))
        assertTrue(text.contains("STATUS=approved"))
        assertFalse(text.contains("TIP_AMOUNT="))
    }

    @Test fun minimalApprovedSetTagsContainsTipAmountWhenPositive() {
        val seq = Arcus2NewWayResultSequenceBuilder.buildPaymentResultSequence(
            cmd,
            PcEcrFinalPaymentResult.Approved(),
            null,
            Arcus2NewWaySettings(minimalResultMode = true),
            tipAmount = BigDecimal("200.00")
        )
        val text = decodeWin1251(seq.first { it.label == "SETTAGS" }.data)
        assertTrue(text.contains("AMOUNT=100.00"))
        assertTrue(text.contains("TIP_AMOUNT=200.00"))
    }

    @Test fun minimalDeclinedSetTagsContainsRc() {
        val seq = Arcus2NewWayResultSequenceBuilder.buildPaymentResultSequence(cmd, PcEcrFinalPaymentResult.Declined(resultCode = "05"), null, Arcus2NewWaySettings(minimalResultMode = true))
        val text = decodeWin1251(seq.first { it.label == "SETTAGS" }.data)
        assertTrue(text.contains("RC=05"))
        assertTrue(text.contains("STATUS=declined"))
    }

    @Test fun setTagsSanitizesEscAndNewlines() {
        val payload = Arcus2TagsBuilder.buildPaymentTags(
            Arcus2PaymentTagData("00", "123\u001B45\n67", null, null, null, null, null, null, "RUB", "approved")
        )
        val text = decodeWin1251(payload)
        assertFalse(text.contains("\n"))
        assertFalse(text.contains("\r"))
        val rrnPart = text.split('\u001B').first { it.startsWith("RRN=") }
        assertFalse(rrnPart.contains('\u001B'))
    }

    @Test fun setTagsDoesNotContainReceiptText() {
        val receipt = "SENSITIVE RECEIPT CONTENT"
        val seq = Arcus2NewWayResultSequenceBuilder.buildPaymentResultSequence(
            cmd,
            PcEcrFinalPaymentResult.Approved(receiptText = receipt),
            receipt,
            Arcus2NewWaySettings(minimalResultMode = true)
        )
        val text = decodeWin1251(seq.first { it.label == "SETTAGS" }.data)
        assertFalse(text.contains(receipt))
    }

    @Test fun setTagsEmptyStillSendsPrefix() {
        val tags = Arcus2TagsBuilder.buildPaymentTags(
            Arcus2PaymentTagData(null, null, null, null, null, null, null, null, null, "")
        )
        assertEquals(0, tags.size)
        val seq = Arcus2NewWayResultSequenceBuilder.buildPaymentResultSequence(
            cmd,
            PcEcrFinalPaymentResult.Error("err"),
            null,
            Arcus2NewWaySettings(minimalResultMode = true)
        )
        assertTrue(decodeWin1251(seq.first { it.label == "SETTAGS" }.data).startsWith("SETTAGS:"))
    }

    @Test fun waiterLoginApprovedSequenceIsMinimalWithoutSetTags() {
        val waiterCmd = cmd.copy(operationType = PcEcrOperationType.WAITER_LOGIN)
        val seq = Arcus2NewWayResultSequenceBuilder.buildPaymentResultSequence(
            waiterCmd,
            PcEcrFinalPaymentResult.Approved(),
            null,
            Arcus2NewWaySettings(minimalResultMode = true)
        )
        assertEquals(listOf("STORERC", "ENDTR"), seq.map { it.label })
        assertEquals(listOf("STORERC:00", "ENDTR"), seq.map { decodeWin1251(it.data) })
    }

    @Test fun waiterLoginErrorSequenceIsMinimalWithoutSetTags() {
        val waiterCmd = cmd.copy(operationType = PcEcrOperationType.WAITER_LOGIN)
        val seq = Arcus2NewWayResultSequenceBuilder.buildPaymentResultSequence(
            waiterCmd,
            PcEcrFinalPaymentResult.Error("bad"),
            null,
            Arcus2NewWaySettings(minimalResultMode = true, errorRc = "999")
        )
        assertEquals(listOf("STORERC", "ENDTR"), seq.map { it.label })
        assertEquals(listOf("STORERC:999", "ENDTR"), seq.map { decodeWin1251(it.data) })
    }

    @Test fun reconciliationApprovedSequencePrintStorercSettagsEndtr() {
        val seq = Arcus2NewWayResultSequenceBuilder.buildReconciliationResultSequence(
            sourceCommand = cmd,
            result = PcEcrFinalPaymentResult.Approved(externalTransactionId = "report-1"),
            receiptText = "SHIFT REPORT",
            settings = Arcus2NewWaySettings(sendSetTags = true),
            terminalId = "TERM0804"
        )
        val labels = seq.map { it.label }
        assertEquals(listOf("PRINT", "STORERC", "SETTAGS", "ENDTR"), labels)
        val texts = seq.map { decodeWin1251(it.data) }
        assertEquals("PRINT:SHIFT REPORT", texts[0])
        assertEquals("STORERC:00", texts[1])
        assertTrue(texts[2].startsWith("SETTAGS:"))
        assertEquals("ENDTR", texts[3])
    }

    @Test fun reconciliationSetTagsAreMoneyNeutral() {
        val seq = Arcus2NewWayResultSequenceBuilder.buildReconciliationResultSequence(
            sourceCommand = cmd,
            result = PcEcrFinalPaymentResult.Approved(externalTransactionId = "report-1"),
            receiptText = null,
            settings = Arcus2NewWaySettings(sendSetTags = true),
            terminalId = "TERM0804"
        )
        val text = decodeWin1251(seq.first { it.label == "SETTAGS" }.data)
        assertTrue(text.contains("RC=00"))
        assertTrue(text.contains("TERM=TERM0804"))
        assertTrue(text.contains("STATUS=success"))
        assertTrue(text.contains("EXTID=report-1"))
        assertFalse(text.contains("AMOUNT="))
        assertFalse(text.contains("CURRENCY="))
        assertFalse(text.contains("RRN="))
        assertFalse(text.contains("TIP_AMOUNT="))
    }

    @Test fun reconciliationErrorSequenceUsesErrorStorercBeforeSettags() {
        val seq = Arcus2NewWayResultSequenceBuilder.buildReconciliationResultSequence(
            sourceCommand = cmd,
            result = PcEcrFinalPaymentResult.Error("fatal"),
            receiptText = null,
            settings = Arcus2NewWaySettings(sendSetTags = true, errorRc = "777")
        )
        val texts = seq.map { decodeWin1251(it.data) }
        assertEquals(listOf("STORERC", "SETTAGS", "ENDTR"), seq.map { it.label })
        assertEquals("STORERC:777", texts[0])
        assertTrue(texts[1].startsWith("SETTAGS:"))
        assertEquals("ENDTR", texts[2])
    }

}
