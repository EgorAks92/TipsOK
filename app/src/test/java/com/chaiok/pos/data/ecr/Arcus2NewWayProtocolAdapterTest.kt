package com.chaiok.pos.data.ecr

import com.chaiok.pos.domain.model.Arcus2NewWaySettings
import com.chaiok.pos.domain.model.PcEcrCommand
import com.chaiok.pos.domain.model.PcEcrProtocol
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Arcus2NewWayProtocolAdapterTest {
    private val logger = NoOpArcus2FrameLogger()

    @Test fun parseSale() {
        val adapter = Arcus2NewWayProtocolAdapter({ Arcus2NewWaySettings() }, logger)
        val data = "1\u001B1\u001B643\u001B100.01\u001B\u001B"
        val r = adapter.parseIncoming(Arcus2BinLenCodec.encode(encodeWin1251(data)))
        val cmd = (r as EcrParseResult.Command).command as PcEcrCommand.Payment
        assertEquals(PcEcrProtocol.ARCUS2_NEWWAY, cmd.rawProtocol)
        assertEquals("RUB", cmd.currency)
    }

    @Test fun standaloneControlResponses() {
        val adapter = Arcus2NewWayProtocolAdapter({ Arcus2NewWaySettings() }, logger)
        assertTrue(adapter.parseIncoming(Arcus2BinLenCodec.encode(encodeWin1251("OK"))) is EcrParseResult.Ack)
        assertTrue(adapter.parseIncoming(Arcus2BinLenCodec.encode(encodeWin1251("ER"))) is EcrParseResult.Ack)
        assertTrue(adapter.parseIncoming(Arcus2BinLenCodec.encode(encodeWin1251("NAK"))) is EcrParseResult.Ack)
    }

    @Test fun decodeAll() {
        val bytes = Arcus2BinLenCodec.encode(encodeWin1251("NAK")) + Arcus2BinLenCodec.encode(encodeWin1251("NAK")) + Arcus2BinLenCodec.encode(encodeWin1251("ER"))
        val frames = Arcus2BinLenCodec.decodeAll(bytes).getOrThrow()
        assertEquals(3, frames.size)
        assertEquals("NAK", decodeWin1251(frames[0].data))
        assertEquals("NAK", decodeWin1251(frames[1].data))
        assertEquals("ER", decodeWin1251(frames[2].data))
    }
}
