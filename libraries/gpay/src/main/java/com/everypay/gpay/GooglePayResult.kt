package com.everypay.gpay

import com.everypay.gpay.models.GooglePayTokenData
import com.google.android.gms.wallet.PaymentData

/**
 * Sealed class representing the result of a Google Pay operation
 */
sealed class GooglePayResult {
    /**
     * Successful payment result (SDK mode only)
     * Payment was processed by EveryPay and completed successfully
     * @param paymentData The payment data returned by Google Pay
     */
    data class Success(val paymentData: PaymentData) : GooglePayResult()

    /**
     * Google Pay token received (Backend mode only)
     * The token should be sent to your backend for processing via POST /api/v4/google_pay/payment_data
     * @param tokenData Extracted Google Pay token data to send to backend
     * @param paymentData The original payment data from Google Pay
     */
    data class TokenReceived(
        val tokenData: GooglePayTokenData,
        val paymentData: PaymentData
    ) : GooglePayResult()

    /**
     * Payment was canceled by the user
     */
    data object Canceled : GooglePayResult()

    /**
     * An error occurred during the payment process
     * @param code Error code
     * @param message Error message
     * @param exception Optional exception that caused the error
     */
    data class Error(
        val code: String,
        val message: String,
        val exception: Throwable? = null
    ) : GooglePayResult()
}

/**
 * Sealed class representing the result of Google Pay readiness check
 */
sealed class GooglePayReadinessResult {
    /**
     * Google Pay is ready to be used
     * @param isReady true if Google Pay is available
     */
    data class Success(val isReady: Boolean) : GooglePayReadinessResult()

    /**
     * An error occurred while checking readiness
     * @param code Error code
     * @param message Error message
     * @param exception Optional exception that caused the error
     */
    data class Error(
        val code: String,
        val message: String,
        val exception: Throwable? = null
    ) : GooglePayReadinessResult()
}
