package com.everypay.gpay.models

/**
 * Backend data for Google Pay integration (combines open_session + create_payment responses)
 *
 * The backend should make two EveryPay API calls:
 * 1. POST /api/v4/google_pay/open_session
 * 2. POST /api/v4/payments/oneoff
 *
 * Then combine the responses into this model and send to the Android SDK.
 * This keeps API credentials secure on the backend.
 *
 * @param merchantId Google Pay merchant identifier (from open_session: googlepay_merchant_identifier)
 * @param merchantName Merchant display name shown to user (from open_session: merchant_name)
 * @param gatewayId Gateway identifier, e.g., "everypay" (from open_session: google_pay_gateway_id)
 * @param gatewayMerchantId Gateway merchant ID (from open_session: googlepay_gateway_merchant_id)
 * @param currency Payment currency code, e.g., "EUR" (from create_payment: currency)
 * @param countryCode Country code for payment, e.g., "EE" (from create_payment: descriptor_country)
 * @param paymentReference Unique payment reference (from create_payment: payment_reference)
 * @param mobileAccessToken Access token for processing payment (from create_payment: mobile_access_token)
 * @param amount Payment amount to display (from create_payment: standing_amount)
 * @param label Payment label/description to display (from create_payment: label)
 */
data class GooglePayBackendData(
    val merchantId: String,
    val merchantName: String,
    val gatewayId: String,
    val gatewayMerchantId: String,
    val currency: String,
    val countryCode: String,
    val paymentReference: String,
    val mobileAccessToken: String,
    val amount: Double,
    val label: String
) {
    init {
        require(merchantId.isNotBlank()) { "merchantId cannot be blank" }
        require(merchantName.isNotBlank()) { "merchantName cannot be blank" }
        require(gatewayId.isNotBlank()) { "gatewayId cannot be blank" }
        require(gatewayMerchantId.isNotBlank()) { "gatewayMerchantId cannot be blank" }
        require(currency.isNotBlank()) { "currency cannot be blank" }
        require(countryCode.isNotBlank()) { "countryCode cannot be blank" }
        require(paymentReference.isNotBlank()) { "paymentReference cannot be blank" }
        require(mobileAccessToken.isNotBlank()) { "mobileAccessToken cannot be blank" }
        require(amount > 0) { "amount must be greater than 0" }
        require(label.isNotBlank()) { "label cannot be blank" }
    }
}
