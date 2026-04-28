package com.chaiok.pos.domain

import com.chaiok.pos.domain.model.WaiterProfile
import com.chaiok.pos.domain.repository.WaiterRepository
import com.chaiok.pos.domain.usecase.LinkCardUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class LinkCardUseCaseTest {
    @Test
    fun `link card delegates to repository`() = runTest {
        val repo = CapturingWaiterRepo()
        val useCase = LinkCardUseCase(repo)

        val result = useCase("sha", "token")

        assertTrue(result.isSuccess)
        assertTrue(repo.called)
    }
}

private class CapturingWaiterRepo : WaiterRepository {
    var called = false
    override suspend fun loadProfile(waiterId: String): Result<WaiterProfile> = Result.failure(IllegalStateException())
    override fun observeProfile(): Flow<WaiterProfile?> = MutableStateFlow(null)
    override suspend fun updateStatus(status: String): Result<Unit> = Result.success(Unit)
    override suspend fun linkCard(cardSha256: String, cardToken: String): Result<Unit> {
        called = true
        return Result.success(Unit)
    }
}
