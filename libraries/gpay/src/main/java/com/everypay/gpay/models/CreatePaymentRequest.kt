package com.everypay.gpay.models

/**
 * Request for creating a payment in EveryPay
 *
 * @param apiUsername EveryPay API username
 * @param accountName EveryPay account name
 * @param amount Payment amount (e.g., 10.50)
 * @param label Payment label/description
 * @param currencyCode ISO 4217 currency code
 * @param countryCode ISO 3166-1 alpha-2 country code
 * @param orderReference Unique order reference/ID
 * @param nonce Unique transaction identifier (UUID)
 * @param mobilePayment Flag indicating mobile payment (always true for Google Pay)
 * @param customerUrl Customer/merchant URL
 * @param customerIp Customer IP address (optional)
 * @param customerEmail Customer email address
 * @param timestamp Payment creation timestamp (ISO 8601 format)
 */
data class CreatePaymentRequest(
    val apiUsername: String,
    val accountName: String,
    val amount: Double,
    val label: String,
    val currencyCode: String,
    val countryCode: String,
    val orderReference: String,
    val nonce: String,
    val mobilePayment: Boolean = true,
    val customerUrl: String,
    val customerIp: String,
    val customerEmail: String,
    val timestamp: String
)
