package com.everypay.gpay.models

/**
 * Request for processing Google Pay payment token with EveryPay
 *
 * @param paymentReference Payment reference from create payment response
 * @param tokenConsentAgreed Token consent flag
 * @param signature Payment signature from Google Pay token
 * @param intermediateSigningKey Intermediate signing key from Google Pay token
 * @param protocolVersion Protocol version from Google Pay token
 * @param signedMessage Signed message from Google Pay token
 */
data class ProcessPaymentRequest(
    val paymentReference: String,
    val tokenConsentAgreed: Boolean,
    val signature: String,
    val intermediateSigningKey: IntermediateSigningKey,
    val protocolVersion: String,
    val signedMessage: String
)

/**
 * Intermediate signing key from Google Pay token
 *
 * @param signedKey Signed key (JSON string)
 * @param signatures List of signatures
 */
data class IntermediateSigningKey(
    val signedKey: String,
    val signatures: List<String>
)
