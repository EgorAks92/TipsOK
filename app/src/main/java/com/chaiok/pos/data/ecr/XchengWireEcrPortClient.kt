package com.chaiok.pos.data.ecr

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import com.xcheng.wiredecr.IComm
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class XchengWireEcrPortClient(context: Context) {
    private val appContext = context.applicationContext
    private var usb: IComm? = null
    private var sp: IComm? = null
    private var usbConn: ServiceConnection? = null
    private var spConn: ServiceConnection? = null

    suspend fun start(): Result<Unit> = withContext(Dispatchers.IO) { runCatching {
        val usbDeferred = CompletableDeferred<IComm>()
        val spDeferred = CompletableDeferred<IComm>()
        usbConn = bind(ACTION_USB, usbDeferred)
        spConn = bind(ACTION_SP, spDeferred)
        usb = kotlinx.coroutines.withTimeout(BIND_TIMEOUT_MS) { usbDeferred.await() }
        sp = kotlinx.coroutines.withTimeout(BIND_TIMEOUT_MS) { spDeferred.await() }
        Log.i(TAG, "bind success")
    } }

    suspend fun openAndConnect(): Result<Unit> = withContext(Dispatchers.IO) { runCatching {
        val usbComm = usb ?: error("USB service missing")
        val spComm = sp ?: error("SP service missing")
        usbComm.open(); spComm.open(); usbComm.connect(USB_DEVICE); spComm.connect(SP_DEVICE)
        val started = System.currentTimeMillis()
        while (System.currentTimeMillis() - started < CONNECT_WAIT_MS) {
            if (usbComm.getConnectStatus() == 2) {
                usbComm.setRecvTimeout(RECV_TIMEOUT_MS)
                return@runCatching
            }
            delay(CONNECT_POLL_MS)
        }
        error("USB connect timeout")
    } }

    suspend fun receiveOnce(): Result<ByteArray?> = withContext(Dispatchers.IO) { runCatching { usb?.recv(RECV_BUFFER_SIZE) } }
    suspend fun send(bytes: ByteArray): Result<Unit> = withContext(Dispatchers.IO) { runCatching { usb?.send(bytes) ?: error("USB service missing") } }
    suspend fun stop() { closeAll() }
    suspend fun closeAll() = withContext(Dispatchers.IO) {
        listOf(usb, sp).forEach { runCatching { it?.cancelRecv(); it?.disconnect(); it?.close() } }
        runCatching { usbConn?.let { appContext.unbindService(it) } }
        runCatching { spConn?.let { appContext.unbindService(it) } }
        usb = null; sp = null; usbConn = null; spConn = null
    }

    private fun bind(action: String, deferred: CompletableDeferred<IComm>): ServiceConnection {
        val intent = Intent(action).setPackage(PKG)
        runCatching { appContext.startService(intent) }
        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                IComm.Stub.asInterface(service)?.let { deferred.complete(it) }
            }
            override fun onServiceDisconnected(name: ComponentName?) {}
        }
        val ok = appContext.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        if (!ok) throw IllegalStateException("bindService false for $action")
        return connection
    }

    companion object {
        private const val TAG = "PcUsbEcrFlow"
        private const val PKG = "com.xcheng.wiredecr"
        private const val ACTION_USB = "com.xcheng.wiredecr.IWireEcrService"
        private const val ACTION_SP = "com.xcheng.wiredecr.IWireEcrServiceGS"
        private const val USB_DEVICE = "/dev/ttyACM0"
        private const val SP_DEVICE = "/dev/ttyS1"
        private const val RECV_TIMEOUT_MS = 3000
        private const val CONNECT_WAIT_MS = 3000L
        private const val CONNECT_POLL_MS = 100L
        private const val BIND_TIMEOUT_MS = 3000L
        private const val RECV_BUFFER_SIZE = 2048
    }
}
