package com.chaiok.pos.data.ecr

import android.util.Log
import com.chaiok.pos.domain.model.PcEcrTransportType
import com.chaiok.pos.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class SettingsAwarePcEcrTransport(
    private val settingsRepository: SettingsRepository,
    private val factory: PcEcrTransportFactory
) : PcEcrTransport {
    private val mutex = Mutex()
    private var delegate: PcEcrTransport? = null
    private var delegateType: PcEcrTransportType? = null

    override suspend fun ensureTransportReady(): Result<Unit> =
        currentTransport().ensureTransportReady()

    override suspend fun receiveOnce(): Result<ByteArray?> =
        currentTransport().receiveOnce()

    override suspend fun receiveOnce(timeoutMs: Long): Result<ByteArray?> =
        currentTransport().receiveOnce(timeoutMs)

    override suspend fun send(bytes: ByteArray): Result<Unit> =
        currentTransport().send(bytes)

    override suspend fun closeCompletely(): Result<Unit> = mutex.withLock {
        val current = delegate ?: return@withLock Result.success(Unit)
        val result = current.closeCompletely()
        delegate = null
        delegateType = null
        result
    }

    override fun isOpen(): Boolean = delegate?.isOpen() == true

    private suspend fun currentTransport(): PcEcrTransport = mutex.withLock {
        val requestedType = settingsRepository.observeSettings().first().pcEcrTransportType
        val current = delegate
        if (current != null && delegateType == requestedType) return@withLock current

        if (current != null) {
            Log.i(TAG, "ECR transport setting changed from $delegateType to $requestedType; recreating transport")
            current.closeCompletely().onFailure { throwable ->
                Log.w(TAG, "Closing previous ECR transport failed while switching type", throwable)
            }
        }

        factory.create(requestedType).also { created ->
            delegate = created
            delegateType = requestedType
        }
    }

    companion object {
        private const val TAG = "SettingsAwarePcEcrTransport"
    }
}
