package com.chaiok.pos.data.service

import kotlinx.coroutines.delay
import kotlin.random.Random

class MockCardReaderService(
    private val mode: Mode = Mode.Random
) : CardReaderService {

    enum class Mode { AlwaysSuccess, AlwaysError, Random }

    override suspend fun readCard(): Result<CardReadResult> {
        delay(Random.nextLong(1000, 2000))
        val success = when (mode) {
            Mode.AlwaysSuccess -> true
            Mode.AlwaysError -> false
            Mode.Random -> Random.nextBoolean()
        }

        if (!success) return Result.failure(IllegalStateException("READ_ERROR"))

        val suffix = Random.nextInt(1000, 9999)
        return Result.success(
            CardReadResult(
                cardSha256 = "sha256_mock_$suffix",
                encryptedCardToken = "enc_token_mock_$suffix"
            )
        )
    }
}
