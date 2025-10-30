package com.everypay.gpay.compose

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.wallet.button.ButtonConstants
import com.google.android.gms.wallet.button.ButtonOptions
import com.google.android.gms.wallet.button.PayButton

/**
 * Google Pay button types
 */
enum class GooglePayButtonType(val value: Int) {
    /** Standard "Buy with Google Pay" button */
    BUY(ButtonConstants.ButtonType.BUY),
    /** "Book with Google Pay" button */
    BOOK(ButtonConstants.ButtonType.BOOK),
    /** "Checkout with Google Pay" button */
    CHECKOUT(ButtonConstants.ButtonType.CHECKOUT),
    /** "Donate with Google Pay" button */
    DONATE(ButtonConstants.ButtonType.DONATE),
    /** "Order with Google Pay" button */
    ORDER(ButtonConstants.ButtonType.ORDER),
    /** Plain "Google Pay" button */
    PAY(ButtonConstants.ButtonType.PAY),
    /** "Subscribe with Google Pay" button */
    SUBSCRIBE(ButtonConstants.ButtonType.SUBSCRIBE)
}

/**
 * Google Pay button themes
 */
enum class GooglePayButtonTheme(val value: Int) {
    /** Dark theme button (white text on black background) */
    DARK(ButtonConstants.ButtonTheme.DARK),
    /** Light theme button (colored text on white background) */
    LIGHT(ButtonConstants.ButtonTheme.LIGHT)
}

/**
 * A Composable that displays the native Google Pay button.
 *
 * This composable wraps Google's native PayButton in an AndroidView for use in Jetpack Compose.
 * The button displays the official Google Pay branding and styling.
 *
 * @param onClick Callback invoked when the button is clicked
 * @param modifier Modifier to be applied to the button
 * @param allowedPaymentMethods JSON string describing allowed payment methods. If null, uses default configuration.
 * @param buttonType The type of button to display (BUY, PAY, etc.)
 * @param buttonTheme The theme of the button (DARK or LIGHT)
 * @param cornerRadius Corner radius in DP for the button (default: 8)
 * @param enabled Whether the button should be enabled or disabled (default: true)
 *
 * @see GooglePayButtonType
 * @see GooglePayButtonTheme
 *
 * Example usage:
 * ```
 * GooglePayButton(
 *     onClick = { /* Handle button click */ },
 *     buttonType = GooglePayButtonType.BUY,
 *     buttonTheme = GooglePayButtonTheme.DARK,
 *     modifier = Modifier.fillMaxWidth()
 * )
 * ```
 */
@Composable
fun GooglePayButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    allowedPaymentMethods: String? = null,
    buttonType: GooglePayButtonType = GooglePayButtonType.BUY,
    buttonTheme: GooglePayButtonTheme = GooglePayButtonTheme.DARK,
    cornerRadius: Int = 8,
    enabled: Boolean = true
) {
    val context = LocalContext.current

    // Check if Google Play Services is available
    val isGooglePlayServicesAvailable = remember {
        checkGooglePlayServices(context)
    }

    if (!isGooglePlayServicesAvailable) {
        Log.e("GooglePayButton", "Google Play Services not available or outdated")
        return
    }

    // Create the native PayButton wrapped in AndroidView
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            createPayButton(
                context = ctx,
                allowedPaymentMethods = allowedPaymentMethods,
                buttonType = buttonType,
                buttonTheme = buttonTheme,
                cornerRadius = cornerRadius,
                onClick = onClick
            )
        },
        update = { payButton ->
            // Update click listener if onClick changes
            payButton.setOnClickListener {
                if (enabled) {
                    onClick()
                }
            }
            payButton.isEnabled = enabled
        }
    )
}

/**
 * Creates the native PayButton with the specified configuration
 */
private fun createPayButton(
    context: Context,
    allowedPaymentMethods: String?,
    buttonType: GooglePayButtonType,
    buttonTheme: GooglePayButtonTheme,
    cornerRadius: Int,
    onClick: () -> Unit
): PayButton {
    val payButton = PayButton(context)

    // Build button options
    val optionsBuilder = ButtonOptions.newBuilder()
        .setButtonType(buttonType.value)
        .setButtonTheme(buttonTheme.value)
        .setCornerRadius(cornerRadius)

    // Set allowed payment methods if provided, otherwise use default
    val paymentMethodsJson = allowedPaymentMethods ?: buildDefaultAllowedPaymentMethods()
    optionsBuilder.setAllowedPaymentMethods(paymentMethodsJson)

    // Initialize the button
    try {
        payButton.initialize(optionsBuilder.build())
        payButton.setOnClickListener { onClick() }
    } catch (e: Exception) {
        Log.e("GooglePayButton", "Error initializing PayButton", e)
    }

    return payButton
}

/**
 * Checks if Google Play Services is available on the device
 */
private fun checkGooglePlayServices(context: Context): Boolean {
    val googleApiAvailability = GoogleApiAvailability.getInstance()
    val status = googleApiAvailability.isGooglePlayServicesAvailable(context)
    return status == ConnectionResult.SUCCESS
}

/**
 * Builds the default allowed payment methods configuration
 */
private fun buildDefaultAllowedPaymentMethods(): String {
    return """
    [
      {
        "type": "CARD",
        "parameters": {
          "allowedAuthMethods": ["PAN_ONLY", "CRYPTOGRAM_3DS"],
          "allowedCardNetworks": ["MASTERCARD", "VISA"]
        }
      }
    ]
    """.trimIndent()
}
