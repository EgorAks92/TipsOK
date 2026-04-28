package com.chaiok.pos.domain.model

data class AppSettings(
    val integrationModeEnabled: Boolean,
    val tableModeEnabled: Boolean,
    val selectedTileBackground: String
)
