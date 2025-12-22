package com.everypay.gpay.models

/**
 * Response from EveryPay payment_data endpoint (POST /api/v4/google_pay/payment_data)
 *
 * For regular payments, this indicates the payment processing result.
 * For token requests (MIT/recurring payments), this response indicates whether the token
 * was successfully created and can be used for future charges.
 *
 * @param state Payment/token state - "settled" or "authorized" means ready for use.
 *              For token requests, this indicates the token is ready for MIT payments.
 * @param paymentReference Payment reference for this transaction
 * @param orderReference Order reference provided in the request
 */
data class ProcessPaymentResponse(
    val state: String,
    val paymentReference: String? = null,
    val orderReference: String? = null
)
