package com.chaiok.pos.data.ecr

import android.content.Context
import android.os.Build
import android.util.Log

fun interface CentermSerialPortDeviceProvider {
    fun create(): CentermSerialPortDevice?
}

enum class PcEcrTransportType {
    AUTO,
    KOZEN,
    CENTERM
}

class PcEcrTransportFactory(
    private val context: Context?,
    private val centermSerialPortDeviceProvider: CentermSerialPortDeviceProvider = CentermSerialPortDeviceProvider { null },
    private val kozenTransportProvider: () -> PcEcrTransport = { XchengWireEcrPortClient(requireNotNull(context) { "Context is required for Kozen transport" }) }
) {
    fun create(requestedType: PcEcrTransportType = PcEcrTransportType.AUTO): PcEcrTransport {
        val resolvedType = resolveType(requestedType)
        Log.i(
            TAG,
            "selected ECR transport requested=$requestedType resolved=$resolvedType manufacturer=${Build.MANUFACTURER} brand=${Build.BRAND} model=${Build.MODEL}"
        )
        return when (resolvedType) {
            PcEcrTransportType.CENTERM -> {
                val device = centermSerialPortDeviceProvider.create()
                if (device == null) {
                    Log.w(TAG, "Centerm device provider returned null; fallback to Kozen transport")
                    kozenTransportProvider()
                } else {
                    CentermSerialPortTransport(device)
                }
            }
            PcEcrTransportType.KOZEN,
            PcEcrTransportType.AUTO -> kozenTransportProvider()
        }
    }

    private fun resolveType(requestedType: PcEcrTransportType): PcEcrTransportType {
        if (requestedType != PcEcrTransportType.AUTO) return requestedType
        val deviceText = listOf(Build.MANUFACTURER, Build.BRAND, Build.MODEL).joinToString(" ")
        return when {
            deviceText.contains("centerm", ignoreCase = true) -> PcEcrTransportType.CENTERM
            deviceText.contains("kozen", ignoreCase = true) ||
                    deviceText.contains("xcheng", ignoreCase = true) -> PcEcrTransportType.KOZEN
            else -> PcEcrTransportType.KOZEN
        }
    }

    companion object {
        private const val TAG = "PcEcrTransportFactory"
    }
}
