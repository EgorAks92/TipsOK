package com.chaiok.pos.data.storage

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.chaiok.pos.domain.model.Arcus2NewWaySettings
import com.chaiok.pos.domain.model.PcCompactPaymentDesignStyle
import com.chaiok.pos.domain.model.PcEcrProtocol
import com.chaiok.pos.domain.model.TipRange
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "chaiok_store")

class AppDataStore(private val context: Context) {
    private object Keys {
        val integrationMode = booleanPreferencesKey("integration_mode")
        val tableMode = booleanPreferencesKey("table_mode")
        val tileBackground = stringPreferencesKey("tile_background")
        val waiterStatus = stringPreferencesKey("waiter_status")
        val hasLinkedCard = booleanPreferencesKey("has_linked_card")
        val cardSha = stringPreferencesKey("card_sha")
        val tipRangePercents = stringPreferencesKey("tip_range_percents")
        val tipRangeStart = intPreferencesKey("tip_range_start")
        val tipRangeFinish = intPreferencesKey("tip_range_finish")
        val tipRangeDefaultIndex = intPreferencesKey("tip_range_default_index")
        val serviceFeePercent = doublePreferencesKey("service_fee_percent")
        val pcUsbMode = booleanPreferencesKey("pc_usb_mode_enabled")
        val pcIdleImages = stringPreferencesKey("pc_idle_images")
        val pcCompactServiceFeeEnabled = booleanPreferencesKey("pc_compact_service_fee_enabled")
        val showCustomTipButton = booleanPreferencesKey("show_custom_tip_button")
        val pcCompactPaymentDesignStyle = stringPreferencesKey("pc_compact_payment_design_style")
        val pcEcrProtocol = stringPreferencesKey("pc_ecr_protocol")

        val arcus2SaleClass = stringPreferencesKey("arcus2_sale_class")
        val arcus2SaleOp = stringPreferencesKey("arcus2_sale_op")
        val arcus2ReversalClass = stringPreferencesKey("arcus2_reversal_class")
        val arcus2ReversalOp = stringPreferencesKey("arcus2_reversal_op")
        val arcus2RefundClass = stringPreferencesKey("arcus2_refund_class")
        val arcus2RefundOp = stringPreferencesKey("arcus2_refund_op")
        val arcus2SettlementClass = stringPreferencesKey("arcus2_settlement_class")
        val arcus2SettlementOp = stringPreferencesKey("arcus2_settlement_op")
        val arcus2PingClass = stringPreferencesKey("arcus2_ping_class")
        val arcus2PingOp = stringPreferencesKey("arcus2_ping_op")
        val arcus2CurrencyRub = stringPreferencesKey("arcus2_currency_rub_code")
        val arcus2CurrencyAmd = stringPreferencesKey("arcus2_currency_amd_code")
        val arcus2SendPrint = booleanPreferencesKey("arcus2_send_print_commands")
        val arcus2SendStartEndPrint = booleanPreferencesKey("arcus2_send_start_end_print")
        val arcus2SendSetTags = booleanPreferencesKey("arcus2_send_set_tags")
        val arcus2SendStatusMessages = booleanPreferencesKey("arcus2_send_status_messages")
        val arcus2WaitOkTimeoutMs = longPreferencesKey("arcus2_wait_ok_timeout_ms")
        val arcus2MaxReceiptPrintBlockBytes = intPreferencesKey("arcus2_max_receipt_print_block_bytes")
        val arcus2EnableRawLog = booleanPreferencesKey("arcus2_enable_raw_log")
        val arcus2DeclinedDefaultRc = stringPreferencesKey("arcus2_declined_default_rc")
        val arcus2CancelledRc = stringPreferencesKey("arcus2_cancelled_rc")
        val arcus2ErrorRc = stringPreferencesKey("arcus2_error_rc")
        val arcus2MinimalResultMode = booleanPreferencesKey("arcus2_minimal_result_mode")
        val arcus2WaitOkAfterEachCommand = booleanPreferencesKey("arcus2_wait_ok_after_each_command")
        val arcus2SendReceiptInMinimalMode = booleanPreferencesKey("arcus2_send_receipt_in_minimal_mode")
        val arcus2UsePrintSessionMarkersInMinimalMode = booleanPreferencesKey("arcus2_use_print_session_markers_in_minimal_mode")
        val arcus2DrainOkAfterCommandMs = longPreferencesKey("arcus2_drain_ok_after_command_ms")
        val arcus2SendBeginTrOnPaymentStart = booleanPreferencesKey("arcus2_send_begin_tr_on_payment_start")
        val arcus2SendStatusOnPaymentStart = booleanPreferencesKey("arcus2_send_status_on_payment_start")
        val arcus2PaymentStartStatusText = stringPreferencesKey("arcus2_payment_start_status_text")
        val arcus2PaymentStatusKeepAliveEnabled = booleanPreferencesKey("arcus2_payment_status_keep_alive_enabled")
        val arcus2CancelStatusKeepAliveEnabled = booleanPreferencesKey("arcus2_cancel_status_keep_alive_enabled")
        val arcus2PaymentStatusKeepAliveIntervalMs = longPreferencesKey("arcus2_payment_status_keep_alive_interval_ms")
        val arcus2CardWaitingStatusText = stringPreferencesKey("arcus2_card_waiting_status_text")
        val arcus2CardDetectedStatusText = stringPreferencesKey("arcus2_card_detected_status_text")
        val arcus2ProcessingStatusText = stringPreferencesKey("arcus2_processing_status_text")
        val arcus2PinRequiredStatusText = stringPreferencesKey("arcus2_pin_required_status_text")
        val arcus2CancellingStatusText = stringPreferencesKey("arcus2_cancelling_status_text")
        val arcus2AdditionalDataRequestEnabled = booleanPreferencesKey("arcus2_additional_data_request_enabled")
        val arcus2AdditionalDataRequestCommand = stringPreferencesKey("arcus2_additional_data_request_command")
        val arcus2AdditionalDataReadTimeoutMs = longPreferencesKey("arcus2_additional_data_read_timeout_ms")
        val arcus2AdditionalDataTotalTimeoutMs = longPreferencesKey("arcus2_additional_data_total_timeout_ms")
        val arcus2AdditionalDataMaxFrames = intPreferencesKey("arcus2_additional_data_max_frames")
        val arcus2AdditionalDataGetTagsResponseMode = stringPreferencesKey("arcus2_additional_data_gettags_response_mode")
        val arcus2FinalStepTimeoutMs = longPreferencesKey("arcus2_final_step_timeout_ms")
        val arcus2SendStatusOnCancelStart = booleanPreferencesKey("arcus2_send_status_on_cancel_start")
        val arcus2SaleAdditionalDataEnabled = booleanPreferencesKey("arcus2_sale_additional_data_enabled")
        val arcus2ReversalAdditionalDataEnabled = booleanPreferencesKey("arcus2_reversal_additional_data_enabled")
        val arcus2RefundAdditionalDataEnabled = booleanPreferencesKey("arcus2_refund_additional_data_enabled")
        val arcus2SettlementAdditionalDataEnabled = booleanPreferencesKey("arcus2_settlement_additional_data_enabled")
        val arcus2RrnTagKeysCsv = stringPreferencesKey("arcus2_rrn_tag_keys_csv")
        val arcus2AmountTagKeysCsv = stringPreferencesKey("arcus2_amount_tag_keys_csv")
        val arcus2CurrencyTagKeysCsv = stringPreferencesKey("arcus2_currency_tag_keys_csv")
        val arcus2OrderIdTagKeysCsv = stringPreferencesKey("arcus2_order_id_tag_keys_csv")
        val arcus2ReceiptNumberTagKeysCsv = stringPreferencesKey("arcus2_receipt_number_tag_keys_csv")
        val arcus2AuthCodeTagKeysCsv = stringPreferencesKey("arcus2_auth_code_tag_keys_csv")
        val arcus2RrnOwTagIdsCsv = stringPreferencesKey("arcus2_rrn_ow_tag_ids_csv")
        val arcus2AmountOwTagIdsCsv = stringPreferencesKey("arcus2_amount_ow_tag_ids_csv")
        val arcus2CurrencyOwTagIdsCsv = stringPreferencesKey("arcus2_currency_ow_tag_ids_csv")
        val arcus2TerminalIdOwTagIdsCsv = stringPreferencesKey("arcus2_terminal_id_ow_tag_ids_csv")
        val arcus2ResponseCodeOwTagIdsCsv = stringPreferencesKey("arcus2_response_code_ow_tag_ids_csv")
    }

    val integrationModeFlow: Flow<Boolean> = context.dataStore.data.map { it[Keys.integrationMode] ?: false }
    val tableModeFlow: Flow<Boolean> = context.dataStore.data.map { it[Keys.tableMode] ?: false }
    val tileBackgroundFlow: Flow<String> = context.dataStore.data.map { it[Keys.tileBackground] ?: "default" }
    val waiterStatusFlow: Flow<String> = context.dataStore.data.map { it[Keys.waiterStatus] ?: "На смене" }
    val hasLinkedCardFlow: Flow<Boolean> = context.dataStore.data.map { it[Keys.hasLinkedCard] ?: false }
    val cardShaFlow: Flow<String?> = context.dataStore.data.map { it[Keys.cardSha] }
    val serviceFeePercentFlow: Flow<Double> = context.dataStore.data.map { it[Keys.serviceFeePercent] ?: 0.0 }
    val pcUsbModeFlow: Flow<Boolean> = context.dataStore.data.map { it[Keys.pcUsbMode] ?: false }
    val pcIdleImagesFlow: Flow<List<String>> = context.dataStore.data.map { prefs -> prefs[Keys.pcIdleImages]?.split(PC_IDLE_IMAGES_SEPARATOR)?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList() }
    val pcCompactServiceFeeEnabledFlow: Flow<Boolean> = context.dataStore.data.map { it[Keys.pcCompactServiceFeeEnabled] ?: true }
    val showCustomTipButtonFlow: Flow<Boolean> = context.dataStore.data.map { it[Keys.showCustomTipButton] ?: true }
    val pcCompactPaymentDesignStyleFlow: Flow<PcCompactPaymentDesignStyle> = context.dataStore.data.map { prefs ->
        prefs[Keys.pcCompactPaymentDesignStyle]
            ?.let { runCatching { PcCompactPaymentDesignStyle.valueOf(it) }.getOrNull() }
            ?: PcCompactPaymentDesignStyle.DEFAULT
    }
    val pcEcrProtocolFlow: Flow<PcEcrProtocol> = context.dataStore.data.map { it[Keys.pcEcrProtocol]?.let { v -> runCatching { PcEcrProtocol.valueOf(v) }.getOrNull() } ?: PcEcrProtocol.CHAIOK_JSON }

    val arcus2NewWaySettingsFlow: Flow<Arcus2NewWaySettings> = context.dataStore.data.map { p ->
        val d = Arcus2NewWaySettings()
        Arcus2NewWaySettings(
            saleClass = p[Keys.arcus2SaleClass] ?: d.saleClass,
            saleOp = p[Keys.arcus2SaleOp] ?: d.saleOp,
            universalReversalClass = p[Keys.arcus2ReversalClass] ?: d.universalReversalClass,
            universalReversalOp = p[Keys.arcus2ReversalOp] ?: d.universalReversalOp,
            refundClass = p[Keys.arcus2RefundClass] ?: d.refundClass,
            refundOp = p[Keys.arcus2RefundOp] ?: d.refundOp,
            settlementClass = p[Keys.arcus2SettlementClass] ?: d.settlementClass,
            settlementOp = p[Keys.arcus2SettlementOp] ?: d.settlementOp,
            pingClass = p[Keys.arcus2PingClass] ?: d.pingClass,
            pingOp = p[Keys.arcus2PingOp] ?: d.pingOp,
            currencyRubCode = p[Keys.arcus2CurrencyRub] ?: d.currencyRubCode,
            currencyAmdCode = p[Keys.arcus2CurrencyAmd] ?: d.currencyAmdCode,
            sendPrintCommands = p[Keys.arcus2SendPrint] ?: d.sendPrintCommands,
            sendStartEndPrint = p[Keys.arcus2SendStartEndPrint] ?: d.sendStartEndPrint,
            sendSetTags = p[Keys.arcus2SendSetTags] ?: d.sendSetTags,
            sendStatusMessages = p[Keys.arcus2SendStatusMessages] ?: d.sendStatusMessages,
            waitOkTimeoutMs = p[Keys.arcus2WaitOkTimeoutMs] ?: d.waitOkTimeoutMs,
            maxReceiptPrintBlockBytes = p[Keys.arcus2MaxReceiptPrintBlockBytes] ?: d.maxReceiptPrintBlockBytes,
            enableRawArcus2Log = p[Keys.arcus2EnableRawLog] ?: d.enableRawArcus2Log,
            declinedDefaultRc = p[Keys.arcus2DeclinedDefaultRc] ?: d.declinedDefaultRc,
            cancelledRc = p[Keys.arcus2CancelledRc] ?: d.cancelledRc,
            errorRc = p[Keys.arcus2ErrorRc] ?: d.errorRc,
            minimalResultMode = p[Keys.arcus2MinimalResultMode] ?: d.minimalResultMode,
            waitOkAfterEachCommand = p[Keys.arcus2WaitOkAfterEachCommand] ?: d.waitOkAfterEachCommand,
            sendReceiptInMinimalMode = p[Keys.arcus2SendReceiptInMinimalMode] ?: d.sendReceiptInMinimalMode,
            usePrintSessionMarkersInMinimalMode = p[Keys.arcus2UsePrintSessionMarkersInMinimalMode] ?: d.usePrintSessionMarkersInMinimalMode,
            drainOkAfterCommandMs = p[Keys.arcus2DrainOkAfterCommandMs] ?: d.drainOkAfterCommandMs,
            sendBeginTrOnPaymentStart = p[Keys.arcus2SendBeginTrOnPaymentStart] ?: d.sendBeginTrOnPaymentStart,
            sendStatusOnPaymentStart = p[Keys.arcus2SendStatusOnPaymentStart] ?: d.sendStatusOnPaymentStart,
            paymentStartStatusText = p[Keys.arcus2PaymentStartStatusText] ?: d.paymentStartStatusText,
            paymentStatusKeepAliveEnabled = p[Keys.arcus2PaymentStatusKeepAliveEnabled] ?: d.paymentStatusKeepAliveEnabled,
            cancelStatusKeepAliveEnabled = p[Keys.arcus2CancelStatusKeepAliveEnabled] ?: d.cancelStatusKeepAliveEnabled,
            paymentStatusKeepAliveIntervalMs = p[Keys.arcus2PaymentStatusKeepAliveIntervalMs] ?: d.paymentStatusKeepAliveIntervalMs,
            cardWaitingStatusText = p[Keys.arcus2CardWaitingStatusText] ?: d.cardWaitingStatusText,
            cardDetectedStatusText = p[Keys.arcus2CardDetectedStatusText] ?: d.cardDetectedStatusText,
            processingStatusText = p[Keys.arcus2ProcessingStatusText] ?: d.processingStatusText,
            pinRequiredStatusText = p[Keys.arcus2PinRequiredStatusText] ?: d.pinRequiredStatusText,
            cancellingStatusText = p[Keys.arcus2CancellingStatusText] ?: d.cancellingStatusText,
            additionalDataRequestEnabled = p[Keys.arcus2AdditionalDataRequestEnabled] ?: d.additionalDataRequestEnabled,
            additionalDataRequestCommand = p[Keys.arcus2AdditionalDataRequestCommand] ?: d.additionalDataRequestCommand,
            additionalDataReadTimeoutMs = p[Keys.arcus2AdditionalDataReadTimeoutMs] ?: d.additionalDataReadTimeoutMs,
            additionalDataTotalTimeoutMs = p[Keys.arcus2AdditionalDataTotalTimeoutMs] ?: d.additionalDataTotalTimeoutMs,
            additionalDataMaxFrames = p[Keys.arcus2AdditionalDataMaxFrames] ?: d.additionalDataMaxFrames,
            additionalDataGetTagsResponseMode = p[Keys.arcus2AdditionalDataGetTagsResponseMode] ?: d.additionalDataGetTagsResponseMode,
            arcus2FinalStepTimeoutMs = p[Keys.arcus2FinalStepTimeoutMs] ?: d.arcus2FinalStepTimeoutMs,
            sendStatusOnCancelStart = p[Keys.arcus2SendStatusOnCancelStart] ?: d.sendStatusOnCancelStart,
            saleAdditionalDataEnabled = p[Keys.arcus2SaleAdditionalDataEnabled] ?: d.saleAdditionalDataEnabled,
            reversalAdditionalDataEnabled = p[Keys.arcus2ReversalAdditionalDataEnabled] ?: d.reversalAdditionalDataEnabled,
            refundAdditionalDataEnabled = p[Keys.arcus2RefundAdditionalDataEnabled] ?: d.refundAdditionalDataEnabled,
            settlementAdditionalDataEnabled = p[Keys.arcus2SettlementAdditionalDataEnabled] ?: d.settlementAdditionalDataEnabled,
            rrnTagKeysCsv = p[Keys.arcus2RrnTagKeysCsv] ?: d.rrnTagKeysCsv,
            amountTagKeysCsv = p[Keys.arcus2AmountTagKeysCsv] ?: d.amountTagKeysCsv,
            currencyTagKeysCsv = p[Keys.arcus2CurrencyTagKeysCsv] ?: d.currencyTagKeysCsv,
            orderIdTagKeysCsv = p[Keys.arcus2OrderIdTagKeysCsv] ?: d.orderIdTagKeysCsv,
            receiptNumberTagKeysCsv = p[Keys.arcus2ReceiptNumberTagKeysCsv] ?: d.receiptNumberTagKeysCsv,
            authCodeTagKeysCsv = p[Keys.arcus2AuthCodeTagKeysCsv] ?: d.authCodeTagKeysCsv,
            rrnOwTagIdsCsv = p[Keys.arcus2RrnOwTagIdsCsv] ?: d.rrnOwTagIdsCsv,
            amountOwTagIdsCsv = p[Keys.arcus2AmountOwTagIdsCsv] ?: d.amountOwTagIdsCsv,
            currencyOwTagIdsCsv = p[Keys.arcus2CurrencyOwTagIdsCsv] ?: d.currencyOwTagIdsCsv,
            terminalIdOwTagIdsCsv = p[Keys.arcus2TerminalIdOwTagIdsCsv] ?: d.terminalIdOwTagIdsCsv,
            responseCodeOwTagIdsCsv = p[Keys.arcus2ResponseCodeOwTagIdsCsv] ?: d.responseCodeOwTagIdsCsv
        )
    }

    val tipRangeFlow: Flow<TipRange?> = context.dataStore.data.map { prefs ->
        val percentsRaw = prefs[Keys.tipRangePercents]?.takeIf { it.isNotBlank() } ?: return@map null
        val percents = percentsRaw.split(",").mapNotNull { it.toDoubleOrNull() }
        if (percents.isEmpty()) return@map null
        TipRange(percents = percents, startRange = prefs[Keys.tipRangeStart] ?: 0, finishRange = prefs[Keys.tipRangeFinish] ?: 0, defaultIndex = prefs[Keys.tipRangeDefaultIndex] ?: 0)
    }

    suspend fun setIntegrationMode(value: Boolean) = context.dataStore.edit { it[Keys.integrationMode] = value }
    suspend fun setTableMode(value: Boolean) = context.dataStore.edit { it[Keys.tableMode] = value }
    suspend fun setTileBackground(value: String) = context.dataStore.edit { it[Keys.tileBackground] = value }
    suspend fun setWaiterStatus(value: String) = context.dataStore.edit { it[Keys.waiterStatus] = value }
    suspend fun setHasLinkedCard(value: Boolean) = context.dataStore.edit { it[Keys.hasLinkedCard] = value }
    suspend fun setCardSha(value: String) = context.dataStore.edit { it[Keys.cardSha] = value }
    suspend fun setServiceFeePercent(value: Double) = context.dataStore.edit { it[Keys.serviceFeePercent] = value.coerceAtLeast(0.0) }
    suspend fun setPcUsbMode(value: Boolean) = context.dataStore.edit { it[Keys.pcUsbMode] = value }
    suspend fun setPcIdleImages(images: List<String>) = context.dataStore.edit { it[Keys.pcIdleImages] = images.map { i -> i.trim() }.filter { it.isNotBlank() }.joinToString(PC_IDLE_IMAGES_SEPARATOR) }
    suspend fun setPcCompactServiceFeeEnabled(value: Boolean) = context.dataStore.edit { it[Keys.pcCompactServiceFeeEnabled] = value }
    suspend fun setShowCustomTipButton(value: Boolean) = context.dataStore.edit { it[Keys.showCustomTipButton] = value }
    suspend fun setPcCompactPaymentDesignStyle(value: PcCompactPaymentDesignStyle) = context.dataStore.edit { it[Keys.pcCompactPaymentDesignStyle] = value.name }
    suspend fun setPcEcrProtocol(value: PcEcrProtocol) = context.dataStore.edit { it[Keys.pcEcrProtocol] = value.name }
    suspend fun setArcus2NewWaySettings(value: Arcus2NewWaySettings) = context.dataStore.edit {
        it[Keys.arcus2SaleClass] = value.saleClass; it[Keys.arcus2SaleOp] = value.saleOp
        it[Keys.arcus2ReversalClass] = value.universalReversalClass; it[Keys.arcus2ReversalOp] = value.universalReversalOp
        it[Keys.arcus2RefundClass] = value.refundClass; it[Keys.arcus2RefundOp] = value.refundOp
        it[Keys.arcus2SettlementClass] = value.settlementClass; it[Keys.arcus2SettlementOp] = value.settlementOp
        it[Keys.arcus2PingClass] = value.pingClass; it[Keys.arcus2PingOp] = value.pingOp
        it[Keys.arcus2CurrencyRub] = value.currencyRubCode; it[Keys.arcus2CurrencyAmd] = value.currencyAmdCode
        it[Keys.arcus2SendPrint] = value.sendPrintCommands; it[Keys.arcus2SendStartEndPrint] = value.sendStartEndPrint
        it[Keys.arcus2SendSetTags] = value.sendSetTags; it[Keys.arcus2SendStatusMessages] = value.sendStatusMessages
        it[Keys.arcus2WaitOkTimeoutMs] = value.waitOkTimeoutMs; it[Keys.arcus2MaxReceiptPrintBlockBytes] = value.maxReceiptPrintBlockBytes
        it[Keys.arcus2EnableRawLog] = value.enableRawArcus2Log
        it[Keys.arcus2DeclinedDefaultRc] = value.declinedDefaultRc; it[Keys.arcus2CancelledRc] = value.cancelledRc; it[Keys.arcus2ErrorRc] = value.errorRc
        it[Keys.arcus2MinimalResultMode] = value.minimalResultMode; it[Keys.arcus2WaitOkAfterEachCommand] = value.waitOkAfterEachCommand
        it[Keys.arcus2SendReceiptInMinimalMode] = value.sendReceiptInMinimalMode; it[Keys.arcus2UsePrintSessionMarkersInMinimalMode] = value.usePrintSessionMarkersInMinimalMode
        it[Keys.arcus2DrainOkAfterCommandMs] = value.drainOkAfterCommandMs
        it[Keys.arcus2SendBeginTrOnPaymentStart] = value.sendBeginTrOnPaymentStart; it[Keys.arcus2SendStatusOnPaymentStart] = value.sendStatusOnPaymentStart
        it[Keys.arcus2PaymentStartStatusText] = value.paymentStartStatusText
        it[Keys.arcus2PaymentStatusKeepAliveEnabled] = value.paymentStatusKeepAliveEnabled
        it[Keys.arcus2CancelStatusKeepAliveEnabled] = value.cancelStatusKeepAliveEnabled
        it[Keys.arcus2PaymentStatusKeepAliveIntervalMs] = value.paymentStatusKeepAliveIntervalMs
        it[Keys.arcus2CardWaitingStatusText] = value.cardWaitingStatusText
        it[Keys.arcus2CardDetectedStatusText] = value.cardDetectedStatusText
        it[Keys.arcus2ProcessingStatusText] = value.processingStatusText
        it[Keys.arcus2PinRequiredStatusText] = value.pinRequiredStatusText
        it[Keys.arcus2CancellingStatusText] = value.cancellingStatusText
        it[Keys.arcus2AdditionalDataRequestEnabled] = value.additionalDataRequestEnabled
        it[Keys.arcus2AdditionalDataRequestCommand] = value.additionalDataRequestCommand
        it[Keys.arcus2AdditionalDataReadTimeoutMs] = value.additionalDataReadTimeoutMs
        it[Keys.arcus2AdditionalDataTotalTimeoutMs] = value.additionalDataTotalTimeoutMs
        it[Keys.arcus2AdditionalDataMaxFrames] = value.additionalDataMaxFrames
        it[Keys.arcus2AdditionalDataGetTagsResponseMode] = value.additionalDataGetTagsResponseMode
        it[Keys.arcus2FinalStepTimeoutMs] = value.arcus2FinalStepTimeoutMs
        it[Keys.arcus2SendStatusOnCancelStart] = value.sendStatusOnCancelStart
        it[Keys.arcus2SaleAdditionalDataEnabled] = value.saleAdditionalDataEnabled
        it[Keys.arcus2ReversalAdditionalDataEnabled] = value.reversalAdditionalDataEnabled
        it[Keys.arcus2RefundAdditionalDataEnabled] = value.refundAdditionalDataEnabled
        it[Keys.arcus2SettlementAdditionalDataEnabled] = value.settlementAdditionalDataEnabled
        it[Keys.arcus2RrnTagKeysCsv] = value.rrnTagKeysCsv
        it[Keys.arcus2AmountTagKeysCsv] = value.amountTagKeysCsv
        it[Keys.arcus2CurrencyTagKeysCsv] = value.currencyTagKeysCsv
        it[Keys.arcus2OrderIdTagKeysCsv] = value.orderIdTagKeysCsv
        it[Keys.arcus2ReceiptNumberTagKeysCsv] = value.receiptNumberTagKeysCsv
        it[Keys.arcus2AuthCodeTagKeysCsv] = value.authCodeTagKeysCsv
        it[Keys.arcus2RrnOwTagIdsCsv] = value.rrnOwTagIdsCsv
        it[Keys.arcus2AmountOwTagIdsCsv] = value.amountOwTagIdsCsv
        it[Keys.arcus2CurrencyOwTagIdsCsv] = value.currencyOwTagIdsCsv
        it[Keys.arcus2TerminalIdOwTagIdsCsv] = value.terminalIdOwTagIdsCsv
        it[Keys.arcus2ResponseCodeOwTagIdsCsv] = value.responseCodeOwTagIdsCsv
    }
    suspend fun setTipRange(value: TipRange) = context.dataStore.edit {
        it[Keys.tipRangePercents] = value.percents.joinToString(",")
        it[Keys.tipRangeStart] = value.startRange
        it[Keys.tipRangeFinish] = value.finishRange
        it[Keys.tipRangeDefaultIndex] = value.defaultIndex
    }
}
private const val PC_IDLE_IMAGES_SEPARATOR = "||"
