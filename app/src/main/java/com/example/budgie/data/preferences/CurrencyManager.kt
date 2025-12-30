package com.example.budgie.data.preferences

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages currency preferences system-wide
 */
class CurrencyManager private constructor(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME, Context.MODE_PRIVATE
    )

    private val _currentCurrency = MutableStateFlow(getSelectedCurrency())
    val currentCurrency: StateFlow<Currency> = _currentCurrency.asStateFlow()

    fun getSelectedCurrency(): Currency {
        val code = prefs.getString(KEY_CURRENCY_CODE, DEFAULT_CURRENCY_CODE) ?: DEFAULT_CURRENCY_CODE
        return currencies.find { it.code == code } ?: currencies.first { it.code == DEFAULT_CURRENCY_CODE }
    }

    fun setSelectedCurrency(currency: Currency) {
        prefs.edit().putString(KEY_CURRENCY_CODE, currency.code).apply()
        _currentCurrency.value = currency
    }

    fun formatAmount(amount: Double): String {
        val currency = getSelectedCurrency()
        return "${currency.symbol}${"%.2f".format(amount)}"
    }

    fun formatAmountWithCode(amount: Double): String {
        val currency = getSelectedCurrency()
        return "${currency.symbol}${"%.2f".format(amount)} ${currency.code}"
    }

    companion object {
        private const val PREFS_NAME = "budgie_currency_prefs"
        private const val KEY_CURRENCY_CODE = "selected_currency_code"
        private const val DEFAULT_CURRENCY_CODE = "KES"

        @Volatile
        private var INSTANCE: CurrencyManager? = null

        fun getInstance(context: Context): CurrencyManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: CurrencyManager(context.applicationContext).also { INSTANCE = it }
            }
        }

        // Complete list of world currencies with exchange rates (base: USD)
        val currencies = listOf(
            // Major currencies
            Currency("USD", "US Dollar", "$", "🇺🇸", 1.0),
            Currency("EUR", "Euro", "€", "🇪🇺", 0.92),
            Currency("GBP", "British Pound", "£", "🇬🇧", 0.79),
            Currency("JPY", "Japanese Yen", "¥", "🇯🇵", 149.50),
            Currency("CHF", "Swiss Franc", "CHF", "🇨🇭", 0.88),
            Currency("CNY", "Chinese Yuan", "¥", "🇨🇳", 7.24),
            Currency("AUD", "Australian Dollar", "A$", "🇦🇺", 1.57),
            Currency("CAD", "Canadian Dollar", "C$", "🇨🇦", 1.44),

            // African currencies
            Currency("KES", "Kenyan Shilling", "KSh", "🇰🇪", 153.50),
            Currency("NGN", "Nigerian Naira", "₦", "🇳🇬", 1550.0),
            Currency("ZAR", "South African Rand", "R", "🇿🇦", 18.65),
            Currency("EGP", "Egyptian Pound", "E£", "🇪🇬", 30.90),
            Currency("GHS", "Ghanaian Cedi", "GH₵", "🇬🇭", 12.50),
            Currency("TZS", "Tanzanian Shilling", "TSh", "🇹🇿", 2510.0),
            Currency("UGX", "Ugandan Shilling", "USh", "🇺🇬", 3780.0),
            Currency("RWF", "Rwandan Franc", "FRw", "🇷🇼", 1280.0),
            Currency("ETB", "Ethiopian Birr", "Br", "🇪🇹", 56.50),
            Currency("MAD", "Moroccan Dirham", "MAD", "🇲🇦", 10.05),
            Currency("XOF", "West African CFA", "CFA", "🌍", 605.0),
            Currency("XAF", "Central African CFA", "FCFA", "🌍", 605.0),

            // Asian currencies
            Currency("INR", "Indian Rupee", "₹", "🇮🇳", 83.12),
            Currency("KRW", "South Korean Won", "₩", "🇰🇷", 1320.0),
            Currency("SGD", "Singapore Dollar", "S$", "🇸🇬", 1.34),
            Currency("HKD", "Hong Kong Dollar", "HK$", "🇭🇰", 7.82),
            Currency("TWD", "Taiwan Dollar", "NT$", "🇹🇼", 31.50),
            Currency("THB", "Thai Baht", "฿", "🇹🇭", 35.20),
            Currency("MYR", "Malaysian Ringgit", "RM", "🇲🇾", 4.72),
            Currency("IDR", "Indonesian Rupiah", "Rp", "🇮🇩", 15750.0),
            Currency("PHP", "Philippine Peso", "₱", "🇵🇭", 55.80),
            Currency("VND", "Vietnamese Dong", "₫", "🇻🇳", 24500.0),
            Currency("PKR", "Pakistani Rupee", "Rs", "🇵🇰", 278.50),
            Currency("BDT", "Bangladeshi Taka", "৳", "🇧🇩", 110.0),
            Currency("LKR", "Sri Lankan Rupee", "Rs", "🇱🇰", 325.0),
            Currency("NPR", "Nepalese Rupee", "Rs", "🇳🇵", 133.0),
            Currency("AED", "UAE Dirham", "د.إ", "🇦🇪", 3.67),
            Currency("SAR", "Saudi Riyal", "﷼", "🇸🇦", 3.75),
            Currency("QAR", "Qatari Riyal", "ر.ق", "🇶🇦", 3.64),
            Currency("KWD", "Kuwaiti Dinar", "د.ك", "🇰🇼", 0.31),
            Currency("BHD", "Bahraini Dinar", "BD", "🇧🇭", 0.38),
            Currency("OMR", "Omani Rial", "ر.ع.", "🇴🇲", 0.38),
            Currency("ILS", "Israeli Shekel", "₪", "🇮🇱", 3.65),
            Currency("TRY", "Turkish Lira", "₺", "🇹🇷", 30.50),

            // European currencies
            Currency("SEK", "Swedish Krona", "kr", "🇸🇪", 10.45),
            Currency("NOK", "Norwegian Krone", "kr", "🇳🇴", 10.75),
            Currency("DKK", "Danish Krone", "kr", "🇩🇰", 6.88),
            Currency("PLN", "Polish Zloty", "zł", "🇵🇱", 4.02),
            Currency("CZK", "Czech Koruna", "Kč", "🇨🇿", 22.80),
            Currency("HUF", "Hungarian Forint", "Ft", "🇭🇺", 358.0),
            Currency("RON", "Romanian Leu", "lei", "🇷🇴", 4.58),
            Currency("BGN", "Bulgarian Lev", "лв", "🇧🇬", 1.80),
            Currency("HRK", "Croatian Kuna", "kn", "🇭🇷", 6.95),
            Currency("RUB", "Russian Ruble", "₽", "🇷🇺", 92.50),
            Currency("UAH", "Ukrainian Hryvnia", "₴", "🇺🇦", 37.50),
            Currency("ISK", "Icelandic Króna", "kr", "🇮🇸", 138.0),

            // Americas
            Currency("MXN", "Mexican Peso", "$", "🇲🇽", 17.15),
            Currency("BRL", "Brazilian Real", "R$", "🇧🇷", 4.95),
            Currency("ARS", "Argentine Peso", "$", "🇦🇷", 850.0),
            Currency("CLP", "Chilean Peso", "$", "🇨🇱", 880.0),
            Currency("COP", "Colombian Peso", "$", "🇨🇴", 3950.0),
            Currency("PEN", "Peruvian Sol", "S/", "🇵🇪", 3.72),
            Currency("UYU", "Uruguayan Peso", "\$U", "🇺🇾", 39.50),
            Currency("BOB", "Bolivian Boliviano", "Bs", "🇧🇴", 6.91),
            Currency("PYG", "Paraguayan Guarani", "₲", "🇵🇾", 7350.0),
            Currency("VES", "Venezuelan Bolívar", "Bs", "🇻🇪", 36.50),
            Currency("DOP", "Dominican Peso", "RD$", "🇩🇴", 57.0),
            Currency("CRC", "Costa Rican Colón", "₡", "🇨🇷", 530.0),
            Currency("GTQ", "Guatemalan Quetzal", "Q", "🇬🇹", 7.82),
            Currency("HNL", "Honduran Lempira", "L", "🇭🇳", 24.70),
            Currency("NIO", "Nicaraguan Córdoba", "C$", "🇳🇮", 36.70),
            Currency("PAB", "Panamanian Balboa", "B/.", "🇵🇦", 1.0),
            Currency("JMD", "Jamaican Dollar", "J$", "🇯🇲", 155.0),
            Currency("TTD", "Trinidad Dollar", "TT$", "🇹🇹", 6.78),
            Currency("BBD", "Barbadian Dollar", "Bds$", "🇧🇧", 2.0),
            Currency("BSD", "Bahamian Dollar", "B$", "🇧🇸", 1.0),
            Currency("HTG", "Haitian Gourde", "G", "🇭🇹", 132.0),
            Currency("CUP", "Cuban Peso", "₱", "🇨🇺", 24.0),

            // Oceania
            Currency("NZD", "New Zealand Dollar", "NZ$", "🇳🇿", 1.64),
            Currency("FJD", "Fijian Dollar", "FJ$", "🇫🇯", 2.25),
            Currency("PGK", "Papua New Guinean Kina", "K", "🇵🇬", 3.75),
            Currency("WST", "Samoan Tala", "WS$", "🇼🇸", 2.75),
            Currency("TOP", "Tongan Paʻanga", "T$", "🇹🇴", 2.38),
            Currency("VUV", "Vanuatu Vatu", "VT", "🇻🇺", 120.0),
            Currency("SBD", "Solomon Islands Dollar", "SI$", "🇸🇧", 8.45),

            // Other
            Currency("BTC", "Bitcoin", "₿", "🪙", 0.000024),
            Currency("ETH", "Ethereum", "Ξ", "🪙", 0.00043),
            Currency("XAU", "Gold (oz)", "XAU", "🥇", 0.00049),
            Currency("XAG", "Silver (oz)", "XAG", "🥈", 0.042)
        )

        fun getCurrencyByCode(code: String): Currency? {
            return currencies.find { it.code.equals(code, ignoreCase = true) }
        }
    }
}

data class Currency(
    val code: String,
    val name: String,
    val symbol: String,
    val flag: String,
    val rateToUSD: Double // How many units of this currency = 1 USD
)

