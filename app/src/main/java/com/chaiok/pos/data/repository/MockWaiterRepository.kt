package com.chaiok.pos.data.repository

import com.chaiok.pos.data.storage.AppDataStore
import com.chaiok.pos.data.storage.SensitiveStorage
import com.chaiok.pos.domain.model.WaiterProfile
import com.chaiok.pos.domain.repository.WaiterRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update

class MockWaiterRepository(
    private val dataStore: AppDataStore,
    private val sensitiveStorage: SensitiveStorage
) : WaiterRepository {

    private val baseProfile = MutableStateFlow<WaiterProfile?>(null)

    override suspend fun loadProfile(waiterId: String): Result<WaiterProfile> {
        // TODO: Replace with real waiter profile API.
        val loaded = WaiterProfile(
            id = waiterId,
            firstName = "Анна",
            lastName = "Смирнова",
            status = "На смене",
            hasLinkedCard = false,
            cardSha256 = null
        )
        baseProfile.value = loaded
        return Result.success(loaded)
    }

    override fun observeProfile(): Flow<WaiterProfile?> {
        return combine(
            baseProfile,
            dataStore.waiterStatusFlow,
            dataStore.hasLinkedCardFlow,
            dataStore.cardShaFlow
        ) { profile, status, hasLinkedCard, cardSha ->
            profile?.copy(
                status = status,
                hasLinkedCard = hasLinkedCard,
                cardSha256 = cardSha
            )
        }
    }

    override suspend fun updateStatus(status: String): Result<Unit> {
        dataStore.setWaiterStatus(status)
        baseProfile.update { it?.copy(status = status) }
        return Result.success(Unit)
    }

    override suspend fun linkCard(cardSha256: String, encryptedCardToken: String): Result<Unit> {
        // TODO: Send linked card token/hash to backend once endpoint is ready.
        sensitiveStorage.saveEncryptedCardToken(encryptedCardToken)
        dataStore.setHasLinkedCard(true)
        dataStore.setCardSha(cardSha256)
        baseProfile.update {
            it?.copy(
                hasLinkedCard = true,
                cardSha256 = cardSha256
            )
        }
        return Result.success(Unit)
    }
}
