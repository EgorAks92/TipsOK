package com.chaiok.pos.presentation.navigation

import java.math.BigDecimal
import java.math.RoundingMode
import java.net.URLEncoder

object Routes {
    const val Launch = "launch"
    const val Login = "login"
    const val Home = "home"
    const val Settings = "settings"
    const val SettingsFromPc = "settings_pc"
    const val Status = "status"
    const val Tips = "tips"
    const val Background = "background"
    const val TipSelection = "tip_selection"
    const val TipSelectionWithArg = "tip_selection/{billAmountKopecks}?source={source}&commandId={commandId}&orderId={orderId}&currency={currency}"
    const val CardPresenting = "card_presenting"
    const val PcCompactTipPayment = "pc_compact_tip_payment/{billAmountKopecks}?commandId={commandId}&orderId={orderId}&currency={currency}&sourceProtocol={sourceProtocol}&operationType={operationType}&rrn={rrn}"
    const val PcCommandIdle = "pc_command_idle"
    const val PcIdleImages = "pc_idle_images"

    fun tipSelectionFromNormal(billAmount: Double): String {
        val kopecks = amountToMinorUnits(BigDecimal.valueOf(billAmount), "RUB")
        return "tip_selection/$kopecks?source=normal&commandId=&orderId=&currency=RUB"
    }

    fun tipSelectionFromPc(amount: BigDecimal, commandId: String?, orderId: String?, currency: String?): String {
        val normalizedCurrency = currency?.trim()?.uppercase().orEmpty().ifBlank { "RUB" }
        val kopecks = amountToMinorUnits(amount, normalizedCurrency)
        return "tip_selection/$kopecks?source=pc_usb&commandId=${enc(commandId)}&orderId=${enc(orderId)}&currency=${enc(normalizedCurrency)}"
    }

    fun pcCompactTipPaymentFromPc(amount: BigDecimal, commandId: String?, orderId: String?, currency: String?, sourceProtocol: String, operationType: String, rrn: String?): String {
        val normalizedCurrency = currency?.trim()?.uppercase().orEmpty().ifBlank { "RUB" }
        val kopecks = amountToMinorUnits(amount, normalizedCurrency)
        return "pc_compact_tip_payment/$kopecks?commandId=${enc(commandId)}&orderId=${enc(orderId)}&currency=${enc(normalizedCurrency)}&sourceProtocol=${enc(sourceProtocol)}&operationType=${enc(operationType)}&rrn=${enc(rrn)}"
    }

    private fun amountToMinorUnits(amount: BigDecimal, currency: String?): Long =
        when (currency?.uppercase()) {
            "AMD" -> amount.setScale(0, RoundingMode.HALF_UP).longValueExact()
            else -> amount.setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValueExact()
        }

    private fun enc(value: String?): String = URLEncoder.encode(value.orEmpty(), Charsets.UTF_8.name())
}
