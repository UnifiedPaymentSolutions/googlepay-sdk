package com.everypay.gpay

import android.content.Context
import android.util.Log
import com.everypay.gpay.util.PaymentsUtil
import com.google.android.gms.wallet.IsReadyToPayRequest
import com.google.android.gms.wallet.PaymentDataRequest
import com.google.android.gms.wallet.PaymentsClient
import com.google.android.gms.wallet.Wallet
import org.json.JSONObject

/**
 * Core Google Pay manager that handles payment operations.
 * This class manages the PaymentsClient and provides methods for checking readiness
 * and creating payment requests.
 *
 * @param context Android context (can be Application or Activity context)
 * @param environment Google Pay environment (WalletConstants.ENVIRONMENT_TEST or WalletConstants.ENVIRONMENT_PRODUCTION)
 * @param allowedCardNetworks List of allowed card networks (e.g., ["MASTERCARD", "VISA"])
 * @param allowedCardAuthMethods List of allowed authentication methods (e.g., ["PAN_ONLY", "CRYPTOGRAM_3DS"])
 */
class GooglePayManager(
    context: Context,
    private val environment: Int,
    private val allowedCardNetworks: List<String>,
    private val allowedCardAuthMethods: List<String>
) {
    private val paymentsClient: PaymentsClient
    private val appContext: Context = context.applicationContext

    companion object {
        private const val TAG = "GooglePayManager"
    }

    init {
        Log.d(TAG, "Initializing GooglePayManager with environment: $environment")
        val walletOptions = Wallet.WalletOptions.Builder()
            .setEnvironment(environment)
            .build()
        paymentsClient = Wallet.getPaymentsClient(appContext, walletOptions)
    }

    /**
     * Checks if Google Pay is ready to be used on this device
     *
     * @param callback Callback to receive the result
     */
    fun isReadyToPay(callback: GooglePayReadinessCallback) {
        val isReadyToPayJson = PaymentsUtil.getIsReadyToPayRequest(
            allowedCardNetworks,
            allowedCardAuthMethods
        )

        if (isReadyToPayJson == null) {
            Log.e(TAG, "Failed to create isReadyToPay request")
            callback.onResult(
                GooglePayReadinessResult.Error(
                    Constants.E_GOOGLE_PAY_API_ERROR,
                    "Failed to create isReadyToPay request"
                )
            )
            return
        }

        val request = IsReadyToPayRequest.fromJson(isReadyToPayJson.toString())
        if (request == null) {
            Log.e(TAG, "Failed to parse isReadyToPay request")
            callback.onResult(
                GooglePayReadinessResult.Error(
                    Constants.E_GOOGLE_PAY_API_ERROR,
                    "Failed to parse isReadyToPay request"
                )
            )
            return
        }

        val task = paymentsClient.isReadyToPay(request)
        task.addOnCompleteListener { completedTask ->
            Log.d(TAG, "isReadyToPay completed. Success: ${completedTask.isSuccessful}")

            if (completedTask.isSuccessful) {
                val result = completedTask.result ?: false
                Log.d(TAG, "Google Pay readiness result: $result")
                callback.onResult(GooglePayReadinessResult.Success(result))
            } else {
                val exception = completedTask.exception
                Log.e(TAG, "isReadyToPay failed", exception)
                callback.onResult(
                    GooglePayReadinessResult.Error(
                        Constants.E_UNABLE_TO_DETERMINE_GOOGLE_PAY_READINESS,
                        exception?.message ?: "Unknown error",
                        exception
                    )
                )
            }
        }
    }

    /**
     * Creates a PaymentDataRequest for Google Pay
     *
     * @param gateway Payment gateway name
     * @param gatewayMerchantId Gateway merchant ID
     * @param currencyCode Currency code (e.g., "EUR", "USD")
     * @param amount Payment amount as a string
     * @param merchantName Merchant name to display
     * @return PaymentDataRequest or null if creation fails
     */
    fun createPaymentDataRequest(
        gateway: String,
        gatewayMerchantId: String,
        currencyCode: String,
        amount: String,
        merchantName: String
    ): PaymentDataRequest? {
        val paymentDataRequestJson = PaymentsUtil.getPaymentDataRequest(
            allowedCardNetworks,
            allowedCardAuthMethods,
            gateway,
            gatewayMerchantId,
            currencyCode,
            amount,
            merchantName
        )

        if (paymentDataRequestJson == null) {
            Log.e(TAG, "Failed to create payment data request")
            return null
        }

        Log.d(TAG, "Payment data request JSON: $paymentDataRequestJson")
        return PaymentDataRequest.fromJson(paymentDataRequestJson.toString())
    }

    /**
     * Creates a PaymentDataRequest from a custom JSON object
     *
     * @param requestJson Custom payment request as JSONObject
     * @return PaymentDataRequest or null if creation fails
     */
    fun createPaymentDataRequestFromJson(requestJson: JSONObject): PaymentDataRequest? {
        return try {
            Log.d(TAG, "Creating payment data request from custom JSON")
            PaymentDataRequest.fromJson(requestJson.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create payment data request from JSON", e)
            null
        }
    }

    /**
     * Gets the PaymentsClient instance
     * This can be used with GooglePayActivityHelper or for custom integrations
     *
     * @return The PaymentsClient instance
     */
    fun getPaymentsClient(): PaymentsClient = paymentsClient

    /**
     * Gets the configured environment
     *
     * @return The environment value
     */
    fun getEnvironment(): Int = environment

    /**
     * Gets the allowed card networks
     *
     * @return List of allowed card networks
     */
    fun getAllowedCardNetworks(): List<String> = allowedCardNetworks

    /**
     * Gets the allowed card authentication methods
     *
     * @return List of allowed authentication methods
     */
    fun getAllowedCardAuthMethods(): List<String> = allowedCardAuthMethods
}
