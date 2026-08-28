package com.chaiok.pos.domain.model

enum class PcEcrTransportType {
    AUTO,
    KOZEN,
    CENTERM;

    companion object {
        fun fromStorageValue(value: String?): PcEcrTransportType =
            entries.firstOrNull { it.name.equals(value?.trim(), ignoreCase = true) } ?: AUTO
    }
}
