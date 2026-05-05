package com.chaiok.pos.presentation.navigation

object Routes {
    const val Login = "login"
    const val Home = "home"
    const val Settings = "settings"
    const val Status = "status"
    const val Tips = "tips"
    const val Background = "background"
    const val TipSelection = "tip_selection"
    const val TipSelectionWithArg = "tip_selection/{billAmountRub}"
    const val CardPresenting = "card_presenting"

    fun tipSelection(billAmount: Double): String =
        "tip_selection/${billAmount.toInt()}"
}