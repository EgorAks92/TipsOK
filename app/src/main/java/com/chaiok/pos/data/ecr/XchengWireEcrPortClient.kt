package com.chaiok.pos.data.ecr

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.xcheng.wiredecr.IComm
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.File

class XchengWireEcrPortClient(context: Context) {

    private val appContext = context.applicationContext
    private val log = PcUsbEcrFileLogger(context)

    private var usb: IComm? = null
    private var usbConn: ServiceConnection? = null

    private var connectedDevice: String? = null

    suspend fun bindService(): Result<Unit> = withContext(Dispatchers.IO) {
        val result = runCatching {
            if (usb != null) {
                log.i("bind skipped: USB service already bound")
                return@runCatching Unit
            }

            log.i("bindService start action=$ACTION_USB package=$PKG")

            val deferred = CompletableDeferred<IComm>()

            usbConn = bind(
                action = ACTION_USB,
                deferred = deferred
            )

            usb = withTimeout(BIND_TIMEOUT_MS) {
                deferred.await()
            }

            log.i("bind success usbBound=${usb != null}")

            Unit
        }

        if (result.isFailure) {
            log.e("bindService failed", result.exceptionOrNull())
            closeAll()
        }

        result
    }

    suspend fun openAndConnect(): Result<Unit> = withContext(Dispatchers.IO) {
        val result = runCatching {
            val usbComm = usb ?: error("USB service missing")

            log.i("openAndConnect start")
            log.i("open USB port using observed AIDL mapping")

            /*
             * TEMPORARY XCHENG AIDL MAPPING WORKAROUND
             *
             * По реальным логам на этом терминале текущий локальный AIDL
             * смещён относительно системного WireEcrService:
             *
             * local usbComm.close()      -> WireEcrService: open port
             * local usbComm.open()       -> WireEcrService: cancelRecv
             * local usbComm.cancelRecv() -> WireEcrService: close port
             *
             * Поэтому:
             * - actual open port делаем через close()
             * - actual close port делаем через cancelRecv()
             * - open() перед connect() не вызываем
             */
            actualOpenPort(usbComm)

            var lastError: String? = null

            for (device in USB_DEVICE_CANDIDATES) {
                log.i("connect probe requested USB=$device")

                runCatching {
                    usbComm.connect(device)
                }.onFailure { throwable ->
                    log.w("connect call failed for $device", throwable)
                    lastError = throwable.message
                }

                val started = System.currentTimeMillis()
                var lastStatus: Int? = null

                while (System.currentTimeMillis() - started < CONNECT_WAIT_PER_DEVICE_MS) {
                    val status = runCatching {
                        usbComm.getConnectStatus()
                    }.getOrElse { throwable ->
                        log.w("getConnectStatus failed for $device", throwable)
                        -1
                    }

                    if (status != lastStatus) {
                        log.i("poll status USB=$device status=$status")
                        lastStatus = status
                    }

                    if (status == CONNECTED_STATUS) {
                        connectedDevice = device

                        runCatching {
                            usbComm.setRecvTimeout(RECV_TIMEOUT_MS)
                        }.onFailure { throwable ->
                            log.w("setRecvTimeout failed for $device", throwable)
                        }

                        log.i("USB connected device=$device status=$status")

                        return@runCatching Unit
                    }

                    delay(CONNECT_POLL_MS)
                }

                log.w("USB probe failed device=$device lastStatus=$lastStatus")

                actualClosePort(usbComm)
                delay(PROBE_RESET_DELAY_MS)

                actualOpenPort(usbComm)
                delay(OPEN_AFTER_DELAY_MS)
            }

            val diagnostics = collectUsbDiagnostics()

            log.e(
                "USB connect timeout. None of candidates opened. " +
                        "candidates=${USB_DEVICE_CANDIDATES.joinToString()} " +
                        "lastError=$lastError\n$diagnostics"
            )

            error(
                "USB ECR порт не найден. Проверьте режим USB/ECR или CDC на терминале, " +
                        "кабель и наличие COM-порта на ПК/кассе."
            )
        }

        if (result.isFailure) {
            log.e("openAndConnect failed", result.exceptionOrNull())
        }

        result
    }

    suspend fun receiveOnce(): Result<ByteArray?> = withContext(Dispatchers.IO) {
        val result = runCatching {
            val usbComm = usb ?: error("USB service missing")

            log.i(
                "recv start USB=${connectedDevice ?: "-"} buffer=$RECV_BUFFER_SIZE"
            )

            val first: ByteArray? = usbComm.recv(RECV_BUFFER_SIZE)

            if (first != null && first.isNotEmpty()) {
                log.i(
                    "recv end bytes=${first.size} hex=${first.toHexPreview()}"
                )

                return@runCatching first
            }

            val nonBlocking: ByteArray? = runCatching {
                usbComm.recvNonBlocking()
            }.getOrNull()

            if (nonBlocking != null && nonBlocking.isNotEmpty()) {
                log.i(
                    "recvNonBlocking received bytes=${nonBlocking.size} " +
                            "hex=${nonBlocking.toHexPreview()}"
                )

                return@runCatching nonBlocking
            }

            val nonBlock: ByteArray? = runCatching {
                usbComm.recvNonBlock()
            }.getOrNull()

            if (nonBlock != null && nonBlock.isNotEmpty()) {
                log.i(
                    "recvNonBlock received bytes=${nonBlock.size} " +
                            "hex=${nonBlock.toHexPreview()}"
                )

                return@runCatching nonBlock
            }

            log.i("recv end empty")

            null
        }

        if (result.isFailure) {
            log.e("receiveOnce failed", result.exceptionOrNull())
        }

        result
    }

    suspend fun send(bytes: ByteArray): Result<Unit> = withContext(Dispatchers.IO) {
        val result = runCatching {
            val usbComm = usb ?: error("USB service missing")

            log.i("send bytes=${bytes.size} hex=${bytes.toHexPreview()}")

            usbComm.send(bytes)

            Unit
        }

        if (result.isFailure) {
            log.e("send failed", result.exceptionOrNull())
        }

        result
    }

    suspend fun stop() {
        closeAll()
    }

    suspend fun closePortOnly() = withContext(Dispatchers.IO) {
        log.i("closePortOnly start using observed AIDL mapping")

        val comm = usb

        if (comm != null) {
            actualClosePort(comm)
        }

        connectedDevice = null

        log.i("closePortOnly end")

        Unit
    }

    suspend fun closeAll() = withContext(Dispatchers.IO) {
        log.i("closeAll start using observed AIDL mapping")

        val comm = usb
        val conn = usbConn

        if (comm != null) {
            actualClosePort(comm)
        }

        runCatching {
            conn?.let { appContext.unbindService(it) }
        }.onFailure {
            log.w("unbindService failed", it)
        }

        usb = null
        usbConn = null
        connectedDevice = null

        log.i("closeAll end")

        Unit
    }

    private suspend fun actualOpenPort(comm: IComm) {
        log.i("actualOpenPort via local close()")

        runCatching {
            comm.close()
        }.onFailure {
            log.w("actualOpenPort failed", it)
        }

        delay(OPEN_AFTER_DELAY_MS)
    }

    private fun actualClosePort(comm: IComm) {
        log.i("actualClosePort via local cancelRecv()")

        runCatching {
            comm.cancelRecv()
        }.onFailure {
            log.w("actualClosePort failed", it)
        }
    }

    private fun bind(
        action: String,
        deferred: CompletableDeferred<IComm>
    ): ServiceConnection {
        val intent = Intent(action).setPackage(PKG)

        runCatching {
            appContext.startService(intent)
        }.onFailure {
            log.w("startService failed for $action", it)
        }

        val connection = object : ServiceConnection {
            override fun onServiceConnected(
                name: ComponentName?,
                service: IBinder?
            ) {
                log.i("service connected: $action component=$name")

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
                log.w("service disconnected: $action component=$name")
                usb = null
                connectedDevice = null
            }

            override fun onBindingDied(name: ComponentName?) {
                log.w("binding died: $action component=$name")
                usb = null
                connectedDevice = null
            }

            override fun onNullBinding(name: ComponentName?) {
                log.w("null binding: $action component=$name")

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

    private fun collectUsbDiagnostics(): String {
        return buildString {
            appendLine("USB diagnostics")
            appendLine("===============")

            appendLine()
            appendLine("TTY devices:")
            val ttyDevices = listTtyDevices()

            if (ttyDevices.isEmpty()) {
                appendLine("none")
            } else {
                ttyDevices.forEach { appendLine(it) }
            }

            appendLine()
            appendLine("USB sys info:")
            listUsbSysInfo().forEach { appendLine(it) }
        }
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
                    "${file.absolutePath} " +
                            "exists=${file.exists()} " +
                            "read=${file.canRead()} " +
                            "write=${file.canWrite()}"
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

        return paths.map { path ->
            val file = File(path)

            runCatching {
                when {
                    !file.exists() -> "$path = <missing>"

                    file.isDirectory -> {
                        val children = file
                            .listFiles()
                            .orEmpty()
                            .joinToString { child -> child.name }

                        "$path = [$children]"
                    }

                    else -> "$path = ${file.readText().trim()}"
                }
            }.getOrElse { throwable ->
                "$path = <error: ${throwable.message}>"
            }
        }
    }

    private fun ByteArray.toHexPreview(limit: Int = 64): String {
        return take(limit).joinToString(" ") { byte ->
            "%02X".format(byte)
        }
    }

    companion object {
        private const val PKG = "com.xcheng.wiredecr"
        private const val ACTION_USB = "com.xcheng.wiredecr.IWireEcrService"

        /*
         * ВАЖНО:
         * Не добавлять сюда /dev/ttyS1.
         * По реальным логам /dev/ttyS1 — системный POS/SP канал.
         */
        private val USB_DEVICE_CANDIDATES = listOf(
            "/dev/ttyACM0",
            "/dev/ttyACM1",
            "/dev/ttyUSB0",
            "/dev/ttyUSB1",
            "/dev/ttyGS0",
            "/dev/ttyGS1"
        )

        private const val CONNECTED_STATUS = 2

        private const val RECV_TIMEOUT_MS = 3000
        private const val CONNECT_WAIT_PER_DEVICE_MS = 1500L
        private const val CONNECT_POLL_MS = 100L
        private const val BIND_TIMEOUT_MS = 3000L

        private const val OPEN_AFTER_DELAY_MS = 150L
        private const val PROBE_RESET_DELAY_MS = 150L

        private const val RECV_BUFFER_SIZE = 2048
    }
}