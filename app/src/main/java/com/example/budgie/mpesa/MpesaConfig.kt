package com.example.budgie.mpesa

/**
 * M-Pesa Daraja API Configuration
 *
 * To get your credentials:
 * 1. Go to https://developer.safaricom.co.ke/
 * 2. Create an account and log in
 * 3. Create a new app to get Consumer Key and Consumer Secret
 * 4. For production, apply for Go Live
 */
object MpesaConfig {
    // Environment: "sandbox" for testing, "production" for live
    const val ENVIRONMENT = "sandbox"

    // API Base URLs
    const val SANDBOX_BASE_URL = "https://sandbox.safaricom.co.ke/"
    const val PRODUCTION_BASE_URL = "https://api.safaricom.co.ke/"

    // Get the appropriate base URL
    val BASE_URL: String
        get() = if (ENVIRONMENT == "sandbox") SANDBOX_BASE_URL else PRODUCTION_BASE_URL

    // Your Daraja API Credentials (Replace with your actual credentials)
    // These are sandbox test credentials - replace with your own
    const val CONSUMER_KEY = "jP3zCuGY0AteRGpyoU9A97MQHBzVrf79GTTpL7j0CXkkq649"
    const val CONSUMER_SECRET = "Zo6mRAcKQYXKu99Ol5FggMyHQrv3YUJTlfZCk4V1pKtKNmpAHAGNA2hUXTKyNvfP"

    // Business Short Code (Paybill or Till Number)
    // For sandbox testing, use: 174379
    const val BUSINESS_SHORT_CODE = "174379"

    // Lipa Na M-Pesa Online Passkey
    // For sandbox testing, use the test passkey below
    const val PASSKEY = "bfb279f9aa9bdbcf158e97dd71a467cd2e0c893059b10f78e6b72ada1ed2c919"

    // Transaction Type
    const val TRANSACTION_TYPE = "CustomerPayBillOnline"

    // Callback URL - This should be your server endpoint that receives M-Pesa responses
    // For testing, you can use a service like ngrok or webhook.site
    const val CALLBACK_URL = "https://your-server.com/mpesa/callback"

    // Account Reference (shown on customer's M-Pesa statement)
    const val ACCOUNT_REFERENCE = "Budgie"

    // Transaction Description
    const val TRANSACTION_DESC = "Bill Payment via Budgie"
}

