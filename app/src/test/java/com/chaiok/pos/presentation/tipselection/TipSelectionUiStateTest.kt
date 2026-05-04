package com.chaiok.pos.presentation.tipselection

import org.junit.Assert.assertEquals
import org.junit.Test

class TipSelectionUiStateTest {
    @Test
    fun amounts_areCalculatedForPresetAndServiceFee() {
        val state = TipSelectionUiState(
            isLoading = false,
            billAmount = 1000.0,
            availablePercents = listOf(5.0, 10.0, 15.0),
            selectedPercentIndex = 1,
            isServiceFeeEnabled = true,
            serviceFeePercent = 2.5
        )
        assertEquals(100.0, state.selectedTipAmount, 0.0)
        assertEquals(2.5, state.serviceFeeAmount, 0.0)
        assertEquals(1102.5, state.totalAmount, 0.0)
    }

    @Test
    fun amounts_useCustomTipWhenSelected() {
        val state = TipSelectionUiState(
            billAmount = 500.0,
            availablePercents = listOf(10.0),
            selectedPercentIndex = 0,
            isCustomSelected = true,
            customTipAmount = 123.45
        )
        assertEquals(123.45, state.selectedTipAmount, 0.0)
        assertEquals(623.45, state.totalAmount, 0.0)
    }

    @Test
    fun evaluation_isClampedToRange_1_5() {
        assertEquals(1, normalizeEvaluation(-2))
        assertEquals(5, normalizeEvaluation(8))
        assertEquals(3, normalizeEvaluation(3))
    }
}
