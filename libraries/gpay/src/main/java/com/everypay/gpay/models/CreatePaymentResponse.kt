package com.everypay.gpay.models

/**
 * Response from EveryPay create payment API
 *
 * @param paymentReference Unique payment reference
 * @param mobileAccessToken Access token for mobile payment processing
 * @param currency Payment currency code
 * @param descriptorCountry Descriptor country code
 * @param googlepayMerchantIdentifier Google Pay merchant identifier
 * @param accountName Account name
 * @param orderReference Order reference
 * @param initialAmount Initial payment amount
 * @param standingAmount Standing/remaining amount
 * @param paymentState Current payment state
 */
data class CreatePaymentResponse(
    val paymentReference: String,
    val mobileAccessToken: String,
    val currency: String,
    val descriptorCountry: String,
    val googlepayMerchantIdentifier: String,
    val accountName: String,
    val orderReference: String,
    val initialAmount: Double,
    val standingAmount: Double,
    val paymentState: String
)
