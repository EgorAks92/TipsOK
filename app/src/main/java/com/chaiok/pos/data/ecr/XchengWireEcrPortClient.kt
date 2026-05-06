package com.chaiok.pos.data.ecr

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import com.xcheng.wiredecr.IComm
import java.io.File
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

class XchengWireEcrPortClient(context: Context) {

    private val appContext = context.applicationContext

    private var usb: IComm? = null
    private var gs: IComm? = null

    private var usbConn: ServiceConnection? = null
    private var gsConn: ServiceConnection? = null

    private var currentUsbDevice: String? = null

    suspend fun bindService(): Result<Unit> = withContext(Dispatchers.IO) {
        val result = runCatching {
            if (usb != null && (!USE_GS_BRIDGE || gs != null)) {
                Log.i(
                    TAG,
                    "bind skipped: service already bound usbBound=${usb != null} gsBound=${gs != null}"
                )
                return@runCatching Unit
            }

            if (usb == null) {
                val usbDeferred = CompletableDeferred<IComm>()

                usbConn = bind(
                    action = ACTION_USB,
                    deferred = usbDeferred,
                    label = "USB"
                )

                usb = withTimeout(BIND_TIMEOUT_MS) {
                    usbDeferred.await()
                }

                Log.i(TAG, "bind USB success usbBound=${usb != null}")
            }

            if (USE_GS_BRIDGE && gs == null) {
                val gsDeferred = CompletableDeferred<IComm>()

                gsConn = bind(
                    action = ACTION_GS,
                    deferred = gsDeferred,
                    label = "GS"
                )

                gs = withTimeout(BIND_TIMEOUT_MS) {
                    gsDeferred.await()
                }

                Log.i(TAG, "bind GS success gsBound=${gs != null}")
            }

            Unit
        }

        if (result.isFailure) {
            Log.e(TAG, "bindService failed", result.exceptionOrNull())
            closeAll()
        }

        result
    }

    suspend fun openAndConnect(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            Log.i(TAG, "openAndConnect start")
            Log.i(TAG, "USE_GS_BRIDGE=$USE_GS_BRIDGE")
            Log.i(TAG, "ACCEPT_TTY_GS0_WITHOUT_STATUS=$ACCEPT_TTY_GS0_WITHOUT_STATUS")

            val usbComm = usb ?: error("USB service missing")

            if (USE_GS_BRIDGE) {
                prepareGsBridge()
            }

            var lastError: Throwable? = null

            USB_DEVICE_CANDIDATES.forEach { device ->
                try {
                    logCandidateState(device)

                    Log.i(TAG, "open USB port for device=$device")

                    safeClosePort(
                        comm = usbComm,
                        reason = "before probe $device"
                    )

                    delay(OPEN_RESET_DELAY_MS)

                    usbComm.open()

                    delay(OPEN_AFTER_DELAY_MS)

                    runCatching {
                        usbComm.setRecvTimeout(RECV_TIMEOUT_MS)
                    }.onFailure {
                        Log.w(TAG, "setRecvTimeout before connect failed device=$device", it)
                    }

                    Log.i(TAG, "connect probe requested USB=$device")
                    usbComm.connect(device)

                    delay(CONNECT_AFTER_DELAY_MS)

                    val immediateStatus = readConnectStatus(
                        comm = usbComm,
                        device = device,
                        prefix = "immediate status"
                    )

                    if (
                        device == TTY_GS0 &&
                        ACCEPT_TTY_GS0_WITHOUT_STATUS &&
                        File(device).exists() &&
                        File(device).canRead() &&
                        File(device).canWrite()
                    ) {
                        currentUsbDevice = device

                        runCatching {
                            usbComm.setRecvTimeout(RECV_TIMEOUT_MS)
                        }.onFailure {
                            Log.w(TAG, "setRecvTimeout after ttyGS0 accept failed", it)
                        }

                        Log.i(
                            TAG,
                            "USB accepted device=$device without status=2, " +
                                    "demo-compatible ttyGS0 mode, immediateStatus=$immediateStatus"
                        )

                        return@runCatching Unit
                    }

                    if (immediateStatus == CONNECTED_STATUS) {
                        currentUsbDevice = device

                        runCatching {
                            usbComm.setRecvTimeout(RECV_TIMEOUT_MS)
                        }.onFailure {
                            Log.w(TAG, "setRecvTimeout after connect failed device=$device", it)
                        }

                        Log.i(TAG, "USB connected device=$device status=$immediateStatus")
                        return@runCatching Unit
                    }

                    val connected = waitConnected(
                        comm = usbComm,
                        device = device
                    )

                    if (connected) {
                        currentUsbDevice = device

                        runCatching {
                            usbComm.setRecvTimeout(RECV_TIMEOUT_MS)
                        }.onFailure {
                            Log.w(TAG, "setRecvTimeout after poll connect failed device=$device", it)
                        }

                        Log.i(TAG, "USB connected device=$device")
                        return@runCatching Unit
                    }

                    Log.w(
                        TAG,
                        "USB probe failed device=$device lastStatus=${readStatusQuietly(usbComm)}"
                    )

                    safeClosePort(
                        comm = usbComm,
                        reason = "after failed probe $device"
                    )

                    delay(BETWEEN_PROBES_DELAY_MS)
                } catch (throwable: Throwable) {
                    lastError = throwable
                    Log.w(TAG, "USB probe exception device=$device", throwable)

                    safeClosePort(
                        comm = usbComm,
                        reason = "after probe exception $device"
                    )

                    delay(BETWEEN_PROBES_DELAY_MS)
                }
            }

            val diagnostics = buildUsbDiagnostics()

            Log.e(
                TAG,
                "USB connect timeout. None of candidates opened. " +
                        "candidates=${USB_DEVICE_CANDIDATES.joinToString()} " +
                        "lastError=${lastError?.message}\n$diagnostics"
            )

            error(
                "USB ECR порт не найден. Проверьте режим USB/ECR или CDC на терминале, " +
                        "кабель и наличие корректного ECR/CDC COM-порта на ПК/кассе."
            )
        }.onFailure { throwable ->
            Log.e(TAG, "openAndConnect failed", throwable)
        }
    }

    suspend fun receiveOnce(): Result<ByteArray?> = withContext(Dispatchers.IO) {
        runCatching {
            val usbComm = usb ?: error("USB service missing")
            val device = currentUsbDevice ?: "unknown"

            runCatching {
                usbComm.setRecvTimeout(RECV_TIMEOUT_MS)
            }.onFailure {
                Log.w(TAG, "setRecvTimeout before recv failed", it)
            }

            Log.i(TAG, "recv start USB=$device buffer=$RECV_BUFFER_SIZE")

            val bytes = usbComm.recv(RECV_BUFFER_SIZE)

            if (bytes != null && bytes.isNotEmpty()) {
                Log.i(
                    TAG,
                    "recv end bytes=${bytes.size} hex=${bytes.toHexPreview()}"
                )
                return@runCatching bytes
            }

            Log.i(TAG, "recv end empty")
            null
        }.onFailure { throwable ->
            Log.e(TAG, "receiveOnce failed", throwable)
        }
    }

    suspend fun send(bytes: ByteArray): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val usbComm = usb ?: error("USB service missing")

            Log.i(TAG, "send bytes=${bytes.size} hex=${bytes.toHexPreview()}")

            usbComm.send(bytes)

            Unit
        }.onFailure { throwable ->
            Log.e(TAG, "send failed", throwable)
        }
    }

    suspend fun stop() {
        closeAll()
    }

    suspend fun closePortOnly() = withContext(Dispatchers.IO) {
        Log.i(TAG, "closePortOnly start")

        usb?.let { comm ->
            safeClosePort(
                comm = comm,
                reason = "closePortOnly USB"
            )
        }

        if (USE_GS_BRIDGE) {
            gs?.let { comm ->
                safeClosePort(
                    comm = comm,
                    reason = "closePortOnly GS"
                )
            }
        }

        currentUsbDevice = null

        Log.i(TAG, "closePortOnly end")
        Unit
    }

    suspend fun closeAll() = withContext(Dispatchers.IO) {
        Log.i(TAG, "closeAll start")

        val usbComm = usb
        val gsComm = gs
        val usbConnection = usbConn
        val gsConnection = gsConn

        usbComm?.let { comm ->
            safeClosePort(
                comm = comm,
                reason = "closeAll USB"
            )
        }

        gsComm?.let { comm ->
            safeClosePort(
                comm = comm,
                reason = "closeAll GS"
            )
        }

        runCatching {
            usbConnection?.let {
                Log.i(TAG, "unbind USB service")
                appContext.unbindService(it)
            }
        }.onFailure { throwable ->
            Log.w(TAG, "USB unbindService failed", throwable)
        }

        runCatching {
            gsConnection?.let {
                Log.i(TAG, "unbind GS service")
                appContext.unbindService(it)
            }
        }.onFailure { throwable ->
            Log.w(TAG, "GS unbindService failed", throwable)
        }

        usb = null
        gs = null
        usbConn = null
        gsConn = null
        currentUsbDevice = null

        delay(AFTER_CLOSE_ALL_DELAY_MS)

        Log.i(TAG, "closeAll end")
        Unit
    }

    private suspend fun prepareGsBridge() {
        val gsComm = gs

        if (gsComm == null) {
            Log.w(TAG, "prepareGsBridge skipped: GS service missing")
            return
        }

        Log.i(TAG, "prepareGsBridge start device=$GS_BRIDGE_DEVICE")

        safeClosePort(
            comm = gsComm,
            reason = "before GS bridge"
        )

        delay(OPEN_RESET_DELAY_MS)

        runCatching {
            gsComm.open()
            delay(OPEN_AFTER_DELAY_MS)
            gsComm.connect(GS_BRIDGE_DEVICE)
        }.onFailure { throwable ->
            Log.w(TAG, "GS bridge connect failed", throwable)
            return
        }

        val started = System.currentTimeMillis()
        var lastStatus: Int? = null

        while (System.currentTimeMillis() - started < GS_BRIDGE_WAIT_MS) {
            val status = readStatusQuietly(gsComm)

            if (status != lastStatus) {
                Log.i(TAG, "GS poll status device=$GS_BRIDGE_DEVICE status=$status")
                lastStatus = status
            }

            if (status == CONNECTED_STATUS) {
                Log.i(TAG, "GS bridge connected")
                return
            }

            delay(CONNECT_POLL_MS)
        }

        Log.w(TAG, "GS bridge not connected finalStatus=$lastStatus")
    }

    private suspend fun waitConnected(
        comm: IComm,
        device: String
    ): Boolean {
        val started = System.currentTimeMillis()
        var lastStatus: Int? = null

        while (System.currentTimeMillis() - started < CONNECT_WAIT_MS) {
            val status = readConnectStatus(
                comm = comm,
                device = device,
                prefix = "poll status"
            )

            if (status != lastStatus) {
                lastStatus = status
            }

            if (status == CONNECTED_STATUS) {
                return true
            }

            delay(CONNECT_POLL_MS)
        }

        return false
    }

    private fun readConnectStatus(
        comm: IComm,
        device: String,
        prefix: String
    ): Int {
        val status = readStatusQuietly(comm)
        Log.i(TAG, "$prefix USB=$device status=$status")
        return status
    }

    private fun readStatusQuietly(comm: IComm): Int {
        return runCatching {
            comm.getConnectStatus()
        }.getOrElse { throwable ->
            Log.w(TAG, "getConnectStatus failed", throwable)
            -1
        }
    }

    private suspend fun safeClosePort(
        comm: IComm,
        reason: String
    ) {
        Log.i(TAG, "safeClosePort start reason=$reason")

        runCatching {
            comm.cancelRecv()
        }.onFailure { throwable ->
            Log.w(TAG, "cancelRecv failed reason=$reason", throwable)
        }

        runCatching {
            comm.disconnect()
        }.onFailure { throwable ->
            Log.w(TAG, "disconnect failed reason=$reason", throwable)
        }

        runCatching {
            comm.close()
        }.onFailure { throwable ->
            Log.w(TAG, "close failed reason=$reason", throwable)
        }

        delay(PORT_CLOSE_SETTLE_DELAY_MS)

        Log.i(TAG, "safeClosePort end reason=$reason")
    }

    private fun bind(
        action: String,
        deferred: CompletableDeferred<IComm>,
        label: String
    ): ServiceConnection {
        val intent = Intent(action).setPackage(PKG)

        Log.i(TAG, "bindService start $label action=$action package=$PKG")

        /*
         * Важно:
         * Не вызываем startService().
         * Для WireECR используем только bindService(..., BIND_AUTO_CREATE).
         */

        val connection = object : ServiceConnection {
            override fun onServiceConnected(
                name: ComponentName?,
                service: IBinder?
            ) {
                Log.i(TAG, "$label service connected: $action component=$name")

                val comm = IComm.Stub.asInterface(service)

                if (comm != null) {
                    if (!deferred.isCompleted) {
                        deferred.complete(comm)
                    }
                } else {
                    if (!deferred.isCompleted) {
                        deferred.completeExceptionally(
                            IllegalStateException("IComm.asInterface returned null for $action")
                        )
                    }
                }
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                Log.w(TAG, "$label service disconnected: $action component=$name")

                if (label == "USB") {
                    usb = null
                    usbConn = null
                } else {
                    gs = null
                    gsConn = null
                }
            }

            override fun onBindingDied(name: ComponentName?) {
                Log.w(TAG, "$label binding died: $action component=$name")

                if (label == "USB") {
                    usb = null
                    usbConn = null
                } else {
                    gs = null
                    gsConn = null
                }

                if (!deferred.isCompleted) {
                    deferred.completeExceptionally(
                        IllegalStateException("Binding died for $action")
                    )
                }
            }

            override fun onNullBinding(name: ComponentName?) {
                Log.w(TAG, "$label null binding: $action component=$name")

                if (!deferred.isCompleted) {
                    deferred.completeExceptionally(
                        IllegalStateException("Null binding for $action")
                    )
                }
            }
        }

        val ok = appContext.bindService(
            intent,
            connection,
            Context.BIND_AUTO_CREATE
        )

        if (!ok) {
            throw IllegalStateException("bindService false for $action")
        }

        return connection
    }

    private fun logCandidateState(device: String) {
        val file = File(device)

        Log.i(
            TAG,
            "candidate state before connect: $device " +
                    "exists=${file.exists()} read=${file.canRead()} write=${file.canWrite()}"
        )
    }

    private fun buildUsbDiagnostics(): String {
        return buildString {
            appendLine("USB diagnostics")
            appendLine("===============")
            appendLine()
            appendLine("Candidate files:")

            USB_DEVICE_CANDIDATES.forEach { path ->
                val file = File(path)
                appendLine(
                    "$path exists=${file.exists()} read=${file.canRead()} write=${file.canWrite()}"
                )
            }

            appendLine()
            appendLine("TTY devices from /dev:")

            val devFiles = runCatching {
                File("/dev").listFiles()
            }.getOrNull()

            if (devFiles == null) {
                appendLine("/dev listFiles returned null")
            } else {
                val ttyFiles = devFiles
                    .map { file -> file.absolutePath }
                    .filter { path ->
                        path.contains("tty", ignoreCase = true) ||
                                path.contains("usb", ignoreCase = true) ||
                                path.contains("gs", ignoreCase = true) ||
                                path.contains("acm", ignoreCase = true)
                    }
                    .sorted()

                if (ttyFiles.isEmpty()) {
                    appendLine("none")
                } else {
                    ttyFiles.forEach { path ->
                        appendLine(path)
                    }
                }
            }

            appendLine()
            appendLine("USB sys info:")

            USB_SYS_INFO_PATHS.forEach { path ->
                appendLine("$path = ${readTextFileOrMissing(path)}")
            }
        }
    }

    private fun readTextFileOrMissing(path: String): String {
        return runCatching {
            val file = File(path)

            if (!file.exists()) {
                "<missing>"
            } else if (file.isDirectory) {
                file.list()?.joinToString().orEmpty().ifBlank { "<empty-dir>" }
            } else {
                file.readText().trim().ifBlank { "<empty>" }
            }
        }.getOrElse { throwable ->
            "<error: ${throwable.message}>"
        }
    }

    private fun ByteArray.toHexPreview(limit: Int = 96): String {
        return take(limit).joinToString(" ") { byte ->
            "%02X".format(byte)
        }
    }

    companion object {
        private const val TAG = "PcUsbEcrFlow"

        private const val PKG = "com.xcheng.wiredecr"

        private const val ACTION_USB = "com.xcheng.wiredecr.IWireEcrService"
        private const val ACTION_GS = "com.xcheng.wiredecr.IWireEcrServiceGS"

        private const val TTY_GS0 = "/dev/ttyGS0"
        private const val GS_BRIDGE_DEVICE = "/dev/ttyS1"

        private const val ACCEPT_TTY_GS0_WITHOUT_STATUS = true
        private const val USE_GS_BRIDGE = false

        private val USB_DEVICE_CANDIDATES = listOf(
            TTY_GS0,
            "/dev/ttyACM0",
            "/dev/ttyACM1",
            "/dev/ttyUSB0",
            "/dev/ttyUSB1"
        )

        private val USB_SYS_INFO_PATHS = listOf(
            "/sys/class/android_usb/android0/state",
            "/sys/class/android_usb/android0/functions",
            "/sys/class/android_usb/android0/enable",
            "/config/usb_gadget/g1/UDC",
            "/config/usb_gadget/g1/functions",
            "/sys/kernel/config/usb_gadget/g1/UDC",
            "/sys/kernel/config/usb_gadget/g1/functions"
        )

        private const val CONNECTED_STATUS = 2

        private const val RECV_TIMEOUT_MS = 3000
        private const val RECV_BUFFER_SIZE = 2048

        private const val BIND_TIMEOUT_MS = 3000L

        private const val CONNECT_WAIT_MS = 3000L
        private const val CONNECT_POLL_MS = 100L
        private const val CONNECT_AFTER_DELAY_MS = 200L

        private const val GS_BRIDGE_WAIT_MS = 1500L

        private const val OPEN_RESET_DELAY_MS = 150L
        private const val OPEN_AFTER_DELAY_MS = 150L
        private const val BETWEEN_PROBES_DELAY_MS = 150L
        private const val PORT_CLOSE_SETTLE_DELAY_MS = 120L

        private const val AFTER_CLOSE_ALL_DELAY_MS = 500L
    }
}