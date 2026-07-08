package com.chaiok.pos.data.ecr

import org.junit.Assert.assertTrue
import org.junit.Test

class PcEcrTransportFactoryTest {
    @Test
    fun factorySelectsCentermWhenExplicitlyRequested() {
        val factory = PcEcrTransportFactory(context = null, centermSerialPortDeviceProvider = CentermSerialPortDeviceProvider { fakeDevice() }, kozenTransportProvider = { FakeKozenTransport() })

        val transport = factory.create(PcEcrTransportType.CENTERM)

        assertTrue(transport is CentermSerialPortTransport)
    }

    @Test
    fun factoryKeepsKozenAsAutoFallback() {
        val factory = PcEcrTransportFactory(context = null, centermSerialPortDeviceProvider = CentermSerialPortDeviceProvider { fakeDevice() }, kozenTransportProvider = { FakeKozenTransport() })

        val transport = factory.create(PcEcrTransportType.AUTO)

        assertTrue(transport is FakeKozenTransport)
    }

    private class FakeKozenTransport : PcEcrTransport {
        override suspend fun ensureTransportReady(): Result<Unit> = Result.success(Unit)
        override suspend fun receiveOnce(): Result<ByteArray?> = Result.success(null)
        override suspend fun receiveOnce(timeoutMs: Long): Result<ByteArray?> = Result.success(null)
        override suspend fun send(bytes: ByteArray): Result<Unit> = Result.success(Unit)
        override suspend fun closeCompletely(): Result<Unit> = Result.success(Unit)
        override fun isOpen(): Boolean = true
    }

    private fun fakeDevice(): CentermSerialPortDevice = object : CentermSerialPortDevice {
        override fun receiveData(buffer: ByteArray, timeout: Int): Int = 0
        override fun sendData(bytes: ByteArray): Boolean = true
        override fun close() = Unit
    }
}
