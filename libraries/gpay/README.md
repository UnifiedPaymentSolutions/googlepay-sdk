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

### 1. Adding the Google Pay Button

The SDK provides two ways to add the Google Pay button to your app:

#### Option A: Jetpack Compose (Recommended for modern apps)

Use the `GooglePayButton` composable:

```kotlin
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.everypay.gpay.compose.GooglePayButton
import com.everypay.gpay.compose.GooglePayButtonType
import com.everypay.gpay.compose.GooglePayButtonTheme

@Composable
fun PaymentScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        GooglePayButton(
            onClick = {
                // Handle button click - start payment flow
                startGooglePayPayment()
            },
            buttonType = GooglePayButtonType.BUY,
            buttonTheme = GooglePayButtonTheme.DARK,
            cornerRadius = 8,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        )
    }
}
```

**Button Types:**
- `GooglePayButtonType.BUY` - "Buy with Google Pay"
- `GooglePayButtonType.PAY` - "Google Pay"
- `GooglePayButtonType.CHECKOUT` - "Checkout with Google Pay"
- `GooglePayButtonType.ORDER` - "Order with Google Pay"
- `GooglePayButtonType.BOOK` - "Book with Google Pay"
- `GooglePayButtonType.DONATE` - "Donate with Google Pay"
- `GooglePayButtonType.SUBSCRIBE` - "Subscribe with Google Pay"

**Button Themes:**
- `GooglePayButtonTheme.DARK` - White text on black background
- `GooglePayButtonTheme.LIGHT` - Colored text on white background

#### Option B: XML/View-based (Traditional Android)

Use Google's native `PayButton` directly:

```xml
<!-- In your layout XML -->
<com.google.android.gms.wallet.button.PayButton
    android:id="@+id/googlePayButton"
    android:layout_width="match_parent"
    android:layout_height="48dp"
    android:layout_margin="16dp" />
```

```kotlin
// In your Activity
import com.google.android.gms.wallet.button.ButtonConstants
import com.google.android.gms.wallet.button.ButtonOptions
import com.google.android.gms.wallet.button.PayButton

val googlePayButton = findViewById<PayButton>(R.id.googlePayButton)

// Configure the button
val buttonOptions = ButtonOptions.newBuilder()
    .setButtonType(ButtonConstants.ButtonType.BUY)
    .setButtonTheme(ButtonConstants.ButtonTheme.DARK)
    .setCornerRadius(8)
    .setAllowedPaymentMethods("""
        [{
            "type": "CARD",
            "parameters": {
                "allowedAuthMethods": ["PAN_ONLY", "CRYPTOGRAM_3DS"],
                "allowedCardNetworks": ["MASTERCARD", "VISA"]
            }
        }]
    """.trimIndent())
    .build()

googlePayButton.initialize(buttonOptions)
googlePayButton.setOnClickListener {
    // Handle button click - start payment flow
    startGooglePayPayment()
}
```

### 2. Complete Jetpack Compose Integration Example

Here's a complete example using Jetpack Compose:

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
import com.google.android.gms.wallet.WalletConstants

class MainActivity : ComponentActivity() {
    private lateinit var googlePayManager: GooglePayManager
    private lateinit var googlePayHelper: GooglePayActivityHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Google Pay Manager
        googlePayManager = GooglePayManager(
            context = this,
            environment = WalletConstants.ENVIRONMENT_TEST,
            allowedCardNetworks = listOf("MASTERCARD", "VISA"),
            allowedCardAuthMethods = listOf("PAN_ONLY", "CRYPTOGRAM_3DS")
        )

        // Initialize Activity Helper
        googlePayHelper = GooglePayActivityHelper(
            activity = this,
            googlePayManager = googlePayManager
        )

        setContent {
            MaterialTheme {
                PaymentScreen(
                    onCheckReadiness = { checkGooglePayAvailability() },
                    onPaymentClick = { startPayment() }
                )
            }
        }
    }

    private fun checkGooglePayAvailability() {
        googlePayHelper.isReadyToPay { result ->
            when (result) {
                is GooglePayReadinessResult.Success -> {
                    if (result.isReady) {
                        Toast.makeText(this, "Google Pay is available", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Google Pay is not available", Toast.LENGTH_SHORT).show()
                    }
                }
                is GooglePayReadinessResult.Error -> {
                    Toast.makeText(this, "Error: ${result.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun startPayment() {
        googlePayHelper.requestPayment(
            gateway = "everypay",
            gatewayMerchantId = "your_merchant_id",
            currencyCode = "EUR",
            amount = "10.00",
            merchantName = "Your Store",
            callback = { result ->
                handlePaymentResult(result)
            }
        )
    }

    private fun handlePaymentResult(result: GooglePayResult) {
        when (result) {
            is GooglePayResult.Success -> {
                val paymentData = result.paymentData
                val json = paymentData.toJson()
                // Process payment with your backend
                Toast.makeText(this, "Payment successful!", Toast.LENGTH_SHORT).show()
                // TODO: Send payment token to your server
            }
            is GooglePayResult.Canceled -> {
                Toast.makeText(this, "Payment canceled", Toast.LENGTH_SHORT).show()
            }
            is GooglePayResult.Error -> {
                Toast.makeText(this, "Payment error: ${result.message}", Toast.LENGTH_LONG).show()
            }
            is GooglePayResult.TokenReceived -> {
                // Not applicable for manual integration
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        googlePayHelper.handleActivityResult(requestCode, resultCode, data)
    }
}

@Composable
fun PaymentScreen(
    onCheckReadiness: () -> Unit,
    onPaymentClick: () -> Unit
) {
    var isGooglePayReady by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        // Check readiness when screen loads
        onCheckReadiness()
        isGooglePayReady = true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Complete Your Purchase",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        // Product info
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Total Amount", style = MaterialTheme.typography.labelMedium)
                Text("€10.00", style = MaterialTheme.typography.headlineLarge)
            }
        }

        // Google Pay Button
        GooglePayButton(
            onClick = onPaymentClick,
            buttonType = GooglePayButtonType.BUY,
            buttonTheme = GooglePayButtonTheme.DARK,
            cornerRadius = 8,
            enabled = isGooglePayReady,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        )
    }
}
```

### 3. Basic Activity Integration (Traditional Approach)

```kotlin
import com.everypay.gpay.*
import com.google.android.gms.wallet.WalletConstants

class PaymentActivity : AppCompatActivity() {
    private lateinit var googlePayManager: GooglePayManager
    private lateinit var googlePayHelper: GooglePayActivityHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize the manager
        googlePayManager = GooglePayManager(
            context = this,
            environment = WalletConstants.ENVIRONMENT_TEST,
            allowedCardNetworks = listOf("MASTERCARD", "VISA"),
            allowedCardAuthMethods = listOf("PAN_ONLY", "CRYPTOGRAM_3DS")
        )

        // Create the activity helper
        googlePayHelper = GooglePayActivityHelper(
            activity = this,
            googlePayManager = googlePayManager
        )

        // Check if Google Pay is available
        googlePayHelper.isReadyToPay { result ->
            when (result) {
                is GooglePayReadinessResult.Success -> {
                    if (result.isReady) {
                        // Enable Google Pay button
                        showGooglePayButton()
                    }
                }
                is GooglePayReadinessResult.Error -> {
                    // Handle error
                    Log.e("GooglePay", "Error: ${result.message}")
                }
            }
        }
    }

    private fun startPayment() {
        googlePayHelper.requestPayment(
            gateway = "everypay",
            gatewayMerchantId = "your_merchant_id",
            currencyCode = "EUR",
            amount = "10.00",
            merchantName = "Your Store",
            callback = { result ->
                when (result) {
                    is GooglePayResult.Success -> {
                        val paymentData = result.paymentData
                        val json = paymentData.toJson()
                        // Process payment with your backend
                        processPayment(json)
                    }
                    is GooglePayResult.Canceled -> {
                        // User canceled the payment
                        Toast.makeText(this, "Payment canceled", Toast.LENGTH_SHORT).show()
                    }
                    is GooglePayResult.Error -> {
                        // Handle error
                        Log.e("GooglePay", "Payment error: ${result.message}")
                    }
                    is GooglePayResult.TokenReceived -> {
                        // Not applicable for manual integration
                    }
                }
            }
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Handle Google Pay result
        if (!googlePayHelper.handleActivityResult(requestCode, resultCode, data)) {
            // Not a Google Pay result, handle other activity results
        }
    }
}
```

## EveryPay SDK Mode (Deprecated - Use Backend Mode Instead)

⚠️ **This SDK mode is deprecated in favor of Backend Mode for security reasons.**

The SDK provides a high-level helper that integrates directly with EveryPay's API, handling all backend requests automatically from the Android app. While simpler to implement, this approach stores API credentials on the device, which is less secure than the recommended Backend Mode.

### Complete Example with EverypayGooglePayHelper

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

        // Configure EveryPay (SDK mode - all credentials required)
        val config = EverypayConfig(
            apiUsername = "your_api_username",
            apiSecret = "your_api_secret",
            apiUrl = "https://sandbox-api.everypay.com", // or production URL
            environment = "TEST", // or "PRODUCTION"
            accountName = "EUR3D1",
            countryCode = "EE",
            customerUrl = "https://yourstore.com/payment/callback",
            currencyCode = "EUR"
        )

        // Initialize helper
        everyPayHelper = EverypayGooglePayHelper(this, config)

        setContent {
            MaterialTheme {
                PaymentScreen(
                    everyPayHelper = everyPayHelper,
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
    everyPayHelper: EverypayGooglePayHelper,
    onInitialize: () -> Unit,
    onPayment: () -> Unit
) {
    val isInitialized = remember { mutableStateOf(false) }
    val isProcessing = remember { mutableStateOf(false) }

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
            onClick = {
                isProcessing.value = true
                onPayment()
            },
            buttonType = GooglePayButtonType.BUY,
            buttonTheme = GooglePayButtonTheme.DARK,
            enabled = isInitialized.value && !isProcessing.value,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        )
    }
}
```

**Note:** The button is automatically disabled while a payment is being processed through the `isProcessing` state variable. This prevents users from clicking the button multiple times. Additionally, the SDK's `makePayment()` method has built-in protection against concurrent payment attempts - if called while a payment is already in progress, it will immediately return an error without making duplicate API calls.

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

## Manual Integration (Advanced)

For advanced use cases where you need full control, you can use the low-level API:

### 2. Custom Request Integration

For advanced use cases, you can create custom payment requests:

```kotlin
import org.json.JSONObject

// Create custom payment request
val customRequest = JSONObject().apply {
    put("apiVersion", 2)
    put("apiVersionMinor", 0)
    // Add your custom configuration
}

googlePayHelper.requestPaymentWithCustomRequest(
    requestJson = customRequest,
    callback = { result ->
        // Handle result
    }
)
```

### 3. Using GooglePayManager Only (No Activity Helper)

For more control or non-Activity contexts:

```kotlin
val googlePayManager = GooglePayManager(
    context = applicationContext,
    environment = WalletConstants.ENVIRONMENT_TEST,
    allowedCardNetworks = listOf("MASTERCARD", "VISA"),
    allowedCardAuthMethods = listOf("PAN_ONLY", "CRYPTOGRAM_3DS")
)

// Check readiness
googlePayManager.isReadyToPay { result ->
    // Handle result
}

// Create payment request
val paymentRequest = googlePayManager.createPaymentDataRequest(
    gateway = "everypay",
    gatewayMerchantId = "your_merchant_id",
    currencyCode = "EUR",
    amount = "10.00",
    merchantName = "Your Store"
)

// Use paymentRequest with your own integration
```

## API Reference

### GooglePayManager

Core manager for Google Pay operations.

**Constructor:**
```kotlin
GooglePayManager(
    context: Context,
    environment: Int,
    allowedCardNetworks: List<String>,
    allowedCardAuthMethods: List<String>
)
```

**Methods:**
- `isReadyToPay(callback: GooglePayReadinessCallback)` - Check if Google Pay is available
- `createPaymentDataRequest(...)` - Create a payment request
- `createPaymentDataRequestFromJson(requestJson: JSONObject)` - Create request from custom JSON
- `getPaymentsClient()` - Get the PaymentsClient instance

### GooglePayActivityHelper

Helper for Activity-based integration.

**Constructor:**
```kotlin
GooglePayActivityHelper(
    activity: Activity,
    googlePayManager: GooglePayManager,
    requestCode: Int = 991
)
```

**Methods:**
- `requestPayment(...)` - Start payment flow
- `requestPaymentWithCustomRequest(requestJson, callback)` - Start payment with custom request
- `handleActivityResult(requestCode, resultCode, data)` - Handle activity result
- `isReadyToPay(callback)` - Check if Google Pay is available

### Result Types

**GooglePayResult:**
- `Success(paymentData: PaymentData)` - Payment successful
- `Canceled` - User canceled payment
- `Error(code: String, message: String, exception: Throwable?)` - Error occurred

**GooglePayReadinessResult:**
- `Success(isReady: Boolean)` - Readiness check result
- `Error(code: String, message: String, exception: Throwable?)` - Error occurred

## Constants

Predefined constants are available in `Constants` object:

```kotlin
Constants.SUPPORTED_NETWORKS // Default: ["MASTERCARD", "VISA"]
Constants.SUPPORTED_METHODS // Default: ["PAN_ONLY", "CRYPTOGRAM_3DS"]
Constants.COUNTRY_CODE // Default: "ET"
Constants.CURRENCY_CODE // Default: "EUR"
```

## Utility Methods

### PaymentsUtil

Helper methods for creating Google Pay JSON objects:

```kotlin
PaymentsUtil.getEnvironment(environment: String): Int
PaymentsUtil.getBaseCardPaymentMethod(...)
PaymentsUtil.getCardPaymentMethod(...)
PaymentsUtil.getTransactionInfo(currencyCode, amount)
PaymentsUtil.getMerchantInfo(merchantName)
PaymentsUtil.getIsReadyToPayRequest(...)
PaymentsUtil.getPaymentDataRequest(...)
```

### ConvertUtil

JSON conversion utilities:

```kotlin
ConvertUtil.jsonStringToObject(json: String): JSONObject
ConvertUtil.jsonStringToArray(json: String): JSONArray
ConvertUtil.mergeJsonObjects(base: JSONObject, override: JSONObject): JSONObject
```

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
