package com.chaiok.pos.presentation.navigation

import java.math.BigDecimal
import java.math.RoundingMode
import java.net.URLEncoder

object Routes {
    const val Login = "login"
    const val Home = "home"
    const val Settings = "settings"
    const val SettingsFromPc = "settings_pc"
    const val Status = "status"
    const val Tips = "tips"
    const val Background = "background"
    const val TipSelection = "tip_selection"
    const val TipSelectionWithArg = "tip_selection/{billAmountKopecks}?source={source}&commandId={commandId}&orderId={orderId}"
    const val CardPresenting = "card_presenting"
    const val PcCommandIdle = "pc_command_idle"

    fun tipSelectionFromNormal(billAmount: Double): String {
        val kopecks = BigDecimal.valueOf(billAmount)
            .setScale(2, RoundingMode.HALF_UP)
            .movePointRight(2)
            .longValueExact()
        return "tip_selection/$kopecks?source=normal&commandId=&orderId="
    }

    fun tipSelectionFromPc(amount: BigDecimal, commandId: String?, orderId: String?): String {
        val kopecks = amount.setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValueExact()
        return "tip_selection/$kopecks?source=pc_usb&commandId=${enc(commandId)}&orderId=${enc(orderId)}"
    }

    private fun enc(value: String?): String = URLEncoder.encode(value.orEmpty(), Charsets.UTF_8.name())
}
