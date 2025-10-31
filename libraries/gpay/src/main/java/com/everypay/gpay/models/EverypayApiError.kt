package com.everypay.gpay.models

/**
 * Represents an error response from the EveryPay API
 *
 * @param code Error code from the API
 * @param message Error message from the API
 * @param httpStatus HTTP status code
 * @param rawResponse Raw response body for debugging
 */
data class EverypayApiError(
    val code: String?,
    val message: String?,
    val httpStatus: Int,
    val rawResponse: String?
) {
    /**
     * Returns a formatted error message for logging and display
     */
    fun toFormattedMessage(): String {
        val parts = mutableListOf<String>()
        parts.add("HTTP $httpStatus")

        if (code != null) {
            parts.add("Error Code: $code")
        }

        if (message != null) {
            parts.add(message)
        } else if (rawResponse != null) {
            parts.add(rawResponse)
        }

        return parts.joinToString(" - ")
    }
}
