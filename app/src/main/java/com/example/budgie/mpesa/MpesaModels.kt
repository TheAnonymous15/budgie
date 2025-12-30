package com.example.budgie.mpesa

import com.google.gson.annotations.SerializedName

/**
 * M-Pesa API Data Models
 */

// Access Token Response
data class AccessTokenResponse(
    @SerializedName("access_token")
    val accessToken: String,
    @SerializedName("expires_in")
    val expiresIn: String
)

// STK Push Request
data class STKPushRequest(
    @SerializedName("BusinessShortCode")
    val businessShortCode: String,
    @SerializedName("Password")
    val password: String,
    @SerializedName("Timestamp")
    val timestamp: String,
    @SerializedName("TransactionType")
    val transactionType: String,
    @SerializedName("Amount")
    val amount: String,
    @SerializedName("PartyA")
    val partyA: String,
    @SerializedName("PartyB")
    val partyB: String,
    @SerializedName("PhoneNumber")
    val phoneNumber: String,
    @SerializedName("CallBackURL")
    val callBackURL: String,
    @SerializedName("AccountReference")
    val accountReference: String,
    @SerializedName("TransactionDesc")
    val transactionDesc: String
)

// STK Push Response
data class STKPushResponse(
    @SerializedName("MerchantRequestID")
    val merchantRequestID: String?,
    @SerializedName("CheckoutRequestID")
    val checkoutRequestID: String?,
    @SerializedName("ResponseCode")
    val responseCode: String?,
    @SerializedName("ResponseDescription")
    val responseDescription: String?,
    @SerializedName("CustomerMessage")
    val customerMessage: String?,
    // Error fields
    @SerializedName("requestId")
    val requestId: String?,
    @SerializedName("errorCode")
    val errorCode: String?,
    @SerializedName("errorMessage")
    val errorMessage: String?
)

// STK Query Request
data class STKQueryRequest(
    @SerializedName("BusinessShortCode")
    val businessShortCode: String,
    @SerializedName("Password")
    val password: String,
    @SerializedName("Timestamp")
    val timestamp: String,
    @SerializedName("CheckoutRequestID")
    val checkoutRequestID: String
)

// STK Query Response
data class STKQueryResponse(
    @SerializedName("ResponseCode")
    val responseCode: String?,
    @SerializedName("ResponseDescription")
    val responseDescription: String?,
    @SerializedName("MerchantRequestID")
    val merchantRequestID: String?,
    @SerializedName("CheckoutRequestID")
    val checkoutRequestID: String?,
    @SerializedName("ResultCode")
    val resultCode: String?,
    @SerializedName("ResultDesc")
    val resultDesc: String?
)

// Callback data models
data class STKCallback(
    @SerializedName("Body")
    val body: STKCallbackBody
)

data class STKCallbackBody(
    @SerializedName("stkCallback")
    val stkCallback: STKCallbackContent
)

data class STKCallbackContent(
    @SerializedName("MerchantRequestID")
    val merchantRequestID: String,
    @SerializedName("CheckoutRequestID")
    val checkoutRequestID: String,
    @SerializedName("ResultCode")
    val resultCode: Int,
    @SerializedName("ResultDesc")
    val resultDesc: String,
    @SerializedName("CallbackMetadata")
    val callbackMetadata: CallbackMetadata?
)

data class CallbackMetadata(
    @SerializedName("Item")
    val item: List<CallbackItem>
)

data class CallbackItem(
    @SerializedName("Name")
    val name: String,
    @SerializedName("Value")
    val value: Any?
)

// Payment Result for UI
data class PaymentResult(
    val success: Boolean,
    val message: String,
    val transactionId: String? = null,
    val amount: Double? = null,
    val phoneNumber: String? = null,
    val timestamp: String? = null
)

// Payment Status
enum class PaymentStatus {
    IDLE,
    INITIATING,
    WAITING_FOR_USER,
    PROCESSING,
    SUCCESS,
    FAILED,
    CANCELLED,
    TIMEOUT
}

