package com.example.budgie.mpesa

import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * M-Pesa STK Push Repository
 * Handles all M-Pesa Daraja API interactions
 */
class MpesaRepository {

    companion object {
        private const val TAG = "MpesaRepository"

        @Volatile
        private var INSTANCE: MpesaRepository? = null

        fun getInstance(): MpesaRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: MpesaRepository().also { INSTANCE = it }
            }
        }
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(MpesaConfig.BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val apiService = retrofit.create(MpesaApiService::class.java)

    // Cached access token
    private var accessToken: String? = null
    private var tokenExpiry: Long = 0

    /**
     * Get OAuth Access Token
     */
    private suspend fun getAccessToken(): String? = withContext(Dispatchers.IO) {
        try {
            // Check if we have a valid cached token
            if (accessToken != null && System.currentTimeMillis() < tokenExpiry) {
                return@withContext accessToken
            }

            // Generate Basic Auth credentials
            val credentials = "${MpesaConfig.CONSUMER_KEY}:${MpesaConfig.CONSUMER_SECRET}"
            val basicAuth = "Basic " + Base64.encodeToString(
                credentials.toByteArray(),
                Base64.NO_WRAP
            )

            val response = apiService.getAccessToken(basicAuth)

            if (response.isSuccessful && response.body() != null) {
                val tokenResponse = response.body()!!
                accessToken = tokenResponse.accessToken
                // Set expiry to 50 minutes (tokens typically last 1 hour)
                tokenExpiry = System.currentTimeMillis() + (50 * 60 * 1000)
                Log.d(TAG, "Access token obtained successfully")
                return@withContext accessToken
            } else {
                Log.e(TAG, "Failed to get access token: ${response.errorBody()?.string()}")
                return@withContext null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting access token", e)
            return@withContext null
        }
    }

    /**
     * Generate Password for STK Push
     * Password = Base64(BusinessShortCode + Passkey + Timestamp)
     */
    private fun generatePassword(timestamp: String): String {
        val dataToEncode = "${MpesaConfig.BUSINESS_SHORT_CODE}${MpesaConfig.PASSKEY}$timestamp"
        return Base64.encodeToString(dataToEncode.toByteArray(), Base64.NO_WRAP)
    }

    /**
     * Generate Timestamp in format YYYYMMDDHHmmss
     */
    private fun generateTimestamp(): String {
        val dateFormat = SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault())
        dateFormat.timeZone = TimeZone.getTimeZone("Africa/Nairobi")
        return dateFormat.format(Date())
    }

    /**
     * Format phone number to 254XXXXXXXXX format
     */
    private fun formatPhoneNumber(phoneNumber: String): String {
        var formatted = phoneNumber.replace(" ", "").replace("-", "")

        // Remove leading + if present
        if (formatted.startsWith("+")) {
            formatted = formatted.substring(1)
        }

        // Handle 07XX format
        if (formatted.startsWith("07") || formatted.startsWith("01")) {
            formatted = "254${formatted.substring(1)}"
        }

        // Handle 7XX format
        if (formatted.length == 9 && (formatted.startsWith("7") || formatted.startsWith("1"))) {
            formatted = "254$formatted"
        }

        return formatted
    }

    /**
     * Initiate STK Push
     * This sends a payment prompt to the customer's phone
     */
    suspend fun initiateSTKPush(
        phoneNumber: String,
        amount: Double,
        accountReference: String = MpesaConfig.ACCOUNT_REFERENCE,
        transactionDesc: String = MpesaConfig.TRANSACTION_DESC
    ): PaymentResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Initiating STK Push for $phoneNumber, amount: $amount")

            // Get access token
            val token = getAccessToken()
            if (token == null) {
                return@withContext PaymentResult(
                    success = false,
                    message = "Failed to authenticate with M-Pesa. Please try again."
                )
            }

            val timestamp = generateTimestamp()
            val password = generatePassword(timestamp)
            val formattedPhone = formatPhoneNumber(phoneNumber)
            val amountInt = amount.toInt().toString() // M-Pesa only accepts whole numbers

            val request = STKPushRequest(
                businessShortCode = MpesaConfig.BUSINESS_SHORT_CODE,
                password = password,
                timestamp = timestamp,
                transactionType = MpesaConfig.TRANSACTION_TYPE,
                amount = amountInt,
                partyA = formattedPhone,
                partyB = MpesaConfig.BUSINESS_SHORT_CODE,
                phoneNumber = formattedPhone,
                callBackURL = MpesaConfig.CALLBACK_URL,
                accountReference = accountReference,
                transactionDesc = transactionDesc
            )

            Log.d(TAG, "STK Push Request: Phone=$formattedPhone, Amount=$amountInt")

            val response = apiService.initiateSTKPush("Bearer $token", request)

            if (response.isSuccessful && response.body() != null) {
                val stkResponse = response.body()!!

                if (stkResponse.responseCode == "0") {
                    Log.d(TAG, "STK Push initiated successfully: ${stkResponse.customerMessage}")
                    return@withContext PaymentResult(
                        success = true,
                        message = stkResponse.customerMessage ?: "Please check your phone and enter M-Pesa PIN",
                        transactionId = stkResponse.checkoutRequestID,
                        amount = amount,
                        phoneNumber = formattedPhone,
                        timestamp = timestamp
                    )
                } else {
                    val errorMsg = stkResponse.responseDescription
                        ?: stkResponse.errorMessage
                        ?: "Payment initiation failed"
                    Log.e(TAG, "STK Push failed: $errorMsg")
                    return@withContext PaymentResult(
                        success = false,
                        message = errorMsg
                    )
                }
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e(TAG, "STK Push request failed: $errorBody")
                return@withContext PaymentResult(
                    success = false,
                    message = "Payment request failed. Please try again."
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initiating STK Push", e)
            return@withContext PaymentResult(
                success = false,
                message = "Network error. Please check your internet connection."
            )
        }
    }

    /**
     * Query STK Push Status
     * Check if the payment was completed
     */
    suspend fun querySTKPushStatus(checkoutRequestId: String): PaymentResult = withContext(Dispatchers.IO) {
        try {
            val token = getAccessToken()
            if (token == null) {
                return@withContext PaymentResult(
                    success = false,
                    message = "Failed to authenticate with M-Pesa"
                )
            }

            val timestamp = generateTimestamp()
            val password = generatePassword(timestamp)

            val request = STKQueryRequest(
                businessShortCode = MpesaConfig.BUSINESS_SHORT_CODE,
                password = password,
                timestamp = timestamp,
                checkoutRequestID = checkoutRequestId
            )

            val response = apiService.querySTKPush("Bearer $token", request)

            if (response.isSuccessful && response.body() != null) {
                val queryResponse = response.body()!!

                return@withContext when (queryResponse.resultCode) {
                    "0" -> PaymentResult(
                        success = true,
                        message = "Payment completed successfully!",
                        transactionId = checkoutRequestId
                    )
                    "1032" -> PaymentResult(
                        success = false,
                        message = "Transaction cancelled by user"
                    )
                    "1037" -> PaymentResult(
                        success = false,
                        message = "Transaction timed out. Please try again."
                    )
                    "1" -> PaymentResult(
                        success = false,
                        message = "Insufficient balance"
                    )
                    else -> PaymentResult(
                        success = false,
                        message = queryResponse.resultDesc ?: "Payment failed"
                    )
                }
            } else {
                return@withContext PaymentResult(
                    success = false,
                    message = "Unable to verify payment status"
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying STK Push status", e)
            return@withContext PaymentResult(
                success = false,
                message = "Unable to verify payment status"
            )
        }
    }
}

