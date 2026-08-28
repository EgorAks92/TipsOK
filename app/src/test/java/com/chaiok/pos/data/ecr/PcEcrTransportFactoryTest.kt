package com.chaiok.pos.data.ecr

import com.chaiok.pos.domain.model.PcEcrTransportType
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class PcEcrTransportFactoryTest {
    @Test
    fun autoSelectsCentermWhenDeviceInfoContainsCentermAndProviderExists() {
        val factory = factory(deviceInfo = FakeDeviceInfoProvider(manufacturer = "Centerm"), centermDevice = fakeDevice())

        val transport = factory.create(PcEcrTransportType.AUTO)

        assertTrue(transport is CentermMcuTransport)
    }

    @Test
    fun autoKeepsCentermTransportWhenSdkInitializationFails() = runTest {
        val factory = factory(deviceInfo = FakeDeviceInfoProvider(model = "Centerm K9"), centermDevice = null)

        val transport = factory.create(PcEcrTransportType.AUTO)

        assertTrue(transport is CentermMcuTransport)
        assertTrue(transport.ensureTransportReady().isFailure)
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

        assertTrue(transport is CentermMcuTransport)
    }

    @Test
    fun explicitCentermDoesNotSilentlyFallBackToKozen() = runTest {
        val factory = factory(centermDevice = null)

        val transport = factory.create(PcEcrTransportType.CENTERM)

        assertTrue(transport is CentermMcuTransport)
        assertTrue(transport.ensureTransportReady().isFailure)
    }

    private fun factory(
        deviceInfo: DeviceInfoProvider = FakeDeviceInfoProvider(),
        centermDevice: CentermMcuDevice?
    ): PcEcrTransportFactory = PcEcrTransportFactory(
        context = null,
        centermMcuDeviceProvider = CentermMcuDeviceProvider {
            centermDevice ?: throw IllegalStateException("Centerm SDK unavailable")
        },
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

    private fun fakeDevice(): CentermMcuDevice = object : CentermMcuDevice {
        override fun open(): Boolean = true
        override fun receiveData(buffer: ByteArray, timeout: Int): Int = 0
        override fun sendData(bytes: ByteArray): Boolean = true
        override fun close(): Boolean = true
    }
}
