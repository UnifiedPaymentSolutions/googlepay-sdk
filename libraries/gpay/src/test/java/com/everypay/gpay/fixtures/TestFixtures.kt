package com.everypay.gpay.fixtures

import com.everypay.gpay.models.*
import org.json.JSONArray
import org.json.JSONObject

/**
 * Test fixtures and factory methods for creating mock data in tests
 */
object TestFixtures {

    // ==================== Config Fixtures ====================

    fun sdkModeConfig(
        apiUsername: String = "test_api_user",
        apiSecret: String = "test_api_secret",
        apiUrl: String = "https://api.test.everypay.com",
        environment: String = "TEST",
        accountName: String = "EUR3D1",
        countryCode: String = "EE",
        customerUrl: String = "https://example.com/return",
        currencyCode: String = "EUR",
        allowedCardNetworks: List<String> = listOf("MASTERCARD", "VISA"),
        allowedCardAuthMethods: List<String> = listOf("PAN_ONLY", "CRYPTOGRAM_3DS")
    ) = EverypayConfig(
        apiUsername = apiUsername,
        apiSecret = apiSecret,
        apiUrl = apiUrl,
        environment = environment,
        accountName = accountName,
        countryCode = countryCode,
        customerUrl = customerUrl,
        currencyCode = currencyCode,
        allowedCardNetworks = allowedCardNetworks,
        allowedCardAuthMethods = allowedCardAuthMethods
    )

    fun backendModeConfig(
        environment: String = "TEST",
        countryCode: String = "EE",
        currencyCode: String = "EUR",
        allowedCardNetworks: List<String> = listOf("MASTERCARD", "VISA"),
        allowedCardAuthMethods: List<String> = listOf("PAN_ONLY", "CRYPTOGRAM_3DS")
    ) = EverypayConfig(
        apiUsername = null,
        apiSecret = null,
        apiUrl = null,
        environment = environment,
        accountName = null,
        countryCode = countryCode,
        customerUrl = null,
        currencyCode = currencyCode,
        allowedCardNetworks = allowedCardNetworks,
        allowedCardAuthMethods = allowedCardAuthMethods
    )

    // ==================== API Response Fixtures ====================

    fun createPaymentResponse(
        paymentReference: String = "test_payment_ref_123",
        mobileAccessToken: String = "test_mobile_token_456",
        currency: String = "EUR",
        descriptorCountry: String = "EE",
        googlepayMerchantIdentifier: String = "merchant_id_789",
        accountName: String = "EUR3D1",
        orderReference: String = "order_ref_abc",
        initialAmount: Double = 10.50,
        standingAmount: Double = 10.50,
        paymentState: String = "initial"
    ) = CreatePaymentResponse(
        paymentReference = paymentReference,
        mobileAccessToken = mobileAccessToken,
        currency = currency,
        descriptorCountry = descriptorCountry,
        googlepayMerchantIdentifier = googlepayMerchantIdentifier,
        accountName = accountName,
        orderReference = orderReference,
        initialAmount = initialAmount,
        standingAmount = standingAmount,
        paymentState = paymentState
    )

    fun processPaymentResponse(
        state: String = "settled",
        orderReference: String? = "order_ref_abc",
        paymentReference: String? = "test_payment_ref_123"
    ) = ProcessPaymentResponse(
        state = state,
        orderReference = orderReference,
        paymentReference = paymentReference
    )

    fun openSessionResponse(
        googlepayMerchantIdentifier: String = "merchant_id_789",
        googlepayEpMerchantId: String = "ep_merchant_123",
        googlepayGatewayMerchantId: String = "gateway_merchant_456",
        merchantName: String = "Test Merchant",
        googlePayGatewayId: String = "everypay",
        acqBrandingDomainIgw: String = "test.everypay.com"
    ) = OpenSessionResponse(
        googlepayMerchantIdentifier = googlepayMerchantIdentifier,
        googlepayEpMerchantId = googlepayEpMerchantId,
        googlepayGatewayMerchantId = googlepayGatewayMerchantId,
        merchantName = merchantName,
        googlePayGatewayId = googlePayGatewayId,
        acqBrandingDomainIgw = acqBrandingDomainIgw
    )

    fun paymentDetailsResponse(
        paymentReference: String = "test_payment_ref_123",
        paymentState: String = "settled",
        ccDetails: CardDetails? = cardDetails(),
        orderReference: String? = "order_ref_abc",
        traceId: String? = "trace_123",
        paymentMethod: String? = "card",
        accountName: String? = "EUR3D1",
        initialAmount: Double? = 10.50,
        standingAmount: Double? = 10.50
    ) = PaymentDetailsResponse(
        paymentReference = paymentReference,
        paymentState = paymentState,
        ccDetails = ccDetails,
        orderReference = orderReference,
        traceId = traceId,
        paymentMethod = paymentMethod,
        accountName = accountName,
        initialAmount = initialAmount,
        standingAmount = standingAmount
    )

    fun cardDetails(
        token: String? = "mit_token_123456789012345",
        lastFourDigits: String? = "1234",
        month: String? = "12",
        year: String? = "2026"
    ) = CardDetails(
        token = token,
        lastFourDigits = lastFourDigits,
        month = month,
        year = year
    )

    fun everyPayApiError(
        code: String? = "1001",
        message: String? = "Invalid payment data",
        httpStatus: Int = 400,
        rawResponse: String? = null
    ) = EverypayApiError(
        code = code,
        message = message,
        httpStatus = httpStatus,
        rawResponse = rawResponse
    )

    // ==================== Request Fixtures ====================

    fun createPaymentRequest(
        apiUsername: String = "test_api_user",
        accountName: String = "EUR3D1",
        amount: Double = 10.50,
        label: String = "Test Payment",
        currencyCode: String = "EUR",
        countryCode: String = "EE",
        orderReference: String = "order_ref_abc",
        nonce: String = "test-nonce-123",
        mobilePayment: Boolean = true,
        customerUrl: String = "https://example.com/return",
        customerIp: String = "192.168.1.1",
        customerEmail: String = "test@example.com",
        timestamp: String = "2025-11-17T12:00:00Z",
        requestToken: Boolean? = null,
        tokenConsentAgreed: Boolean? = null,
        tokenAgreement: String? = null
    ) = CreatePaymentRequest(
        apiUsername = apiUsername,
        accountName = accountName,
        amount = amount,
        label = label,
        currencyCode = currencyCode,
        countryCode = countryCode,
        orderReference = orderReference,
        nonce = nonce,
        mobilePayment = mobilePayment,
        customerUrl = customerUrl,
        customerIp = customerIp,
        customerEmail = customerEmail,
        timestamp = timestamp,
        requestToken = requestToken,
        tokenConsentAgreed = tokenConsentAgreed,
        tokenAgreement = tokenAgreement
    )

    fun processPaymentRequest(
        paymentReference: String = "test_payment_ref_123",
        tokenConsentAgreed: Boolean = false,
        signature: String = "test_signature_xyz",
        intermediateSigningKey: IntermediateSigningKey = intermediateSigningKey(),
        protocolVersion: String = "ECv2",
        signedMessage: String = "test_signed_message"
    ) = ProcessPaymentRequest(
        paymentReference = paymentReference,
        tokenConsentAgreed = tokenConsentAgreed,
        signature = signature,
        intermediateSigningKey = intermediateSigningKey,
        protocolVersion = protocolVersion,
        signedMessage = signedMessage
    )

    fun intermediateSigningKey(
        signedKey: String = "test_signed_key",
        signatures: List<String> = listOf("sig1", "sig2")
    ) = IntermediateSigningKey(
        signedKey = signedKey,
        signatures = signatures
    )

    // ==================== Google Pay Token Fixtures ====================

    fun googlePayTokenData(
        paymentReference: String = "test_payment_ref_123",
        mobileAccessToken: String = "test_mobile_token_456",
        signature: String = "test_signature_xyz",
        intermediateSigningKey: IntermediateSigningKey = intermediateSigningKey(),
        protocolVersion: String = "ECv2",
        signedMessage: String = "test_signed_message",
        tokenConsentAgreed: Boolean = false
    ) = GooglePayTokenData(
        paymentReference = paymentReference,
        mobileAccessToken = mobileAccessToken,
        signature = signature,
        intermediateSigningKey = intermediateSigningKey,
        protocolVersion = protocolVersion,
        signedMessage = signedMessage,
        tokenConsentAgreed = tokenConsentAgreed
    )

    fun googlePayBackendData(
        merchantId: String = "merchant_id_789",
        merchantName: String = "Test Merchant",
        gatewayId: String = "everypay",
        gatewayMerchantId: String = "gateway_merchant_456",
        currency: String = "EUR",
        countryCode: String = "EE",
        paymentReference: String = "test_payment_ref_123",
        mobileAccessToken: String = "test_mobile_token_456",
        amount: Double = 10.50,
        label: String = "Test Payment"
    ) = GooglePayBackendData(
        merchantId = merchantId,
        merchantName = merchantName,
        gatewayId = gatewayId,
        gatewayMerchantId = gatewayMerchantId,
        currency = currency,
        countryCode = countryCode,
        paymentReference = paymentReference,
        mobileAccessToken = mobileAccessToken,
        amount = amount,
        label = label
    )

    // ==================== JSON Fixtures ====================

    /**
     * Mock Google Pay token JSON response (simplified version)
     */
    fun googlePayTokenJson(
        signature: String = "test_signature_xyz",
        signedKey: String = "test_signed_key",
        signatures: List<String> = listOf("sig1", "sig2"),
        protocolVersion: String = "ECv2",
        signedMessage: String = "test_signed_message"
    ): String = JSONObject().apply {
        put("signature", signature)
        put("intermediateSigningKey", JSONObject().apply {
            put("signedKey", signedKey)
            put("signatures", JSONArray(signatures))
        })
        put("protocolVersion", protocolVersion)
        put("signedMessage", signedMessage)
    }.toString()

    /**
     * Mock EveryPay create payment API response JSON
     */
    fun createPaymentResponseJson(
        paymentReference: String = "test_payment_ref_123",
        mobileAccessToken: String = "test_mobile_token_456",
        currency: String = "EUR",
        descriptorCountry: String = "EE",
        googlepayMerchantIdentifier: String = "merchant_id_789",
        accountName: String = "EUR3D1",
        orderReference: String = "order_ref_abc",
        initialAmount: Double = 10.50,
        standingAmount: Double = 10.50,
        paymentState: String = "initial"
    ): String = JSONObject().apply {
        put("payment_reference", paymentReference)
        put("mobile_access_token", mobileAccessToken)
        put("currency", currency)
        put("descriptor_country", descriptorCountry)
        put("googlepay_merchant_identifier", googlepayMerchantIdentifier)
        put("account_name", accountName)
        put("order_reference", orderReference)
        put("initial_amount", initialAmount)
        put("standing_amount", standingAmount)
        put("payment_state", paymentState)
    }.toString()

    /**
     * Mock EveryPay process payment API response JSON
     */
    fun processPaymentResponseJson(
        state: String = "settled",
        orderReference: String? = "order_ref_abc",
        paymentReference: String? = "test_payment_ref_123"
    ): String = JSONObject().apply {
        put("state", state)
        if (orderReference != null) put("order_reference", orderReference)
        if (paymentReference != null) put("payment_reference", paymentReference)
    }.toString()

    /**
     * Mock EveryPay open session API response JSON
     */
    fun openSessionResponseJson(
        googlepayMerchantIdentifier: String = "merchant_id_789",
        googlepayEpMerchantId: String = "ep_merchant_123",
        googlepayGatewayMerchantId: String = "gateway_merchant_456",
        merchantName: String = "Test Merchant",
        googlePayGatewayId: String = "everypay",
        acqBrandingDomainIgw: String = "test.everypay.com"
    ): String = JSONObject().apply {
        put("googlepay_merchant_identifier", googlepayMerchantIdentifier)
        put("googlepay_ep_merchant_id", googlepayEpMerchantId)
        put("googlepay_gateway_merchant_id", googlepayGatewayMerchantId)
        put("merchant_name", merchantName)
        put("google_pay_gateway_id", googlePayGatewayId)
        put("acq_branding_domain_igw", acqBrandingDomainIgw)
    }.toString()

    /**
     * Mock EveryPay payment details API response JSON
     */
    fun paymentDetailsResponseJson(
        paymentReference: String = "test_payment_ref_123",
        paymentState: String = "settled",
        orderReference: String? = "order_ref_abc",
        traceId: String? = "trace_123",
        paymentMethod: String? = "card",
        accountName: String? = "EUR3D1",
        initialAmount: Double? = 10.50,
        standingAmount: Double? = 10.50,
        mitToken: String? = "mit_token_123456789012345",
        lastFourDigits: String? = "1234",
        month: String? = "12",
        year: String? = "2026"
    ): String = JSONObject().apply {
        put("payment_reference", paymentReference)
        put("payment_state", paymentState)
        if (orderReference != null) put("order_reference", orderReference)
        if (traceId != null) put("trace_id", traceId)
        if (paymentMethod != null) put("payment_method", paymentMethod)
        if (accountName != null) put("account_name", accountName)
        if (initialAmount != null) put("initial_amount", initialAmount)
        if (standingAmount != null) put("standing_amount", standingAmount)
        if (mitToken != null || lastFourDigits != null || month != null || year != null) {
            put("cc_details", JSONObject().apply {
                if (mitToken != null) put("token", mitToken)
                if (lastFourDigits != null) put("last_four_digits", lastFourDigits)
                if (month != null) put("month", month)
                if (year != null) put("year", year)
            })
        }
    }.toString()

    /**
     * Mock EveryPay API error response JSON
     */
    fun apiErrorResponseJson(
        errorCode: Int = 1001,
        errorMessage: String = "Invalid payment data",
        message: String = "Payment processing failed"
    ): String = JSONObject().apply {
        put("error", JSONObject().apply {
            put("code", errorCode)
            put("message", errorMessage)
        })
        put("message", message)
    }.toString()
}
