package com.chaiok.pos.data.ecr

import android.content.Context
import com.chaiok.pos.domain.model.PcEcrTransportType
import android.util.Log

fun interface CentermSerialPortDeviceProvider {
    fun create(): CentermSerialPortDevice?
}

class PcEcrTransportFactory(
    private val context: Context?,
    private val centermSerialPortDeviceProvider: CentermSerialPortDeviceProvider = CentermSerialPortDeviceProvider { null /* TODO: connect RealCentermSerialPortDeviceAdapter when Centerm SDK SerialPortDevice dependency/class name is available. */ },
    private val deviceInfoProvider: DeviceInfoProvider = AndroidBuildDeviceInfoProvider,
    private val kozenTransportProvider: () -> PcEcrTransport = { XchengWireEcrPortClient(requireNotNull(context) { "Context is required for Kozen transport" }) }
) {
    fun create(requestedType: PcEcrTransportType = PcEcrTransportType.AUTO): PcEcrTransport {
        val resolvedType = resolveType(requestedType)
        Log.i(
            TAG,
            "selected ECR transport requested=$requestedType resolved=$resolvedType manufacturer=${deviceInfoProvider.manufacturer} brand=${deviceInfoProvider.brand} model=${deviceInfoProvider.model}"
        )
        return when (resolvedType) {
            PcEcrTransportType.CENTERM -> {
                val device = centermSerialPortDeviceProvider.create()
                if (device == null) {
                    Log.w(TAG, "requested $requestedType resolved $resolvedType but Centerm provider is unavailable; fallback to Kozen")
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
        val deviceText = listOf(deviceInfoProvider.manufacturer, deviceInfoProvider.brand, deviceInfoProvider.model).joinToString(" ")
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
