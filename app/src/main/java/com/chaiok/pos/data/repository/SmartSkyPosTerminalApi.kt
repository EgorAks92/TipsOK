package com.chaiok.pos.data.repository

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.os.RemoteException
import android.util.Log
import com.chaiok.pos.domain.model.PaymentResult
import com.skytech.smartskyposlib.ISmartSkyPos
import com.skytech.smartskyposlib.TransactionCallback
import com.skytech.smartskyposlib.TransactionParams
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.math.BigDecimal
import java.math.RoundingMode

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

    override suspend fun pay(amountRub: Double): PaymentResult = withContext(Dispatchers.IO) {
        if (amountRub <= 0.0) return@withContext PaymentResult.Error("Сумма должна быть больше нуля")
        val service = connectIfNeeded() ?: return@withContext PaymentResult.Error("Терминал недоступен")

        try {
            val amount = BigDecimal.valueOf(amountRub).setScale(2, RoundingMode.HALF_UP)
            val params = TransactionParams(amount, CURRENCY_CODE)
            Log.i(PAYMENT_TAG, "payment started amount=$amount")

            val callback = object : TransactionCallback.Stub() {
                override fun onStateChanged(state: Int, message: String?) {
                    Log.d(PAYMENT_TAG, "state=$state message=$message")
                }

                override fun onQrReading(qrData: String?, qrType: String?) {
                    Log.d(PAYMENT_TAG, "qrReading type=$qrType")
                }

                override fun onOperationNameChanged(operationName: String?) {
                    Log.d(PAYMENT_TAG, "operation=$operationName")
                }

                override fun onRequestPassword(prompt: String?): String = ""
            }

            val result = service.payment(params, callback)
                ?: return@withContext PaymentResult.Error("Пустой ответ терминала")

            val approved = result.isApproved() == true || result.getCode() == 0
            if (approved) {
                PaymentResult.Approved(
                    transactionId = result.getReceiptNumber().toString(),
                    rrn = result.getRrn(),
                    authCode = result.getAuthCode(),
                    rawMessage = result.getMessage()
                )
            } else {
                PaymentResult.Declined(
                    reason = result.getMessage().orEmpty().ifBlank { "Оплата отклонена" },
                    code = result.getRc(),
                    rawMessage = result.toString()
                )
            }
        } catch (error: RemoteException) {
            Log.e(PAYMENT_TAG, "RemoteException", error)
            PaymentResult.Error("Ошибка связи с терминалом", error)
        } catch (error: SecurityException) {
            Log.e(PAYMENT_TAG, "SecurityException", error)
            PaymentResult.Error("Нет доступа к терминалу", error)
        } catch (error: Exception) {
            Log.e(PAYMENT_TAG, "Unexpected payment error", error)
            PaymentResult.Error(error.message ?: "Ошибка оплаты", error)
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
        private const val PAYMENT_TAG = "TipsPaymentFlow"
        private const val SMART_SKY_POS_PACKAGE = "com.skytech.smartskypos"
        private const val SMART_SKY_POS_ACTION = "com.skytech.smartskypos.ISmartSkyPos"
        private const val SERVICE_BIND_TIMEOUT_MS = 3_000L
        private const val CURRENCY_CODE = "643"
    }
}
