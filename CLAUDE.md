# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

EveryPay Google Pay SDK for Android — a native Android library (`com.every-pay:gpay`) for integrating Google Pay payments via the EveryPay payment gateway. Written in Kotlin, targeting Android API 24+, JVM 17.

## Build Commands

```bash
# Build the library
./gradlew :libraries:gpay:assembleRelease

# Run unit tests (library)
./gradlew :libraries:gpay:testDebugUnitTest

# Run a single test class
./gradlew :libraries:gpay:testDebugUnitTest --tests "com.everypay.gpay.api.EverypayApiClientTest"

# Run a single test method
./gradlew :libraries:gpay:testDebugUnitTest --tests "com.everypay.gpay.api.EverypayApiClientTest.openSession should return session data on success"

# Build the sample app
./gradlew :app:assembleDebug

# Publish to Maven Central (CI handles this on GitHub release; requires signing + Maven Central credentials)
./gradlew :libraries:gpay:publishAndReleaseToMavenCentral --no-configuration-cache
```

## Architecture

### Two-module Gradle project

- **`app/`** — Sample/test application (`com.everypay.googlepaysdk`)
- **`libraries/gpay/`** — The SDK library (`com.everypay.gpay`), published as `com.every-pay:gpay` to Maven Central

### Two integration modes

The SDK supports **Backend Mode** (recommended) and **SDK Mode**:

- **Backend Mode**: App's backend calls EveryPay APIs; the SDK only handles Google Pay UI and token extraction. Config has null credentials (`apiUsername`, `apiSecret`, etc.). Returns `GooglePayResult.TokenReceived`.
- **SDK Mode**: SDK makes all EveryPay API calls directly (open_session → create_payment → payment_data). Config has all credentials populated. Returns `GooglePayResult.Success`.

Mode is auto-detected via `EverypayConfig.isBackendMode()` (true when API credentials are null).

### Key classes (all under `com.everypay.gpay`)

- **`EverypayGooglePayHelper`** — Main entry point. Orchestrates the full payment flow. Has parallel method pairs: `initialize()`/`initializeWithBackendData()`, `makePayment()`/`makePaymentWithBackendData()`, `requestToken()`/`requestTokenWithBackendData()`.
- **`GooglePayManager`** — Wraps Google Play Services `PaymentsClient`. Handles readiness checks and payment request creation.
- **`GooglePayActivityHelper`** — Bridges between `GooglePayManager` and Activity lifecycle (launching Google Pay intent, handling `onActivityResult`).
- **`api/EverypayApiClient`** — HTTP client (OkHttp) for EveryPay REST API. SDK mode only. Endpoints: `open_session`, `payments/oneoff`, `google_pay/payment_data`, `GET payments/{ref}`.
- **`GooglePayResult`** — Sealed class: `Success`, `TokenReceived`, `Canceled`, `Error`.
- **`GooglePayReadinessResult`** — Sealed class for readiness check results.
- **`models/`** — Data classes for API requests/responses, config, token data.
- **`util/PaymentsUtil`** — Builds Google Pay JSON request structures.

### Recurring payments / MIT tokens

Token requests use zero-amount payments with `request_token=true` and `token_consent_agreed=true`. In SDK mode, after processing, the SDK calls `GET /payments/{ref}` to retrieve the MIT token from `cc_details.token`. See `RECURRING_PAYMENTS.md`.

## Testing

Tests use JUnit 4, MockK, OkHttp MockWebServer, and Google Truth assertions. Test fixtures are in `TestFixtures.kt`. The library's `build.gradle` sets `unitTests.returnDefaultValues = true` for Android framework stubs.

## Publishing

Library version is derived from `VERSION_NAME` env var (default `0.1.0`), with leading `v` stripped. Published to Maven Central (`com.every-pay:gpay`) via the `com.vanniktech.maven.publish` plugin targeting `SonatypeHost.CENTRAL_PORTAL`. CI publishes automatically on GitHub release creation. Required GitHub secrets: `MAVEN_CENTRAL_USERNAME`, `MAVEN_CENTRAL_PASSWORD`, `SIGNING_KEY_ID`, `SIGNING_KEY`, `SIGNING_PASSWORD`.
