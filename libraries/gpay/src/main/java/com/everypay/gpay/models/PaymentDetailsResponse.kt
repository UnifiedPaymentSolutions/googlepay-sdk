package com.everypay.gpay.models

/**
 * Response from GET /api/v4/payments/{payment_reference}
 * Contains payment details including the MIT token for recurring payments
 *
 * @param paymentReference Unique payment identifier
 * @param paymentState Current payment state (e.g., "settled", "authorized", "completed")
 * @param ccDetails Card tokenization details - contains the MIT token in ccDetails.token
 * @param orderReference Merchant's order reference
 * @param traceId Unique identifier tracking the initial tokenization transaction
 * @param paymentMethod Payment method used (e.g., "card")
 * @param accountName Processing account ID
 * @param initialAmount Original transaction amount
 * @param standingAmount Amount after refunds/voids
 */
data class PaymentDetailsResponse(
    val paymentReference: String,
    val paymentState: String,
    val ccDetails: CardDetails?,
    val orderReference: String?,
    val traceId: String?,
    val paymentMethod: String? = null,
    val accountName: String? = null,
    val initialAmount: Double? = null,
    val standingAmount: Double? = null
)
