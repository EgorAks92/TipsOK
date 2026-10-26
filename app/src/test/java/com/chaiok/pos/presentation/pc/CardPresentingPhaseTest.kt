package com.chaiok.pos.presentation.pc

import com.chaiok.pos.presentation.cardpresenting.CardPresentingStage
import org.junit.Assert.assertEquals
import org.junit.Test

class CardPresentingPhaseTest {
    @Test
    fun `card waiting processing and results use the matching visuals`() {
        val expected = mapOf(
            PcCompactPaymentScreenPhase.TipSelection to listOf(
                CardPresentingStage.Idle, CardPresentingStage.Preparing, CardPresentingStage.WaitingForCard
            ),
            PcCompactPaymentScreenPhase.Processing to listOf(
                CardPresentingStage.CardDetected, CardPresentingStage.Processing,
                CardPresentingStage.PinRequired, CardPresentingStage.Cancelling
            ),
            PcCompactPaymentScreenPhase.Approved to listOf(CardPresentingStage.Approved),
            PcCompactPaymentScreenPhase.Declined to listOf(
                CardPresentingStage.Declined, CardPresentingStage.Error, CardPresentingStage.Cancelled
            )
        )

        assertEquals(CardPresentingStage.entries.toSet(), expected.values.flatten().toSet())
        expected.forEach { (phase, stages) ->
            stages.forEach { stage -> assertEquals(stage.name, phase, cardPresentingPhase(stage)) }
        }
    }
}
