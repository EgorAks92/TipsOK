package com.chaiok.pos.data.ecr

import android.content.Context
import android.os.Bundle
import android.util.Log
import java.io.IOException
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Loads the Centerm Cpay SDK at runtime so Kozen builds do not acquire a hard
 * compile-time dependency on a vendor AAR. On a Centerm image the classes must
 * be provided by the firmware/shared SDK or packaged with the application.
 */
class CentermSdkMcuDeviceProvider(
    context: Context
) : CentermMcuDeviceProvider {
    private val appContext = context.applicationContext

    override suspend fun create(): CentermMcuDevice = suspendCancellableCoroutine { continuation ->
        val completed = AtomicBoolean(false)
        continuation.invokeOnCancellation { completed.set(true) }

        fun complete(result: Result<CentermMcuDevice>) {
            if (completed.compareAndSet(false, true) && continuation.isActive) {
                result.fold(continuation::resume, continuation::resumeWithException)
            }
        }

        try {
            val classLoader = appContext.classLoader
            val devicesFactoryClass = Class.forName(DEVICES_FACTORY_CLASS, true, classLoader)
            val callbackClass = Class.forName(RESULT_CALLBACK_CLASS, true, classLoader)
            if (!callbackClass.isInterface) {
                complete(Result.failure(IOException("Centerm ResultCallback is not an interface")))
                return@suspendCancellableCoroutine
            }

            val callback = Proxy.newProxyInstance(
                callbackClass.classLoader ?: classLoader,
                arrayOf(callbackClass)
            ) { proxy, method, args ->
                when (method.name) {
                    "onFinish" -> {
                        runCatching {
                            val deviceManager = args?.firstOrNull()
                                ?: throw IOException("Centerm DevicesFactory returned null DeviceManager")
                            val sdkDevice = deviceManager.javaClass
                                .getMethod("getMcuDevice")
                                .invoke(deviceManager)
                                ?: throw IOException("Centerm DeviceManager returned null McuDevice")
                            ReflectiveCentermMcuDevice(sdkDevice, classLoader)
                        }.fold(
                            onSuccess = { complete(Result.success(it)) },
                            onFailure = { complete(Result.failure(it.unwrapReflectionException())) }
                        )
                        null
                    }

                    "onError" -> {
                        val code = args?.getOrNull(0)
                        val message = args?.getOrNull(1)
                        complete(Result.failure(IOException("Centerm SDK initialization failed: code=$code message=$message")))
                        null
                    }

                    "toString" -> "CentermResultCallback"
                    "hashCode" -> System.identityHashCode(proxy)
                    "equals" -> proxy === args?.firstOrNull()
                    else -> null
                }
            }

            val createMethod = devicesFactoryClass.methods.firstOrNull { method ->
                method.name == "create" &&
                    method.parameterTypes.size == 2 &&
                    method.parameterTypes[0].isAssignableFrom(appContext.javaClass) &&
                    method.parameterTypes[1].isAssignableFrom(callbackClass)
            } ?: throw NoSuchMethodException("Centerm DevicesFactory.create(Context, ResultCallback)")

            createMethod.invoke(null, appContext, callback)
        } catch (throwable: Throwable) {
            val cause = throwable.unwrapReflectionException()
            Log.e(TAG, "Centerm SDK is unavailable", cause)
            complete(
                Result.failure(
                    IOException(
                        "Centerm Cpay SDK classes are unavailable. Add the SDK AAR or expose it as a shared device library.",
                        cause
                    )
                )
            )
        }
    }

    private companion object {
        private const val TAG = "CentermSdkProvider"
        private const val DEVICES_FACTORY_CLASS = "com.pos.sdk.DevicesFactory"
        private const val RESULT_CALLBACK_CLASS = "com.pos.sdk.callback.ResultCallback"
    }
}

private class ReflectiveCentermMcuDevice(
    private val delegate: Any,
    private val classLoader: ClassLoader
) : CentermMcuDevice {
    private val delegateClass = delegate.javaClass
    private val openMethod: Method = delegateClass.getMethod("open", Integer.TYPE, Bundle::class.java)
    private val receiveMethod: Method = delegateClass.getMethod(
        "receiveData",
        ByteArray::class.java,
        Integer.TYPE
    )
    private val sendMethod: Method = delegateClass.getMethod("sendData", ByteArray::class.java)
    private val closeMethod: Method = delegateClass.getMethod("close")

    override fun open(): Boolean {
        val parameters = serialPortParameters()
        return openMethod.invoke(delegate, CentermMcuTransport.BAUD_RATE, parameters) as? Boolean
            ?: throw IOException("Centerm McuDevice.open returned a non-boolean value")
    }

    override fun receiveData(buffer: ByteArray, timeout: Int): Int =
        (receiveMethod.invoke(delegate, buffer, timeout) as? Number)?.toInt()
            ?: throw IOException("Centerm McuDevice.receiveData returned a non-numeric value")

    override fun sendData(bytes: ByteArray): Boolean =
        sendMethod.invoke(delegate, bytes) as? Boolean
            ?: throw IOException("Centerm McuDevice.sendData returned a non-boolean value")

    override fun close(): Boolean {
        val result = closeMethod.invoke(delegate)
        return result as? Boolean ?: true
    }

    private fun serialPortParameters(): Bundle {
        val parameterClass = Class.forName(SERIAL_PORT_PARAMETER_CLASS, true, classLoader)
        return Bundle().apply {
            putByte(parameterClass.stringField("TAG_DATA_BITS"), parameterClass.byteField("DATA_BITS_8"))
            putByte(parameterClass.stringField("TAG_CHECK_BITS"), parameterClass.byteField("CHECK_BITS_NONE"))
            putByte(parameterClass.stringField("TAG_STOP_BITS"), parameterClass.byteField("STOP_BITS_1"))
            putByte(parameterClass.stringField("TAG_FLOW_CONTROL"), parameterClass.byteField("FLOW_CONTROL_NONE"))
        }
    }

    private fun Class<*>.stringField(name: String): String =
        getField(name).get(null) as? String
            ?: throw IOException("Centerm SerialPortParameter.$name is not a String")

    private fun Class<*>.byteField(name: String): Byte =
        (getField(name).get(null) as? Number)?.toByte()
            ?: throw IOException("Centerm SerialPortParameter.$name is not numeric")

    private companion object {
        private const val SERIAL_PORT_PARAMETER_CLASS = "com.pos.sdk.serialport.SerialPortParameter"
    }
}

private fun Throwable.unwrapReflectionException(): Throwable =
    if (this is InvocationTargetException) targetException ?: this else this
