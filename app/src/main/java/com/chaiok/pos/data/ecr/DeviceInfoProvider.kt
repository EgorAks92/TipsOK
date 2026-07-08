package com.chaiok.pos.data.ecr

import android.os.Build

interface DeviceInfoProvider {
    val manufacturer: String
    val brand: String
    val model: String
}

object AndroidBuildDeviceInfoProvider : DeviceInfoProvider {
    override val manufacturer: String get() = Build.MANUFACTURER.orEmpty()
    override val brand: String get() = Build.BRAND.orEmpty()
    override val model: String get() = Build.MODEL.orEmpty()
}
