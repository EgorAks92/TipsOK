package com.chaiok.pos.data.repository

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.os.RemoteException
import com.skytech.smartskyposlib.ISmartSkyPos
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class SmartSkyPosTerminalApi(
    private val context: Context
) : PaymentTerminalApi {

    private val bindMutex = Mutex()
    private var smartSkyPos: ISmartSkyPos? = null
    private var serviceConnection: ServiceConnection? = null

    override suspend fun getTerminalData(): PaymentTerminalDataResult = withContext(Dispatchers.IO) {
        val service = connectIfNeeded() ?: return@withContext PaymentTerminalDataResult.Error("SmartSkyPos service bind timeout")
        try {
            val terminalData = service.terminalData ?: return@withContext PaymentTerminalDataResult.Error("TerminalData is null")
            val code = terminalData.code
            if (code != 0) {
                return@withContext PaymentTerminalDataResult.Error(terminalData.message)
            }

            val serialNumber = terminalData.serialNumber.orEmpty().trim()
            val tidFromData = terminalData.terminalId.orEmpty().trim()
            val tidFromFirstTerminal = terminalData.terminals
                ?.firstOrNull()
                ?.terminalId
                .orEmpty()
                .trim()
            val tid = tidFromData.ifBlank { tidFromFirstTerminal }

            if (serialNumber.isBlank() || tid.isBlank()) {
                return@withContext PaymentTerminalDataResult.Error("TerminalData is missing serialNumber or tid")
            }

            PaymentTerminalDataResult.Success(PaymentTerminalData(serialNumber = serialNumber, tid = tid))
        } catch (_: RemoteException) {
            PaymentTerminalDataResult.Error("SmartSkyPos getTerminalData RemoteException")
        } catch (error: SecurityException) {
            PaymentTerminalDataResult.Error(error.message ?: "SmartSkyPos security error")
        }
    }

    private suspend fun connectIfNeeded(): ISmartSkyPos? = bindMutex.withLock {
        smartSkyPos?.let { return it }
        val deferred = CompletableDeferred<ISmartSkyPos?>()
        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                val api = ISmartSkyPos.Stub.asInterface(binder)
                smartSkyPos = api
                if (!deferred.isCompleted) deferred.complete(api)
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                smartSkyPos = null
            }

            override fun onNullBinding(name: ComponentName?) {
                smartSkyPos = null
                if (!deferred.isCompleted) deferred.complete(null)
            }
        }
        serviceConnection = connection
        val intent = Intent(SMART_SKY_POS_ACTION).setPackage(SMART_SKY_POS_PACKAGE)
        // TODO: confirm exact SmartSkyPos service action/class from SDK docs if action differs.
        val bound = runCatching {
            context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }.getOrDefault(false)
        if (!bound) {
            cleanupConnection(connection)
            return null
        }
        deferred.invokeOnCompletion {
            if (deferred.isCancelled && smartSkyPos == null) {
                cleanupConnection(connection)
            }
        }
        withTimeoutOrNull(SERVICE_BIND_TIMEOUT_MS) { deferred.await() } ?: run {
            cleanupConnection(connection)
            null
        }
    }

    private fun cleanupConnection(connection: ServiceConnection) {
        runCatching { context.unbindService(connection) }
        if (serviceConnection === connection) {
            serviceConnection = null
        }
        if (smartSkyPos == null) {
            serviceConnection = null
        }
    }

    companion object {
        private const val SMART_SKY_POS_PACKAGE = "com.skytech.smartskypos"
        private const val SMART_SKY_POS_ACTION = "com.skytech.smartskyposlib.ISmartSkyPos"
        private const val SERVICE_BIND_TIMEOUT_MS = 3_000L
    }
}
