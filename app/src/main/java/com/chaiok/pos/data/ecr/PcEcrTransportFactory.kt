package com.chaiok.pos.data.ecr

import android.content.Context
import android.util.Log
import com.chaiok.pos.domain.model.PcEcrTransportType

class PcEcrTransportFactory(
    private val context: Context?,
    private val centermMcuDeviceProvider: CentermMcuDeviceProvider =
        CentermMcuDeviceProvider {
            val appContext = context
            if (appContext == null) {
                throw IllegalStateException("Context is required for Centerm transport")
            } else {
                CentermSdkMcuDeviceProvider(appContext).create()
            }
        },
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
            PcEcrTransportType.CENTERM -> CentermMcuTransport(centermMcuDeviceProvider)
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
