package com.everypay.gpay.models

import org.json.JSONArray
import org.json.JSONObject

/**
 * Google Pay token data extracted by SDK to be sent to backend for processing
 *
 * The SDK extracts the Google Pay token and combines it with backend data.
 * Your app should send this to your backend, which will call:
 * POST /api/v4/google_pay/payment_data
 *
 * @param paymentReference Payment reference (from backend data)
 * @param mobileAccessToken Access token for processing (from backend data)
 * @param signature Payment signature from Google Pay token
 * @param intermediateSigningKey Intermediate signing key from Google Pay token
 * @param protocolVersion Protocol version from Google Pay token
 * @param signedMessage Signed message from Google Pay token
 * @param tokenConsentAgreed Token consent flag - true when requesting token for MIT/recurring payments,
 *                            false for immediate one-time payments (default: false)
 */
data class GooglePayTokenData(
    val paymentReference: String,
    val mobileAccessToken: String,
    val signature: String,
    val intermediateSigningKey: IntermediateSigningKey,
    val protocolVersion: String,
    val signedMessage: String,
    val tokenConsentAgreed: Boolean = false
) {
    /**
     * Converts to ProcessPaymentRequest for direct API usage
     * Useful if backend wants to use the same model as SDK
     */
    fun toProcessPaymentRequest(): ProcessPaymentRequest {
        return ProcessPaymentRequest(
            paymentReference = paymentReference,
            tokenConsentAgreed = tokenConsentAgreed,
            signature = signature,
            intermediateSigningKey = intermediateSigningKey,
            protocolVersion = protocolVersion,
            signedMessage = signedMessage
        )
    }

    /**
     * Converts to JSON for sending to backend
     * Includes all fields needed for the backend to process the payment via EveryPay API
     *
     * Format matches EveryPay's POST /api/v4/google_pay/payment_data endpoint:
     * - payment_reference, token_consent_agreed, signature: snake_case
     * - intermediateSigningKey, protocolVersion, signedMessage: camelCase
     * - mobile_access_token: included for backend convenience (not sent to EveryPay in body)
     */
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("payment_reference", paymentReference)
            put("mobile_access_token", mobileAccessToken)
            put("token_consent_agreed", tokenConsentAgreed)
            put("signature", signature)
            put("intermediateSigningKey", JSONObject().apply {
                put("signedKey", intermediateSigningKey.signedKey)
                put("signatures", JSONArray(intermediateSigningKey.signatures))
            })
            put("protocolVersion", protocolVersion)
            put("signedMessage", signedMessage)
        }
    }
}
