package com.chaiok.pos.data.ecr

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import com.xcheng.wiredecr.IComm
import java.io.File
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.ThreadFactory
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

class XchengWireEcrPortClient(context: Context) : PcEcrTransport {

    private val appContext = context.applicationContext

    private var usb: IComm? = null
    private var gs: IComm? = null

    private var usbConn: ServiceConnection? = null
    private var gsConn: ServiceConnection? = null

    private var currentUsbDevice: String? = null

    @Volatile
    private var transportReady: Boolean = false

    @Volatile
    private var transportPausedForPayment: Boolean = false

    private val transportMutex = Mutex()
    @Volatile
    private var recvExecutor: ExecutorService = newRecvExecutor()
    private val pendingRecvLock = Any()
    @Volatile
    private var pendingIdleRecvFuture: Future<ByteArray?>? = null
    @Volatile
    private var pendingIdleRecvDevice: String? = null

    suspend fun ensureConnected(): Result<Unit> =
        withContext(Dispatchers.IO) {
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
                Log.e(TAG, "ensureConnected failed", result.exceptionOrNull())
                closeAll()
            }

            result
        }

    override suspend fun ensureTransportReady(): Result<Unit> =
        withContext(Dispatchers.IO) {
            transportMutex.withLock {
                runCatching {
                    ensureConnected().getOrThrow()

                    if (transportReady && !transportPausedForPayment && currentUsbDevice != null) {
                        Log.i(
                            TAG,
                            "ensureTransportReady skipped: transport already ready device=$currentUsbDevice"
                        )
                        return@runCatching Unit
                    }

                    Log.i(TAG, "ensureTransportReady opening transport")
                    openAndConnect().getOrThrow()

                    transportReady = true
                    transportPausedForPayment = false

                    Unit
                }.onFailure { throwable ->
                    transportReady = false
                    currentUsbDevice = null
                    Log.e(TAG, "ensureTransportReady failed", throwable)
                }
            }
        }

    suspend fun openAndConnect(): Result<Unit> =
        withContext(Dispatchers.IO) {
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
                            transportReady = true
                            transportPausedForPayment = false

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
                            transportReady = true
                            transportPausedForPayment = false

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
                            transportReady = true
                            transportPausedForPayment = false

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

                transportReady = false
                currentUsbDevice = null

                error(
                    "USB ECR порт не найден. Проверьте режим USB/ECR или CDC на терминале, " +
                            "кабель и наличие корректного ECR/CDC COM-порта на ПК/кассе."
                )
            }.onFailure { throwable ->
                Log.e(TAG, "openAndConnect failed", throwable)
            }
        }

    override suspend fun receiveOnce(): Result<ByteArray?> = receiveOnce(RECV_TIMEOUT_MS.toLong())

    override suspend fun receiveOnce(timeoutMs: Long): Result<ByteArray?> =
        withContext(Dispatchers.IO) {
            try {
                val usbComm = usb ?: error("USB service missing")
                val device = currentUsbDevice ?: error("USB device missing")

                Log.i(TAG, "recv start USB=$device buffer=$RECV_BUFFER_SIZE timeoutMs=$timeoutMs")
                val startedAt = SystemClock.elapsedRealtime()
                val isIdleListenReceive = timeoutMs == IDLE_LISTEN_RECV_TIMEOUT_MS.toLong()
                val recvResult = if (isIdleListenReceive) {
                    blockingRecvWithIdlePending(
                        usbComm = usbComm,
                        device = device,
                        timeoutMs = timeoutMs.coerceAtLeast(1L)
                    )
                } else {
                    blockingRecvOneShot(
                        usbComm = usbComm,
                        timeoutMs = timeoutMs.coerceAtLeast(1L)
                    )
                }

                val bytes = when (recvResult) {
                    is RecvAttemptResult.Bytes -> recvResult.data
                    RecvAttemptResult.TimeoutNoData -> null
                }

                val elapsed = SystemClock.elapsedRealtime() - startedAt
                Log.i(TAG, "recv requested timeoutMs=$timeoutMs actualElapsedMs=$elapsed bytes=${bytes?.size ?: 0}")
                if (elapsed > timeoutMs + RECEIVE_TIMEOUT_WARN_DELTA_MS) {
                    Log.w(TAG, "recv elapsed exceeded timeout requested=$timeoutMs actual=$elapsed")
                }

                if (bytes != null && bytes.isNotEmpty()) {
                    if (isArcusWaiterLoginPayload(bytes)) {
                        Log.i(TAG, "recv end bytes=${bytes.size} command=WAITER_LOGIN rawMasked=true")
                    } else {
                        Log.i(TAG, "recv end bytes=${bytes.size} hex=${bytes.toHexPreview()}")
                    }
                    return@withContext Result.success(bytes)
                }

                Log.i(TAG, "recv hard timeout requested=$timeoutMs actual=$elapsed no data")
                if (elapsed > timeoutMs + RECEIVE_TIMEOUT_GRACE_MS) {
                    Log.w(TAG, "recv hard timeout exceeded requested=$timeoutMs actual=$elapsed")
                }
                Log.i(TAG, "recv end empty")
                Result.success(null)
            } catch (throwable: Throwable) {
                if (throwable is CancellationException) throw throwable
                Log.e(TAG, "receiveOnce failed", throwable)
                Result.failure(throwable)
            }
        }

    private fun cancelPendingIdleRecv(reason: String) {
        val future = synchronized(pendingRecvLock) {
            val old = pendingIdleRecvFuture
            pendingIdleRecvFuture = null
            pendingIdleRecvDevice = null
            old
        }
        future?.cancel(true)
        Log.i(TAG, "recv idle pending read cancelled reason=$reason")
    }

    private fun getOrCreatePendingIdleRecv(
        usbComm: IComm,
        device: String
    ): PendingIdleRecv {
        synchronized(pendingRecvLock) {
            val current = pendingIdleRecvFuture
            if (current != null && !current.isDone && pendingIdleRecvDevice == device) {
                return PendingIdleRecv(future = current, reused = true)
            }
            if (current != null && !current.isDone && pendingIdleRecvDevice != device) {
                current.cancel(true)
            }
            runCatching {
                usbComm.setRecvTimeout(RECV_TIMEOUT_MS)
            }.onFailure {
                Log.w(TAG, "setRecvTimeout before idle pending recv failed", it)
            }
            val executor = recvExecutor
            val created = executor.submit<ByteArray?> {
                usbComm.recv(RECV_BUFFER_SIZE)
            }
            pendingIdleRecvFuture = created
            pendingIdleRecvDevice = device
            return PendingIdleRecv(future = created, reused = false)
        }
    }

    private fun clearPendingIdleRecv(future: Future<ByteArray?>) {
        synchronized(pendingRecvLock) {
            if (pendingIdleRecvFuture === future) {
                pendingIdleRecvFuture = null
                pendingIdleRecvDevice = null
            }
        }
    }

    private fun blockingRecvOneShot(
        usbComm: IComm,
        timeoutMs: Long
    ): RecvAttemptResult {
        runCatching {
            usbComm.setRecvTimeout(timeoutMs.toInt().coerceAtLeast(1))
        }.onFailure {
            Log.w(TAG, "setRecvTimeout before worker recv failed", it)
        }

        val startedAt = SystemClock.elapsedRealtime()
        val executor = recvExecutor
        val future = executor.submit<ByteArray?> {
            usbComm.recv(RECV_BUFFER_SIZE)
        }

        return try {
            val chunk = future.get(timeoutMs + RECEIVE_TIMEOUT_GRACE_MS, TimeUnit.MILLISECONDS)
            if (chunk != null && chunk.isNotEmpty()) {
                RecvAttemptResult.Bytes(chunk)
            } else {
                RecvAttemptResult.TimeoutNoData
            }
        } catch (_: TimeoutException) {
            future.cancel(true)
            RecvAttemptResult.TimeoutNoData
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            future.cancel(true)
            Log.w(TAG, "recv worker interrupted requested=$timeoutMs", e)
            RecvAttemptResult.TimeoutNoData
        } catch (e: ExecutionException) {
            Log.e(TAG, "recv worker failed", e.cause ?: e)
            throw (e.cause ?: e)
        }
    }

    private fun blockingRecvWithIdlePending(
        usbComm: IComm,
        device: String,
        timeoutMs: Long
    ): RecvAttemptResult {
        val startedAt = SystemClock.elapsedRealtime()
        val pending = getOrCreatePendingIdleRecv(usbComm = usbComm, device = device)
        val future = pending.future
        val pendingState = if (pending.reused) "reused" else "created"
        Log.i(TAG, "recv idle pending read $pendingState device=$device timeoutMs=$timeoutMs")
        return try {
            val chunk = future.get(timeoutMs + RECEIVE_TIMEOUT_GRACE_MS, TimeUnit.MILLISECONDS)
            clearPendingIdleRecv(future)
            if (chunk != null && chunk.isNotEmpty()) {
                RecvAttemptResult.Bytes(chunk)
            } else {
                RecvAttemptResult.TimeoutNoData
            }
        } catch (_: TimeoutException) {
            val elapsed = SystemClock.elapsedRealtime() - startedAt
            Log.i(
                TAG,
                "recv idle wait timeout requested=$timeoutMs actual=$elapsed; pending recv kept alive"
            )
            RecvAttemptResult.TimeoutNoData
        } catch (e: InterruptedException) {
            clearPendingIdleRecv(future)
            Thread.currentThread().interrupt()
            future.cancel(true)
            Log.w(TAG, "recv idle pending interrupted requested=$timeoutMs", e)
            RecvAttemptResult.TimeoutNoData
        } catch (e: ExecutionException) {
            clearPendingIdleRecv(future)
            Log.e(TAG, "recv idle pending failed", e.cause ?: e)
            throw (e.cause ?: e)
        }
    }

    override suspend fun send(bytes: ByteArray): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val usbComm = usb ?: error("USB service missing")

                Log.i(TAG, "send bytes=${bytes.size} hex=${bytes.toHexPreview()}")

                usbComm.send(bytes)

                Unit
            }.onFailure { throwable ->
                Log.e(TAG, "send failed", throwable)
            }
        }

    suspend fun pauseTransportForPayment(): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                Log.i(TAG, "ECR pause transport for SSP payment")

                if (transportPausedForPayment) {
                    Log.i(TAG, "ECR pause skipped: already paused")
                    return@runCatching Unit
                }

                transportPausedForPayment = true

                if (transportReady || currentUsbDevice != null) {
                    closePortOnly()
                } else {
                    Log.i(TAG, "ECR pause: transport already closed")
                    transportReady = false
                    currentUsbDevice = null
                }

                Unit
            }.onFailure { throwable ->
                Log.e(TAG, "pauseTransportForPayment failed", throwable)
            }
        }

    suspend fun resumeTransportAfterPayment(): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                Log.i(TAG, "ECR resume transport after SSP payment")

                transportPausedForPayment = false
                ensureTransportReady().getOrThrow()

                transportReady = true
                transportPausedForPayment = false

                Log.i(TAG, "ECR resumed transport after SSP payment")

                Unit
            }.onFailure { throwable ->
                Log.e(TAG, "resumeTransportAfterPayment failed", throwable)
            }
        }

    override suspend fun closeCompletely(): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                Log.i(TAG, "ECR close transport completely")

                closeAll()

                transportReady = false
                transportPausedForPayment = false
                currentUsbDevice = null

                Unit
            }.onFailure { throwable ->
                Log.e(TAG, "closeCompletely failed", throwable)
            }
        }

    override fun isOpen(): Boolean = transportReady

    suspend fun closePortOnly() =
        withContext(Dispatchers.IO) {
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
            transportReady = false

            Log.i(TAG, "closePortOnly end")

            Unit
        }

    suspend fun closeAll() =
        withContext(Dispatchers.IO) {
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
            transportReady = false
            transportPausedForPayment = false

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
        cancelPendingIdleRecv(reason = "safeClosePort:$reason")
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


    private fun isArcusWaiterLoginPayload(bytes: ByteArray): Boolean {
        val payload = Arcus2BinLenCodec.decode(bytes).getOrNull()?.data ?: bytes
        val fields = decodeWin1251(payload).trim('\u0000', ' ', '\n', '\r', '\t').split('\u001B')
        return fields.getOrNull(0) == "2" && fields.getOrNull(1) == "9"
    }

    private fun ByteArray.toHexPreview(limit: Int = 96): String =
        take(limit).joinToString(" ") { byte ->
            "%02X".format(byte)
        }

    companion object {
        private const val TAG = "PcUsbEcrFlow"

        private fun newRecvExecutor(): ExecutorService =
            Executors.newSingleThreadExecutor(
                ThreadFactory { runnable ->
                    Thread(runnable, "PcUsbEcrRecvWorker").apply { isDaemon = true }
                }
            )

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
        private const val RECEIVE_TIMEOUT_GRACE_MS = 100L
        private const val RECEIVE_TIMEOUT_WARN_DELTA_MS = 300L
        private const val IDLE_LISTEN_RECV_TIMEOUT_MS = RECV_TIMEOUT_MS

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

    private sealed interface RecvAttemptResult {
        data class Bytes(val data: ByteArray) : RecvAttemptResult
        data object TimeoutNoData : RecvAttemptResult
    }

    private data class PendingIdleRecv(
        val future: Future<ByteArray?>,
        val reused: Boolean
    )

}
