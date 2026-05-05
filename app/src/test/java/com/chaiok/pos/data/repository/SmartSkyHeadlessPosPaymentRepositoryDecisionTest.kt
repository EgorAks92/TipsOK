package com.chaiok.pos.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartSkyHeadlessPosPaymentRepositoryDecisionTest {

    @Test
    fun `rc 00 code 0 isApproved true is approved`() {
        val decision = SmartSkyHeadlessPosPaymentRepository.resolveSspApprovalDecision(
            isApproved = true,
            rc = "00",
            code = 0
        )

        assertTrue(decision.approved)
        assertTrue(decision.approvedByRc)
        assertFalse(decision.declinedByRc)
    }

    @Test
    fun `rc 99 code 0 isApproved true is declined`() {
        val decision = SmartSkyHeadlessPosPaymentRepository.resolveSspApprovalDecision(
            isApproved = true,
            rc = "99",
            code = 0
        )

        assertFalse(decision.approved)
        assertFalse(decision.approvedByRc)
        assertTrue(decision.declinedByRc)
    }

    @Test
    fun `rc 99 code 0 isApproved false is declined`() {
        val decision = SmartSkyHeadlessPosPaymentRepository.resolveSspApprovalDecision(
            isApproved = false,
            rc = "99",
            code = 0
        )

        assertFalse(decision.approved)
        assertTrue(decision.declinedByRc)
    }

    @Test
    fun `rc 05 code 0 isApproved true is declined`() {
        val decision = SmartSkyHeadlessPosPaymentRepository.resolveSspApprovalDecision(
            isApproved = true,
            rc = "05",
            code = 0
        )

        assertFalse(decision.approved)
        assertTrue(decision.declinedByRc)
    }

    @Test
    fun `rc null code 0 isApproved true is approved fallback`() {
        val decision = SmartSkyHeadlessPosPaymentRepository.resolveSspApprovalDecision(
            isApproved = true,
            rc = null,
            code = 0
        )

        assertTrue(decision.approved)
        assertFalse(decision.declinedByRc)
    }

    @Test
    fun `rc null code 0 isApproved false is declined`() {
        val decision = SmartSkyHeadlessPosPaymentRepository.resolveSspApprovalDecision(
            isApproved = false,
            rc = null,
            code = 0
        )

        assertFalse(decision.approved)
    }

    @Test
    fun `rc with spaces 00 is approved`() {
        val decision = SmartSkyHeadlessPosPaymentRepository.resolveSspApprovalDecision(
            isApproved = false,
            rc = " 00 ",
            code = 0
        )

        assertTrue(decision.approved)
        assertEquals("00", decision.normalizedRc)
    }

    @Test
    fun `rc with spaces 99 is declined`() {
        val decision = SmartSkyHeadlessPosPaymentRepository.resolveSspApprovalDecision(
            isApproved = true,
            rc = " 99 ",
            code = 0
        )

        assertFalse(decision.approved)
        assertEquals("99", decision.normalizedRc)
    }
}
