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
        val transport = transport(device, readBufferSize = 8)

        transport.ensureTransportReady().getOrThrow()
        val result = transport.receiveOnce(timeoutMs = 123).getOrThrow()

        assertArrayEquals(byteArrayOf(1, 2, 3), result)
        assertEquals(123, device.lastTimeout)
    }

    @Test
    fun readWithZeroBytesDoesNotFail() = runTest {
        val device = FakeCentermSerialPortDevice(receiveBytes = byteArrayOf())
        val transport = transport(device, readBufferSize = 8)

        transport.ensureTransportReady().getOrThrow()
        val result = transport.receiveOnce(timeoutMs = 100).getOrThrow()

        assertEquals(null, result)
    }

    @Test
    fun negativeReadResultIsReportedAsFailure() = runTest {
        val device = FakeCentermSerialPortDevice(receiveResultOverride = -1)
        val transport = transport(device)

        transport.ensureTransportReady().getOrThrow()
        val result = transport.receiveOnce(timeoutMs = 100)

        assertTrue(result.isFailure)
    }

    @Test
    fun writeSendsExactBytes() = runTest {
        val device = FakeCentermSerialPortDevice()
        val transport = transport(device)
        val bytes = byteArrayOf(9, 8, 7)

        transport.ensureTransportReady().getOrThrow()
        transport.send(bytes).getOrThrow()

        assertArrayEquals(bytes, device.sentBytes.single())
    }

    @Test
    fun writeFailureReturnsFailure() = runTest {
        val device = FakeCentermSerialPortDevice(sendResult = false)
        val transport = transport(device)

        transport.ensureTransportReady().getOrThrow()
        val result = transport.send(byteArrayOf(1))

        assertTrue(result.isFailure)
    }

    @Test
    fun closeCallsDeviceCloseAndIsIdempotent() = runTest {
        val device = FakeCentermSerialPortDevice()
        val transport = transport(device)

        transport.ensureTransportReady().getOrThrow()
        transport.closeCompletely().getOrThrow()
        transport.closeCompletely().getOrThrow()

        assertEquals(1, device.closeCalls)
        assertFalse(transport.isOpen())
    }

    @Test
    fun ensureReadyOpensPortOnce() = runTest {
        val device = FakeCentermSerialPortDevice()
        val transport = transport(device)

        transport.ensureTransportReady().getOrThrow()
        transport.ensureTransportReady().getOrThrow()

        assertEquals(1, device.openCalls)
        assertTrue(transport.isOpen())
    }

    @Test
    fun openFailureIsReportedAndTransportStaysClosed() = runTest {
        val device = FakeCentermSerialPortDevice(openResult = false)
        val transport = transport(device)

        val result = transport.ensureTransportReady()

        assertTrue(result.isFailure)
        assertFalse(transport.isOpen())
    }

    private fun transport(
        device: CentermSerialPortDevice,
        readBufferSize: Int = 4096
    ): CentermSerialPortTransport = CentermSerialPortTransport(
        serialPortDeviceProvider = CentermSerialPortDeviceProvider { device },
        readBufferSize = readBufferSize
    )

    private class FakeCentermSerialPortDevice(
        private val receiveBytes: ByteArray = byteArrayOf(),
        private val sendResult: Boolean = true,
        private val openResult: Boolean = true,
        private val receiveResultOverride: Int? = null
    ) : CentermSerialPortDevice {
        val sentBytes = mutableListOf<ByteArray>()
        var openCalls = 0
        var closeCalls = 0
        var lastTimeout = 0

        override fun open(): Boolean {
            openCalls += 1
            return openResult
        }

        override fun receiveData(buffer: ByteArray, timeout: Int): Int {
            lastTimeout = timeout
            receiveResultOverride?.let { return it }
            receiveBytes.copyInto(buffer, endIndex = receiveBytes.size.coerceAtMost(buffer.size))
            return receiveBytes.size.coerceAtMost(buffer.size)
        }

        override fun sendData(bytes: ByteArray): Boolean {
            sentBytes += bytes.copyOf()
            return sendResult
        }

        override fun close(): Boolean {
            closeCalls += 1
            return true
        }
    }
}
