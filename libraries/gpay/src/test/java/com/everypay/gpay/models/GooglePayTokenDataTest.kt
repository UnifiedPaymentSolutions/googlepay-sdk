package com.everypay.gpay.models

import com.everypay.gpay.fixtures.TestFixtures
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Unit tests for GooglePayTokenData
 *
 * Tests JSON serialization and conversion methods
 */
class GooglePayTokenDataTest {

    // ==================== toJson() Tests ====================

    @Test
    fun `toJson should create correct JSON structure`() {
        // Given
        val tokenData = TestFixtures.googlePayTokenData(
            paymentReference = "payment_123",
            mobileAccessToken = "token_456",
            signature = "sig_xyz",
            protocolVersion = "ECv2",
            signedMessage = "msg_abc",
            tokenConsentAgreed = false
        )

        // When
        val json = tokenData.toJson()

        // Then
        assertThat(json.getString("payment_reference")).isEqualTo("payment_123")
        assertThat(json.getString("mobile_access_token")).isEqualTo("token_456")
        assertThat(json.getBoolean("token_consent_agreed")).isFalse()
        assertThat(json.getString("signature")).isEqualTo("sig_xyz")
        assertThat(json.getString("protocolVersion")).isEqualTo("ECv2")
        assertThat(json.getString("signedMessage")).isEqualTo("msg_abc")
    }

    @Test
    fun `toJson should include intermediateSigningKey object`() {
        // Given
        val tokenData = TestFixtures.googlePayTokenData(
            intermediateSigningKey = IntermediateSigningKey(
                signedKey = "key_123",
                signatures = listOf("sig1", "sig2", "sig3")
            )
        )

        // When
        val json = tokenData.toJson()

        // Then
        assertThat(json.has("intermediateSigningKey")).isTrue()
        val signingKey = json.getJSONObject("intermediateSigningKey")
        assertThat(signingKey.getString("signedKey")).isEqualTo("key_123")

        val signatures = signingKey.getJSONArray("signatures")
        assertThat(signatures.length()).isEqualTo(3)
        assertThat(signatures.getString(0)).isEqualTo("sig1")
        assertThat(signatures.getString(1)).isEqualTo("sig2")
        assertThat(signatures.getString(2)).isEqualTo("sig3")
    }

    @Test
    fun `toJson should use snake_case for payment fields`() {
        // Given
        val tokenData = TestFixtures.googlePayTokenData(
            paymentReference = "ref_123",
            mobileAccessToken = "token_456",
            tokenConsentAgreed = true
        )

        // When
        val json = tokenData.toJson()

        // Then
        // These fields should be in snake_case
        assertThat(json.has("payment_reference")).isTrue()
        assertThat(json.has("mobile_access_token")).isTrue()
        assertThat(json.has("token_consent_agreed")).isTrue()

        // These fields should be in camelCase (per EveryPay API spec)
        assertThat(json.has("protocolVersion")).isTrue()
        assertThat(json.has("signedMessage")).isTrue()
        assertThat(json.has("intermediateSigningKey")).isTrue()
    }

    @Test
    fun `toJson should handle tokenConsentAgreed true`() {
        // Given
        val tokenData = TestFixtures.googlePayTokenData(
            tokenConsentAgreed = true
        )

        // When
        val json = tokenData.toJson()

        // Then
        assertThat(json.getBoolean("token_consent_agreed")).isTrue()
    }

    @Test
    fun `toJson should handle tokenConsentAgreed false`() {
        // Given
        val tokenData = TestFixtures.googlePayTokenData(
            tokenConsentAgreed = false
        )

        // When
        val json = tokenData.toJson()

        // Then
        assertThat(json.getBoolean("token_consent_agreed")).isFalse()
    }

    @Test
    fun `toJson should handle empty signatures list`() {
        // Given
        val tokenData = TestFixtures.googlePayTokenData(
            intermediateSigningKey = IntermediateSigningKey(
                signedKey = "key_abc",
                signatures = emptyList()
            )
        )

        // When
        val json = tokenData.toJson()

        // Then
        val signingKey = json.getJSONObject("intermediateSigningKey")
        val signatures = signingKey.getJSONArray("signatures")
        assertThat(signatures.length()).isEqualTo(0)
    }

    @Test
    fun `toJson should handle single signature`() {
        // Given
        val tokenData = TestFixtures.googlePayTokenData(
            intermediateSigningKey = IntermediateSigningKey(
                signedKey = "key_xyz",
                signatures = listOf("single_sig")
            )
        )

        // When
        val json = tokenData.toJson()

        // Then
        val signingKey = json.getJSONObject("intermediateSigningKey")
        val signatures = signingKey.getJSONArray("signatures")
        assertThat(signatures.length()).isEqualTo(1)
        assertThat(signatures.getString(0)).isEqualTo("single_sig")
    }

    // ==================== toProcessPaymentRequest() Tests ====================

    @Test
    fun `toProcessPaymentRequest should convert all fields correctly`() {
        // Given
        val intermediateKey = IntermediateSigningKey(
            signedKey = "key_123",
            signatures = listOf("sig1", "sig2")
        )
        val tokenData = GooglePayTokenData(
            paymentReference = "payment_ref_789",
            mobileAccessToken = "token_not_included",
            signature = "signature_abc",
            intermediateSigningKey = intermediateKey,
            protocolVersion = "ECv2",
            signedMessage = "message_xyz",
            tokenConsentAgreed = true
        )

        // When
        val request = tokenData.toProcessPaymentRequest()

        // Then
        assertThat(request.paymentReference).isEqualTo("payment_ref_789")
        assertThat(request.tokenConsentAgreed).isTrue()
        assertThat(request.signature).isEqualTo("signature_abc")
        assertThat(request.protocolVersion).isEqualTo("ECv2")
        assertThat(request.signedMessage).isEqualTo("message_xyz")
        assertThat(request.intermediateSigningKey).isEqualTo(intermediateKey)
    }

    @Test
    fun `toProcessPaymentRequest should not include mobileAccessToken`() {
        // Given
        val tokenData = TestFixtures.googlePayTokenData(
            mobileAccessToken = "this_should_not_be_in_request"
        )

        // When
        val request = tokenData.toProcessPaymentRequest()

        // Then
        // ProcessPaymentRequest doesn't have mobileAccessToken field
        // Just verify the conversion works and other fields are present
        assertThat(request.paymentReference).isNotNull()
        assertThat(request.signature).isNotNull()
        assertThat(request.protocolVersion).isNotNull()
        assertThat(request.signedMessage).isNotNull()
    }

    @Test
    fun `toProcessPaymentRequest should preserve tokenConsentAgreed false`() {
        // Given
        val tokenData = TestFixtures.googlePayTokenData(
            tokenConsentAgreed = false
        )

        // When
        val request = tokenData.toProcessPaymentRequest()

        // Then
        assertThat(request.tokenConsentAgreed).isFalse()
    }

    @Test
    fun `toProcessPaymentRequest should preserve tokenConsentAgreed true`() {
        // Given
        val tokenData = TestFixtures.googlePayTokenData(
            tokenConsentAgreed = true
        )

        // When
        val request = tokenData.toProcessPaymentRequest()

        // Then
        assertThat(request.tokenConsentAgreed).isTrue()
    }

    @Test
    fun `toProcessPaymentRequest should preserve intermediateSigningKey`() {
        // Given
        val signingKey = IntermediateSigningKey(
            signedKey = "unique_key_abc",
            signatures = listOf("signature_1", "signature_2", "signature_3")
        )
        val tokenData = TestFixtures.googlePayTokenData(
            intermediateSigningKey = signingKey
        )

        // When
        val request = tokenData.toProcessPaymentRequest()

        // Then
        assertThat(request.intermediateSigningKey).isEqualTo(signingKey)
        assertThat(request.intermediateSigningKey.signedKey).isEqualTo("unique_key_abc")
        assertThat(request.intermediateSigningKey.signatures).hasSize(3)
    }

    // ==================== Data Class Tests ====================

    @Test
    fun `should create GooglePayTokenData with all fields`() {
        // Given
        val intermediateKey = IntermediateSigningKey("key", listOf("sig"))

        // When
        val tokenData = GooglePayTokenData(
            paymentReference = "ref",
            mobileAccessToken = "token",
            signature = "sig",
            intermediateSigningKey = intermediateKey,
            protocolVersion = "ECv2",
            signedMessage = "msg",
            tokenConsentAgreed = true
        )

        // Then
        assertThat(tokenData.paymentReference).isEqualTo("ref")
        assertThat(tokenData.mobileAccessToken).isEqualTo("token")
        assertThat(tokenData.signature).isEqualTo("sig")
        assertThat(tokenData.intermediateSigningKey).isEqualTo(intermediateKey)
        assertThat(tokenData.protocolVersion).isEqualTo("ECv2")
        assertThat(tokenData.signedMessage).isEqualTo("msg")
        assertThat(tokenData.tokenConsentAgreed).isTrue()
    }

    @Test
    fun `should use false as default for tokenConsentAgreed`() {
        // When
        val tokenData = GooglePayTokenData(
            paymentReference = "ref",
            mobileAccessToken = "token",
            signature = "sig",
            intermediateSigningKey = IntermediateSigningKey("key", listOf("sig")),
            protocolVersion = "ECv2",
            signedMessage = "msg"
            // tokenConsentAgreed not provided, should default to false
        )

        // Then
        assertThat(tokenData.tokenConsentAgreed).isFalse()
    }

    @Test
    fun `two GooglePayTokenData with same values should be equal`() {
        // Given
        val key = IntermediateSigningKey("key", listOf("sig1"))
        val tokenData1 = GooglePayTokenData(
            paymentReference = "ref1",
            mobileAccessToken = "token1",
            signature = "sig1",
            intermediateSigningKey = key,
            protocolVersion = "ECv2",
            signedMessage = "msg1",
            tokenConsentAgreed = false
        )
        val tokenData2 = GooglePayTokenData(
            paymentReference = "ref1",
            mobileAccessToken = "token1",
            signature = "sig1",
            intermediateSigningKey = key,
            protocolVersion = "ECv2",
            signedMessage = "msg1",
            tokenConsentAgreed = false
        )

        // When & Then
        assertThat(tokenData1).isEqualTo(tokenData2)
        assertThat(tokenData1.hashCode()).isEqualTo(tokenData2.hashCode())
    }

    @Test
    fun `two GooglePayTokenData with different values should not be equal`() {
        // Given
        val key = IntermediateSigningKey("key", listOf("sig1"))
        val tokenData1 = GooglePayTokenData(
            paymentReference = "ref1",
            mobileAccessToken = "token1",
            signature = "sig1",
            intermediateSigningKey = key,
            protocolVersion = "ECv2",
            signedMessage = "msg1",
            tokenConsentAgreed = false
        )
        val tokenData2 = GooglePayTokenData(
            paymentReference = "ref2", // different
            mobileAccessToken = "token1",
            signature = "sig1",
            intermediateSigningKey = key,
            protocolVersion = "ECv2",
            signedMessage = "msg1",
            tokenConsentAgreed = false
        )

        // When & Then
        assertThat(tokenData1).isNotEqualTo(tokenData2)
    }

    // ==================== Integration Tests ====================

    @Test
    fun `full flow - create tokenData, convert to JSON, verify structure`() {
        // Given
        val signingKey = IntermediateSigningKey(
            signedKey = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE...",
            signatures = listOf(
                "MEYCIQCbtZLk3...",
                "MEUCIQDxVfY7..."
            )
        )
        val tokenData = GooglePayTokenData(
            paymentReference = "pmt_abc123xyz",
            mobileAccessToken = "mtok_456def789",
            signature = "MEQCIH8Y...",
            intermediateSigningKey = signingKey,
            protocolVersion = "ECv2",
            signedMessage = "{\"encryptedMessage\":\"...\"}",
            tokenConsentAgreed = true
        )

        // When
        val json = tokenData.toJson()
        val request = tokenData.toProcessPaymentRequest()

        // Then - JSON should have all required fields
        assertThat(json.getString("payment_reference")).isEqualTo("pmt_abc123xyz")
        assertThat(json.getString("mobile_access_token")).isEqualTo("mtok_456def789")
        assertThat(json.getBoolean("token_consent_agreed")).isTrue()

        // Then - ProcessPaymentRequest should have correct fields
        assertThat(request.paymentReference).isEqualTo("pmt_abc123xyz")
        assertThat(request.tokenConsentAgreed).isTrue()
        assertThat(request.signature).isEqualTo("MEQCIH8Y...")
        assertThat(request.intermediateSigningKey.signatures).hasSize(2)
    }
}
