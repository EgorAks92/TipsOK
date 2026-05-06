package com.chaiok.pos.data.ecr

import android.content.Context
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PcUsbEcrFileLogger(context: Context) {

    private val appContext = context.applicationContext
    private val logFile: File = File(appContext.filesDir, FILE_NAME)

    fun i(message: String) {
        write(level = "I", message = message, throwable = null)
        Log.i(TAG, message)
    }

    fun w(message: String, throwable: Throwable? = null) {
        write(level = "W", message = message, throwable = throwable)
        Log.w(TAG, message, throwable)
    }

    fun e(message: String, throwable: Throwable? = null) {
        write(level = "E", message = message, throwable = throwable)
        Log.e(TAG, message, throwable)
    }

    fun clear() {
        runCatching {
            if (logFile.exists()) {
                logFile.delete()
            }
        }
    }

    fun readText(): String {
        return runCatching {
            if (logFile.exists()) {
                logFile.readText()
            } else {
                ""
            }
        }.getOrDefault("")
    }

    @Synchronized
    private fun write(
        level: String,
        message: String,
        throwable: Throwable?
    ) {
        runCatching {
            rotateIfNeeded()

            val timestamp = DATE_FORMAT.format(Date())

            val text = buildString {
                append(timestamp)
                append(" ")
                append(level)
                append(" ")
                append(message)
                append("\n")

                if (throwable != null) {
                    append(Log.getStackTraceString(throwable))
                    append("\n")
                }
            }

            logFile.appendText(text)
        }
    }

    private fun rotateIfNeeded() {
        if (logFile.exists() && logFile.length() > MAX_LOG_SIZE_BYTES) {
            logFile.writeText("")
        }
    }

    private companion object {
        private const val TAG = "PcUsbEcrFlow"
        private const val FILE_NAME = "pc_usb_ecr.log"
        private const val MAX_LOG_SIZE_BYTES = 512 * 1024

        private val DATE_FORMAT = SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss.SSS",
            Locale.US
        )
    }
}