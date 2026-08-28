package com.chaiok.pos.domain

import com.chaiok.pos.domain.model.PcEcrTransportType
import org.junit.Assert.assertEquals
import org.junit.Test

class PcEcrTransportTypeTest {
    @Test
    fun unknownStorageValueMapsToAuto() {
        assertEquals(PcEcrTransportType.AUTO, PcEcrTransportType.fromStorageValue("unknown"))
    }

    @Test
    fun centermStorageValueMapsToCenterm() {
        assertEquals(PcEcrTransportType.CENTERM, PcEcrTransportType.fromStorageValue("CENTERM"))
    }

    @Test
    fun kozenStorageValueMapsToKozen() {
        assertEquals(PcEcrTransportType.KOZEN, PcEcrTransportType.fromStorageValue("KOZEN"))
    }
}
