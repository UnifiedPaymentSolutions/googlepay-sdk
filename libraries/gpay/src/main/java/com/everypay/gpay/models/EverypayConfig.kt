package com.everypay.gpay.models

/**
 * Configuration for EveryPay Google Pay integration
 *
 * Supports two modes:
 * 1. **Backend Mode (RECOMMENDED)**: Backend makes API calls, SDK only handles Google Pay UI
 *    - Leave apiUsername, apiSecret, apiUrl, accountName as null
 *    - Use initializeWithBackendData() and makePaymentWithBackendData() methods
 *    - More secure: API credentials never leave backend
 *
 * 2. **SDK Mode (Legacy)**: SDK makes all API calls automatically
 *    - Provide all API credentials (apiUsername, apiSecret, apiUrl, accountName)
 *    - Use initialize() and makePayment() methods
 *    - Simpler but less secure: credentials stored on device
 *
 * @param apiUsername EveryPay API username (required for SDK mode, null for backend mode)
 * @param apiSecret EveryPay API secret (required for SDK mode, null for backend mode)
 * @param apiUrl EveryPay API base URL (required for SDK mode, null for backend mode)
 * @param environment Google Pay environment: "TEST" or "PRODUCTION" (always required)
 * @param accountName EveryPay account name, e.g., "EUR3D1" (required for SDK mode, null for backend mode)
 * @param countryCode ISO 3166-1 alpha-2 country code, e.g., "EE" (always required)
 * @param customerUrl URL where customer is redirected after payment (required for SDK mode, null for backend mode)
 *                    payment_reference and order_reference parameters are added on redirect
 *                    Must be a fully qualified domain name (not an IP address or localhost)
 * @param currencyCode ISO 4217 currency code (default: "EUR")
 * @param allowedCardNetworks List of allowed card networks (default: ["MASTERCARD", "VISA"])
 * @param allowedCardAuthMethods List of allowed authentication methods (default: ["PAN_ONLY", "CRYPTOGRAM_3DS"])
 */
data class EverypayConfig(
    val apiUsername: String? = null,
    val apiSecret: String? = null,
    val apiUrl: String? = null,
    val environment: String,
    val accountName: String? = null,
    val countryCode: String,
    val customerUrl: String? = null,
    val currencyCode: String = "EUR",
    val allowedCardNetworks: List<String> = listOf("MASTERCARD", "VISA"),
    val allowedCardAuthMethods: List<String> = listOf("PAN_ONLY", "CRYPTOGRAM_3DS")
) {
    init {
        require(environment.uppercase() in setOf("TEST", "PRODUCTION")) {
            "Environment must be TEST or PRODUCTION, got: $environment"
        }
        require(countryCode.matches(Regex("[A-Z]{2}"))) {
            "Country code must be 2-letter ISO code (uppercase), got: $countryCode"
        }
        require(currencyCode.matches(Regex("[A-Z]{3}"))) {
            "Currency code must be 3-letter ISO code (uppercase), got: $currencyCode"
        }

        // Validate SDK mode credentials if provided
        if (isSdkMode()) {
            require(!apiUsername.isNullOrBlank()) {
                "API username is required for SDK mode"
            }
            require(!apiSecret.isNullOrBlank()) {
                "API secret is required for SDK mode"
            }
            require(!apiUrl.isNullOrBlank()) {
                "API URL is required for SDK mode"
            }
            require(!accountName.isNullOrBlank()) {
                "Account name is required for SDK mode"
            }
            require(!customerUrl.isNullOrBlank()) {
                "Customer URL is required for SDK mode"
            }
            require(customerUrl!!.startsWith("http://") || customerUrl.startsWith("https://")) {
                "Customer URL must be a valid URL starting with http:// or https://"
            }
            require(!customerUrl.contains("localhost", ignoreCase = true) &&
                    !customerUrl.matches(Regex(".*\\d+\\.\\d+\\.\\d+\\.\\d+.*"))) {
                "Customer URL must be a fully qualified domain name (not localhost or IP address)"
            }
        }

        require(allowedCardNetworks.isNotEmpty()) {
            "At least one card network must be allowed"
        }
        require(allowedCardAuthMethods.isNotEmpty()) {
            "At least one card auth method must be allowed"
        }
    }

    /**
     * Returns true if configured for backend mode (API credentials not provided)
     */
    fun isBackendMode(): Boolean {
        return apiUsername.isNullOrBlank() || apiSecret.isNullOrBlank()
    }

    /**
     * Returns true if configured for SDK mode (API credentials provided)
     */
    fun isSdkMode(): Boolean {
        return !isBackendMode()
    }
}
