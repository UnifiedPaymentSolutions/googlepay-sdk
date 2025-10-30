package com.everypay.gpay

import com.google.android.gms.wallet.PaymentData

/**
 * Sealed class representing the result of a Google Pay operation
 */
sealed class GooglePayResult {
    /**
     * Successful payment result
     * @param paymentData The payment data returned by Google Pay
     */
    data class Success(val paymentData: PaymentData) : GooglePayResult()

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
