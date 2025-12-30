package com.example.budgie.mpesa

import retrofit2.Response
import retrofit2.http.*

/**
 * M-Pesa Daraja API Service Interface
 */
interface MpesaApiService {

    /**
     * Get OAuth Access Token
     * This is required for all other API calls
     */
    @GET("oauth/v1/generate?grant_type=client_credentials")
    suspend fun getAccessToken(
        @Header("Authorization") authorization: String
    ): Response<AccessTokenResponse>

    /**
     * Initiate STK Push (Lipa Na M-Pesa Online)
     * This sends a payment prompt to the customer's phone
     */
    @POST("mpesa/stkpush/v1/processrequest")
    suspend fun initiateSTKPush(
        @Header("Authorization") authorization: String,
        @Body request: STKPushRequest
    ): Response<STKPushResponse>

    /**
     * Query STK Push Status
     * Check the status of a previously initiated STK Push
     */
    @POST("mpesa/stkpushquery/v1/query")
    suspend fun querySTKPush(
        @Header("Authorization") authorization: String,
        @Body request: STKQueryRequest
    ): Response<STKQueryResponse>
}

