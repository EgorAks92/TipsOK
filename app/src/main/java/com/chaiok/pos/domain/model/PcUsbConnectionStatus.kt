package com.chaiok.pos.domain.model

sealed interface PcUsbConnectionStatus {
    data object Idle : PcUsbConnectionStatus

    data object BindingService : PcUsbConnectionStatus
    data object ServiceBound : PcUsbConnectionStatus

    data object OpeningPort : PcUsbConnectionStatus
    data object ConnectingPort : PcUsbConnectionStatus
    data object Connected : PcUsbConnectionStatus

    data object WaitingForData : PcUsbConnectionStatus

    data class Error(val message: String) : PcUsbConnectionStatus
}