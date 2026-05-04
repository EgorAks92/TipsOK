package com.chaiok.pos.presentation.payment

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.util.Log
import com.chaiok.pos.domain.model.PaymentResult
import com.skytech.smartskyposlib.TransactionParams
import com.skytech.smartskyposlib.TransactionResult
import com.skytech.smartskyposlib.ui.PaymentActivity
import java.math.BigDecimal
import java.math.RoundingMode
import org.json.JSONObject

object SmartSkyPaymentIntentFactory {

    fun createPaymentIntent(
        amount: BigDecimal,
        waiterId: String,
        terminalId: String,
        tipAmount: Double,
        serviceFee: Double,
        feesCovered: Boolean
    ): Intent {
        val extraTransactionData = buildExtraTransactionData(
            waiterId = waiterId,
            tipAmount = tipAmount,
            serviceFee = serviceFee,
            feesCovered = feesCovered
        )

        Log.i(
            PAYMENT_TAG,
            "creating payment intent amount=$amount " +
                    "terminalId=***${terminalId.takeLast(4)} " +
                    "extraTransactionData=$extraTransactionData"
        )

        val transactionParams = TransactionParams(amount).apply {
            currencyCode = CURRENCY_CODE_RUB
            this.terminalId = terminalId
            this.extraTransactionData = extraTransactionData
        }

        return Intent(SMART_SKY_PAYMENT_ACTION).apply {
            putExtra(PaymentActivity.PARAMS_KEY, transactionParams)
            putExtra(PaymentActivity.TYPE_KEY, PaymentActivity.TYPE_PAYMENT)
        }
    }

    fun mapPaymentActivityResult(
        resultCode: Int,
        data: Intent?
    ): PaymentResult {
        val transactionResult = data?.getTransactionResultCompat()

        Log.i(
            PAYMENT_TAG,
            "transaction result code=${transactionResult?.code} " +
                    "rc=${transactionResult?.rc} " +
                    "message=${transactionResult?.message} " +
                    "approved=${transactionResult?.isApproved}"
        )

        val isApproved = resultCode == Activity.RESULT_OK &&
                (transactionResult == null || transactionResult.isApproved == true)

        return if (isApproved) {
            Log.i(PAYMENT_TAG, "mapped result Approved")

            PaymentResult.Approved(
                transactionId = transactionResult?.receiptNumber?.toString(),
                rrn = transactionResult?.rrn,
                authCode = transactionResult?.authCode,
                rawMessage = transactionResult?.message
                    ?: data?.getStringExtra("message")
                    ?: "Оплата одобрена"
            )
        } else {
            Log.i(PAYMENT_TAG, "mapped result Declined")

            PaymentResult.Declined(
                reason = transactionResult?.message
                    ?: data?.getStringExtra("message")
                    ?: "Оплата отменена или отклонена",
                code = transactionResult?.rc,
                rawMessage = transactionResult?.toString()
            )
        }
    }

    private fun buildExtraTransactionData(
        waiterId: String,
        tipAmount: Double,
        serviceFee: Double,
        feesCovered: Boolean
    ): String {
        return JSONObject()
            .put("waiterId", waiterId)
            .put("tipAmount", amountToExtraString(tipAmount))
            .put("bankFee", "0")
            .put("serviceFee", amountToExtraString(serviceFee))
            .put("feesCovered", feesCovered)
            .toString()
    }

    private fun amountToExtraString(value: Double): String {
        return BigDecimal
            .valueOf(value)
            .setScale(2, RoundingMode.HALF_UP)
            .stripTrailingZeros()
            .toPlainString()
    }

    @Suppress("DEPRECATION")
    private fun Intent.getTransactionResultCompat(): TransactionResult? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getParcelableExtra(
                PaymentActivity.RESULT_KEY,
                TransactionResult::class.java
            )
        } else {
            getParcelableExtra(PaymentActivity.RESULT_KEY) as? TransactionResult
        }
    }

    private const val PAYMENT_TAG = "TipsPaymentFlow"
    private const val SMART_SKY_PAYMENT_ACTION = "com.skytech.smartskypos.PAYMENT"
    private const val CURRENCY_CODE_RUB = "643"
}