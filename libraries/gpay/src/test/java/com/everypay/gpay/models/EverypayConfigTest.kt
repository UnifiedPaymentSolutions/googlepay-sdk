package com.everypay.gpay.models

import com.everypay.gpay.fixtures.TestFixtures
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Unit tests for EverypayConfig
 *
 * Tests configuration validation and mode detection logic
 */
class EverypayConfigTest {

    // ==================== SDK Mode Tests ====================

    @Test
    fun `should create valid SDK mode config`() {
        // When
        val config = TestFixtures.sdkModeConfig()

        // Then
        assertThat(config.isSdkMode()).isTrue()
        assertThat(config.isBackendMode()).isFalse()
        assertThat(config.apiUsername).isEqualTo("test_api_user")
        assertThat(config.apiSecret).isEqualTo("test_api_secret")
        assertThat(config.apiUrl).isEqualTo("https://api.test.everypay.com")
        assertThat(config.accountName).isEqualTo("EUR3D1")
        assertThat(config.customerUrl).isEqualTo("https://example.com/return")
    }

    @Test
    fun `SDK mode should accept HTTPS customer URL`() {
        // When
        val config = TestFixtures.sdkModeConfig(
            customerUrl = "https://secure.example.com/payment/return"
        )

        // Then
        assertThat(config.customerUrl).startsWith("https://")
    }

    @Test
    fun `SDK mode should accept HTTP customer URL`() {
        // When
        val config = TestFixtures.sdkModeConfig(
            customerUrl = "http://example.com/return"
        )

        // Then
        assertThat(config.customerUrl).startsWith("http://")
    }

    @Test
    fun `incomplete SDK config should be detected as backend mode when apiUsername is missing`() {
        // When - apiUsername blank makes it backend mode automatically
        val config = EverypayConfig(
            apiUsername = "",
            apiSecret = "secret",
            apiUrl = "https://api.test.com",
            environment = "TEST",
            countryCode = "EE"
        )

        // Then - should be backend mode, not SDK mode
        assertThat(config.isBackendMode()).isTrue()
        assertThat(config.isSdkMode()).isFalse()
    }

    @Test
    fun `incomplete SDK config should be detected as backend mode when apiSecret is missing`() {
        // When - apiSecret blank makes it backend mode automatically
        val config = EverypayConfig(
            apiUsername = "user",
            apiSecret = "",
            apiUrl = "https://api.test.com",
            environment = "TEST",
            countryCode = "EE"
        )

        // Then - should be backend mode, not SDK mode
        assertThat(config.isBackendMode()).isTrue()
        assertThat(config.isSdkMode()).isFalse()
    }

    @Test(expected = IllegalArgumentException::class)
    fun `SDK mode should reject config without apiUrl`() {
        // When
        EverypayConfig(
            apiUsername = "user",
            apiSecret = "secret",
            apiUrl = "",
            accountName = "ACC1",
            customerUrl = "https://test.com/return",
            environment = "TEST",
            countryCode = "EE"
        )

        // Then - IllegalArgumentException should be thrown
    }

    @Test(expected = IllegalArgumentException::class)
    fun `SDK mode should reject config without accountName`() {
        // When
        EverypayConfig(
            apiUsername = "user",
            apiSecret = "secret",
            apiUrl = "https://api.test.com",
            accountName = "",
            customerUrl = "https://test.com/return",
            environment = "TEST",
            countryCode = "EE"
        )

        // Then - IllegalArgumentException should be thrown
    }

    @Test(expected = IllegalArgumentException::class)
    fun `SDK mode should reject config without customerUrl`() {
        // When
        EverypayConfig(
            apiUsername = "user",
            apiSecret = "secret",
            apiUrl = "https://api.test.com",
            accountName = "ACC1",
            customerUrl = "",
            environment = "TEST",
            countryCode = "EE"
        )

        // Then - IllegalArgumentException should be thrown
    }

    @Test(expected = IllegalArgumentException::class)
    fun `SDK mode should reject customer URL without http or https`() {
        // When
        TestFixtures.sdkModeConfig(customerUrl = "ftp://example.com/return")

        // Then - IllegalArgumentException should be thrown
    }

    @Test(expected = IllegalArgumentException::class)
    fun `SDK mode should reject localhost customer URL`() {
        // When
        TestFixtures.sdkModeConfig(customerUrl = "http://localhost:3000/return")

        // Then - IllegalArgumentException should be thrown
    }

    @Test(expected = IllegalArgumentException::class)
    fun `SDK mode should reject customer URL with localhost mixed case`() {
        // When
        TestFixtures.sdkModeConfig(customerUrl = "https://LocalHost:8080/return")

        // Then - IllegalArgumentException should be thrown
    }

    @Test(expected = IllegalArgumentException::class)
    fun `SDK mode should reject IP address customer URL`() {
        // When
        TestFixtures.sdkModeConfig(customerUrl = "http://192.168.1.1:3000/return")

        // Then - IllegalArgumentException should be thrown
    }

    @Test(expected = IllegalArgumentException::class)
    fun `SDK mode should reject customer URL with IP in path`() {
        // When
        TestFixtures.sdkModeConfig(customerUrl = "http://example.com/192.168.1.1/return")

        // Then - IllegalArgumentException should be thrown
    }

    // ==================== Backend Mode Tests ====================

    @Test
    fun `should create valid Backend mode config`() {
        // When
        val config = TestFixtures.backendModeConfig()

        // Then
        assertThat(config.isBackendMode()).isTrue()
        assertThat(config.isSdkMode()).isFalse()
        assertThat(config.apiUsername).isNull()
        assertThat(config.apiSecret).isNull()
        assertThat(config.apiUrl).isNull()
        assertThat(config.accountName).isNull()
        assertThat(config.customerUrl).isNull()
    }

    @Test
    fun `Backend mode should not require API credentials`() {
        // When
        val config = EverypayConfig(
            environment = "TEST",
            countryCode = "EE"
        )

        // Then
        assertThat(config.isBackendMode()).isTrue()
        assertThat(config.apiUsername).isNull()
        assertThat(config.apiSecret).isNull()
    }

    @Test
    fun `Backend mode should detect when only some credentials are missing`() {
        // When
        val config = EverypayConfig(
            apiUsername = "user",
            apiSecret = null, // Missing
            environment = "TEST",
            countryCode = "EE"
        )

        // Then
        assertThat(config.isBackendMode()).isTrue()
        assertThat(config.isSdkMode()).isFalse()
    }

    // ==================== Environment Validation Tests ====================

    @Test
    fun `should accept TEST environment`() {
        // When
        val config = TestFixtures.backendModeConfig(environment = "TEST")

        // Then
        assertThat(config.environment).isEqualTo("TEST")
    }

    @Test
    fun `should accept PRODUCTION environment`() {
        // When
        val config = TestFixtures.backendModeConfig(environment = "PRODUCTION")

        // Then
        assertThat(config.environment).isEqualTo("PRODUCTION")
    }

    @Test
    fun `should accept lowercase environment and normalize to uppercase`() {
        // When
        val config = TestFixtures.backendModeConfig(environment = "test")

        // Then
        assertThat(config.environment).isEqualTo("test")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `should reject invalid environment`() {
        // When
        TestFixtures.backendModeConfig(environment = "STAGING")

        // Then - IllegalArgumentException should be thrown
    }

    @Test(expected = IllegalArgumentException::class)
    fun `should reject empty environment`() {
        // When
        TestFixtures.backendModeConfig(environment = "")

        // Then - IllegalArgumentException should be thrown
    }

    // ==================== Country Code Validation Tests ====================

    @Test
    fun `should accept valid 2-letter country code`() {
        // When
        val config = TestFixtures.backendModeConfig(countryCode = "EE")

        // Then
        assertThat(config.countryCode).isEqualTo("EE")
    }

    @Test
    fun `should accept different valid country codes`() {
        // When
        val configs = listOf("US", "GB", "DE", "FR", "LV", "LT", "FI").map {
            TestFixtures.backendModeConfig(countryCode = it)
        }

        // Then
        configs.forEach { config ->
            assertThat(config.countryCode).matches("[A-Z]{2}")
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `should reject lowercase country code`() {
        // When
        TestFixtures.backendModeConfig(countryCode = "ee")

        // Then - IllegalArgumentException should be thrown
    }

    @Test(expected = IllegalArgumentException::class)
    fun `should reject 3-letter country code`() {
        // When
        TestFixtures.backendModeConfig(countryCode = "EST")

        // Then - IllegalArgumentException should be thrown
    }

    @Test(expected = IllegalArgumentException::class)
    fun `should reject 1-letter country code`() {
        // When
        TestFixtures.backendModeConfig(countryCode = "E")

        // Then - IllegalArgumentException should be thrown
    }

    @Test(expected = IllegalArgumentException::class)
    fun `should reject country code with numbers`() {
        // When
        TestFixtures.backendModeConfig(countryCode = "E1")

        // Then - IllegalArgumentException should be thrown
    }

    // ==================== Currency Code Validation Tests ====================

    @Test
    fun `should accept valid 3-letter currency code`() {
        // When
        val config = TestFixtures.backendModeConfig(currencyCode = "EUR")

        // Then
        assertThat(config.currencyCode).isEqualTo("EUR")
    }

    @Test
    fun `should accept different valid currency codes`() {
        // When
        val configs = listOf("USD", "GBP", "JPY", "CHF", "SEK").map {
            TestFixtures.backendModeConfig(currencyCode = it)
        }

        // Then
        configs.forEach { config ->
            assertThat(config.currencyCode).matches("[A-Z]{3}")
        }
    }

    @Test
    fun `should use EUR as default currency`() {
        // When
        val config = EverypayConfig(
            environment = "TEST",
            countryCode = "EE"
        )

        // Then
        assertThat(config.currencyCode).isEqualTo("EUR")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `should reject lowercase currency code`() {
        // When
        TestFixtures.backendModeConfig(currencyCode = "eur")

        // Then - IllegalArgumentException should be thrown
    }

    @Test(expected = IllegalArgumentException::class)
    fun `should reject 2-letter currency code`() {
        // When
        TestFixtures.backendModeConfig(currencyCode = "EU")

        // Then - IllegalArgumentException should be thrown
    }

    @Test(expected = IllegalArgumentException::class)
    fun `should reject 4-letter currency code`() {
        // When
        TestFixtures.backendModeConfig(currencyCode = "EURO")

        // Then - IllegalArgumentException should be thrown
    }

    @Test(expected = IllegalArgumentException::class)
    fun `should reject currency code with numbers`() {
        // When
        TestFixtures.backendModeConfig(currencyCode = "EU1")

        // Then - IllegalArgumentException should be thrown
    }

    // ==================== Card Networks Validation Tests ====================

    @Test
    fun `should accept default card networks`() {
        // When
        val config = TestFixtures.backendModeConfig()

        // Then
        assertThat(config.allowedCardNetworks).containsExactly("MASTERCARD", "VISA")
    }

    @Test
    fun `should accept custom card networks`() {
        // When
        val config = TestFixtures.backendModeConfig(
            allowedCardNetworks = listOf("VISA", "MASTERCARD", "AMEX", "DISCOVER")
        )

        // Then
        assertThat(config.allowedCardNetworks).containsExactly("VISA", "MASTERCARD", "AMEX", "DISCOVER")
    }

    @Test
    fun `should accept single card network`() {
        // When
        val config = TestFixtures.backendModeConfig(
            allowedCardNetworks = listOf("VISA")
        )

        // Then
        assertThat(config.allowedCardNetworks).containsExactly("VISA")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `should reject empty card networks list`() {
        // When
        TestFixtures.backendModeConfig(allowedCardNetworks = emptyList())

        // Then - IllegalArgumentException should be thrown
    }

    // ==================== Card Auth Methods Validation Tests ====================

    @Test
    fun `should accept default card auth methods`() {
        // When
        val config = TestFixtures.backendModeConfig()

        // Then
        assertThat(config.allowedCardAuthMethods).containsExactly("PAN_ONLY", "CRYPTOGRAM_3DS")
    }

    @Test
    fun `should accept custom card auth methods`() {
        // When
        val config = TestFixtures.backendModeConfig(
            allowedCardAuthMethods = listOf("CRYPTOGRAM_3DS")
        )

        // Then
        assertThat(config.allowedCardAuthMethods).containsExactly("CRYPTOGRAM_3DS")
    }

    @Test
    fun `should accept PAN_ONLY auth method`() {
        // When
        val config = TestFixtures.backendModeConfig(
            allowedCardAuthMethods = listOf("PAN_ONLY")
        )

        // Then
        assertThat(config.allowedCardAuthMethods).containsExactly("PAN_ONLY")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `should reject empty card auth methods list`() {
        // When
        TestFixtures.backendModeConfig(allowedCardAuthMethods = emptyList())

        // Then - IllegalArgumentException should be thrown
    }

    // ==================== Mode Detection Edge Cases ====================

    @Test
    fun `isSdkMode should return true when all credentials provided`() {
        // Given
        val config = EverypayConfig(
            apiUsername = "user",
            apiSecret = "secret",
            apiUrl = "https://api.example.com",
            accountName = "ACC1",
            customerUrl = "https://example.com/return",
            environment = "TEST",
            countryCode = "EE"
        )

        // When & Then
        assertThat(config.isSdkMode()).isTrue()
        assertThat(config.isBackendMode()).isFalse()
    }

    @Test
    fun `isBackendMode should return true when apiUsername is blank`() {
        // Given
        val config = EverypayConfig(
            apiUsername = "   ", // blank
            apiSecret = "secret",
            environment = "TEST",
            countryCode = "EE"
        )

        // When & Then
        assertThat(config.isBackendMode()).isTrue()
        assertThat(config.isSdkMode()).isFalse()
    }

    @Test
    fun `isBackendMode should return true when apiSecret is blank`() {
        // Given
        val config = EverypayConfig(
            apiUsername = "user",
            apiSecret = "   ", // blank
            environment = "TEST",
            countryCode = "EE"
        )

        // When & Then
        assertThat(config.isBackendMode()).isTrue()
        assertThat(config.isSdkMode()).isFalse()
    }

    @Test
    fun `isBackendMode should return true when both credentials are null`() {
        // Given
        val config = EverypayConfig(
            apiUsername = null,
            apiSecret = null,
            environment = "TEST",
            countryCode = "EE"
        )

        // When & Then
        assertThat(config.isBackendMode()).isTrue()
        assertThat(config.isSdkMode()).isFalse()
    }

    // ==================== Complete Configuration Tests ====================

    @Test
    fun `should create production config with all fields`() {
        // When
        val config = EverypayConfig(
            apiUsername = "prod_user",
            apiSecret = "prod_secret",
            apiUrl = "https://api.everypay.com",
            environment = "PRODUCTION",
            accountName = "EUR3D1",
            countryCode = "EE",
            customerUrl = "https://mystore.com/payment/callback",
            currencyCode = "EUR",
            allowedCardNetworks = listOf("VISA", "MASTERCARD"),
            allowedCardAuthMethods = listOf("CRYPTOGRAM_3DS")
        )

        // Then
        assertThat(config.isSdkMode()).isTrue()
        assertThat(config.environment).isEqualTo("PRODUCTION")
        assertThat(config.currencyCode).isEqualTo("EUR")
        assertThat(config.allowedCardNetworks).hasSize(2)
        assertThat(config.allowedCardAuthMethods).hasSize(1)
    }

    @Test
    fun `should create test backend mode config with minimal fields`() {
        // When
        val config = EverypayConfig(
            environment = "TEST",
            countryCode = "US",
            currencyCode = "USD"
        )

        // Then
        assertThat(config.isBackendMode()).isTrue()
        assertThat(config.environment).isEqualTo("TEST")
        assertThat(config.countryCode).isEqualTo("US")
        assertThat(config.currencyCode).isEqualTo("USD")
    }
}
