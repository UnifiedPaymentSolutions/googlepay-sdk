# Google Pay Token Requests for Recurring Payments

This guide explains how to request Google Pay tokens for Merchant Initiated Transactions (MIT) and recurring payments.

## Table of Contents
- [Overview](#overview)
- [User Consent Requirements](#user-consent-requirements)
- [Backend Mode (Recommended)](#backend-mode-recommended)
- [SDK Mode](#sdk-mode)
- [Token Data Structure](#token-data-structure)

---

## Overview

When setting up recurring payments or card-on-file functionality, you need to collect a payment token from Google Pay that can be used for future Merchant Initiated Transactions (MIT). This is different from a regular one-time payment.

### Token Request vs Payment Request

| Feature | Payment Request | Token Request |
|---------|----------------|---------------|
| Purpose | Process immediate payment | Collect token for future use |
| Google Pay Status | `totalPriceStatus: "FINAL"` | `totalPriceStatus: "ESTIMATED"` |
| Amount | Actual payment amount | `"0"` (zero) |
| Token Consent | `false` | `true` |
| Backend Processing | Immediate charge | MIT token required from EveryPay |

---

## User Consent Requirements

**Before requesting a payment token, your app MUST obtain explicit user consent to store their card details for future payments.**

### What Users Should Know

When collecting a token for recurring payments, users must be clearly informed about:

1. **Purpose** - Why you're requesting to save their card (e.g., "Save card for future subscriptions", "Enable one-click checkout")
2. **Scope** - What the stored card will be used for (e.g., "Automatic monthly billing", "Future purchases")
3. **Amount** - If applicable, the expected payment amounts or frequency
4. **Cancellation** - How they can revoke consent and remove the stored card

### Best Practices for Consent UI

```kotlin
// Example: Show consent dialog before requesting token
AlertDialog.Builder(context)
    .setTitle("Save Card for Future Payments")
    .setMessage(
        "By continuing, you authorize us to securely store your card details " +
        "for future purchases. You can remove your saved card at any time in Settings."
    )
    .setPositiveButton("I Agree") { _, _ ->
        // User consented - proceed with token request
        helper.requestToken("Card verification") { result ->
            // Handle result
        }
    }
    .setNegativeButton("Cancel", null)
    .show()
```

### Compliance Considerations

- **PCI DSS** - You're responsible for securely handling and storing the MIT token
- **GDPR/Privacy Laws** - Users have the right to revoke consent and request data deletion
- **Card Network Rules** - Visa, Mastercard require explicit cardholder consent for stored credentials
- **Audit Trail** - Keep records of when and how consent was obtained

### Technical Implementation

The SDK automatically sets `token_consent_agreed: true` when you use `requestToken()` or `requestTokenWithBackendData()`. However, **you must obtain user consent in your app UI before calling these methods**.

---

## Backend Mode (Recommended)

### Flow Diagram

```
Backend (Your Server)              Android App                 Google Pay
      │                                │                          │
      │  1. Request token collection   │                          │
      │◄───────────────────────────────│                          │
      │                                │                          │
      │  2. Call EveryPay API          │                          │
      │    - open_session              │                          │
      │    - oneoff (amount=0,         │                          │
      │      request_token=true)       │                          │
      │                                │                          │
      │  3. Return session + payment   │                          │
      │     data (payment_reference,   │                          │
      │     mobile_access_token)       │                          │
      │───────────────────────────────►│                          │
      │                                │                          │
      │                                │  4. Show Google Pay      │
      │                                │  (amount: $0, ESTIMATED) │
      │                                │─────────────────────────►│
      │                                │                          │
      │                                │  5. User authorizes      │
      │                                │◄─────────────────────────│
      │                                │                          │
      │  6. Send Google Pay token      │                          │
      │◄───────────────────────────────│                          │
      │                                │                          │
      │  7. Call EveryPay API          │                          │
      │    - payment_data (process)    │                          │
      │    - GET /payments (MIT token) │                          │
      │                                │                          │
      │  8. Store MIT token            │                          │
      │                                │                          │
      │  9. Return confirmation        │                          │
      │───────────────────────────────►│                          │
```

The main difference between the usual payment and MIT token request is that the `payments/oneoff` request must be made with additional parameters and amount = 0. After payment finalization, the additional `GET /payments/{payment_reference}?api_username=123...` must be made to receive the token to use later for the MIT (Merchant Initiated Transactions) payments.

### Step 1: Configure for Backend Mode

```kotlin
val config = EverypayConfig(
    apiUsername = null,      // Backend handles credentials
    apiSecret = null,
    apiUrl = null,
    environment = "TEST",    // or "PRODUCTION"
    accountName = null,
    countryCode = "EE",
    currencyCode = "EUR"
)

val helper = EverypayGooglePayHelper(activity, config)
```

### Step 2: Get Session and Payment Data from Backend

```kotlin
// 1. Request token collection from your backend
// Your backend calls:
//    - POST /api/v4/google_pay/open_session
//    - POST /api/v4/payments/oneoff (with amount=0, request_token=true,
//      token_consent_agreed=true, token_agreement=unscheduled)
val backendData = yourBackend.requestTokenCollection(
    label = "Card verification"
)

// backendData contains:
// - Session info (googlepay_merchant_identifier, merchant_name, etc.)
// - Payment info (payment_reference, mobile_access_token)

// 2. Initialize SDK with backend data
helper.initializeWithBackendData(backendData) { result ->
    when (result) {
        is GooglePayReadinessResult.Success -> {
            println("Google Pay ready for token request")
        }
        is GooglePayReadinessResult.Error -> {
            println("Error: ${result.message}")
        }
    }
}
```

### Step 3: Request Token

```kotlin
helper.requestTokenWithBackendData(backendData) { result ->
    when (result) {
        is GooglePayResult.TokenReceived -> {
            // Token received! Send to your backend for processing
            val tokenData = result.tokenData

            // tokenData.tokenConsentAgreed will be true
            // tokenData contains the Google Pay token that needs processing by EveryPay

            // Send to your backend for EveryPay processing
            yourBackend.processGooglePayToken(
                paymentReference = tokenData.paymentReference,
                signature = tokenData.signature,
                intermediateSigningKey = tokenData.intermediateSigningKey,
                protocolVersion = tokenData.protocolVersion,
                signedMessage = tokenData.signedMessage,
                tokenConsentAgreed = tokenData.tokenConsentAgreed
            ) { success ->
                // Backend stores the MIT token and returns confirmation
                if (success) {
                    println("Token successfully saved on backend")
                    // Token is stored on backend, ready for future recurring payments
                } else {
                    println("Failed to save token")
                }
            }
        }

        is GooglePayResult.Canceled -> {
            println("User canceled token collection")
        }

        is GooglePayResult.Error -> {
            println("Error: ${result.message}")
        }

        else -> {
            println("Unexpected result")
        }
    }
}
```

### Step 4: Backend Implementation (Your Server)

Your backend must implement two endpoints:

#### Endpoint 1: Request Token Collection

```
POST /your-backend/request-token-collection

Backend Processing:
1. Call POST /api/v4/google_pay/open_session
2. Call POST /api/v4/payments/oneoff with:
   - amount: 0
   - request_token: true
   - token_consent_agreed: true
   - token_agreement: "unscheduled"
   - label: "MIT token request"
3. Combine responses into GooglePayBackendData format
4. Return combined data to app

Response (GooglePayBackendData):
{
  "merchantId": "...",
  "merchantName": "...",
  "gatewayId": "everypay",
  "gatewayMerchantId": "...",
  "currency": "EUR",
  "countryCode": "EE",
  "paymentReference": "abc123...",      // from oneoff response
  "mobileAccessToken": "token_xyz...",  // from oneoff response
  "amount": 0.0,
  "label": "Card verification"
}
```

#### Endpoint 2: Process Google Pay Token

```
POST /your-backend/process-google-pay-token

Request Body:
{
  "paymentReference": "abc123...",
  "signature": "...",
  "intermediateSigningKey": {
    "signedKey": "...",
    "signatures": ["..."]
  },
  "protocolVersion": "ECv2",
  "signedMessage": "...",
  "tokenConsentAgreed": true
}

Backend Processing:
1. Call POST /api/v4/google_pay/payment_data
   - Use mobile_access_token as Bearer token
   - Pass all Google Pay token fields
   - Include token_consent_agreed: true

2. Call GET /api/v4/payments/{payment_reference}
   - Use Basic Auth (api_username:api_secret)
   - Extract cc_details.token (MIT token)

3. Store MIT token in your database

4. Return success confirmation to app

Response:
{
  "success": true,
  "message": "Token successfully saved"
}

5. Use the stored MIT token to make the payments without user interaction:
 - POST /payments/mit
 - POST /payments/charge
```
Detailed Everypay API documentation: https://support.every-pay.com/api-documentation/

---

## SDK Mode

### Flow Diagram

```
Android App                 EveryPay API               Google Pay
     │                           │                          │
     │  1. Initialize            │                          │
     │──────────────────────────►│                          │
     │  (open_session)           │                          │
     │                           │                          │
     │  2. Session data          │                          │
     │◄──────────────────────────│                          │
     │                           │                          │
     │  3. Request token         │                          │
     │  (POST /oneoff amount=0)  │                          │
     │──────────────────────────►│                          │
     │                           │                          │
     │                           │  4. Show Google Pay      │
     │                           │  (amount: $0, ESTIMATED) │
     │───────────────────────────┼─────────────────────────►│
     │                           │                          │
     │                           │  5. User authorizes      │
     │◄──────────────────────────┼──────────────────────────│
     │                           │                          │
     │  6. Process token (DPAN)  │                          │
     │  (POST /payment_data)     │                          │
     │──────────────────────────►│                          │
     │                           │                          │
     │  7. Get MIT token         │                          │
     │  (GET /payments)          │                          │
     │──────────────────────────►│                          │
     │                           │                          │
     │  8. MIT token returned    │                          │
     │◄──────────────────────────│                          │
     │                           │                          │
     │  9. Store MIT token       │                          │
     │  for recurring payments   │                          │
```

### Step 1: Configure for SDK Mode

```kotlin
val config = EverypayConfig(
    apiUsername = "your_api_username",
    apiSecret = "your_api_secret",
    apiUrl = "https://api.sandbox.everypay.com",
    environment = "TEST",
    accountName = "EUR3D1",
    countryCode = "EE",
    customerUrl = "https://your-domain.com/callback",
    currencyCode = "EUR"
)

val helper = EverypayGooglePayHelper(activity, config)
```

### Step 2: Initialize

```kotlin
helper.initialize { result ->
    when (result) {
        is GooglePayReadinessResult.Success -> {
            println("Google Pay ready for token request")
        }
        is GooglePayReadinessResult.Error -> {
            println("Error: ${result.message}")
        }
    }
}
```

### Step 3: Request Token

```kotlin
helper.requestToken("Card verification") { result ->
    when (result) {
        is GooglePayResult.TokenReceived -> {
            // Token received!
            val tokenData = result.tokenData
            val paymentResponse = result.paymentResponse  // EveryPay payment_data response
            val paymentDetails = result.paymentDetails    // Payment details with MIT token

            // Extract the MIT token (24-character alphanumeric string)
            val mitToken = paymentDetails?.ccDetails?.token

            if (mitToken != null) {
                // Send MIT token to your backend for storage
                yourBackend.saveRecurringToken(
                    mitToken = mitToken,
                    paymentReference = paymentDetails.paymentReference
                ) { tokenReference ->
                    println("Token saved with reference: $tokenReference")
                }
            } else {
                println("MIT token not available")
            }
        }

        is GooglePayResult.Canceled -> {
            println("User canceled token collection")
        }

        is GooglePayResult.Error -> {
            println("Error: ${result.message}")
        }

        else -> {
            println("Unexpected result")
        }
    }
}
```

---

## Token Data Structure

When you receive a `GooglePayResult.TokenReceived`, the `tokenData` contains:

```kotlin
data class GooglePayTokenData(
    val paymentReference: String,        // Empty for SDK mode token requests
    val mobileAccessToken: String,       // Empty for SDK mode token requests
    val signature: String,               // Google Pay signature
    val intermediateSigningKey: IntermediateSigningKey,
    val protocolVersion: String,         // Usually "ECv2"
    val signedMessage: String,           // Encrypted payment data (DPAN)
    val tokenConsentAgreed: Boolean      // true for token requests
)

data class IntermediateSigningKey(
    val signedKey: String,
    val signatures: List<String>
)
```

---

## Security Considerations

1. **Backend processing only** - Never expose API credentials in the Android app
2. **Use HTTPS only** - All communication must be encrypted
3. **Validate token consent** - Always check `tokenConsentAgreed` flag
4. **Secure MIT token storage** - Store MIT tokens securely in your database
5. **Authorization** - Use Bearer tokens (mobile_access_token) for payment_data calls
6. **Authentication** - Use Basic Auth for GET /payments and other EveryPay API calls

---

## Troubleshooting

### Common Issues

**Issue:** Google Pay shows error "Developer Error"
- **Solution:** Ensure `com.google.android.gms.wallet.api.enabled` is set in AndroidManifest.xml

**Issue:** Token has `tokenConsentAgreed: false`
- **Solution:** Use `requestToken()` or `requestTokenWithBackendData()`, not regular payment methods. Ensure request_token=true and token_consent_agreed=true in `POST /oneoff` call.

**Issue:** MIT token not returned in GET /payments response
- **Solution:** Ensure you called POST /google_pay/payment_data first. The MIT token is only available after processing the Google Pay token.

**Issue:** Recurring payment fails with stored MIT token
- **Solution:** Check that the card hasn't expired. Use POST /payments/mit or /payments/charge endpoints with the stored MIT token.

---

## FAQ

**Q: Can I use the MIT token immediately for a payment?**
A: Yes, once you receive the MIT token from GET /payments, you can use it for recurring payments with POST /payments/mit or /payments/charge.

**Q: How long is the MIT token valid?**
A: MIT tokens are valid until the underlying card expires. Monitor the expiry_month and expiry_year from cc_details.

**Q: Do I need to show $0 to the user?**
A: Yes, Google Pay will display your label (e.g., "Card verification") with zero amount and ESTIMATED status.

**Q: Can I collect the first payment and token together?**
A: Not directly with this flow. Make a regular payment first, then request the token separately for future recurring payments.

**Q: What if the user has multiple cards?**
A: Google Pay allows the user to select which card to tokenize. You receive the MIT token for their selected card.

**Q: What's the difference between SDK mode and Backend mode?**
A: SDK mode makes EveryPay API calls directly from the app (requires API credentials in app). Backend mode keeps credentials secure on your server and the app only handles the Google Pay UI.

---

## Additional Resources

- [Google Pay API Documentation](https://developers.google.com/pay/api)
- [EveryPay API Documentation](https://support.every-pay.com/api-documentation/)
- [EveryPay MIT Payments](https://support.every-pay.com/merchant-support/integrate-automatically-collected-payments-mit/)

---
