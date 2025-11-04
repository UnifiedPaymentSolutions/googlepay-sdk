# EveryPay Google Pay SDK for Android

A native Android SDK for integrating Google Pay into your Android applications.

## Features

- Simple and clean API for Google Pay integration
- Support for both Activity-based and custom integrations
- Type-safe result handling with sealed classes
- Comprehensive error handling
- No external dependencies except Google Play Services Wallet

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

Add the following required permissions and meta-data to your `AndroidManifest.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <!-- Required: Internet permission for EveryPay API calls -->
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

**Important:** Without these configurations, Google Pay will fail with a `DEVELOPER_ERROR` (error code 10).

## Quick Start

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

## EveryPay Integration (Simplified)

The SDK provides a high-level helper that integrates directly with EveryPay's API, handling all backend requests automatically. This is the **recommended approach** for EveryPay merchants.

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

        // Configure EveryPay
        val config = EverypayConfig(
            apiUsername = "your_api_username",
            apiSecret = "your_api_secret",
            apiUrl = "https://sandbox-api.everypay.com", // or production URL
            environment = "TEST", // or "PRODUCTION"
            accountName = "EUR3D1",
            countryCode = "EE",
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
    val apiUsername: String,        // Your EveryPay API username
    val apiSecret: String,           // Your EveryPay API secret
    val apiUrl: String,              // "https://api.everypay.com" or "https://sandbox-api.everypay.com"
    val environment: String,         // "TEST" or "PRODUCTION"
    val accountName: String,         // Your EveryPay account name (e.g., "EUR3D1")
    val countryCode: String,         // ISO country code (e.g., "EE")
    val currencyCode: String = "EUR",                                    // ISO currency code (optional)
    val allowedCardNetworks: List<String> = listOf("MASTERCARD", "VISA"), // Optional
    val allowedCardAuthMethods: List<String> = listOf("PAN_ONLY", "CRYPTOGRAM_3DS") // Optional
)
```

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
- Google Play Services Wallet 19.4.0+
- Kotlin 1.8+

## License

Copyright 2024 EveryPay
