# EveryPay Google Pay SDK for Android

A native Android SDK for integrating Google Pay into your Android applications.

## Features

- Simple and clean API for Google Pay integration
- Support for both Activity-based and custom integrations
- Type-safe result handling with sealed classes
- Comprehensive error handling
- Backend mode for secure API credential management
- SDK mode for direct EveryPay API integration
- Jetpack Compose support with GooglePayButton composable

## Installation

### 1. Add the library to your project

```gradle
dependencies {
    implementation project(':libraries:gpay')
    // or when published:
    // implementation 'com.everypay:gpay:VERSION'
}
```

### 2. Configure AndroidManifest.xml

Add the following required meta-data to your `AndroidManifest.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <!-- Required for SDK mode only: Internet permission for EveryPay API calls -->
    <uses-permission android:name="android.permission.INTERNET" />

    <application
        ...>

        <!-- Required: Enable Google Pay API -->
        <meta-data
            android:name="com.google.android.gms.wallet.api.enabled"
            android:value="true" />

        <activity ...>
            ...
        </activity>
    </application>
</manifest>
```

**Important:**
- The Google Pay meta-data is **required for both modes**. Without it, Google Pay will fail with a `DEVELOPER_ERROR` (error code 10).
- The INTERNET permission is **only required for SDK mode** (when the SDK makes EveryPay API calls directly). Backend mode doesn't need it since your backend handles all API communication.

## Integration Modes

This SDK supports two integration modes:

1. **Backend Mode (RECOMMENDED)** - Backend makes EveryPay API calls, SDK handles Google Pay UI
   - ✅ More secure: API credentials stay on backend
   - ✅ Better control: Payment logic centralized
   - ✅ Easier compliance: PCI requirements reduced
   - See [Backend Integration](#backend-integration-recommended) below

2. **SDK Mode** - SDK makes all EveryPay API calls automatically
   - ⚠️ Less secure: Credentials stored on device
   - ✅ Simpler: No backend needed
   - See [SDK Mode Integration](#sdk-mode-integration-legacy) below

---

## Backend Integration (RECOMMENDED)

### Overview

In backend mode, your server makes the EveryPay API calls and the Android SDK only handles the Google Pay UI and token extraction. This is the recommended option because API credentials never leave your backend.

### Flow Diagram

```
Backend (Your Server)              Android App                 Google Pay
       │                                │                          │
       │  1. Create payment request     │                          │
       │◄───────────────────────────────│                          │
       │                                │                          │
       │  2. Call EveryPay APIs         │                          │
       │    - open_session              │                          │
       │    - create_payment            │                          │
       │                                │                          │
       │  3. Return session + payment   │                          │
       │    data                        │                          │
       │───────────────────────────────►│                          │
       │                                │                          │
       │                                │  4. Show Google Pay      │
       │                                │─────────────────────────►│
       │                                │                          │
       │                                │  5. User completes       │
       │                                │◄─────────────────────────│
       │                                │                          │
       │  6. Send token to backend      │                          │
       │◄───────────────────────────────│                          │
       │                                │                          │
       │  7. Process token via          │                          │
       │     EveryPay API               │                          │
       │                                │                          │
       │  8. Return result              │                          │
       │───────────────────────────────►│                          │
```

### Step 1: Backend API Endpoints

Your backend needs to implement 3 EveryPay API calls. Here are the curl examples:

#### 1.1. Open Session

```bash
curl -X POST https://{API_URL}/api/v4/google_pay/open_session \
  -u "your_api_username:your_api_secret" \
  -H "Content-Type: application/json" \
  -d '{
    "api_username": "your_api_username",
    "account_name": "EUR3D1"
  }'
```

**Response:**
```json
{
  "googlepay_merchant_identifier": "gateway:...",
  "googlepay_ep_merchant_id": "123456...",
  "googlepay_gateway_merchant_id": "merchant_123",
  "merchant_name": "Your Store",
  "google_pay_gateway_id": "gatewayId",
  "acq_branding_domain_igw": "every-pay.com"
}
```

#### 1.2. Create Payment

```bash
curl -X POST https://{API_URL}/api/v4/payments/oneoff \
  -u "your_api_username:your_api_secret" \
  -H "Content-Type: application/json" \
  -d '{
    "api_username": "your_api_username",
    "account_name": "EUR3D1",
    "amount": 10.00,
    "label": "Product Purchase",
    "currency_code": "EUR",
    "country_code": "EE",
    "order_reference": "ORDER-123",
    "nonce": "550e8400-e29b-41d4-a716-446655440000",
    "mobile_payment": true,
    "customer_url": "https://yourstore.com/payment/callback",
    "customer_ip": "192.168.1.1",
    "customer_email": "customer@example.com",
    "timestamp": "2024-01-15T10:30:00Z"
  }'
```

**Response:**
```json
{
  "payment_reference": "abc123def456...",
  "mobile_access_token": "123xyz789...",
  "currency": "EUR",
  "descriptor_country": "EE",
  "googlepay_merchant_identifier": "gateway:...",
  "account_name": "EUR3D1",
  "order_reference": "ORDER-123",
  "initial_amount": 10.00,
  "standing_amount": 10.00,
  "payment_state": "initial"
}
```

#### 1.3. Process Google Pay Token

This endpoint is called after the Android SDK returns the payment token. Your backend receives the token from the Android app and processes it with EveryPay.

**Important:** Use the `mobile_access_token` from the create_payment response (step 1.2) as the Bearer token in the Authorization header.

```bash
curl -X POST https://{API_URL}/api/v4/google_pay/payment_data \
  -H "Authorization: Bearer {mobile_access_token}" \
  -H "Content-Type: application/json" \
  -d '{
    "payment_reference": "abc123def456...",
    "token_consent_agreed": false,
    "signature": "MEQCIF...",
    "intermediateSigningKey": {
      "signedKey": "{\"keyValue\":\"...\"}",
      "signatures": ["MEUCIQ..."]
    },
    "protocolVersion": "ECv2",
    "signedMessage": "{\"encryptedMessage\":\"...\"}"
  }'
```

**Note:** The Android SDK's `GooglePayTokenData` includes the `mobile_access_token` field for your convenience. Your backend should extract this token and use it in the Authorization header when calling this endpoint.

**Response:**
```json
{
  "state": "settled",
  "paymentReference": "abc123def456..."
}
```

### Step 2: Backend Endpoint for Android App

Create an endpoint that combines the two responses:

```
POST /api/google-pay/create-payment
```

**Request Body:**
If you calculate these values on the back-end side then this input is not needed
```json
{
  "amount": "10.00",
  "order_reference": "ORDER-123",
  "customer_email": "customer@example.com"
}
```

**Response Body (GooglePayBackendData):**
Map the EveryPay API `open_session` and `oneoff` response values to this structure:
```json
{
  "merchantId": "<googlepay_merchant_identifier from open_session>",
  "merchantName": "<merchant_name from open_session>",
  "gatewayId": "<google_pay_gateway_id from open_session>",
  "gatewayMerchantId": "<googlepay_gateway_merchant_id from open_session>",
  "currency": "<currency from create_payment>",
  "countryCode": "<descriptor_country from create_payment>",
  "paymentReference": "<payment_reference from create_payment>",
  "mobileAccessToken": "<mobile_access_token from create_payment>",
  "amount": "<standing_amount from create_payment>",
  "label": "<label from create_payment request>"
}
```

#### 2.2. Process Token Endpoint

Create an endpoint that processes the Google Pay token after the Android SDK returns it:

```
POST /api/google-pay/process-token
```

**Request Body (GooglePayTokenData from Android SDK):**
```json
{
  "payment_reference": "abc123def456...",
  "mobile_access_token": "123xyz789...",
  "token_consent_agreed": false,
  "signature": "MEQCIF...",
  "intermediateSigningKey": {
    "signedKey": "{\"keyValue\":\"...\"}",
    "signatures": ["MEUCIQ..."]
  },
  "protocolVersion": "ECv2",
  "signedMessage": "{\"encryptedMessage\":\"...\"}"
}
```

**Backend Implementation:**
Your backend should:
1. Extract the `mobile_access_token` from the request
2. Use it as the Bearer token to call EveryPay's `/api/v4/google_pay/payment_data` endpoint (see Step 1.3 above)
3. Return the payment result to the Android app

**Response Body:**
```json
{
  "state": "settled",
  "payment_reference": "abc123def456...",
  "order_reference": "ORDER-123"
}
```

### Step 3: Android Implementation

```kotlin
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.lifecycle.lifecycleScope
import com.everypay.gpay.*
import com.everypay.gpay.compose.GooglePayButton
import com.everypay.gpay.compose.GooglePayButtonTheme
import com.everypay.gpay.compose.GooglePayButtonType
import com.everypay.gpay.models.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private lateinit var everyPayHelper: EverypayGooglePayHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configure for backend mode (no API credentials)
        val config = EverypayConfig(
            environment = "TEST", // or "PRODUCTION"
            countryCode = "EE",
            currencyCode = "EUR"
            // Note: apiUsername, apiSecret, apiUrl, accountName are null (backend mode)
        )

        // Initialize helper
        everyPayHelper = EverypayGooglePayHelper(this, config)

        // Set up UI with Google Pay button
        setContent {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                GooglePayButton(
                    onClick = { makePayment() },
                    buttonType = GooglePayButtonType.BUY,
                    buttonTheme = GooglePayButtonTheme.DARK,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                )
            }
        }
    }

    private fun makePayment() {
        lifecycleScope.launch {
            // 1. Call backend to initialize payment (backend calls EveryPay open_session + create_payment)
            val paymentData = createPaymentOnBackend(
                amount = "10.00",
                orderReference = "ORDER-${System.currentTimeMillis()}",
                customerEmail = "customer@example.com"
            )

            // 2. Initialize the SDK session with backend data
            everyPayHelper.initializeWithBackendData(paymentData) { initResult ->
                when (initResult) {
                    is GooglePayReadinessResult.Success -> {
                        if (initResult.isReady) {
                            // 3. Show Google Pay sheet (amount and label come from backendData)
                            everyPayHelper.makePaymentWithBackendData(
                                backendData = paymentData
                            ) { result ->
                                when (result) {
                                    is GooglePayResult.TokenReceived -> {
                                        // 4. Send token to backend for processing
                                        lifecycleScope.launch {
                                            processTokenOnBackend(result.tokenData)
                                        }
                                    }
                                    is GooglePayResult.Canceled -> {
                                        Toast.makeText(this@MainActivity, "Payment canceled", Toast.LENGTH_SHORT).show()
                                    }
                                    is GooglePayResult.Error -> {
                                        Toast.makeText(this@MainActivity, "Error: ${result.message}", Toast.LENGTH_LONG).show()
                                    }
                                    else -> {}
                                }
                            }
                        } else {
                            Toast.makeText(this@MainActivity, "Google Pay not available", Toast.LENGTH_SHORT).show()
                        }
                    }
                    is GooglePayReadinessResult.Error -> {
                        Toast.makeText(this@MainActivity, "Initialization error: ${initResult.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    // Create payment on your backend
    // Backend makes BOTH EveryPay API calls (open_session + create_payment) and returns combined data
    private suspend fun createPaymentOnBackend(
        amount: String,
        orderReference: String,
        customerEmail: String
    ): GooglePayBackendData = withContext(Dispatchers.IO) {
        // Call your backend endpoint that internally calls both:
        // 1. EveryPay open_session API
        // 2. EveryPay create_payment API
        val response = yourApiClient.post("/api/google-pay/create-payment") {
            body = mapOf(
                "amount" to amount,
                "order_reference" to orderReference,
                "customer_email" to customerEmail
            )
        }
        // Backend returns combined session + payment data
        GooglePayBackendData(
            merchantId = response.merchantId,
            merchantName = response.merchantName,
            gatewayId = response.gatewayId,
            gatewayMerchantId = response.gatewayMerchantId,
            currency = response.currency,
            countryCode = response.countryCode,
            paymentReference = response.paymentReference,
            mobileAccessToken = response.mobileAccessToken,
            amount = response.amount.toDouble(),
            label = response.label
        )
    }

    // Process token on your backend
    private suspend fun processTokenOnBackend(tokenData: GooglePayTokenData) = withContext(Dispatchers.IO) {
        try {
            // Send token to backend for processing via EveryPay API
            val response = yourApiClient.post("/api/google-pay/process-token") {
                body = tokenData.toJson().toString()
            }

            withContext(Dispatchers.Main) {
                if (response.state == "settled" || response.state == "authorized") {
                    Toast.makeText(this@MainActivity, "Payment successful!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@MainActivity, "Payment failed: ${response.state}", Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        everyPayHelper.handleActivityResult(requestCode, resultCode, data)
    }
}
```

### Security Benefits

- ✅ **API credentials never on device** - No risk of credential extraction
- ✅ **Centralized payment logic** - All business logic on backend
- ✅ **Better audit trail** - All API calls logged on backend
- ✅ **Easier credential rotation** - Update backend only
- ✅ **Reduced PCI scope** - Payment data flows through backend

---

## SDK Mode Integration

⚠️ **Backend Mode is recommended over SDK Mode for security reasons.**

SDK Mode allows the SDK to make all EveryPay API calls directly from the Android app. While simpler to implement (no backend needed), this approach stores API credentials on the device, which is less secure than Backend Mode.

### Complete Integration Example

```kotlin
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.everypay.gpay.*
import com.everypay.gpay.compose.GooglePayButton
import com.everypay.gpay.compose.GooglePayButtonType
import com.everypay.gpay.compose.GooglePayButtonTheme
import com.everypay.gpay.models.EverypayConfig

class MainActivity : ComponentActivity() {
    private lateinit var everyPayHelper: EverypayGooglePayHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configure EveryPay for SDK mode (all credentials required)
        val config = EverypayConfig(
            apiUsername = "<API_USERNAME>",
            apiSecret = "<API_SECRET>",
            apiUrl = "<API_URL>", // e.g., "https://payment.sandbox.lhv.ee" for test or production URL
            environment = "TEST", // or "PRODUCTION"
            accountName = "<ACCOUNT_NAME>", // e.g., "EUR3D1"
            countryCode = "EE",
            customerUrl = "<CUSTOMER_URL>", // e.g., "https://yourstore.com/payment/callback"
            currencyCode = "EUR"
        )

        // Initialize helper
        everyPayHelper = EverypayGooglePayHelper(this, config)

        setContent {
            MaterialTheme {
                PaymentScreen(
                    onInitialize = { initializeGooglePay() },
                    onPayment = { makePayment() }
                )
            }
        }
    }

    private fun initializeGooglePay() {
        everyPayHelper.initialize { result ->
            when (result) {
                is GooglePayReadinessResult.Success -> {
                    if (result.isReady) {
                        Toast.makeText(this, "Google Pay ready", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Google Pay not available", Toast.LENGTH_SHORT).show()
                    }
                }
                is GooglePayReadinessResult.Error -> {
                    Toast.makeText(this, "Error: ${result.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun makePayment() {
        everyPayHelper.makePayment(
            amount = "10.00",
            label = "Product Purchase",
            orderReference = "ORDER-${System.currentTimeMillis()}",
            customerEmail = "customer@example.com",
            customerIp = "192.168.1.1" // optional
        ) { result ->
            when (result) {
                is GooglePayResult.Success -> {
                    Toast.makeText(this, "Payment successful!", Toast.LENGTH_SHORT).show()
                    // Payment completed
                }
                is GooglePayResult.Canceled -> {
                    Toast.makeText(this, "Payment canceled", Toast.LENGTH_SHORT).show()
                }
                is GooglePayResult.Error -> {
                    Toast.makeText(this, "Payment failed: ${result.message}", Toast.LENGTH_LONG).show()
                }
                is GooglePayResult.TokenReceived -> {
                    // This should not happen in SDK mode, but handle it for completeness
                    Log.e("Payment", "Unexpected TokenReceived in SDK mode")
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        everyPayHelper.handleActivityResult(requestCode, resultCode, data)
    }
}

@Composable
fun PaymentScreen(
    onInitialize: () -> Unit,
    onPayment: () -> Unit
) {
    val isInitialized = remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        onInitialize()
        isInitialized.value = true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Pay with Google Pay",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        GooglePayButton(
            onClick = onPayment,
            buttonType = GooglePayButtonType.BUY,
            buttonTheme = GooglePayButtonTheme.DARK,
            enabled = isInitialized.value,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        )
    }
}
```

### EverypayConfig Parameters

```kotlin
data class EverypayConfig(
    // SDK Mode - Required fields
    val apiUsername: String? = null,        // Your EveryPay API username (required for SDK mode, null for backend mode)
    val apiSecret: String? = null,           // Your EveryPay API secret (required for SDK mode, null for backend mode)
    val apiUrl: String? = null,              // Everypay API URL (required for SDK mode, null for backend mode)
    val accountName: String? = null,         // Your EveryPay account name, e.g., "EUR3D1" (required for SDK mode, null for backend mode)
    val customerUrl: String? = null,         // Customer redirect URL (required for SDK mode, null for backend mode)

    // Both Modes - Required fields
    val environment: String,                 // "TEST" or "PRODUCTION" (always required)
    val countryCode: String,                 // ISO country code, e.g., "EE" (always required)

    // Both Modes - Optional fields
    val currencyCode: String = "EUR",                                    // ISO currency code (default: "EUR")
    val allowedCardNetworks: List<String> = listOf("MASTERCARD", "VISA"), // Allowed card networks
    val allowedCardAuthMethods: List<String> = listOf("PAN_ONLY", "CRYPTOGRAM_3DS") // Allowed auth methods
)
```

**Mode Detection:**
- **Backend Mode**: Leave `apiUsername`, `apiSecret`, `apiUrl`, `accountName`, `customerUrl` as `null`
- **SDK Mode**: Provide all API credentials

### What EverypayGooglePayHelper Does Automatically

1. **Opens EveryPay session** - Fetches gateway configuration from EveryPay API
2. **Initializes Google Pay** - Sets up Google Pay with correct parameters
3. **Creates payment** - Creates payment in EveryPay before showing Google Pay
4. **Processes token** - Submits Google Pay token to EveryPay backend
5. **Returns final result** - Provides payment state (settled, authorized, failed)

### Benefits of EverypayGooglePayHelper

- **One initialization** - Simple config, no manual API calls
- **Automatic backend integration** - Handles all EveryPay API requests
- **Type-safe** - Kotlin data classes for all models
- **Error handling** - Comprehensive error reporting
- **Same config as React Native library** - Easy to maintain consistency

---

## Error Codes

Available error codes in `Constants`:

- `E_INIT_ERROR` - Initialization error
- `E_PAYMENT_CANCELED` - Payment canceled by user
- `E_PAYMENT_ERROR` - General payment error
- `E_UNABLE_TO_DETERMINE_GOOGLE_PAY_READINESS` - Cannot check Google Pay availability
- `E_GOOGLE_PAY_API_ERROR` - Google Pay API error

## Testing

Use `WalletConstants.ENVIRONMENT_TEST` for testing and `WalletConstants.ENVIRONMENT_PRODUCTION` for production.

## Requirements

- Android API 24+
- Kotlin 1.8+

### Dependencies

The SDK requires the following dependencies (automatically included when you add the SDK):

- **Google Play Services Wallet** 19.4.0+ - For Google Pay functionality
- **OkHttp** 4.12.0+ - For HTTP communication with EveryPay API (SDK mode only)
- **Kotlinx Coroutines** 1.7.3+ - For async operations
- **Jetpack Compose** 1.7.6+ (optional) - Only needed if using `GooglePayButton` composable
- **AndroidX Core KTX** 1.17.0+
- **AndroidX AppCompat** 1.7.1+

## License

Copyright 2025 EveryPay
