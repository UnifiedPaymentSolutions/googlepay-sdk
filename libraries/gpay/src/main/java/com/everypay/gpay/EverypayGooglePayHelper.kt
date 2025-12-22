package com.everypay.gpay

import android.app.Activity
import android.content.Intent
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.everypay.gpay.api.EverypayApiClient
import com.everypay.gpay.models.*
import com.everypay.gpay.util.PaymentsUtil
import com.google.android.gms.wallet.PaymentData
import com.google.android.gms.wallet.WalletConstants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

/**
 * High-level helper for EveryPay Google Pay integration.
 * Handles all API calls and Google Pay flow automatically.
 *
 * Usage:
 * ```
 * val helper = EverypayGooglePayHelper(activity, config)
 *
 * // In onCreate or initialization
 * helper.initialize { result ->
 *     when (result) {
 *         is GooglePayReadinessResult.Success -> // Ready to use
 *         is GooglePayReadinessResult.Error -> // Handle error
 *     }
 * }
 *
 * // When user wants to pay
 * helper.makePayment(
 *     amount = "10.00",
 *     label = "Product",
 *     orderReference = "ORDER123",
 *     customerEmail = "user@example.com"
 * ) { result ->
 *     when (result) {
 *         is GooglePayResult.Success -> // Payment successful
 *         is GooglePayResult.Error -> // Handle error
 *         is GooglePayResult.Canceled -> // User canceled
 *     }
 * }
 *
 * // In onActivityResult
 * helper.handleActivityResult(requestCode, resultCode, data)
 * ```
 *
 * @param activity The Activity that will host the Google Pay flow
 * @param config EveryPay configuration
 * @param requestCode Optional request code for Google Pay activity (default: 991)
 */
class EverypayGooglePayHelper(
    private val activity: Activity,
    private val config: EverypayConfig,
    private val requestCode: Int = DEFAULT_REQUEST_CODE,
    private val coroutineScope: CoroutineScope = (activity as? ComponentActivity)?.lifecycleScope
        ?: CoroutineScope(Dispatchers.Main + SupervisorJob())
) {
    private val apiClient = EverypayApiClient(config)
    private var sessionInfo: OpenSessionResponse? = null
    private var googlePayManager: GooglePayManager? = null
    private var googlePayHelper: GooglePayActivityHelper? = null
    private var currentPaymentCallback: GooglePayPaymentCallback? = null
    private var currentPaymentInfo: CreatePaymentResponse? = null
    private var currentBackendData: GooglePayBackendData? = null
    private val processingLock = Any()
    private var isProcessingPayment = false
    private var isTokenRequest = false

    companion object {
        const val DEFAULT_REQUEST_CODE = 991
        private const val TAG = "EverypayGooglePayHelper"
    }

    /**
     * Returns true if a payment is currently being processed
     */
    fun isProcessingPayment(): Boolean = synchronized(processingLock) { isProcessingPayment }

    /**
     * Gateway ID from the session info, or null if not initialized
     */
    val gatewayId: String?
        get() = sessionInfo?.googlePayGatewayId

    /**
     * Gateway merchant ID from the session info, or null if not initialized
     */
    val gatewayMerchantId: String?
        get() = sessionInfo?.googlepayGatewayMerchantId

    /**
     * Resets all payment-related state variables
     * Call this after payment completion or errors to ensure clean state
     */
    private fun resetPaymentState() {
        synchronized(processingLock) {
            isProcessingPayment = false
            isTokenRequest = false
            currentPaymentCallback = null
            currentPaymentInfo = null
            currentBackendData = null
        }
    }

    /**
     * Initializes the helper by opening an EveryPay session and setting up Google Pay (SDK mode).
     * Call this method once during Activity initialization.
     *
     * Note: This method requires API credentials in config (SDK mode).
     * For backend mode, use initializeWithBackendData() instead.
     *
     * @param callback Callback to receive initialization result
     */
    fun initialize(callback: GooglePayReadinessCallback) {
        if (config.isBackendMode()) {
            Log.e(TAG, "Cannot use initialize() in backend mode. Use initializeWithBackendData() instead.")
            callback.onResult(
                GooglePayReadinessResult.Error(
                    Constants.E_INIT_ERROR,
                    "Config is in backend mode. Use initializeWithBackendData() method instead of initialize()."
                )
            )
            return
        }

        Log.d(TAG, "Initializing EveryPay Google Pay Helper (SDK mode)")

        coroutineScope.launch {
            try {
                // Open EveryPay session in background
                val session = withContext(Dispatchers.IO) {
                    apiClient.openSession()
                }

                sessionInfo = session
                Log.d(TAG, "Session opened: merchant=${session.merchantName}")

                // Initialize Google Pay Manager
                val environment = when (config.environment.uppercase()) {
                    "PRODUCTION" -> WalletConstants.ENVIRONMENT_PRODUCTION
                    else -> WalletConstants.ENVIRONMENT_TEST
                }

                val manager = GooglePayManager(
                    context = activity,
                    environment = environment,
                    allowedCardNetworks = config.allowedCardNetworks,
                    allowedCardAuthMethods = config.allowedCardAuthMethods
                )
                googlePayManager = manager

                val helper = GooglePayActivityHelper(
                    activity = activity,
                    googlePayManager = manager,
                    requestCode = requestCode
                )
                googlePayHelper = helper

                // Check if Google Pay is available
                helper.isReadyToPay(callback)

            } catch (e: Exception) {
                val errorMessage = e.message ?: "Unknown error"
                Log.e(TAG, "Initialization failed: $errorMessage", e)
                callback.onResult(
                    GooglePayReadinessResult.Error(
                        Constants.E_INIT_ERROR,
                        errorMessage,
                        e
                    )
                )
            }
        }
    }

    /**
     * Initializes the helper with backend data (Backend mode - RECOMMENDED).
     * Use this when your backend makes the EveryPay API calls.
     *
     * Your backend should call:
     * 1. POST /api/v4/google_pay/open_session
     * 2. Combine response into GooglePayBackendData
     * 3. Send to Android app
     *
     * @param backendData Combined session data from backend's open_session call
     * @param callback Callback to receive initialization result
     */
    fun initializeWithBackendData(
        backendData: GooglePayBackendData,
        callback: GooglePayReadinessCallback
    ) {
        Log.d(TAG, "Initializing EveryPay Google Pay Helper (Backend mode)")

        coroutineScope.launch {
            try {
                // Store backend data (no API call needed)
                currentBackendData = backendData

                // Create OpenSessionResponse from backend data for compatibility
                sessionInfo = OpenSessionResponse(
                    googlepayMerchantIdentifier = backendData.merchantId,
                    googlepayEpMerchantId = "", // Not needed for Google Pay request
                    googlepayGatewayMerchantId = backendData.gatewayMerchantId,
                    merchantName = backendData.merchantName,
                    googlePayGatewayId = backendData.gatewayId,
                    acqBrandingDomainIgw = "" // Not needed for Google Pay request
                )

                Log.d(TAG, "Backend data loaded: merchant=${backendData.merchantName}")

                // Initialize Google Pay Manager
                val environment = when (config.environment.uppercase()) {
                    "PRODUCTION" -> WalletConstants.ENVIRONMENT_PRODUCTION
                    else -> WalletConstants.ENVIRONMENT_TEST
                }

                val manager = GooglePayManager(
                    context = activity,
                    environment = environment,
                    allowedCardNetworks = config.allowedCardNetworks,
                    allowedCardAuthMethods = config.allowedCardAuthMethods
                )
                googlePayManager = manager

                val helper = GooglePayActivityHelper(
                    activity = activity,
                    googlePayManager = manager,
                    requestCode = requestCode
                )
                googlePayHelper = helper

                // Check if Google Pay is available
                helper.isReadyToPay(callback)

            } catch (e: Exception) {
                val errorMessage = e.message ?: "Unknown error"
                Log.e(TAG, "Initialization with backend data failed: $errorMessage", e)
                callback.onResult(
                    GooglePayReadinessResult.Error(
                        Constants.E_INIT_ERROR,
                        errorMessage,
                        e
                    )
                )
            }
        }
    }

    /**
     * Initiates a Google Pay payment (SDK mode).
     * SDK will create payment in EveryPay, show Google Pay, and process the token.
     *
     * Note: This method requires API credentials in config (SDK mode).
     * For backend mode, use makePaymentWithBackendData() instead.
     *
     * @param amount Payment amount as string (e.g., "10.00")
     * @param label Payment label/description
     * @param orderReference Unique order reference
     * @param customerEmail Customer email address
     * @param customerIp Customer IP address (optional)
     * @param callback Callback to receive payment result
     */
    fun makePayment(
        amount: String,
        label: String,
        orderReference: String,
        customerEmail: String,
        customerIp: String? = null,
        callback: GooglePayPaymentCallback
    ) {
        if (config.isBackendMode()) {
            Log.e(TAG, "Cannot use makePayment() in backend mode. Use makePaymentWithBackendData() instead.")
            callback.onResult(
                GooglePayResult.Error(
                    Constants.E_PAYMENT_ERROR,
                    "Config is in backend mode. Use makePaymentWithBackendData() method instead of makePayment()."
                )
            )
            return
        }

        // Prevent concurrent payment attempts with thread safety
        synchronized(processingLock) {
            if (isProcessingPayment) {
                Log.w(TAG, "Payment already in progress, ignoring request")
                callback.onResult(
                    GooglePayResult.Error(
                        Constants.E_PAYMENT_ERROR,
                        "Operation already in progress. Please wait for completion."
                    )
                )
                return
            }
            isProcessingPayment = true
            currentPaymentCallback = callback
        }

        if (sessionInfo == null) {
            Log.e(TAG, "Session not initialized. Call initialize() first.")
            callback.onResult(
                GooglePayResult.Error(
                    Constants.E_INIT_ERROR,
                    "Helper not initialized. Call initialize() before making requests."
                )
            )
            resetPaymentState()
            return
        }

        val helper = googlePayHelper
        if (helper == null) {
            Log.e(TAG, "Google Pay helper not initialized")
            callback.onResult(
                GooglePayResult.Error(
                    Constants.E_INIT_ERROR,
                    "Google Pay not initialized."
                )
            )
            resetPaymentState()
            return
        }

        Log.d(TAG, "Creating payment for amount: $amount")

        coroutineScope.launch {
            try {
                // Validate amount format
                val amountValue = try {
                    amount.toDouble()
                } catch (e: NumberFormatException) {
                    Log.e(TAG, "Invalid amount format: $amount", e)
                    currentPaymentCallback?.onResult(
                        GooglePayResult.Error(
                            Constants.E_PAYMENT_ERROR,
                            "Invalid amount format: $amount. Expected format: '10.00'"
                        )
                    )
                    resetPaymentState()
                    return@launch
                }

                // Create payment in EveryPay
                // Note: !! is safe here because we already validated SDK mode at the start of this method
                val paymentRequest = CreatePaymentRequest(
                    apiUsername = config.apiUsername!!,
                    accountName = config.accountName!!,
                    amount = amountValue,
                    label = label,
                    currencyCode = config.currencyCode,
                    countryCode = config.countryCode,
                    orderReference = orderReference,
                    nonce = UUID.randomUUID().toString(),
                    mobilePayment = true,
                    customerUrl = config.customerUrl!!,
                    customerIp = customerIp ?: "",
                    customerEmail = customerEmail,
                    timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                        timeZone = TimeZone.getTimeZone("UTC")
                    }.format(Date())
                )

                val paymentInfo = withContext(Dispatchers.IO) {
                    apiClient.createPayment(paymentRequest)
                }

                currentPaymentInfo = paymentInfo
                Log.d(TAG, "Payment created: reference=${paymentInfo.paymentReference}")

                // Build Google Pay request with EveryPay data
                val googlePayRequest = buildGooglePayRequest(
                    sessionInfo = sessionInfo!!,
                    paymentInfo = paymentInfo,
                    amount = amount,
                    label = label
                )

                // Launch Google Pay
                helper.requestPaymentWithCustomRequest(
                    requestJson = googlePayRequest,
                    callback = { result ->
                        handleGooglePayResult(result)
                    }
                )

            } catch (e: Exception) {
                val errorMessage = e.message ?: "Unknown error"
                Log.e(TAG, "Failed to create payment: $errorMessage", e)
                currentPaymentCallback?.onResult(
                    GooglePayResult.Error(
                        Constants.E_PAYMENT_ERROR,
                        errorMessage,
                        e
                    )
                )
                resetPaymentState()
            }
        }
    }

    /**
     * Requests a Google Pay token for future MIT (Merchant Initiated Transactions) / recurring payments (SDK mode).
     * Creates a payment with amount=0 and token flags, then shows Google Pay.
     *
     * This method:
     * 1. Creates payment in EveryPay with amount=0 and token flags
     * 2. Shows Google Pay with zero amount (ESTIMATED status)
     * 3. Processes token with EveryPay backend
     * 4. Returns GooglePayResult.TokenReceived with token data
     *
     * Note: This method requires API credentials in config (SDK mode).
     * Use requestTokenWithBackendData() for backend mode.
     *
     * @param label User-facing label shown in Google Pay sheet (e.g., "Card verification")
     * @param callback Callback to receive token result (GooglePayResult.TokenReceived)
     */
    fun requestToken(label: String, callback: GooglePayPaymentCallback) {
        if (config.isBackendMode()) {
            Log.e(TAG, "Cannot use requestToken() in backend mode. Use requestTokenWithBackendData() instead.")
            callback.onResult(
                GooglePayResult.Error(
                    Constants.E_PAYMENT_ERROR,
                    "Config is in backend mode. Use requestTokenWithBackendData() method instead of requestToken()."
                )
            )
            return
        }

        // Prevent concurrent operations with thread safety
        synchronized(processingLock) {
            if (isProcessingPayment) {
                Log.w(TAG, "Payment or token request already in progress, ignoring request")
                callback.onResult(
                    GooglePayResult.Error(
                        Constants.E_PAYMENT_ERROR,
                        "Operation already in progress. Please wait for completion."
                    )
                )
                return
            }
            isProcessingPayment = true
            isTokenRequest = true
            currentPaymentCallback = callback
        }

        if (sessionInfo == null) {
            Log.e(TAG, "Session not initialized. Call initialize() first.")
            callback.onResult(
                GooglePayResult.Error(
                    Constants.E_INIT_ERROR,
                    "Helper not initialized. Call initialize() before making requests."
                )
            )
            resetPaymentState()
            return
        }

        val helper = googlePayHelper
        if (helper == null) {
            Log.e(TAG, "Google Pay helper not initialized")
            callback.onResult(
                GooglePayResult.Error(
                    Constants.E_INIT_ERROR,
                    "Google Pay not initialized."
                )
            )
            resetPaymentState()
            return
        }

        Log.d(TAG, "Requesting Google Pay token for future MIT")

        coroutineScope.launch {
            try {
                // Create payment with amount=0 and token flags
                val paymentRequest = CreatePaymentRequest(
                    apiUsername = config.apiUsername!!,
                    accountName = config.accountName!!,
                    amount = 0.0,  // Zero amount for token request
                    label = label,
                    currencyCode = config.currencyCode,
                    countryCode = config.countryCode,
                    orderReference = UUID.randomUUID().toString(),
                    nonce = UUID.randomUUID().toString(),
                    mobilePayment = true,
                    customerUrl = config.customerUrl!!,
                    customerIp = "",
                    customerEmail = "",
                    timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                        timeZone = TimeZone.getTimeZone("UTC")
                    }.format(Date()),
                    requestToken = true,
                    tokenConsentAgreed = true,
                    tokenAgreement = "unscheduled"
                )

                val paymentInfo = withContext(Dispatchers.IO) {
                    apiClient.createPayment(paymentRequest)
                }

                currentPaymentInfo = paymentInfo
                Log.d(TAG, "Token request payment created: reference=${paymentInfo.paymentReference}")

                // Build Google Pay request with token payment data
                val googlePayRequest = buildGooglePayRequest(
                    sessionInfo = sessionInfo!!,
                    paymentInfo = paymentInfo,
                    amount = "0",
                    label = label,
                    isTokenRequest = true
                )

                // Launch Google Pay
                helper.requestPaymentWithCustomRequest(
                    requestJson = googlePayRequest,
                    callback = { result ->
                        handleGooglePayResult(result)
                    }
                )

            } catch (e: Exception) {
                val errorMessage = e.message ?: "Unknown error"
                Log.e(TAG, "Failed to create token request payment: $errorMessage", e)
                currentPaymentCallback?.onResult(
                    GooglePayResult.Error(
                        Constants.E_PAYMENT_ERROR,
                        errorMessage,
                        e
                    )
                )
                resetPaymentState()
            }
        }
    }

    /**
     * Initiates a Google Pay payment with backend data (Backend mode - RECOMMENDED).
     * Use this when your backend makes the EveryPay API calls.
     *
     * Your backend should call:
     * 1. POST /api/v4/payments/oneoff
     * 2. Combine response with session data into GooglePayBackendData (including standing_amount and label)
     * 3. Send to Android app
     *
     * The SDK will show Google Pay with the amount and label from backend data.
     * You'll receive TokenReceived result with the token to send to your backend.
     *
     * @param backendData Combined payment data from backend (includes amount and label from standing_amount)
     * @param callback Callback to receive token result
     */
    fun makePaymentWithBackendData(
        backendData: GooglePayBackendData,
        callback: GooglePayPaymentCallback
    ) {
        // Prevent concurrent payment attempts with thread safety
        synchronized(processingLock) {
            if (isProcessingPayment) {
                Log.w(TAG, "Payment already in progress, ignoring request")
                callback.onResult(
                    GooglePayResult.Error(
                        Constants.E_PAYMENT_ERROR,
                        "Operation already in progress. Please wait for completion."
                    )
                )
                return
            }
            isProcessingPayment = true
            currentPaymentCallback = callback
            currentBackendData = backendData
        }

        if (sessionInfo == null) {
            Log.e(TAG, "Session not initialized. Call initializeWithBackendData() first.")
            callback.onResult(
                GooglePayResult.Error(
                    Constants.E_INIT_ERROR,
                    "Helper not initialized. Call initializeWithBackendData() before making requests."
                )
            )
            resetPaymentState()
            return
        }

        val helper = googlePayHelper
        if (helper == null) {
            Log.e(TAG, "Google Pay helper not initialized")
            callback.onResult(
                GooglePayResult.Error(
                    Constants.E_INIT_ERROR,
                    "Google Pay not initialized."
                )
            )
            resetPaymentState()
            return
        }

        val session = sessionInfo
        if (session == null) {
            Log.e(TAG, "Session not initialized")
            callback.onResult(
                GooglePayResult.Error(
                    Constants.E_INIT_ERROR,
                    "Session not initialized. Call initializeWithBackendData() before making requests."
                )
            )
            resetPaymentState()
            return
        }

        Log.d(TAG, "Starting payment with backend data: reference=${backendData.paymentReference}")

        coroutineScope.launch {
            try {
                // Create a minimal CreatePaymentResponse from backend data for buildGooglePayRequest
                val paymentInfo = CreatePaymentResponse(
                    paymentReference = backendData.paymentReference,
                    mobileAccessToken = backendData.mobileAccessToken,
                    currency = backendData.currency,
                    descriptorCountry = backendData.countryCode,
                    googlepayMerchantIdentifier = backendData.merchantId,
                    accountName = "", // Not needed
                    orderReference = "", // Not needed
                    initialAmount = 0.0, // Not needed
                    standingAmount = 0.0, // Not needed
                    paymentState = "" // Not needed
                )

                // Build Google Pay request with backend data
                val googlePayRequest = buildGooglePayRequest(
                    sessionInfo = session,
                    paymentInfo = paymentInfo,
                    amount = backendData.amount.toString(),
                    label = backendData.label
                )

                // Launch Google Pay
                helper.requestPaymentWithCustomRequest(
                    requestJson = googlePayRequest,
                    callback = { result ->
                        handleGooglePayResult(result)
                    }
                )

            } catch (e: Exception) {
                val errorMessage = e.message ?: "Unknown error"
                Log.e(TAG, "Failed to start payment with backend data: $errorMessage", e)
                currentPaymentCallback?.onResult(
                    GooglePayResult.Error(
                        Constants.E_PAYMENT_ERROR,
                        errorMessage,
                        e
                    )
                )
                resetPaymentState()
            }
        }
    }

    /**
     * Requests a Google Pay token for future MIT / recurring payments with backend data (Backend mode).
     * Uses totalPriceStatus="ESTIMATED" with zero amount for card verification.
     *
     * This method is for backend mode where your backend:
     * 1. Opens session (POST /api/v4/google_pay/open_session)
     * 2. Sends session data to Android app (NO payment creation)
     * 3. SDK shows Google Pay with zero amount
     * 4. SDK returns token to app
     * 5. App sends token to backend
     * 6. Backend receives the MIT token from EveryPay (GET /api/v4/payments/{payment_reference})
     *
     * @param backendData Session data from backend (only session info needed, not payment info)
     * @param callback Callback to receive token result (GooglePayResult.TokenReceived)
     */
    fun requestTokenWithBackendData(
        backendData: GooglePayBackendData,
        callback: GooglePayPaymentCallback
    ) {
        // Prevent concurrent operations with thread safety
        synchronized(processingLock) {
            if (isProcessingPayment) {
                Log.w(TAG, "Payment or token request already in progress, ignoring request")
                callback.onResult(
                    GooglePayResult.Error(
                        Constants.E_PAYMENT_ERROR,
                        "Operation already in progress. Please wait for completion."
                    )
                )
                return
            }
            isProcessingPayment = true
            isTokenRequest = true
            currentPaymentCallback = callback
            currentBackendData = backendData
        }

        if (sessionInfo == null) {
            Log.e(TAG, "Session not initialized. Call initializeWithBackendData() first.")
            callback.onResult(
                GooglePayResult.Error(
                    Constants.E_INIT_ERROR,
                    "Helper not initialized. Call initializeWithBackendData() before making requests."
                )
            )
            resetPaymentState()
            return
        }

        val helper = googlePayHelper
        if (helper == null) {
            Log.e(TAG, "Google Pay helper not initialized")
            callback.onResult(
                GooglePayResult.Error(
                    Constants.E_INIT_ERROR,
                    "Google Pay not initialized."
                )
            )
            resetPaymentState()
            return
        }

        val session = sessionInfo
        if (session == null) {
            Log.e(TAG, "Session not initialized")
            callback.onResult(
                GooglePayResult.Error(
                    Constants.E_INIT_ERROR,
                    "Session not initialized. Call initializeWithBackendData() before making requests."
                )
            )
            resetPaymentState()
            return
        }

        Log.d(TAG, "Requesting Google Pay token for future MIT with backend data")

        coroutineScope.launch {
            try {
                // Build Google Pay request for token collection
                val googlePayRequest = buildGooglePayRequest(
                    sessionInfo = session,
                    amount = backendData.amount.toString(),
                    label = backendData.label,
                    isTokenRequest = true
                )

                // Launch Google Pay
                helper.requestPaymentWithCustomRequest(
                    requestJson = googlePayRequest,
                    callback = { result ->
                        handleGooglePayResult(result)
                    }
                )

            } catch (e: Exception) {
                val errorMessage = e.message ?: "Unknown error"
                Log.e(TAG, "Failed to request token with backend data: $errorMessage", e)
                currentPaymentCallback?.onResult(
                    GooglePayResult.Error(
                        Constants.E_PAYMENT_ERROR,
                        errorMessage,
                        e
                    )
                )
                resetPaymentState()
            }
        }
    }

    /**
     * Handles the activity result from Google Pay.
     * Call this from your Activity's onActivityResult() method.
     *
     * @param requestCode Request code from onActivityResult
     * @param resultCode Result code from onActivityResult
     * @param data Intent data from onActivityResult
     * @return true if this was a Google Pay result, false otherwise
     */
    fun handleActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        return googlePayHelper?.handleActivityResult(requestCode, resultCode, data) ?: false
    }

    /**
     * Handles Google Pay result and processes payment with EveryPay (SDK mode)
     * or extracts token (Backend mode)
     */
    private fun handleGooglePayResult(result: GooglePayResult) {
        when (result) {
            is GooglePayResult.Success -> {
                // Determine mode and handle accordingly
                val backendData = currentBackendData

                if (backendData != null) {
                    // Backend mode: Extract token and return to app (both payments and token requests)
                    val isRecurring = isTokenRequest
                    Log.d(TAG, "Google Pay successful, extracting token for backend (isRecurring: $isRecurring)")
                    try {
                        val tokenData = extractGooglePayToken(
                            paymentData = result.paymentData,
                            backendData = backendData,
                            isRecurringPayment = isRecurring
                        )
                        currentPaymentCallback?.onResult(
                            GooglePayResult.TokenReceived(tokenData, result.paymentData)
                        )
                    } catch (e: Exception) {
                        val errorMessage = e.message ?: "Unknown error"
                        Log.e(TAG, "Failed to extract Google Pay token: $errorMessage", e)
                        currentPaymentCallback?.onResult(
                            GooglePayResult.Error(
                                Constants.E_PAYMENT_ERROR,
                                "Failed to extract token: $errorMessage",
                                e
                            )
                        )
                    } finally {
                        resetPaymentState()
                    }
                } else {
                    // SDK mode: Process payment or token request with EveryPay
                    val requestType = if (isTokenRequest) "token request" else "payment"
                    Log.d(TAG, "Google Pay successful, processing $requestType with EveryPay (SDK mode)")
                    processPaymentWithEverypay(result.paymentData)
                }
            }
            is GooglePayResult.Canceled -> {
                Log.d(TAG, "Google Pay canceled")
                currentPaymentCallback?.onResult(result)
                resetPaymentState()
            }
            is GooglePayResult.Error -> {
                Log.e(TAG, "Google Pay error: ${result.message}")
                currentPaymentCallback?.onResult(result)
                resetPaymentState()
            }
            else -> {
                // This should not happen, but handle it gracefully
                Log.e(TAG, "Unexpected Google Pay result type: $result")
            }
        }
    }

    /**
     * Processes Google Pay token with EveryPay backend
     */
    private fun processPaymentWithEverypay(paymentData: PaymentData) {
        val paymentInfo = currentPaymentInfo
        if (paymentInfo == null) {
            Log.e(TAG, "No payment info available")
            currentPaymentCallback?.onResult(
                GooglePayResult.Error(
                    Constants.E_PAYMENT_ERROR,
                    "Payment info not available"
                )
            )
            currentPaymentCallback = null
            return
        }

        coroutineScope.launch {
            try {
                // Parse Google Pay token with detailed error context
                val paymentDataJson = try {
                    JSONObject(paymentData.toJson())
                } catch (e: Exception) {
                    throw IllegalStateException("Failed to parse Google Pay payment data as JSON", e)
                }

                val paymentMethodData = paymentDataJson.optJSONObject("paymentMethodData")
                    ?: throw IllegalStateException("Missing 'paymentMethodData' in Google Pay response")

                val tokenizationData = paymentMethodData.optJSONObject("tokenizationData")
                    ?: throw IllegalStateException("Missing 'tokenizationData' in payment method data")

                val tokenString = tokenizationData.optString("token")
                if (tokenString.isNullOrBlank()) {
                    throw IllegalStateException("Missing or empty 'token' in tokenization data")
                }

                val tokenJson = try {
                    JSONObject(tokenString)
                } catch (e: Exception) {
                    throw IllegalStateException("Failed to parse Google Pay token as JSON", e)
                }

                // Extract token components with validation
                val signature = tokenJson.optString("signature")
                if (signature.isNullOrBlank()) {
                    throw IllegalStateException("Missing 'signature' in Google Pay token")
                }

                val intermediateSigningKey = tokenJson.optJSONObject("intermediateSigningKey")
                    ?: throw IllegalStateException("Missing 'intermediateSigningKey' in Google Pay token")

                val signedKey = intermediateSigningKey.optString("signedKey")
                if (signedKey.isNullOrBlank()) {
                    throw IllegalStateException("Missing 'signedKey' in intermediate signing key")
                }

                val signaturesArray = intermediateSigningKey.optJSONArray("signatures")
                    ?: throw IllegalStateException("Missing 'signatures' array in intermediate signing key")

                val signatures = mutableListOf<String>()
                for (i in 0 until signaturesArray.length()) {
                    signatures.add(signaturesArray.getString(i))
                }

                if (signatures.isEmpty()) {
                    throw IllegalStateException("Empty 'signatures' array in intermediate signing key")
                }

                val protocolVersion = tokenJson.optString("protocolVersion")
                if (protocolVersion.isNullOrBlank()) {
                    throw IllegalStateException("Missing 'protocolVersion' in Google Pay token")
                }

                val signedMessage = tokenJson.optString("signedMessage")
                if (signedMessage.isNullOrBlank()) {
                    throw IllegalStateException("Missing 'signedMessage' in Google Pay token")
                }

                // Process payment with EveryPay
                val processRequest = ProcessPaymentRequest(
                    paymentReference = paymentInfo.paymentReference,
                    tokenConsentAgreed = isTokenRequest,  // true for token requests, false for regular payments
                    signature = signature,
                    intermediateSigningKey = IntermediateSigningKey(
                        signedKey = signedKey,
                        signatures = signatures
                    ),
                    protocolVersion = protocolVersion,
                    signedMessage = signedMessage
                )

                val processResponse = withContext(Dispatchers.IO) {
                    apiClient.processPayment(paymentInfo.mobileAccessToken, processRequest)
                }

                Log.d(TAG, "Payment processed: state=${processResponse.state}")

                // For token requests, validate state and return the token data
                if (isTokenRequest) {
                    // Validate payment state before retrieving token
                    val stateValid = processResponse.state.lowercase() in listOf("settled", "authorized", "completed")
                    if (!stateValid) {
                        Log.e(TAG, "Token request failed with invalid state: ${processResponse.state}")
                        currentPaymentCallback?.onResult(
                            GooglePayResult.Error(
                                Constants.E_PAYMENT_ERROR,
                                "Token request failed with state: ${processResponse.state}"
                            )
                        )
                        resetPaymentState()
                        return@launch
                    }

                    // Call GET /payments to retrieve MIT token from cc_details.token
                    val paymentDetails = try {
                        Log.d(TAG, "Retrieving MIT token from payment details...")
                        withContext(Dispatchers.IO) {
                            apiClient.getPaymentDetails(paymentInfo.paymentReference)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to get payment details: ${e.message}", e)
                        currentPaymentCallback?.onResult(
                            GooglePayResult.Error(
                                Constants.E_PAYMENT_ERROR,
                                "Failed to retrieve token details: ${e.message}",
                                e
                            )
                        )
                        resetPaymentState()
                        return@launch
                    }

                    // Validate token is available
                    if (paymentDetails.ccDetails?.token == null) {
                        Log.e(TAG, "Token not available in payment details")
                        currentPaymentCallback?.onResult(
                            GooglePayResult.Error(
                                Constants.E_PAYMENT_ERROR,
                                "Token not available. Payment state: ${processResponse.state}"
                            )
                        )
                        resetPaymentState()
                        return@launch
                    }

                    // Log the MIT token if available
                    Log.d(TAG, "✅ MIT Token retrieved")

                    val tokenData = GooglePayTokenData(
                        paymentReference = paymentInfo.paymentReference,
                        mobileAccessToken = paymentInfo.mobileAccessToken,
                        signature = signature,
                        intermediateSigningKey = IntermediateSigningKey(
                            signedKey = signedKey,
                            signatures = signatures
                        ),
                        protocolVersion = protocolVersion,
                        signedMessage = signedMessage,
                        tokenConsentAgreed = true
                    )

                    Log.d(TAG, "Token request completed. Process response state: ${processResponse.state}")
                    currentPaymentCallback?.onResult(
                        GooglePayResult.TokenReceived(tokenData, paymentData, processResponse, paymentDetails)
                    )
                    resetPaymentState()
                    return@launch
                }

                // Notify callback based on payment state (for regular payments)
                when (processResponse.state.lowercase()) {
                    "settled", "authorized", "completed" -> {
                        // Payment successful
                        currentPaymentCallback?.onResult(GooglePayResult.Success(paymentData))
                    }
                    "waiting_for_3ds", "waiting_for_3ds_response", "processing" -> {
                        // Payment is pending - typically shouldn't happen with Google Pay
                        // but handle it gracefully
                        Log.w(TAG, "Payment is in pending state: ${processResponse.state}")
                        currentPaymentCallback?.onResult(
                            GooglePayResult.Error(
                                Constants.E_PAYMENT_ERROR,
                                "Payment is pending additional authentication: ${processResponse.state}"
                            )
                        )
                    }
                    "failed", "voided", "abandoned" -> {
                        // Payment explicitly failed
                        currentPaymentCallback?.onResult(
                            GooglePayResult.Error(
                                Constants.E_PAYMENT_ERROR,
                                "Payment failed with state: ${processResponse.state}"
                            )
                        )
                    }
                    else -> {
                        // Unknown state - log it and treat as error
                        Log.w(TAG, "Unknown payment state: ${processResponse.state}")
                        currentPaymentCallback?.onResult(
                            GooglePayResult.Error(
                                Constants.E_PAYMENT_ERROR,
                                "Unknown payment state: ${processResponse.state}"
                            )
                        )
                    }
                }

            } catch (e: Exception) {
                val errorMessage = e.message ?: "Unknown error"
                Log.e(TAG, "Failed to process payment with EveryPay: $errorMessage", e)
                currentPaymentCallback?.onResult(
                    GooglePayResult.Error(
                        Constants.E_PAYMENT_ERROR,
                        errorMessage,
                        e
                    )
                )
            } finally {
                resetPaymentState()
            }
        }
    }

    /**
     * Extracts Google Pay token data from PaymentData (Backend mode)
     * Parses the token and combines it with backend data for sending to backend
     *
     * @param paymentData Google Pay payment data containing the encrypted token
     * @param backendData Backend session and payment data
     * @param isRecurringPayment true for recurring/MIT payments, false for one-time payments
     */
    private fun extractGooglePayToken(
        paymentData: PaymentData,
        backendData: GooglePayBackendData,
        isRecurringPayment: Boolean
    ): GooglePayTokenData {
        // Parse Google Pay token with detailed error context
        val paymentDataJson = try {
            JSONObject(paymentData.toJson())
        } catch (e: Exception) {
            throw IllegalStateException("Failed to parse Google Pay payment data as JSON", e)
        }

        val paymentMethodData = paymentDataJson.optJSONObject("paymentMethodData")
            ?: throw IllegalStateException("Missing 'paymentMethodData' in Google Pay response")

        val tokenizationData = paymentMethodData.optJSONObject("tokenizationData")
            ?: throw IllegalStateException("Missing 'tokenizationData' in payment method data")

        val tokenString = tokenizationData.optString("token")
        if (tokenString.isNullOrBlank()) {
            throw IllegalStateException("Missing or empty 'token' in tokenization data")
        }

        val tokenJson = try {
            JSONObject(tokenString)
        } catch (e: Exception) {
            throw IllegalStateException("Failed to parse Google Pay token as JSON", e)
        }

        // Extract token components with validation
        val signature = tokenJson.optString("signature")
        if (signature.isNullOrBlank()) {
            throw IllegalStateException("Missing 'signature' in Google Pay token")
        }

        val intermediateSigningKey = tokenJson.optJSONObject("intermediateSigningKey")
            ?: throw IllegalStateException("Missing 'intermediateSigningKey' in Google Pay token")

        val signedKey = intermediateSigningKey.optString("signedKey")
        if (signedKey.isNullOrBlank()) {
            throw IllegalStateException("Missing 'signedKey' in intermediate signing key")
        }

        val signaturesArray = intermediateSigningKey.optJSONArray("signatures")
            ?: throw IllegalStateException("Missing 'signatures' array in intermediate signing key")

        val signatures = mutableListOf<String>()
        for (i in 0 until signaturesArray.length()) {
            signatures.add(signaturesArray.getString(i))
        }

        if (signatures.isEmpty()) {
            throw IllegalStateException("Empty 'signatures' array in intermediate signing key")
        }

        val protocolVersion = tokenJson.optString("protocolVersion")
        if (protocolVersion.isNullOrBlank()) {
            throw IllegalStateException("Missing 'protocolVersion' in Google Pay token")
        }

        val signedMessage = tokenJson.optString("signedMessage")
        if (signedMessage.isNullOrBlank()) {
            throw IllegalStateException("Missing 'signedMessage' in Google Pay token")
        }

        // Return combined token data for backend
        return GooglePayTokenData(
            paymentReference = backendData.paymentReference,
            mobileAccessToken = backendData.mobileAccessToken,
            signature = signature,
            intermediateSigningKey = IntermediateSigningKey(
                signedKey = signedKey,
                signatures = signatures
            ),
            protocolVersion = protocolVersion,
            signedMessage = signedMessage,
            tokenConsentAgreed = isRecurringPayment
        )
    }

    /**
     * Builds Google Pay request JSON
     * @param isTokenRequest If true, builds request for token collection (ESTIMATED, 0 amount)
     */
    private fun buildGooglePayRequest(
        sessionInfo: OpenSessionResponse,
        paymentInfo: CreatePaymentResponse? = null,
        amount: String = "0",
        label: String,
        isTokenRequest: Boolean = false
    ): JSONObject {
        return JSONObject().apply {
            put("apiVersion", 2)
            put("apiVersionMinor", 0)
            put("allowedPaymentMethods", org.json.JSONArray().apply {
                put(PaymentsUtil.getCardPaymentMethod(
                    config.allowedCardNetworks,
                    config.allowedCardAuthMethods,
                    sessionInfo.googlePayGatewayId.lowercase(),
                    sessionInfo.googlepayGatewayMerchantId.lowercase()
                ))
            })
            put("merchantInfo", JSONObject().apply {
                put("merchantId", sessionInfo.googlepayMerchantIdentifier.lowercase())
                put("merchantName", sessionInfo.merchantName)
            })
            put("transactionInfo", JSONObject().apply {
                if (isTokenRequest) {
                    put("totalPriceStatus", "ESTIMATED")
                    put("totalPrice", "0")
                    put("currencyCode", config.currencyCode)
                    put("countryCode", config.countryCode)
                } else {
                    put("totalPriceStatus", "FINAL")
                    put("totalPrice", amount)
                    put("currencyCode", paymentInfo?.currency ?: config.currencyCode)
                    put("countryCode", paymentInfo?.descriptorCountry ?: config.countryCode)
                    put("totalPriceLabel", label)
                }
            })
        }
    }
}
