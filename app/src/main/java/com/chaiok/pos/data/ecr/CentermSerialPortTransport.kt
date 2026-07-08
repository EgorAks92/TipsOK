package com.chaiok.pos.data.ecr

import android.os.SystemClock
import android.util.Log
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface CentermSerialPortDevice {
    fun receiveData(buffer: ByteArray, timeout: Int): Int
    fun sendData(bytes: ByteArray): Boolean
    fun close()
}

class CentermSerialPortTransport(
    private val serialPortDevice: CentermSerialPortDevice,
    private val readBufferSize: Int = DEFAULT_READ_BUFFER_SIZE
) : PcEcrTransport {
    @Volatile
    private var open: Boolean = false

    override suspend fun ensureTransportReady(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            open = true
            Log.i(TAG, "Centerm serial transport open result=success")
            Unit
        }.onFailure { throwable ->
            Log.e(TAG, "Centerm serial transport open failed", throwable)
        }
    }

    override suspend fun receiveOnce(): Result<ByteArray?> = receiveOnce(DEFAULT_RECV_TIMEOUT_MS)

    override suspend fun receiveOnce(timeoutMs: Long): Result<ByteArray?> = withContext(Dispatchers.IO) {
        runCatching {
            if (!open) error("Centerm serial transport is not open")
            val timeout = timeoutMs.toInt().coerceAtLeast(1)
            val buffer = ByteArray(readBufferSize)
            Log.i(TAG, "Centerm recv start buffer=$readBufferSize timeoutMs=$timeout")
            val startedAt = SystemClock.elapsedRealtime()
            val readBytes = try {
                serialPortDevice.receiveData(buffer, timeout)
            } catch (throwable: Throwable) {
                Log.w(TAG, "Calling receiveData-method failed: $throwable")
                throw IOException("Centerm receiveData failed", throwable)
            }
            val elapsed = SystemClock.elapsedRealtime() - startedAt
            if (readBytes <= 0) {
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
            if (!open) error("Centerm serial transport is not open")
            val bytesForWriting = bytes.copyOfRange(0, bytes.size)
            Log.i(TAG, "Centerm send bytes=${bytesForWriting.size}")
            val isSent = try {
                serialPortDevice.sendData(bytesForWriting)
            } catch (throwable: Throwable) {
                Log.w(TAG, "Calling sendData-method failed: $throwable")
                throw IOException("Centerm sendData failed", throwable)
            }
            if (!isSent) throw IOException("Centerm sendData returned false")
            Unit
        }.onFailure { throwable ->
            Log.e(TAG, "Centerm send failed", throwable)
        }
    }

    override suspend fun closeCompletely(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            if (!open) {
                Log.i(TAG, "Centerm close skipped: already closed")
                return@runCatching Unit
            }
            Log.i(TAG, "Centerm close serial port")
            try {
                serialPortDevice.close()
            } finally {
                open = false
            }
            Unit
        }.onFailure { throwable ->
            Log.e(TAG, "Centerm close failed", throwable)
        }
    }

    override fun isOpen(): Boolean = open

    companion object {
        private const val TAG = "CentermSerialPortTransport"
        private const val DEFAULT_READ_BUFFER_SIZE = 4096
        private const val DEFAULT_RECV_TIMEOUT_MS = 2000L
    }
}
