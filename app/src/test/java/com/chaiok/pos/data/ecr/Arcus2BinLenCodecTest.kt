package com.chaiok.pos.data.ecr

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Arcus2BinLenCodecTest {
    @Test fun encodeOk() {
        val frame = Arcus2BinLenCodec.encode("OK".toByteArray())
        assertArrayEquals(byteArrayOf(0x01,0x00,0x02,0x4F,0x4B), frame)
    }
    @Test fun decodeOk() {
        val decoded = Arcus2BinLenCodec.decode(byteArrayOf(0x01,0x00,0x02,0x4F,0x4B)).getOrThrow()
        assertTrue(decoded.data.contentEquals("OK".toByteArray()))
    }
    @Test fun rejectWrongSoh() { assertTrue(Arcus2BinLenCodec.decode(byteArrayOf(0x02,0x00,0x00)).isFailure) }
    @Test fun rejectLengthMismatch() { assertTrue(Arcus2BinLenCodec.decode(byteArrayOf(0x01,0x00,0x05,0x4F)).isFailure) }
}
