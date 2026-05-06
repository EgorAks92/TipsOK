package com.chaiok.pos.data.ecr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PcPaymentCommandParserTest {
    @Test fun jsonValid() { assertEquals("1250.50", PcPaymentCommandParser.parse("{"+"\"proto\":\"chaiok-ecr\",\"version\":1,\"type\":\"payment\",\"commandId\":\"ORD-1001\",\"amount\":\"1250.50\",\"currency\":\"RUB\"}".toByteArray())?.amount.toString()) }
    @Test fun jsonWithNewline() { assertEquals("10.00", PcPaymentCommandParser.parse("{\"proto\":\"chaiok-ecr\",\"version\":1,\"type\":\"payment\",\"amount\":\"10.00\"}\n".toByteArray())?.amount.toString()) }
    @Test fun multipleLinesFirstInvalidSecondValid() { assertEquals("15.00", PcPaymentCommandParser.parse("{}\n{\"proto\":\"chaiok-ecr\",\"version\":1,\"type\":\"payment\",\"amount\":\"15.00\"}".toByteArray())?.amount.toString()) }
    @Test fun wrongProtoRejected() { assertNull(PcPaymentCommandParser.parse("{\"proto\":\"x\",\"version\":1,\"type\":\"payment\",\"amount\":\"1.00\"}".toByteArray())) }
    @Test fun wrongVersionRejected() { assertNull(PcPaymentCommandParser.parse("{\"proto\":\"chaiok-ecr\",\"version\":2,\"type\":\"payment\",\"amount\":\"1.00\"}".toByteArray())) }
    @Test fun wrongTypeRejected() { assertNull(PcPaymentCommandParser.parse("{\"proto\":\"chaiok-ecr\",\"version\":1,\"type\":\"status\",\"amount\":\"1.00\"}".toByteArray())) }
    @Test fun unsupportedCurrencyRejected() { assertNull(PcPaymentCommandParser.parse("{\"proto\":\"chaiok-ecr\",\"version\":1,\"type\":\"payment\",\"amount\":\"1.00\",\"currency\":\"USD\"}".toByteArray())) }
    @Test fun amountNumberValid() { assertEquals("1.25", PcPaymentCommandParser.parse("{\"proto\":\"chaiok-ecr\",\"version\":1,\"type\":\"payment\",\"amount\":1.25}".toByteArray())?.amount.toString()) }
    @Test fun commaFallbackValid() { assertEquals("1250.00", PcPaymentCommandParser.parse("1250,00".toByteArray())?.amount.toString()) }
    @Test fun zeroNegativeRejected() { assertNull(PcPaymentCommandParser.parse("0".toByteArray())); assertNull(PcPaymentCommandParser.parse("-1".toByteArray())) }
    @Test fun invalidTextRejected() { assertNull(PcPaymentCommandParser.parse("hello".toByteArray())) }
    @Test fun nullPaddedPayloadValid() { assertEquals("10.00", PcPaymentCommandParser.parse("PAY 10.00\u0000\u0000".toByteArray())?.amount.toString()) }
}
