package com.everypay.gpay.util

import com.google.common.truth.Truth.assertThat
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Test

/**
 * Unit tests for PaymentsUtil
 *
 * Tests Google Pay API request building utilities
 */
class PaymentsUtilTest {

    // ==================== getGatewayTokenizationSpecification() Tests ====================

    @Test
    fun `getGatewayTokenizationSpecification should create correct JSON structure`() {
        // When
        val result = PaymentsUtil.getGatewayTokenizationSpecification(
            gateway = "everypay",
            gatewayMerchantId = "merchant_123"
        )

        // Then
        assertThat(result.getString("type")).isEqualTo("PAYMENT_GATEWAY")
        val params = result.getJSONObject("parameters")
        assertThat(params.getString("gateway")).isEqualTo("everypay")
        assertThat(params.getString("gatewayMerchantId")).isEqualTo("merchant_123")
    }

    @Test
    fun `getGatewayTokenizationSpecification should handle different gateway names`() {
        // When
        val result = PaymentsUtil.getGatewayTokenizationSpecification(
            gateway = "stripe",
            gatewayMerchantId = "acct_1234567890"
        )

        // Then
        val params = result.getJSONObject("parameters")
        assertThat(params.getString("gateway")).isEqualTo("stripe")
        assertThat(params.getString("gatewayMerchantId")).isEqualTo("acct_1234567890")
    }

    @Test
    fun `getGatewayTokenizationSpecification should handle special characters in merchant ID`() {
        // When
        val result = PaymentsUtil.getGatewayTokenizationSpecification(
            gateway = "everypay",
            gatewayMerchantId = "merchant_123-ABC_xyz.test"
        )

        // Then
        val params = result.getJSONObject("parameters")
        assertThat(params.getString("gatewayMerchantId")).isEqualTo("merchant_123-ABC_xyz.test")
    }

    // ==================== getBaseCardPaymentMethod() Tests ====================

    @Test
    fun `getBaseCardPaymentMethod should create correct JSON structure`() {
        // Given
        val cardNetworks = listOf("MASTERCARD", "VISA")
        val authMethods = listOf("PAN_ONLY", "CRYPTOGRAM_3DS")

        // When
        val result = PaymentsUtil.getBaseCardPaymentMethod(cardNetworks, authMethods)

        // Then
        assertThat(result.getString("type")).isEqualTo("CARD")
        val params = result.getJSONObject("parameters")
        val networks = params.getJSONArray("allowedCardNetworks")
        val methods = params.getJSONArray("allowedAuthMethods")

        assertThat(networks.length()).isEqualTo(2)
        assertThat(networks.getString(0)).isEqualTo("MASTERCARD")
        assertThat(networks.getString(1)).isEqualTo("VISA")

        assertThat(methods.length()).isEqualTo(2)
        assertThat(methods.getString(0)).isEqualTo("PAN_ONLY")
        assertThat(methods.getString(1)).isEqualTo("CRYPTOGRAM_3DS")
    }

    @Test
    fun `getBaseCardPaymentMethod should handle single card network`() {
        // Given
        val cardNetworks = listOf("VISA")
        val authMethods = listOf("CRYPTOGRAM_3DS")

        // When
        val result = PaymentsUtil.getBaseCardPaymentMethod(cardNetworks, authMethods)

        // Then
        val params = result.getJSONObject("parameters")
        val networks = params.getJSONArray("allowedCardNetworks")
        assertThat(networks.length()).isEqualTo(1)
        assertThat(networks.getString(0)).isEqualTo("VISA")
    }

    @Test
    fun `getBaseCardPaymentMethod should handle multiple card networks`() {
        // Given
        val cardNetworks = listOf("MASTERCARD", "VISA", "AMEX", "DISCOVER")
        val authMethods = listOf("PAN_ONLY")

        // When
        val result = PaymentsUtil.getBaseCardPaymentMethod(cardNetworks, authMethods)

        // Then
        val params = result.getJSONObject("parameters")
        val networks = params.getJSONArray("allowedCardNetworks")
        assertThat(networks.length()).isEqualTo(4)
        assertThat(networks.getString(0)).isEqualTo("MASTERCARD")
        assertThat(networks.getString(1)).isEqualTo("VISA")
        assertThat(networks.getString(2)).isEqualTo("AMEX")
        assertThat(networks.getString(3)).isEqualTo("DISCOVER")
    }

    @Test
    fun `getBaseCardPaymentMethod should handle empty lists`() {
        // Given
        val cardNetworks = emptyList<String>()
        val authMethods = emptyList<String>()

        // When
        val result = PaymentsUtil.getBaseCardPaymentMethod(cardNetworks, authMethods)

        // Then
        val params = result.getJSONObject("parameters")
        val networks = params.getJSONArray("allowedCardNetworks")
        val methods = params.getJSONArray("allowedAuthMethods")
        assertThat(networks.length()).isEqualTo(0)
        assertThat(methods.length()).isEqualTo(0)
    }

    // ==================== getCardPaymentMethod() Tests ====================

    @Test
    fun `getCardPaymentMethod should include tokenization specification`() {
        // Given
        val cardNetworks = listOf("VISA", "MASTERCARD")
        val authMethods = listOf("CRYPTOGRAM_3DS")

        // When
        val result = PaymentsUtil.getCardPaymentMethod(
            allowedCardNetworks = cardNetworks,
            allowedCardAuthMethods = authMethods,
            gateway = "everypay",
            gatewayMerchantId = "merchant_xyz"
        )

        // Then
        assertThat(result.getString("type")).isEqualTo("CARD")
        assertThat(result.has("tokenizationSpecification")).isTrue()

        val tokenSpec = result.getJSONObject("tokenizationSpecification")
        assertThat(tokenSpec.getString("type")).isEqualTo("PAYMENT_GATEWAY")

        val tokenParams = tokenSpec.getJSONObject("parameters")
        assertThat(tokenParams.getString("gateway")).isEqualTo("everypay")
        assertThat(tokenParams.getString("gatewayMerchantId")).isEqualTo("merchant_xyz")
    }

    @Test
    fun `getCardPaymentMethod should include card parameters`() {
        // Given
        val cardNetworks = listOf("VISA")
        val authMethods = listOf("PAN_ONLY", "CRYPTOGRAM_3DS")

        // When
        val result = PaymentsUtil.getCardPaymentMethod(
            allowedCardNetworks = cardNetworks,
            allowedCardAuthMethods = authMethods,
            gateway = "stripe",
            gatewayMerchantId = "merchant_abc"
        )

        // Then
        val params = result.getJSONObject("parameters")
        val networks = params.getJSONArray("allowedCardNetworks")
        val methods = params.getJSONArray("allowedAuthMethods")

        assertThat(networks.getString(0)).isEqualTo("VISA")
        assertThat(methods.length()).isEqualTo(2)
    }

    // ==================== getTransactionInfo() Tests ====================

    @Test
    fun `getTransactionInfo should create correct JSON structure`() {
        // When
        val result = PaymentsUtil.getTransactionInfo(
            currencyCode = "EUR",
            amount = "10.50"
        )

        // Then
        assertThat(result.getString("totalPrice")).isEqualTo("10.50")
        assertThat(result.getString("totalPriceStatus")).isEqualTo("FINAL")
        assertThat(result.getString("currencyCode")).isEqualTo("EUR")
    }

    @Test
    fun `getTransactionInfo should handle different currencies`() {
        // When
        val resultUSD = PaymentsUtil.getTransactionInfo("USD", "25.99")
        val resultGBP = PaymentsUtil.getTransactionInfo("GBP", "15.00")
        val resultJPY = PaymentsUtil.getTransactionInfo("JPY", "1000")

        // Then
        assertThat(resultUSD.getString("currencyCode")).isEqualTo("USD")
        assertThat(resultUSD.getString("totalPrice")).isEqualTo("25.99")

        assertThat(resultGBP.getString("currencyCode")).isEqualTo("GBP")
        assertThat(resultGBP.getString("totalPrice")).isEqualTo("15.00")

        assertThat(resultJPY.getString("currencyCode")).isEqualTo("JPY")
        assertThat(resultJPY.getString("totalPrice")).isEqualTo("1000")
    }

    @Test
    fun `getTransactionInfo should handle decimal amounts`() {
        // When
        val result1 = PaymentsUtil.getTransactionInfo("EUR", "0.99")
        val result2 = PaymentsUtil.getTransactionInfo("EUR", "100.00")
        val result3 = PaymentsUtil.getTransactionInfo("EUR", "1234.56")

        // Then
        assertThat(result1.getString("totalPrice")).isEqualTo("0.99")
        assertThat(result2.getString("totalPrice")).isEqualTo("100.00")
        assertThat(result3.getString("totalPrice")).isEqualTo("1234.56")
    }

    @Test
    fun `getTransactionInfo should always set totalPriceStatus to FINAL`() {
        // When
        val result = PaymentsUtil.getTransactionInfo("EUR", "50.00")

        // Then
        assertThat(result.getString("totalPriceStatus")).isEqualTo("FINAL")
    }

    // ==================== getMerchantInfo() Tests ====================

    @Test
    fun `getMerchantInfo should create correct JSON structure`() {
        // When
        val result = PaymentsUtil.getMerchantInfo("Test Merchant")

        // Then
        assertThat(result.getString("merchantName")).isEqualTo("Test Merchant")
    }

    @Test
    fun `getMerchantInfo should handle different merchant names`() {
        // When
        val result1 = PaymentsUtil.getMerchantInfo("Acme Corp")
        val result2 = PaymentsUtil.getMerchantInfo("My Online Store")
        val result3 = PaymentsUtil.getMerchantInfo("Café & Bakery")

        // Then
        assertThat(result1.getString("merchantName")).isEqualTo("Acme Corp")
        assertThat(result2.getString("merchantName")).isEqualTo("My Online Store")
        assertThat(result3.getString("merchantName")).isEqualTo("Café & Bakery")
    }

    @Test
    fun `getMerchantInfo should handle empty merchant name`() {
        // When
        val result = PaymentsUtil.getMerchantInfo("")

        // Then
        assertThat(result.getString("merchantName")).isEqualTo("")
    }

    // ==================== getIsReadyToPayRequest() Tests ====================

    @Test
    fun `getIsReadyToPayRequest should create correct JSON structure`() {
        // Given
        val cardNetworks = listOf("VISA", "MASTERCARD")
        val authMethods = listOf("PAN_ONLY", "CRYPTOGRAM_3DS")

        // When
        val result = PaymentsUtil.getIsReadyToPayRequest(cardNetworks, authMethods)

        // Then
        assertThat(result).isNotNull()
        assertThat(result!!.getInt("apiVersion")).isEqualTo(2)
        assertThat(result.getInt("apiVersionMinor")).isEqualTo(0)

        val allowedMethods = result.getJSONArray("allowedPaymentMethods")
        assertThat(allowedMethods.length()).isEqualTo(1)

        val paymentMethod = allowedMethods.getJSONObject(0)
        assertThat(paymentMethod.getString("type")).isEqualTo("CARD")
    }

    @Test
    fun `getIsReadyToPayRequest should include card networks and auth methods`() {
        // Given
        val cardNetworks = listOf("VISA")
        val authMethods = listOf("CRYPTOGRAM_3DS")

        // When
        val result = PaymentsUtil.getIsReadyToPayRequest(cardNetworks, authMethods)

        // Then
        assertThat(result).isNotNull()
        val paymentMethods = result!!.getJSONArray("allowedPaymentMethods")
        val cardMethod = paymentMethods.getJSONObject(0)
        val params = cardMethod.getJSONObject("parameters")

        val networks = params.getJSONArray("allowedCardNetworks")
        val methods = params.getJSONArray("allowedAuthMethods")

        assertThat(networks.getString(0)).isEqualTo("VISA")
        assertThat(methods.getString(0)).isEqualTo("CRYPTOGRAM_3DS")
    }

    @Test
    fun `getIsReadyToPayRequest should handle empty lists`() {
        // Given
        val cardNetworks = emptyList<String>()
        val authMethods = emptyList<String>()

        // When
        val result = PaymentsUtil.getIsReadyToPayRequest(cardNetworks, authMethods)

        // Then
        assertThat(result).isNotNull()
        assertThat(result!!.has("allowedPaymentMethods")).isTrue()
    }

    // ==================== getPaymentDataRequest() Tests ====================

    @Test
    fun `getPaymentDataRequest should create complete request structure`() {
        // Given
        val cardNetworks = listOf("VISA", "MASTERCARD")
        val authMethods = listOf("PAN_ONLY", "CRYPTOGRAM_3DS")

        // When
        val result = PaymentsUtil.getPaymentDataRequest(
            allowedCardNetworks = cardNetworks,
            allowedCardAuthMethods = authMethods,
            gateway = "everypay",
            gatewayMerchantId = "merchant_123",
            currencyCode = "EUR",
            amount = "10.50",
            merchantName = "Test Store"
        )

        // Then
        assertThat(result).isNotNull()
        assertThat(result!!.getInt("apiVersion")).isEqualTo(2)
        assertThat(result.getInt("apiVersionMinor")).isEqualTo(0)
        assertThat(result.has("allowedPaymentMethods")).isTrue()
        assertThat(result.has("transactionInfo")).isTrue()
        assertThat(result.has("merchantInfo")).isTrue()
    }

    @Test
    fun `getPaymentDataRequest should include transaction info`() {
        // When
        val result = PaymentsUtil.getPaymentDataRequest(
            allowedCardNetworks = listOf("VISA"),
            allowedCardAuthMethods = listOf("CRYPTOGRAM_3DS"),
            gateway = "everypay",
            gatewayMerchantId = "merchant_abc",
            currencyCode = "USD",
            amount = "25.99",
            merchantName = "My Shop"
        )

        // Then
        assertThat(result).isNotNull()
        val transactionInfo = result!!.getJSONObject("transactionInfo")
        assertThat(transactionInfo.getString("totalPrice")).isEqualTo("25.99")
        assertThat(transactionInfo.getString("currencyCode")).isEqualTo("USD")
        assertThat(transactionInfo.getString("totalPriceStatus")).isEqualTo("FINAL")
    }

    @Test
    fun `getPaymentDataRequest should include merchant info`() {
        // When
        val result = PaymentsUtil.getPaymentDataRequest(
            allowedCardNetworks = listOf("VISA"),
            allowedCardAuthMethods = listOf("PAN_ONLY"),
            gateway = "everypay",
            gatewayMerchantId = "merchant_xyz",
            currencyCode = "EUR",
            amount = "15.00",
            merchantName = "Awesome Store"
        )

        // Then
        assertThat(result).isNotNull()
        val merchantInfo = result!!.getJSONObject("merchantInfo")
        assertThat(merchantInfo.getString("merchantName")).isEqualTo("Awesome Store")
    }

    @Test
    fun `getPaymentDataRequest should include payment methods with tokenization`() {
        // When
        val result = PaymentsUtil.getPaymentDataRequest(
            allowedCardNetworks = listOf("VISA", "MASTERCARD"),
            allowedCardAuthMethods = listOf("CRYPTOGRAM_3DS"),
            gateway = "everypay",
            gatewayMerchantId = "merchant_def",
            currencyCode = "EUR",
            amount = "100.00",
            merchantName = "Test"
        )

        // Then
        assertThat(result).isNotNull()
        val paymentMethods = result!!.getJSONArray("allowedPaymentMethods")
        assertThat(paymentMethods.length()).isEqualTo(1)

        val cardMethod = paymentMethods.getJSONObject(0)
        assertThat(cardMethod.getString("type")).isEqualTo("CARD")
        assertThat(cardMethod.has("tokenizationSpecification")).isTrue()

        val tokenSpec = cardMethod.getJSONObject("tokenizationSpecification")
        val tokenParams = tokenSpec.getJSONObject("parameters")
        assertThat(tokenParams.getString("gateway")).isEqualTo("everypay")
        assertThat(tokenParams.getString("gatewayMerchantId")).isEqualTo("merchant_def")
    }

    @Test
    fun `getPaymentDataRequest should handle all parameters correctly`() {
        // When
        val result = PaymentsUtil.getPaymentDataRequest(
            allowedCardNetworks = listOf("AMEX", "DISCOVER", "MASTERCARD", "VISA"),
            allowedCardAuthMethods = listOf("PAN_ONLY", "CRYPTOGRAM_3DS"),
            gateway = "stripe",
            gatewayMerchantId = "acct_1234567890",
            currencyCode = "GBP",
            amount = "49.99",
            merchantName = "British Shop Ltd"
        )

        // Then
        assertThat(result).isNotNull()

        // Verify all components are present and correct
        val paymentMethods = result!!.getJSONArray("allowedPaymentMethods")
        val cardMethod = paymentMethods.getJSONObject(0)
        val cardParams = cardMethod.getJSONObject("parameters")
        val networks = cardParams.getJSONArray("allowedCardNetworks")

        assertThat(networks.length()).isEqualTo(4)

        val transactionInfo = result.getJSONObject("transactionInfo")
        assertThat(transactionInfo.getString("currencyCode")).isEqualTo("GBP")
        assertThat(transactionInfo.getString("totalPrice")).isEqualTo("49.99")

        val merchantInfo = result.getJSONObject("merchantInfo")
        assertThat(merchantInfo.getString("merchantName")).isEqualTo("British Shop Ltd")
    }

    @Test
    fun `getPaymentDataRequest should handle minimum required parameters`() {
        // When
        val result = PaymentsUtil.getPaymentDataRequest(
            allowedCardNetworks = listOf("VISA"),
            allowedCardAuthMethods = listOf("CRYPTOGRAM_3DS"),
            gateway = "everypay",
            gatewayMerchantId = "m",
            currencyCode = "EUR",
            amount = "1",
            merchantName = "M"
        )

        // Then
        assertThat(result).isNotNull()
        assertThat(result!!.has("allowedPaymentMethods")).isTrue()
        assertThat(result.has("transactionInfo")).isTrue()
        assertThat(result.has("merchantInfo")).isTrue()
    }

    // ==================== Base Request Tests ====================

    @Test
    fun `all request methods should include apiVersion 2 and apiVersionMinor 0`() {
        // Given
        val cardNetworks = listOf("VISA")
        val authMethods = listOf("CRYPTOGRAM_3DS")

        // When
        val readyToPayRequest = PaymentsUtil.getIsReadyToPayRequest(cardNetworks, authMethods)
        val paymentDataRequest = PaymentsUtil.getPaymentDataRequest(
            cardNetworks, authMethods, "everypay", "merchant", "EUR", "10", "Test"
        )

        // Then
        assertThat(readyToPayRequest!!.getInt("apiVersion")).isEqualTo(2)
        assertThat(readyToPayRequest.getInt("apiVersionMinor")).isEqualTo(0)

        assertThat(paymentDataRequest!!.getInt("apiVersion")).isEqualTo(2)
        assertThat(paymentDataRequest.getInt("apiVersionMinor")).isEqualTo(0)
    }

    // ==================== Edge Cases ====================

    @Test
    fun `should handle large amounts`() {
        // When
        val result = PaymentsUtil.getTransactionInfo("EUR", "9999999.99")

        // Then
        assertThat(result.getString("totalPrice")).isEqualTo("9999999.99")
    }

    @Test
    fun `should handle very small amounts`() {
        // When
        val result = PaymentsUtil.getTransactionInfo("EUR", "0.01")

        // Then
        assertThat(result.getString("totalPrice")).isEqualTo("0.01")
    }

    @Test
    fun `should handle long merchant names`() {
        // Given
        val longName = "Very Long Merchant Name That Exceeds Normal Length But Should Still Work Fine"

        // When
        val result = PaymentsUtil.getMerchantInfo(longName)

        // Then
        assertThat(result.getString("merchantName")).isEqualTo(longName)
    }

    @Test
    fun `should handle special characters in gateway merchant ID`() {
        // When
        val result = PaymentsUtil.getGatewayTokenizationSpecification(
            gateway = "everypay",
            gatewayMerchantId = "merchant_123-ABC.xyz_test!@#"
        )

        // Then
        val params = result.getJSONObject("parameters")
        assertThat(params.getString("gatewayMerchantId")).isEqualTo("merchant_123-ABC.xyz_test!@#")
    }
}
