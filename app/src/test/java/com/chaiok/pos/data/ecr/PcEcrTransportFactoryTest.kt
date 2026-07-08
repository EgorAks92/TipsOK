package com.chaiok.pos.data.ecr

import com.chaiok.pos.domain.model.PcEcrTransportType
import org.junit.Assert.assertTrue
import org.junit.Test

class PcEcrTransportFactoryTest {
    @Test
    fun autoSelectsCentermWhenDeviceInfoContainsCentermAndProviderExists() {
        val factory = factory(deviceInfo = FakeDeviceInfoProvider(manufacturer = "Centerm"), centermDevice = fakeDevice())

        val transport = factory.create(PcEcrTransportType.AUTO)

        assertTrue(transport is CentermSerialPortTransport)
    }

    @Test
    fun autoFallsBackToKozenWhenCentermDetectedButProviderUnavailable() {
        val factory = factory(deviceInfo = FakeDeviceInfoProvider(model = "Centerm K9"), centermDevice = null)

        val transport = factory.create(PcEcrTransportType.AUTO)

        assertTrue(transport is FakeKozenTransport)
    }

    @Test
    fun autoUnknownDeviceKeepsKozenFallback() {
        val factory = factory(deviceInfo = FakeDeviceInfoProvider(manufacturer = "Unknown"), centermDevice = fakeDevice())

        val transport = factory.create(PcEcrTransportType.AUTO)

        assertTrue(transport is FakeKozenTransport)
    }

    @Test
    fun explicitCentermSelectsCentermWhenProviderExists() {
        val factory = factory(centermDevice = fakeDevice())

        val transport = factory.create(PcEcrTransportType.CENTERM)

        assertTrue(transport is CentermSerialPortTransport)
    }

    @Test
    fun explicitCentermFallsBackToKozenWhenProviderUnavailable() {
        val factory = factory(centermDevice = null)

        val transport = factory.create(PcEcrTransportType.CENTERM)

        assertTrue(transport is FakeKozenTransport)
    }

    private fun factory(
        deviceInfo: DeviceInfoProvider = FakeDeviceInfoProvider(),
        centermDevice: CentermSerialPortDevice?
    ): PcEcrTransportFactory = PcEcrTransportFactory(
        context = null,
        centermSerialPortDeviceProvider = CentermSerialPortDeviceProvider { centermDevice },
        deviceInfoProvider = deviceInfo,
        kozenTransportProvider = { FakeKozenTransport() }
    )

    private data class FakeDeviceInfoProvider(
        override val manufacturer: String = "",
        override val brand: String = "",
        override val model: String = ""
    ) : DeviceInfoProvider

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
