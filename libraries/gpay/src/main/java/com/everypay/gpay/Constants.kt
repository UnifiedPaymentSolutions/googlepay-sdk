package com.everypay.gpay

import com.google.android.gms.wallet.WalletConstants

/**
 * This file contains several constants used for Google Pay integration.
 * Most values will be configured dynamically, but these serve as defaults.
 */
object Constants {

    /**
     * Default environment for testing. Can be overridden during initialization.
     */
    const val PAYMENTS_ENVIRONMENT = WalletConstants.ENVIRONMENT_TEST

    /**
     * The allowed networks to be requested from the API. If the user has cards from networks not
     * specified here in their account, these will not be offered for them to choose in the popup.
     */
    val SUPPORTED_NETWORKS = listOf(
        "MASTERCARD",
        "VISA"
    )

    /**
     * The Google Pay API may return cards on file on Google.com (PAN_ONLY) and/or a device token on
     * an Android device authenticated with a 3-D Secure cryptogram (CRYPTOGRAM_3DS).
     */
    val SUPPORTED_METHODS = listOf(
        "PAN_ONLY",
        "CRYPTOGRAM_3DS"
    )

    /**
     * Default country code. Can be overridden during configuration.
     */
    const val COUNTRY_CODE = "ET"

    /**
     * Default currency code. Can be overridden during configuration.
     */
    const val CURRENCY_CODE = "EUR"

    /**
     * Default parameters for payment gateway.
     * Will be overridden during configuration.
     */
    val PAYMENT_GATEWAY_TOKENIZATION_PARAMETERS = mapOf(
        "gateway" to "",
        "gatewayMerchantId" to ""
    )

    /**
     * Error codes
     */
    const val E_INIT_ERROR = "INIT_ERROR"
    const val E_PAYMENT_CANCELED = "E_PAYMENT_CANCELED"
    const val E_PAYMENT_ERROR = "E_PAYMENT_ERROR"
    const val E_UNABLE_TO_DETERMINE_GOOGLE_PAY_READINESS = "E_UNABLE_TO_DETERMINE_GOOGLE_PAY_READINESS"
    const val E_GOOGLE_PAY_API_ERROR = "E_GOOGLE_PAY_API_ERROR"
}