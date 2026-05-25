package com.chaiok.pos.data.ecr

import com.chaiok.pos.domain.model.Arcus2NewWaySettings
import com.chaiok.pos.domain.model.PcEcrProtocol
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Arcus2NewWayProtocolAdapterTest {
    private val logger = Arcus2RawFrameLogger()
    @Test fun parseSale() {
        val adapter = Arcus2NewWayProtocolAdapter({ Arcus2NewWaySettings() }, logger)
        val data = "1\u001B1\u001B643\u001B100.01\u001B\u001B"
        val r = adapter.parseIncoming(Arcus2BinLenCodec.encode(encodeWin1251(data)))
        assertTrue(r is EcrParseResult.Command)
        val cmd = (r as EcrParseResult.Command).command as PcEcrCommand.Payment
        assertEquals(PcEcrProtocol.ARCUS2_NEWWAY, cmd.rawProtocol)
        assertEquals("RUB", cmd.currency)
    }
    @Test fun parseSettlement() {
        val adapter = Arcus2NewWayProtocolAdapter({ Arcus2NewWaySettings() }, logger)
        val r = adapter.parseIncoming(Arcus2BinLenCodec.encode(encodeWin1251("2\u001B1")))
        assertTrue((r as EcrParseResult.Command).command is PcEcrCommand.Settlement)
    }
    @Test fun unknownUnipay() {
        val adapter = Arcus2NewWayProtocolAdapter({ Arcus2NewWaySettings() }, logger)
        val r = adapter.parseIncoming(Arcus2BinLenCodec.encode(encodeWin1251("0\u001B128")))
        assertTrue(r is EcrParseResult.Unknown)
    }
}
