package com.everypay.gpay

import com.everypay.gpay.models.GooglePayTokenData
import com.everypay.gpay.models.PaymentDetailsResponse
import com.everypay.gpay.models.ProcessPaymentResponse
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
     * Google Pay token received
     *
     * The token should be sent to your backend for processing.
     *
     * @param tokenData Extracted Google Pay token data
     * @param paymentData The original payment data from Google Pay
     * @param paymentResponse Response from EveryPay payment_data endpoint (SDK mode only).
     *                        Contains payment state and token information for MIT payments.
     *                        Null for backend mode (backend processes payment_data directly).
     * @param paymentDetails Payment details from GET /payments endpoint (SDK mode only).
     *                       Contains the MIT token in ccDetails.token field for recurring payments.
     *                       Null for backend mode (backend calls GET /payments directly).
     */
    data class TokenReceived(
        val tokenData: GooglePayTokenData,
        val paymentData: PaymentData,
        val paymentResponse: ProcessPaymentResponse? = null,
        val paymentDetails: PaymentDetailsResponse? = null
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
