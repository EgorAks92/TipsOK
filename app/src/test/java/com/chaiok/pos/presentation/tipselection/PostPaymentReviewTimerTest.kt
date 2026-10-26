package com.chaiok.pos.presentation.tipselection

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PostPaymentReviewTimerTest {
    @Test
    fun `review expires after fifteen seconds`() = runTest {
        var secondsLeft = 15
        var expired = false
        launch {
            countDownPostPaymentReview(secondsLeft) { secondsLeft = it }
            expired = true
        }
        advanceTimeBy(14_999)
        assertFalse(expired)
        assertEquals(1, secondsLeft)
        advanceTimeBy(1)
        runCurrent()
        assertTrue(expired)
        assertEquals(0, secondsLeft)
    }

    @Test
    fun `leaving ratings cancels the timeout`() = runTest {
        var expired = false
        val timer = launch {
            countDownPostPaymentReview(15) { }
            expired = true
        }
        advanceTimeBy(5_000)
        timer.cancel()
        advanceTimeBy(15_000)
        runCurrent()
        assertFalse(expired)
    }
}
