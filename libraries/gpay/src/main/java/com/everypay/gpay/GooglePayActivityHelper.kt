package com.everypay.gpay

import android.app.Activity
import android.content.Intent
import android.util.Log
import com.google.android.gms.common.api.Status
import com.google.android.gms.wallet.AutoResolveHelper
import com.google.android.gms.wallet.PaymentData
import com.google.android.gms.wallet.PaymentDataRequest
import org.json.JSONObject

/**
 * Helper class for integrating Google Pay with Android Activities.
 * This class handles the Activity-based payment flow including activity results.
 *
 * Usage:
 * 1. Create an instance in your Activity
 * 2. Call requestPayment() to start the payment flow
 * 3. Call handleActivityResult() in your Activity's onActivityResult()
 *
 * @param activity The Activity that will host the Google Pay flow
 * @param googlePayManager The GooglePayManager instance
 * @param requestCode Request code for the payment activity (default: 991)
 */
class GooglePayActivityHelper(
    private val activity: Activity,
    private val googlePayManager: GooglePayManager,
    private val requestCode: Int = DEFAULT_REQUEST_CODE
) {
    private var paymentCallback: GooglePayPaymentCallback? = null

    companion object {
        const val DEFAULT_REQUEST_CODE = 991
        private const val TAG = "GooglePayActivityHelper"
    }

    /**
     * Starts the Google Pay payment flow
     *
     * @param gateway Payment gateway name
     * @param gatewayMerchantId Gateway merchant ID
     * @param currencyCode Currency code (e.g., "EUR", "USD")
     * @param amount Payment amount as a string
     * @param merchantName Merchant name to display
     * @param callback Callback to receive the payment result
     */
    fun requestPayment(
        gateway: String,
        gatewayMerchantId: String,
        currencyCode: String,
        amount: String,
        merchantName: String,
        callback: GooglePayPaymentCallback
    ) {
        val request = googlePayManager.createPaymentDataRequest(
            gateway,
            gatewayMerchantId,
            currencyCode,
            amount,
            merchantName
        )

        if (request == null) {
            Log.e(TAG, "Failed to create payment data request")
            callback.onResult(
                GooglePayResult.Error(
                    Constants.E_GOOGLE_PAY_API_ERROR,
                    "Failed to create payment data request"
                )
            )
            return
        }

        requestPaymentInternal(request, callback)
    }

    /**
     * Starts the Google Pay payment flow with a custom payment request
     *
     * @param requestJson Custom payment request as JSONObject
     * @param callback Callback to receive the payment result
     */
    fun requestPaymentWithCustomRequest(
        requestJson: JSONObject,
        callback: GooglePayPaymentCallback
    ) {
        val request = googlePayManager.createPaymentDataRequestFromJson(requestJson)

        if (request == null) {
            Log.e(TAG, "Failed to create payment data request from JSON")
            callback.onResult(
                GooglePayResult.Error(
                    Constants.E_GOOGLE_PAY_API_ERROR,
                    "Failed to create payment data request from JSON"
                )
            )
            return
        }

        requestPaymentInternal(request, callback)
    }

    /**
     * Starts the Google Pay payment flow with a PaymentDataRequest
     *
     * @param request The PaymentDataRequest
     * @param callback Callback to receive the payment result
     */
    fun requestPaymentWithRequest(
        request: PaymentDataRequest,
        callback: GooglePayPaymentCallback
    ) {
        requestPaymentInternal(request, callback)
    }

    private fun requestPaymentInternal(
        request: PaymentDataRequest,
        callback: GooglePayPaymentCallback
    ) {
        this.paymentCallback = callback
        Log.d(TAG, "Starting Google Pay payment flow")

        try {
            AutoResolveHelper.resolveTask(
                googlePayManager.getPaymentsClient().loadPaymentData(request),
                activity,
                requestCode
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start payment flow", e)
            callback.onResult(
                GooglePayResult.Error(
                    Constants.E_PAYMENT_ERROR,
                    "Failed to start payment flow: ${e.message}",
                    e
                )
            )
            this.paymentCallback = null
        }
    }

    /**
     * Handles the activity result from Google Pay.
     * Call this method from your Activity's onActivityResult() or registerForActivityResult() callback.
     *
     * @param requestCode The request code from onActivityResult
     * @param resultCode The result code from onActivityResult
     * @param data The intent data from onActivityResult
     * @return true if this was a Google Pay result and was handled, false otherwise
     */
    fun handleActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        if (requestCode != this.requestCode) {
            return false
        }

        val callback = paymentCallback ?: run {
            Log.w(TAG, "Payment callback is null, cannot handle result")
            return true
        }

        when (resultCode) {
            Activity.RESULT_OK -> {
                Log.d(TAG, "Payment successful")
                data?.let { intent ->
                    val paymentData = PaymentData.getFromIntent(intent)
                    if (paymentData != null) {
                        Log.d(TAG, "Payment data: ${paymentData.toJson()}")
                        callback.onResult(GooglePayResult.Success(paymentData))
                    } else {
                        Log.e(TAG, "Payment data is null")
                        callback.onResult(
                            GooglePayResult.Error(
                                Constants.E_PAYMENT_ERROR,
                                "Payment data is null"
                            )
                        )
                    }
                } ?: run {
                    Log.e(TAG, "Intent data is null")
                    callback.onResult(
                        GooglePayResult.Error(
                            Constants.E_PAYMENT_ERROR,
                            "Intent data is null"
                        )
                    )
                }
            }

            Activity.RESULT_CANCELED -> {
                Log.d(TAG, "Payment was canceled by user")
                callback.onResult(GooglePayResult.Canceled)
            }

            AutoResolveHelper.RESULT_ERROR -> {
                val status = AutoResolveHelper.getStatusFromIntent(data)
                handlePaymentError(status, callback)
            }

            else -> {
                Log.w(TAG, "Unexpected result code: $resultCode")
                callback.onResult(
                    GooglePayResult.Error(
                        Constants.E_PAYMENT_ERROR,
                        "Unexpected result code: $resultCode"
                    )
                )
            }
        }

        // Clear the callback after handling
        paymentCallback = null
        return true
    }

    private fun handlePaymentError(status: Status?, callback: GooglePayPaymentCallback) {
        if (status != null) {
            val errorMessage = status.statusMessage ?: "Unknown error"
            val errorCode = status.statusCode.toString()
            Log.e(TAG, "Payment error: $errorMessage (code: $errorCode)")
            callback.onResult(
                GooglePayResult.Error(
                    Constants.E_PAYMENT_ERROR,
                    "Google Pay payment error: $errorMessage (code: $errorCode)"
                )
            )
        } else {
            Log.e(TAG, "Payment error with null status")
            callback.onResult(
                GooglePayResult.Error(
                    Constants.E_PAYMENT_ERROR,
                    "Google Pay payment error with unknown status"
                )
            )
        }
    }

    /**
     * Checks if Google Pay is ready to be used on this device
     *
     * @param callback Callback to receive the result
     */
    fun isReadyToPay(callback: GooglePayReadinessCallback) {
        googlePayManager.isReadyToPay(callback)
    }
}
