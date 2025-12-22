package com.everypay.gpay.util

import com.google.android.gms.wallet.WalletConstants
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * Contains helper methods for dealing with the Google Pay API.
 */
object PaymentsUtil {

    /**
     * Converts environment string to WalletConstants environment value
     *
     * @param environment String indicating "TEST" or "PRODUCTION" environment
     * @return WalletConstants environment value
     */
    fun getEnvironment(environment: String): Int {
        return when (environment.uppercase()) {
            "PRODUCTION" -> WalletConstants.ENVIRONMENT_PRODUCTION
            else -> WalletConstants.ENVIRONMENT_TEST
        }
    }


    /**
     * Create a Google Pay API base request object with properties used in all requests.
     *
     * @return Google Pay API base request object.
     */
    private fun getBaseRequest(): JSONObject {
        return JSONObject().apply {
            put("apiVersion", 2)
            put("apiVersionMinor", 0)
        }
    }

    /**
     * Creates an instance of [JSONObject] which describes a Gateway tokenization method.
     *
     * @param gateway the gateway name
     * @param gatewayMerchantId the gateway merchant ID
     * @return [JSONObject] with gateway tokenization specification
     */
    fun getGatewayTokenizationSpecification(gateway: String, gatewayMerchantId: String): JSONObject {
        return JSONObject().apply {
            put("type", "PAYMENT_GATEWAY")
            put("parameters", JSONObject().apply {
                put("gateway", gateway)
                put("gatewayMerchantId", gatewayMerchantId)
            })
        }
    }

    /**
     * An object describing accepted cards and authentication methods required by your app.
     *
     * @param allowedCardNetworks list of allowed card networks
     * @param allowedCardAuthMethods list of allowed card auth methods
     * @return [JSONObject] which describes allowed payment methods
     */
    fun getBaseCardPaymentMethod(allowedCardNetworks: List<String>, allowedCardAuthMethods: List<String>): JSONObject {
        return JSONObject().apply {
            put("type", "CARD")
            put("parameters", JSONObject().apply {
                put("allowedAuthMethods", JSONArray(allowedCardAuthMethods))
                put("allowedCardNetworks", JSONArray(allowedCardNetworks))
            })
        }
    }

    /**
     * Creates an instance of [JSONObject] which describes allowed payment methods
     *
     * @param allowedCardNetworks list of allowed card networks
     * @param allowedCardAuthMethods list of allowed card auth methods
     * @param gateway the gateway name
     * @param gatewayMerchantId the gateway merchant ID
     * @return [JSONObject] which describes allowed payment methods
     */
    fun getCardPaymentMethod(
        allowedCardNetworks: List<String>,
        allowedCardAuthMethods: List<String>,
        gateway: String,
        gatewayMerchantId: String
    ): JSONObject {
        return getBaseCardPaymentMethod(allowedCardNetworks, allowedCardAuthMethods)
            .put("tokenizationSpecification", getGatewayTokenizationSpecification(gateway, gatewayMerchantId))
    }

    /**
     * Provide Google Pay API with a payment amount, currency, and amount status.
     *
     * @param currencyCode currency of the payment
     * @param amount cost of the item
     * @return information about the requested payment's total.
     */
    fun getTransactionInfo(currencyCode: String, amount: String): JSONObject {
        return JSONObject().apply {
            put("totalPrice", amount)
            put("totalPriceStatus", "FINAL")
            put("currencyCode", currencyCode)
        }
    }

    /**
     * Information about the merchant requesting payment information
     *
     * @param merchantName name of the merchant
     * @return information about the merchant
     */
    fun getMerchantInfo(merchantName: String): JSONObject {
        return JSONObject().apply {
            put("merchantName", merchantName)
        }
    }

    /**
     * An object describing information requested in a Google Pay payment sheet
     *
     * @param allowedCardNetworks list of allowed card networks
     * @param allowedCardAuthMethods list of allowed card auth methods
     * @return information about the requested payment's total.
     */
    fun getIsReadyToPayRequest(allowedCardNetworks: List<String>, allowedCardAuthMethods: List<String>): JSONObject? {
        return try {
            getBaseRequest().apply {
                put("allowedPaymentMethods", JSONArray().apply {
                    put(getBaseCardPaymentMethod(allowedCardNetworks, allowedCardAuthMethods))
                })
            }
        } catch (e: JSONException) {
            null
        }
    }

    /**
     * Creates a complete payment data request for Google Pay
     *
     * @param allowedCardNetworks list of allowed card networks
     * @param allowedCardAuthMethods list of allowed card auth methods
     * @param gateway the gateway name
     * @param gatewayMerchantId the gateway merchant ID
     * @param currencyCode currency of the payment
     * @param amount cost of the item
     * @param merchantName name of the merchant
     * @return complete payment data request as JSONObject
     */
    fun getPaymentDataRequest(
        allowedCardNetworks: List<String>,
        allowedCardAuthMethods: List<String>,
        gateway: String,
        gatewayMerchantId: String,
        currencyCode: String,
        amount: String,
        merchantName: String
    ): JSONObject? {
        return try {
            getBaseRequest().apply {
                put("allowedPaymentMethods", JSONArray().apply {
                    put(getCardPaymentMethod(allowedCardNetworks, allowedCardAuthMethods, gateway, gatewayMerchantId))
                })
                put("transactionInfo", getTransactionInfo(currencyCode, amount))
                put("merchantInfo", getMerchantInfo(merchantName))
            }
        } catch (e: JSONException) {
            null
        }
    }
}