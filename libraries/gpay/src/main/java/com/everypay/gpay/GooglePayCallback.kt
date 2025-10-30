package com.everypay.gpay

/**
 * Callback interface for Google Pay readiness check
 */
fun interface GooglePayReadinessCallback {
    /**
     * Called when the readiness check completes
     * @param result The result of the readiness check
     */
    fun onResult(result: GooglePayReadinessResult)
}

/**
 * Callback interface for Google Pay payment operations
 */
fun interface GooglePayPaymentCallback {
    /**
     * Called when the payment operation completes
     * @param result The result of the payment operation
     */
    fun onResult(result: GooglePayResult)
}
