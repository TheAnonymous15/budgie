package com.example.budgie.data.api

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

private const val TAG = "ExchangeRateService"

/**
 * Service for fetching live exchange rates from free APIs
 * Uses exchangerate-api.com free tier (1500 requests/month)
 * Fallback to frankfurter.app (unlimited, EU rates)
 */
class ExchangeRateService private constructor(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME, Context.MODE_PRIVATE
    )

    companion object {
        private const val PREFS_NAME = "exchange_rate_cache"
        private const val KEY_RATES = "cached_rates"
        private const val KEY_LAST_UPDATE = "last_update_timestamp"
        private const val KEY_BASE_CURRENCY = "base_currency"

        // Cache duration: 1 hour
        private val CACHE_DURATION_MS = TimeUnit.HOURS.toMillis(1)

        // Free API endpoints (ordered by reliability and rate limits)
        // 1. Open Exchange Rates API - Very reliable, good coverage (free tier: 1000 requests/month)
        private const val PRIMARY_API = "https://open.er-api.com/v6/latest/USD"
        // 2. ExchangeRate-API - Good reliability (free tier: 1500 requests/month)
        private const val SECONDARY_API = "https://api.exchangerate-api.com/v4/latest/USD"
        // 3. Frankfurter API - EU Central Bank rates, unlimited but fewer currencies
        private const val TERTIARY_API = "https://api.frankfurter.app/latest?from=USD"
        // 4. Fixer-compatible free API
        private const val BACKUP_API = "https://api.fxratesapi.com/latest?base=USD"

        @Volatile
        private var INSTANCE: ExchangeRateService? = null

        fun getInstance(context: Context): ExchangeRateService {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ExchangeRateService(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    /**
     * Get exchange rate from source currency to target currency
     * @return exchange rate, or null if unavailable
     */
    suspend fun getExchangeRate(from: String, to: String): Double? {
        val rates = getRates()
        if (rates.isEmpty()) return null

        val fromRate = rates[from] ?: return null
        val toRate = rates[to] ?: return null

        // Convert via USD (all rates are relative to USD)
        return toRate / fromRate
    }

    /**
     * Convert amount from one currency to another
     */
    suspend fun convert(amount: Double, from: String, to: String): Double? {
        val rate = getExchangeRate(from, to) ?: return null
        return amount * rate
    }

    /**
     * Get all exchange rates (base: USD)
     * Returns cached rates if valid, otherwise fetches fresh rates
     */
    suspend fun getRates(): Map<String, Double> {
        // Check cache first
        val cachedRates = getCachedRates()
        if (cachedRates.isNotEmpty() && !isCacheExpired()) {
            Log.d(TAG, "Using cached exchange rates")
            return cachedRates
        }

        // Fetch fresh rates
        return fetchFreshRates()
    }

    /**
     * Force refresh exchange rates from API
     */
    suspend fun refreshRates(): Map<String, Double> {
        return fetchFreshRates()
    }

    /**
     * Get the timestamp of the last rate update
     */
    fun getLastUpdateTime(): Long {
        return prefs.getLong(KEY_LAST_UPDATE, 0)
    }

    /**
     * Check if rates are stale (older than cache duration)
     */
    fun isRatesStale(): Boolean {
        return isCacheExpired()
    }

    private fun isCacheExpired(): Boolean {
        val lastUpdate = prefs.getLong(KEY_LAST_UPDATE, 0)
        return System.currentTimeMillis() - lastUpdate > CACHE_DURATION_MS
    }

    private fun getCachedRates(): Map<String, Double> {
        val ratesJson = prefs.getString(KEY_RATES, null) ?: return emptyMap()
        return try {
            val json = JSONObject(ratesJson)
            val rates = mutableMapOf<String, Double>()
            json.keys().forEach { key ->
                rates[key] = json.getDouble(key)
            }
            rates
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse cached rates: ${e.message}")
            emptyMap()
        }
    }

    private suspend fun fetchFreshRates(): Map<String, Double> = withContext(Dispatchers.IO) {
        // Try primary API first (Open ER API - most reliable)
        var rates = fetchFromApi(PRIMARY_API, ::parseOpenErApi)

        // Fallback to secondary API (ExchangeRate-API)
        if (rates.isEmpty()) {
            Log.w(TAG, "Primary API failed, trying secondary...")
            rates = fetchFromApi(SECONDARY_API, ::parsePrimaryApi)
        }

        // Fallback to tertiary API (Frankfurter - EU Central Bank)
        if (rates.isEmpty()) {
            Log.w(TAG, "Secondary API failed, trying tertiary...")
            rates = fetchFromApi(TERTIARY_API, ::parseFrankfurterApi)
        }

        // Fallback to backup API (FX Rates API)
        if (rates.isEmpty()) {
            Log.w(TAG, "Tertiary API failed, trying backup...")
            rates = fetchFromApi(BACKUP_API, ::parseFxRatesApi)
        }

        // If all APIs fail, return cached rates or hardcoded defaults
        if (rates.isEmpty()) {
            Log.w(TAG, "All APIs failed, using fallback rates")
            val cached = getCachedRates()
            return@withContext if (cached.isNotEmpty()) cached else getDefaultRates()
        }

        // Cache the fresh rates
        cacheRates(rates)
        rates
    }

    private fun fetchFromApi(apiUrl: String, parser: (String) -> Map<String, Double>): Map<String, Double> {
        return try {
            val url = URL(apiUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/json")

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val rates = parser(response)
                Log.d(TAG, "Successfully fetched ${rates.size} rates from $apiUrl")
                rates
            } else {
                Log.e(TAG, "API returned ${connection.responseCode} from $apiUrl")
                emptyMap()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch from $apiUrl: ${e.message}")
            emptyMap()
        }
    }

    private fun parsePrimaryApi(response: String): Map<String, Double> {
        return try {
            val json = JSONObject(response)
            val ratesJson = json.getJSONObject("rates")
            val rates = mutableMapOf<String, Double>()
            rates["USD"] = 1.0 // Base currency
            ratesJson.keys().forEach { key ->
                rates[key] = ratesJson.getDouble(key)
            }
            rates
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse primary API response: ${e.message}")
            emptyMap()
        }
    }

    private fun parseFrankfurterApi(response: String): Map<String, Double> {
        return try {
            val json = JSONObject(response)
            val ratesJson = json.getJSONObject("rates")
            val rates = mutableMapOf<String, Double>()
            rates["USD"] = 1.0 // Base currency
            ratesJson.keys().forEach { key ->
                rates[key] = ratesJson.getDouble(key)
            }
            rates
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse Frankfurter API response: ${e.message}")
            emptyMap()
        }
    }

    private fun parseOpenErApi(response: String): Map<String, Double> {
        return try {
            val json = JSONObject(response)
            val ratesJson = json.getJSONObject("rates")
            val rates = mutableMapOf<String, Double>()
            ratesJson.keys().forEach { key ->
                rates[key] = ratesJson.getDouble(key)
            }
            rates
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse Open ER API response: ${e.message}")
            emptyMap()
        }
    }

    private fun parseFxRatesApi(response: String): Map<String, Double> {
        return try {
            val json = JSONObject(response)
            val ratesJson = json.getJSONObject("rates")
            val rates = mutableMapOf<String, Double>()
            rates["USD"] = 1.0 // Base currency
            ratesJson.keys().forEach { key ->
                rates[key] = ratesJson.getDouble(key)
            }
            rates
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse FX Rates API response: ${e.message}")
            emptyMap()
        }
    }

    private fun cacheRates(rates: Map<String, Double>) {
        try {
            val json = JSONObject()
            rates.forEach { (key, value) ->
                json.put(key, value)
            }
            prefs.edit()
                .putString(KEY_RATES, json.toString())
                .putLong(KEY_LAST_UPDATE, System.currentTimeMillis())
                .putString(KEY_BASE_CURRENCY, "USD")
                .apply()
            Log.d(TAG, "Cached ${rates.size} exchange rates")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cache rates: ${e.message}")
        }
    }

    /**
     * Default rates as fallback when all APIs fail and no cache exists
     * These are approximate rates and should only be used as last resort
     */
    private fun getDefaultRates(): Map<String, Double> {
        return mapOf(
            "USD" to 1.0,
            "EUR" to 0.92,
            "GBP" to 0.79,
            "JPY" to 149.50,
            "CHF" to 0.88,
            "CNY" to 7.24,
            "AUD" to 1.57,
            "CAD" to 1.44,
            "KES" to 153.50,
            "NGN" to 1550.0,
            "ZAR" to 18.65,
            "EGP" to 30.90,
            "GHS" to 12.50,
            "TZS" to 2510.0,
            "UGX" to 3780.0,
            "RWF" to 1280.0,
            "ETB" to 56.50,
            "MAD" to 10.05,
            "INR" to 83.12,
            "KRW" to 1320.0,
            "SGD" to 1.34,
            "HKD" to 7.82,
            "THB" to 35.20,
            "MYR" to 4.72,
            "IDR" to 15750.0,
            "PHP" to 55.80,
            "AED" to 3.67,
            "SAR" to 3.75,
            "BRL" to 4.97,
            "MXN" to 17.15,
            "NZD" to 1.68,
            "SEK" to 10.42,
            "NOK" to 10.55,
            "DKK" to 6.87,
            "PLN" to 3.98,
            "CZK" to 22.85,
            "HUF" to 355.0,
            "TRY" to 32.50,
            "RUB" to 92.0,
            "ILS" to 3.65
        )
    }
}

