package com.chaiok.pos.data.ecr

import com.chaiok.pos.domain.model.ChaiOkEcrPaymentResultFrame
import org.json.JSONObject

object ChaiOkEcrFrameEncoder {
    fun encodePaymentResultLine(frame: ChaiOkEcrPaymentResultFrame): ByteArray {
        val json = JSONObject()
            .put("proto", frame.proto)
            .put("version", frame.version)
            .put("type", frame.type)
            .put("commandId", frame.commandId)
            .put("orderId", frame.orderId)
            .put("status", frame.status)
            .put("success", frame.success)
            .put("resultCode", frame.resultCode)
            .put("message", frame.message)
            .put("currency", frame.currency)
            .put("billAmount", frame.billAmount)
            .put("tipAmount", frame.tipAmount)
            .put("totalAmount", frame.totalAmount)
            .put("externalTransactionId", frame.externalTransactionId)
            .put("rrn", frame.rrn)
            .put("authCode", frame.authCode)
            .put("terminalId", frame.terminalId)
            .put("createdAt", frame.createdAt)

        frame.receipt?.let {
            json.put("receipt", JSONObject()
                .put("format", it.format)
                .put("encoding", it.encoding)
                .put("text", it.text)
            )
        }
        return (json.toString() + "\n").toByteArray(Charsets.UTF_8)
    }
}
