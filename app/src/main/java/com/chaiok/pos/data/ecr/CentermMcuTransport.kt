package com.chaiok.pos.data.ecr

import android.os.SystemClock
import android.util.Log
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

/** Minimal SDK-independent view of Centerm K6's MCU USB device. */
interface CentermMcuDevice {
    fun open(): Boolean
    fun receiveData(buffer: ByteArray, timeout: Int): Int
    fun sendData(bytes: ByteArray): Boolean
    fun close(): Boolean
}

fun interface CentermMcuDeviceProvider {
    suspend fun create(): CentermMcuDevice
}

class CentermMcuTransport(
    private val mcuDeviceProvider: CentermMcuDeviceProvider,
    private val readBufferSize: Int = DEFAULT_READ_BUFFER_SIZE
) : PcEcrTransport {
    private val lifecycleMutex = Mutex()

    @Volatile
    private var open: Boolean = false

    @Volatile
    private var mcuDevice: CentermMcuDevice? = null

    override suspend fun ensureTransportReady(): Result<Unit> = lifecycleMutex.withLock {
        withContext(Dispatchers.IO) {
            runCatching {
                if (open) return@runCatching Unit

                val device = mcuDevice
                    ?: withTimeout(SDK_INITIALIZATION_TIMEOUT_MS) {
                        mcuDeviceProvider.create()
                    }.also { mcuDevice = it }

                if (!device.open()) {
                    throw IOException("Centerm McuDevice.open returned false")
                }

                open = true
                Log.i(TAG, "Centerm MCU USB transport open result=success baudRate=$BAUD_RATE format=8N1")
                Unit
            }.onFailure { throwable ->
                open = false
                mcuDevice?.let { device ->
                    runCatching { device.close() }
                        .onFailure { Log.w(TAG, "Centerm cleanup after open failure failed", it) }
                }
                mcuDevice = null
                Log.e(TAG, "Centerm MCU USB transport open failed", throwable)
            }
        }
    }

    override suspend fun receiveOnce(): Result<ByteArray?> = receiveOnce(DEFAULT_RECV_TIMEOUT_MS)

    override suspend fun receiveOnce(timeoutMs: Long): Result<ByteArray?> = withContext(Dispatchers.IO) {
        runCatching {
            if (!open) error("Centerm MCU USB transport is not open")
            val device = checkNotNull(mcuDevice) { "Centerm McuDevice is unavailable" }
            val timeout = timeoutMs.coerceIn(1L, Int.MAX_VALUE.toLong()).toInt()
            val buffer = ByteArray(readBufferSize)
            Log.i(TAG, "Centerm recv start buffer=$readBufferSize timeoutMs=$timeout")
            val startedAt = SystemClock.elapsedRealtime()
            val readBytes = try {
                device.receiveData(buffer, timeout)
            } catch (throwable: Throwable) {
                throw IOException("Centerm receiveData failed", throwable)
            }
            val elapsed = SystemClock.elapsedRealtime() - startedAt
            if (readBytes < 0) {
                throw IOException("Centerm receiveData returned error code $readBytes")
            }
            if (readBytes == 0) {
                Log.i(TAG, "Centerm recv zero bytes requested=$timeout actualElapsedMs=$elapsed")
                return@runCatching null
            }
            val safeReadBytes = readBytes.coerceAtMost(buffer.size)
            Log.i(TAG, "Centerm recv end bytes=$safeReadBytes actualElapsedMs=$elapsed")
            buffer.copyOf(safeReadBytes)
        }.onFailure { throwable ->
            Log.e(TAG, "Centerm receiveOnce failed", throwable)
        }
    }

    override suspend fun send(bytes: ByteArray): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            if (!open) error("Centerm MCU USB transport is not open")
            val device = checkNotNull(mcuDevice) { "Centerm McuDevice is unavailable" }
            val bytesForWriting = bytes.copyOf()
            Log.i(TAG, "Centerm send bytes=${bytesForWriting.size}")
            val isSent = try {
                device.sendData(bytesForWriting)
            } catch (throwable: Throwable) {
                throw IOException("Centerm sendData failed", throwable)
            }
            if (!isSent) throw IOException("Centerm sendData returned false")
            Unit
        }.onFailure { throwable ->
            Log.e(TAG, "Centerm send failed", throwable)
        }
    }

    override suspend fun closeCompletely(): Result<Unit> = lifecycleMutex.withLock {
        withContext(Dispatchers.IO) {
            runCatching {
                val device = mcuDevice
                if (device == null) {
                    open = false
                    Log.i(TAG, "Centerm close skipped: device was not initialized")
                    return@runCatching Unit
                }

                Log.i(TAG, "Centerm close MCU USB transport")
                try {
                    if (open && !device.close()) {
                        throw IOException("Centerm McuDevice.close returned false")
                    }
                } finally {
                    open = false
                    mcuDevice = null
                }
                Unit
            }.onFailure { throwable ->
                Log.e(TAG, "Centerm close failed", throwable)
            }
        }
    }

    override fun isOpen(): Boolean = open

    companion object {
        private const val TAG = "CentermMcuTransport"
        internal const val BAUD_RATE = 115200
        private const val SDK_INITIALIZATION_TIMEOUT_MS = 10_000L
        private const val DEFAULT_READ_BUFFER_SIZE = 4096
        private const val DEFAULT_RECV_TIMEOUT_MS = 2000L
    }
}
