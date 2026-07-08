package com.chaiok.pos.data.ecr

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CentermSerialPortTransportTest {
    @Test
    fun readCopiesBytesAndReturnsCorrectCount() = runTest {
        val device = FakeCentermSerialPortDevice(receiveBytes = byteArrayOf(1, 2, 3))
        val transport = CentermSerialPortTransport(device, readBufferSize = 8)

        transport.ensureTransportReady().getOrThrow()
        val result = transport.receiveOnce(timeoutMs = 123).getOrThrow()

        assertArrayEquals(byteArrayOf(1, 2, 3), result)
        assertEquals(123, device.lastTimeout)
    }

    @Test
    fun readWithZeroBytesDoesNotFail() = runTest {
        val device = FakeCentermSerialPortDevice(receiveBytes = byteArrayOf())
        val transport = CentermSerialPortTransport(device, readBufferSize = 8)

        transport.ensureTransportReady().getOrThrow()
        val result = transport.receiveOnce(timeoutMs = 100).getOrThrow()

        assertEquals(null, result)
    }

    @Test
    fun writeSendsExactBytes() = runTest {
        val device = FakeCentermSerialPortDevice()
        val transport = CentermSerialPortTransport(device)
        val bytes = byteArrayOf(9, 8, 7)

        transport.ensureTransportReady().getOrThrow()
        transport.send(bytes).getOrThrow()

        assertArrayEquals(bytes, device.sentBytes.single())
    }

    @Test
    fun writeFailureReturnsFailure() = runTest {
        val device = FakeCentermSerialPortDevice(sendResult = false)
        val transport = CentermSerialPortTransport(device)

        transport.ensureTransportReady().getOrThrow()
        val result = transport.send(byteArrayOf(1))

        assertTrue(result.isFailure)
    }

    @Test
    fun closeCallsDeviceCloseAndIsIdempotent() = runTest {
        val device = FakeCentermSerialPortDevice()
        val transport = CentermSerialPortTransport(device)

        transport.ensureTransportReady().getOrThrow()
        transport.closeCompletely().getOrThrow()
        transport.closeCompletely().getOrThrow()

        assertEquals(1, device.closeCalls)
        assertFalse(transport.isOpen())
    }

    private class FakeCentermSerialPortDevice(
        private val receiveBytes: ByteArray = byteArrayOf(),
        private val sendResult: Boolean = true
    ) : CentermSerialPortDevice {
        val sentBytes = mutableListOf<ByteArray>()
        var closeCalls = 0
        var lastTimeout = 0

        override fun receiveData(buffer: ByteArray, timeout: Int): Int {
            lastTimeout = timeout
            receiveBytes.copyInto(buffer, endIndex = receiveBytes.size.coerceAtMost(buffer.size))
            return receiveBytes.size.coerceAtMost(buffer.size)
        }

        override fun sendData(bytes: ByteArray): Boolean {
            sentBytes += bytes.copyOf()
            return sendResult
        }

        override fun close() {
            closeCalls += 1
        }
    }
}
