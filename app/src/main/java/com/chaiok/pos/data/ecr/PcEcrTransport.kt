package com.chaiok.pos.data.ecr

interface PcEcrTransport {
    suspend fun ensureTransportReady(): Result<Unit>
    suspend fun receiveOnce(): Result<ByteArray?>
    suspend fun receiveOnce(timeoutMs: Long): Result<ByteArray?>
    suspend fun send(bytes: ByteArray): Result<Unit>
    suspend fun closeCompletely(): Result<Unit>
    fun isOpen(): Boolean
}
