package com.chaiok.pos.domain.model

data class Arcus2NewWaySettings(
    val saleClass: String = "1",
    val saleOp: String = "1",
    val universalReversalClass: String = "1",
    val universalReversalOp: String = "5",
    val refundClass: String = "1",
    val refundOp: String = "11",
    val settlementClass: String = "2",
    val settlementOp: String = "1",
    val pingClass: String = "9",
    val pingOp: String = "6",
    val currencyRubCode: String = "643",
    val currencyAmdCode: String = "051",
    val sendPrintCommands: Boolean = true,
    val sendStartEndPrint: Boolean = true,
    val sendSetTags: Boolean = true,
    val sendStatusMessages: Boolean = true,
    val waitOkTimeoutMs: Long = 5000L,
    val maxReceiptPrintBlockBytes: Int = 500,
    val enableRawArcus2Log: Boolean = true,
    val declinedDefaultRc: String = "05",
    val cancelledRc: String = "999",
    val errorRc: String = "999",
    val minimalResultMode: Boolean = true,
    val waitOkAfterEachCommand: Boolean = false,
    val sendBeginTrOnPaymentStart: Boolean = true,
    val sendStatusOnPaymentStart: Boolean = true,
    val paymentStartStatusText: String = "Ожидание карты"
)

data class AppSettings(
    val integrationModeEnabled: Boolean,
    val tableModeEnabled: Boolean,
    val tileBackground: String = "default",
    val pcUsbModeEnabled: Boolean = false,
    val pcIdleImages: List<String> = emptyList(),
    val pcCompactServiceFeeEnabled: Boolean = true,
    val showCustomTipButton: Boolean = true,
    val pcEcrProtocol: PcEcrProtocol = PcEcrProtocol.CHAIOK_JSON,
    val arcus2NewWaySettings: Arcus2NewWaySettings = Arcus2NewWaySettings()
)
