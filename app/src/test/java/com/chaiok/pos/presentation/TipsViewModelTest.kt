package com.chaiok.pos.presentation

import com.chaiok.pos.domain.model.TipRecord
import com.chaiok.pos.domain.repository.TipsRepository
import com.chaiok.pos.domain.usecase.GetTipsUseCase
import com.chaiok.pos.presentation.tips.TipsViewModel
import com.chaiok.pos.util.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDateTime

class TipsViewModelTest {
    @get:Rule
    val rule = MainDispatcherRule()

    @Test
    fun `loads tips summary`() = runTest {
        val vm = TipsViewModel(GetTipsUseCase(FakeTipsRepo()))
        assertTrue(vm.uiState.value.tips.isNotEmpty())
        assertTrue(vm.uiState.value.summary.count > 0)
    }
}

private class FakeTipsRepo : TipsRepository {
    override suspend fun getTips(): Result<List<TipRecord>> = Result.success(
        listOf(TipRecord("1", LocalDateTime.now(), 1000.0, 10, 100.0))
    )
}
