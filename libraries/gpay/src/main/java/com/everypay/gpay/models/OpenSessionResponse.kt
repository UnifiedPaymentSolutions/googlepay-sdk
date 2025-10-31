package com.everypay.gpay.models

/**
 * Response from EveryPay open session API
 *
 * @param googlepayMerchantIdentifier Google Pay merchant identifier
 * @param googlepayEpMerchantId EveryPay merchant ID for Google Pay
 * @param googlepayGatewayMerchantId Gateway merchant ID
 * @param merchantName Merchant display name
 * @param googlePayGatewayId Gateway identifier (e.g., "everypay")
 * @param acqBrandingDomainIgw Acquirer branding domain
 */
data class OpenSessionResponse(
    val googlepayMerchantIdentifier: String,
    val googlepayEpMerchantId: String,
    val googlepayGatewayMerchantId: String,
    val merchantName: String,
    val googlePayGatewayId: String,
    val acqBrandingDomainIgw: String
)
