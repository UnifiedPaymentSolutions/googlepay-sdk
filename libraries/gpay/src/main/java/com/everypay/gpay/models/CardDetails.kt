package com.everypay.gpay.models

/**
 * Card tokenization details from payment response
 * Contains the MIT token for recurring payments
 *
 * @param token MIT token for recurring payments (24-character alphanumeric string)
 * @param lastFourDigits Last 4 digits of the card number
 * @param month Card expiration month
 * @param year Card expiration year (YYYY format)
 */
data class CardDetails(
    val token: String?,
    val lastFourDigits: String?,
    val month: String?,
    val year: String?
)
