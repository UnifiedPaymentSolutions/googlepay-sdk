package com.everypay.gpay.models

/**
 * Response from EveryPay process payment API
 *
 * @param state Payment state (e.g., "settled", "failed", "waiting_for_3ds")
 * @param paymentReference Payment reference
 * @param orderReference Order reference
 */
data class ProcessPaymentResponse(
    val state: String,
    val paymentReference: String? = null,
    val orderReference: String? = null
)
