package com.chaiok.pos.domain.model

data class AppSettings(
    val integrationModeEnabled: Boolean,
    val tableModeEnabled: Boolean,
    val tileBackground: String = "default",
    val pcUsbModeEnabled: Boolean = false,
    val pcIdleImages: List<String> = emptyList()
)
