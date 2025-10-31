package com.everypay.gpay.models

/**
 * Configuration for EveryPay Google Pay integration
 *
 * @param apiUsername EveryPay API username
 * @param apiSecret EveryPay API secret
 * @param apiUrl EveryPay API base URL
 * @param environment Google Pay environment: "TEST" or "PRODUCTION"
 * @param accountName EveryPay account name (e.g., "EUR3D1")
 * @param countryCode ISO 3166-1 alpha-2 country code (e.g., "EE" for Estonia)
 * @param currencyCode ISO 4217 currency code (default: "EUR")
 * @param allowedCardNetworks List of allowed card networks (default: ["MASTERCARD", "VISA"])
 * @param allowedCardAuthMethods List of allowed authentication methods (default: ["PAN_ONLY", "CRYPTOGRAM_3DS"])
 */
data class EverypayConfig(
    val apiUsername: String,
    val apiSecret: String,
    val apiUrl: String,
    val environment: String,
    val accountName: String,
    val countryCode: String,
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
        require(allowedCardNetworks.isNotEmpty()) {
            "At least one card network must be allowed"
        }
        require(allowedCardAuthMethods.isNotEmpty()) {
            "At least one card auth method must be allowed"
        }
    }
}
