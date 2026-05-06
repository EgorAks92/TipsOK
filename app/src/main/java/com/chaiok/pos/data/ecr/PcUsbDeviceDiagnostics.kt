package com.chaiok.pos.data.ecr

import java.io.File

object PcUsbDeviceDiagnostics {

    fun collectSummary(): String {
        val ttyDevices = listTtyDevices()
        val usbInfo = listUsbSysInfo()

        return buildString {
            appendLine("TTY devices:")
            if (ttyDevices.isEmpty()) {
                appendLine("none")
            } else {
                ttyDevices.forEach { appendLine(it) }
            }

            appendLine()
            appendLine("USB sys info:")
            if (usbInfo.isEmpty()) {
                appendLine("none")
            } else {
                usbInfo.forEach { appendLine(it) }
            }
        }.trim()
    }

    private fun listTtyDevices(): List<String> {
        return runCatching {
            File("/dev")
                .listFiles()
                .orEmpty()
                .filter { file ->
                    val name = file.name
                    name.startsWith("ttyACM") ||
                            name.startsWith("ttyUSB") ||
                            name.startsWith("ttyGS")
                }
                .sortedBy { it.name }
                .map { file ->
                    "${file.absolutePath} read=${file.canRead()} write=${file.canWrite()} exists=${file.exists()}"
                }
        }.getOrDefault(emptyList())
    }

    private fun listUsbSysInfo(): List<String> {
        val paths = listOf(
            "/sys/class/android_usb/android0/state",
            "/sys/class/android_usb/android0/functions",
            "/sys/class/android_usb/android0/enable",
            "/config/usb_gadget/g1/UDC",
            "/config/usb_gadget/g1/functions"
        )

        return paths.mapNotNull { path ->
            val file = File(path)

            runCatching {
                if (!file.exists()) {
                    "$path = <missing>"
                } else if (file.isDirectory) {
                    "$path = ${file.listFiles().orEmpty().joinToString { it.name }}"
                } else {
                    "$path = ${file.readText().trim()}"
                }
            }.getOrNull()
        }
    }
}