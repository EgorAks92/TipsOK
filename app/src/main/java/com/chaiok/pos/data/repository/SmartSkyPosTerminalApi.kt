package com.chaiok.pos.data.repository

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.os.RemoteException
import android.util.Log
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

    override suspend fun getTerminalData(): PaymentTerminalDataResult = withContext(Dispatchers.IO) {
        val service = connectIfNeeded() ?: return@withContext PaymentTerminalDataResult.Error(
            reason = "SmartSkyPos service is not available",
            type = PaymentTerminalDataErrorType.NotReady
        )

        try {
            Log.e(TAG, "getTerminalData started")
            val terminalData = service.getTerminalData() ?: return@withContext PaymentTerminalDataResult.Error(
                reason = "TerminalData is null",
                type = PaymentTerminalDataErrorType.NotReady
            ).also {
                Log.e(TAG, "TerminalData is null")
            }

            val code = terminalData.getCode()
            val message = terminalData.getMessage()
            Log.e(TAG, "getTerminalData code=$code, message=$message")
            if (code != 0) {
                return@withContext PaymentTerminalDataResult.Error(
                    reason = message,
                    type = PaymentTerminalDataErrorType.NotReady
                )
            }

            val serialNumber = terminalData.getSerialNumber().orEmpty().trim()

            val tid = "47310967"

            if (serialNumber.isBlank() || tid.isBlank()) {
                Log.e(TAG, "TerminalData missing serialNumber or tid")
                return@withContext PaymentTerminalDataResult.Error(
                    reason = "TerminalData is missing serialNumber or tid",
                    type = PaymentTerminalDataErrorType.InvalidData
                )
            }

            Log.e(TAG, "terminal data loaded serial=***${serialNumber.takeLast(4)} tid=***${tid.takeLast(4)}")
            PaymentTerminalDataResult.Success(
                PaymentTerminalData(
                    serialNumber = serialNumber,
                    tid = tid
                )
            )
        } catch (error: RemoteException) {
            Log.e(TAG, "getTerminalData RemoteException", error)
            PaymentTerminalDataResult.Error(
                reason = error.message,
                type = PaymentTerminalDataErrorType.NotReady
            )
        } catch (error: SecurityException) {
            Log.e(TAG, "getTerminalData SecurityException", error)
            PaymentTerminalDataResult.Error(
                reason = error.message,
                type = PaymentTerminalDataErrorType.NotReady
            )
        } catch (error: Exception) {
            Log.e(TAG, "getTerminalData unexpected error", error)
            PaymentTerminalDataResult.Error(
                reason = error.message,
                type = PaymentTerminalDataErrorType.NotReady
            )
        }
    }

    private suspend fun connectIfNeeded(): ISmartSkyPos? = bindMutex.withLock {
        smartSkyPos?.let { return it }

        Log.e(TAG, "bind started")
        val deferred = CompletableDeferred<ISmartSkyPos?>()
        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                val api = ISmartSkyPos.Stub.asInterface(binder)
                smartSkyPos = api
                Log.e(TAG, "bind success")
                if (!deferred.isCompleted) deferred.complete(api)
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                smartSkyPos = null
                Log.e(TAG, "service disconnected")
            }

            override fun onNullBinding(name: ComponentName?) {
                Log.e(TAG, "bind null binding")
                smartSkyPos = null
                if (!deferred.isCompleted) deferred.complete(null)
            }
        }

        val intent = Intent(SMART_SKY_POS_ACTION).setPackage(SMART_SKY_POS_PACKAGE)
        val bound = try {
            context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        } catch (error: SecurityException) {
            Log.e(TAG, "bind security exception", error)
            false
        } catch (error: Exception) {
            Log.e(TAG, "bind failed", error)
            false
        }

        if (!bound) {
            Log.e(TAG, "bind failed: service unavailable")
            return null
        }
        Log.e(TAG, "bind success")

        val api = withTimeoutOrNull(SERVICE_BIND_TIMEOUT_MS) { deferred.await() }
        if (api == null) {
            Log.e(TAG, "bind timeout")
            runCatching { context.unbindService(connection) }
        }

        api
    }

    companion object {
        private const val TAG = "SmartSkyPosTerminalApi"
        private const val SMART_SKY_POS_PACKAGE = "com.skytech.smartskypos"
        private const val SMART_SKY_POS_ACTION = "com.skytech.smartskypos.ISmartSkyPos"
        private const val SERVICE_BIND_TIMEOUT_MS = 3_000L
    }
}
