package com.everypay.gpay.api

import android.util.Base64
import android.util.Log
import com.everypay.gpay.models.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * HTTP client for EveryPay API interactions
 *
 * @param config EveryPay configuration
 */
class EverypayApiClient(private val config: EverypayConfig) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    companion object {
        private const val TAG = "EverypayApiClient"
    }

    /**
     * Opens a session with EveryPay to get Google Pay gateway configuration
     *
     * Note: This method is only called in SDK mode where credentials are required and validated
     *
     * @return OpenSessionResponse containing gateway configuration
     * @throws IOException if network request fails
     * @throws Exception if response parsing fails
     */
    fun openSession(): OpenSessionResponse {
        // Safe to use !! because this is only called in SDK mode where config validation ensures these are not null
        val url = "${config.apiUrl!!}/api/v4/google_pay/open_session"

        val requestBody = JSONObject().apply {
            put("api_username", config.apiUsername!!)
            put("account_name", config.accountName!!)
        }

        val request = Request.Builder()
            .url(url)
            .post(requestBody.toString().toRequestBody(jsonMediaType))
            .addHeader("Authorization", createBasicAuthHeader())
            .addHeader("Content-Type", "application/json")
            .build()

        Log.d(TAG, "Opening EveryPay session: $url")

        client.newCall(request).execute().use { response ->
            val responseBody = response.body?.string()

            if (!response.isSuccessful) {
                val apiError = parseErrorResponse(response.code, responseBody)
                val errorMessage = apiError.toFormattedMessage()
                Log.e(TAG, "Failed to open session: $errorMessage")
                throw IOException("Failed to open session: $errorMessage")
            }

            if (responseBody == null) {
                Log.e(TAG, "Empty response body from open session")
                throw IOException("Empty response body from open session")
            }

            Log.d(TAG, "Session opened successfully")
            return parseOpenSessionResponse(responseBody)
        }
    }

    /**
     * Creates a payment in EveryPay
     *
     * @param request Payment creation request
     * @return CreatePaymentResponse containing payment details and access token
     * @throws IOException if network request fails
     * @throws Exception if response parsing fails
     */
    fun createPayment(request: CreatePaymentRequest): CreatePaymentResponse {
        val url = "${config.apiUrl}/api/v4/payments/oneoff"

        val requestBody = JSONObject().apply {
            put("api_username", request.apiUsername)
            put("account_name", request.accountName)
            put("amount", request.amount)
            put("label", request.label)
            put("currency_code", request.currencyCode)
            put("country_code", request.countryCode)
            put("order_reference", request.orderReference)
            put("nonce", request.nonce)
            put("mobile_payment", request.mobilePayment)
            put("customer_url", request.customerUrl)
            put("customer_ip", request.customerIp)
            put("customer_email", request.customerEmail)
            put("timestamp", request.timestamp)
        }

        val httpRequest = Request.Builder()
            .url(url)
            .post(requestBody.toString().toRequestBody(jsonMediaType))
            .addHeader("Authorization", createBasicAuthHeader())
            .addHeader("Content-Type", "application/json")
            .build()

        Log.d(TAG, "Creating payment: $url")

        client.newCall(httpRequest).execute().use { response ->
            val responseBody = response.body?.string()

            if (!response.isSuccessful) {
                val apiError = parseErrorResponse(response.code, responseBody)
                val errorMessage = apiError.toFormattedMessage()
                Log.e(TAG, "Failed to create payment: $errorMessage")
                throw IOException("Failed to create payment: $errorMessage")
            }

            if (responseBody == null) {
                Log.e(TAG, "Empty response body from create payment")
                throw IOException("Empty response body from create payment")
            }

            Log.d(TAG, "Payment created successfully")
            return parseCreatePaymentResponse(responseBody)
        }
    }

    /**
     * Processes Google Pay payment token with EveryPay
     *
     * @param accessToken Mobile access token from create payment response
     * @param request Payment processing request containing Google Pay token data
     * @return ProcessPaymentResponse containing payment state
     * @throws IOException if network request fails
     * @throws Exception if response parsing fails
     */
    fun processPayment(accessToken: String, request: ProcessPaymentRequest): ProcessPaymentResponse {
        val url = "${config.apiUrl}/api/v4/google_pay/payment_data"

        val requestBody = JSONObject().apply {
            put("payment_reference", request.paymentReference)
            put("token_consent_agreed", request.tokenConsentAgreed)
            put("signature", request.signature)
            put("intermediateSigningKey", JSONObject().apply {
                put("signedKey", request.intermediateSigningKey.signedKey)
                put("signatures", JSONArray(request.intermediateSigningKey.signatures))
            })
            put("protocolVersion", request.protocolVersion)
            put("signedMessage", request.signedMessage)
        }

        val httpRequest = Request.Builder()
            .url(url)
            .post(requestBody.toString().toRequestBody(jsonMediaType))
            .addHeader("Authorization", "Bearer $accessToken")
            .addHeader("Content-Type", "application/json")
            .build()

        Log.d(TAG, "Processing payment: $url")

        client.newCall(httpRequest).execute().use { response ->
            val responseBody = response.body?.string()

            if (!response.isSuccessful) {
                val apiError = parseErrorResponse(response.code, responseBody)
                val errorMessage = apiError.toFormattedMessage()
                Log.e(TAG, "Failed to process payment: $errorMessage")
                throw IOException("Failed to process payment: $errorMessage")
            }

            if (responseBody == null) {
                Log.e(TAG, "Empty response body from process payment")
                throw IOException("Empty response body from process payment")
            }

            Log.d(TAG, "Payment processed successfully")
            return parseProcessPaymentResponse(responseBody)
        }
    }

    /**
     * Creates Basic Authentication header
     *
     * Note: This method is only called in SDK mode where credentials are required and validated
     */
    private fun createBasicAuthHeader(): String {
        // Safe to use !! because this is only called in SDK mode where config validation ensures these are not null
        val credentials = "${config.apiUsername!!}:${config.apiSecret!!}"
        val encodedCredentials = Base64.encodeToString(
            credentials.toByteArray(Charsets.UTF_8),
            Base64.NO_WRAP
        )
        return "Basic $encodedCredentials"
    }

    /**
     * Parses error response from EveryPay API
     */
    private fun parseErrorResponse(httpStatus: Int, responseBody: String?): EverypayApiError {
        if (responseBody.isNullOrBlank()) {
            return EverypayApiError(
                code = null,
                message = "Empty response body",
                httpStatus = httpStatus,
                rawResponse = null
            )
        }

        return try {
            val json = JSONObject(responseBody)

            // EveryPay may return errors in different formats, try to parse common structures
            val errorCode = when {
                json.has("error_code") -> json.optString("error_code")
                json.has("code") -> json.optString("code")
                json.has("error") -> {
                    val errorObj = json.optJSONObject("error")
                    errorObj?.optString("code")
                }
                else -> null
            }

            val errorMessage = when {
                json.has("error_message") -> json.optString("error_message")
                json.has("message") -> json.optString("message")
                json.has("error") -> {
                    val errorObj = json.optJSONObject("error")
                    errorObj?.optString("message") ?: json.optString("error")
                }
                else -> null
            }

            EverypayApiError(
                code = errorCode,
                message = errorMessage,
                httpStatus = httpStatus,
                rawResponse = responseBody
            )
        } catch (e: Exception) {
            // If JSON parsing fails, return raw response
            EverypayApiError(
                code = null,
                message = null,
                httpStatus = httpStatus,
                rawResponse = responseBody
            )
        }
    }

    /**
     * Parses open session response JSON
     */
    private fun parseOpenSessionResponse(json: String): OpenSessionResponse {
        val jsonObject = JSONObject(json)
        return OpenSessionResponse(
            googlepayMerchantIdentifier = jsonObject.getString("googlepay_merchant_identifier"),
            googlepayEpMerchantId = jsonObject.getString("googlepay_ep_merchant_id"),
            googlepayGatewayMerchantId = jsonObject.getString("googlepay_gateway_merchant_id"),
            merchantName = jsonObject.getString("merchant_name"),
            googlePayGatewayId = jsonObject.getString("google_pay_gateway_id"),
            acqBrandingDomainIgw = jsonObject.getString("acq_branding_domain_igw")
        )
    }

    /**
     * Parses create payment response JSON
     */
    private fun parseCreatePaymentResponse(json: String): CreatePaymentResponse {
        val jsonObject = JSONObject(json)
        return CreatePaymentResponse(
            paymentReference = jsonObject.getString("payment_reference"),
            mobileAccessToken = jsonObject.getString("mobile_access_token"),
            currency = jsonObject.getString("currency"),
            descriptorCountry = jsonObject.getString("descriptor_country"),
            googlepayMerchantIdentifier = jsonObject.getString("googlepay_merchant_identifier"),
            accountName = jsonObject.getString("account_name"),
            orderReference = jsonObject.getString("order_reference"),
            initialAmount = jsonObject.getDouble("initial_amount"),
            standingAmount = jsonObject.getDouble("standing_amount"),
            paymentState = jsonObject.getString("payment_state")
        )
    }

    /**
     * Parses process payment response JSON
     */
    private fun parseProcessPaymentResponse(json: String): ProcessPaymentResponse {
        val jsonObject = JSONObject(json)
        return ProcessPaymentResponse(
            state = jsonObject.getString("state"),
            paymentReference = if (jsonObject.has("payment_reference")) jsonObject.getString("payment_reference") else null,
            orderReference = if (jsonObject.has("order_reference")) jsonObject.getString("order_reference") else null
        )
    }
}
