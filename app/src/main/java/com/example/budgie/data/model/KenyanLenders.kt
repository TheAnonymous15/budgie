package com.example.budgie.data.model

/**
 * Comprehensive list of Kenyan financial institutions with their paybill numbers
 * This data is used for loan registration and bill payments
 */

// Lender category enum
enum class LenderCategory(val displayName: String, val icon: String) {
    BANK("Bank", "🏦"),
    SACCO("SACCO", "🤝"),
    MICROFINANCE("Microfinance", "💰"),
    MOBILE_LENDER("Mobile Lender", "📱"),
    GOVERNMENT("Government", "🏛️"),
    EMPLOYER("Employer", "🏢"),
    FAMILY_FRIENDS("Family & Friends", "👨‍👩‍👧‍👦"),
    OTHER("Other", "📋")
}

// Data class for a lender
data class KenyanLender(
    val name: String,
    val shortName: String,
    val category: LenderCategory,
    val paybillNumber: String,
    val accountFormat: String = "", // Hint for account number format
    val icon: String = "🏦"
)

/**
 * Complete list of Kenyan Banks with their paybill numbers
 */
object KenyanBanks {
    val banks = listOf(
        // Tier 1 Banks
        KenyanLender("Kenya Commercial Bank", "KCB", LenderCategory.BANK, "522522", "Loan Account Number", "🏦"),
        KenyanLender("Equity Bank", "Equity", LenderCategory.BANK, "247247", "Loan Account Number", "🏦"),
        KenyanLender("Co-operative Bank", "Co-op", LenderCategory.BANK, "400200", "Loan Account Number", "🏦"),
        KenyanLender("ABSA Bank Kenya", "ABSA", LenderCategory.BANK, "303030", "Loan Account Number", "🏦"),
        KenyanLender("Standard Chartered Bank", "StanChart", LenderCategory.BANK, "329329", "Loan Account Number", "🏦"),
        KenyanLender("Stanbic Bank", "Stanbic", LenderCategory.BANK, "600100", "Loan Account Number", "🏦"),
        KenyanLender("NCBA Bank", "NCBA", LenderCategory.BANK, "880100", "Loan Account Number", "🏦"),
        KenyanLender("I&M Bank", "I&M", LenderCategory.BANK, "542542", "Loan Account Number", "🏦"),
        KenyanLender("Diamond Trust Bank", "DTB", LenderCategory.BANK, "516600", "Loan Account Number", "🏦"),

        // Tier 2 Banks
        KenyanLender("Family Bank", "Family", LenderCategory.BANK, "222111", "Loan Account Number", "🏦"),
        KenyanLender("Prime Bank", "Prime", LenderCategory.BANK, "968200", "Loan Account Number", "🏦"),
        KenyanLender("National Bank of Kenya", "NBK", LenderCategory.BANK, "625625", "Loan Account Number", "🏦"),
        KenyanLender("Bank of Africa", "BOA", LenderCategory.BANK, "972900", "Loan Account Number", "🏦"),
        KenyanLender("GT Bank Kenya", "GTBank", LenderCategory.BANK, "739300", "Loan Account Number", "🏦"),
        KenyanLender("Bank of Baroda", "BOB", LenderCategory.BANK, "800201", "Loan Account Number", "🏦"),
        KenyanLender("Victoria Commercial Bank", "VCB", LenderCategory.BANK, "405405", "Loan Account Number", "🏦"),
        KenyanLender("Guardian Bank", "Guardian", LenderCategory.BANK, "985050", "Loan Account Number", "🏦"),
        KenyanLender("Sidian Bank", "Sidian", LenderCategory.BANK, "637637", "Loan Account Number", "🏦"),
        KenyanLender("Ecobank Kenya", "Ecobank", LenderCategory.BANK, "700201", "Loan Account Number", "🏦"),
        KenyanLender("Credit Bank", "Credit", LenderCategory.BANK, "654321", "Loan Account Number", "🏦"),
        KenyanLender("Housing Finance Company", "HF", LenderCategory.BANK, "200200", "Loan Account Number", "🏦"),
        KenyanLender("Development Bank of Kenya", "DBK", LenderCategory.BANK, "415141", "Loan Account Number", "🏦"),
        KenyanLender("African Banking Corporation", "ABC", LenderCategory.BANK, "461461", "Loan Account Number", "🏦"),
        KenyanLender("Consolidated Bank", "Consolidated", LenderCategory.BANK, "781781", "Loan Account Number", "🏦"),
        KenyanLender("Middle East Bank", "MEB", LenderCategory.BANK, "899899", "Loan Account Number", "🏦"),
        KenyanLender("Paramount Bank", "Paramount", LenderCategory.BANK, "636363", "Loan Account Number", "🏦"),
        KenyanLender("Mayfair Bank", "Mayfair", LenderCategory.BANK, "824824", "Loan Account Number", "🏦"),
        KenyanLender("SBM Bank Kenya", "SBM", LenderCategory.BANK, "700800", "Loan Account Number", "🏦"),
        KenyanLender("UBA Kenya", "UBA", LenderCategory.BANK, "501501", "Loan Account Number", "🏦"),
        KenyanLender("Access Bank Kenya", "Access", LenderCategory.BANK, "488488", "Loan Account Number", "🏦"),
        KenyanLender("Kingdom Bank", "Kingdom", LenderCategory.BANK, "515151", "Loan Account Number", "🏦"),
        KenyanLender("M-Oriental Bank", "M-Oriental", LenderCategory.BANK, "300300", "Loan Account Number", "🏦"),
        KenyanLender("First Community Bank", "FCB", LenderCategory.BANK, "575757", "Loan Account Number", "🏦"),
        KenyanLender("DIB Bank Kenya", "DIB", LenderCategory.BANK, "646464", "Loan Account Number", "🏦"),
        KenyanLender("Habib Bank AG Zurich", "HBZ", LenderCategory.BANK, "717171", "Loan Account Number", "🏦"),
        KenyanLender("Citibank Kenya", "Citi", LenderCategory.BANK, "820820", "Loan Account Number", "🏦"),
        KenyanLender("Gulf African Bank", "GAB", LenderCategory.BANK, "985800", "Loan Account Number", "🏦"),

        // Additional Banks
        KenyanLender("Spire Bank", "Spire", LenderCategory.BANK, "800400", "Loan Account Number", "🏦"),
        KenyanLender("Transnational Bank", "TNB", LenderCategory.BANK, "770770", "Loan Account Number", "🏦"),
        KenyanLender("Jamii Bora Bank", "Jamii Bora", LenderCategory.BANK, "585585", "Loan Account Number", "🏦"),
        KenyanLender("Chase Bank Kenya", "Chase", LenderCategory.BANK, "544544", "Loan Account Number", "🏦"),
        KenyanLender("Imperial Bank Kenya", "Imperial", LenderCategory.BANK, "566566", "Loan Account Number", "🏦"),
        KenyanLender("Charterhouse Bank", "Charterhouse", LenderCategory.BANK, "533533", "Loan Account Number", "🏦"),
        KenyanLender("Postbank Kenya", "Postbank", LenderCategory.BANK, "498498", "Loan Account Number", "🏦"),
    )
}

/**
 * Complete list of Kenyan SACCOs with their paybill numbers
 */
object KenyanSaccos {
    val saccos = listOf(
        // Major SACCOs
        KenyanLender("Kenya Police SACCO", "Kenya Police", LenderCategory.SACCO, "511800", "Member Number", "🤝"),
        KenyanLender("Mwalimu National SACCO", "Mwalimu", LenderCategory.SACCO, "400222", "TSC Number", "🤝"),
        KenyanLender("Stima SACCO", "Stima", LenderCategory.SACCO, "222666", "Member Number", "🤝"),
        KenyanLender("Harambee SACCO", "Harambee", LenderCategory.SACCO, "444555", "Member Number", "🤝"),
        KenyanLender("Ukulima SACCO", "Ukulima", LenderCategory.SACCO, "333222", "Member Number", "🤝"),
        KenyanLender("Afya SACCO", "Afya", LenderCategory.SACCO, "777888", "Staff Number", "🤝"),
        KenyanLender("Unaitas SACCO", "Unaitas", LenderCategory.SACCO, "801700", "Member Number", "🤝"),
        KenyanLender("Tower SACCO", "Tower", LenderCategory.SACCO, "800700", "Member Number", "🤝"),
        KenyanLender("Imarika SACCO", "Imarika", LenderCategory.SACCO, "558855", "Member Number", "🤝"),
        KenyanLender("Boresha SACCO", "Boresha", LenderCategory.SACCO, "228228", "Member Number", "🤝"),
        KenyanLender("Kenya Bankers SACCO", "KBS", LenderCategory.SACCO, "550550", "Member Number", "🤝"),
        KenyanLender("Nation SACCO", "Nation", LenderCategory.SACCO, "514514", "Member Number", "🤝"),
        KenyanLender("Safaricom SACCO", "Safaricom", LenderCategory.SACCO, "100100", "Staff ID", "🤝"),
        KenyanLender("Gusii Mwalimu SACCO", "Gusii Mwalimu", LenderCategory.SACCO, "551551", "TSC Number", "🤝"),
        KenyanLender("Mentor SACCO", "Mentor", LenderCategory.SACCO, "600700", "Member Number", "🤝"),
        KenyanLender("Dimkes SACCO", "Dimkes", LenderCategory.SACCO, "700700", "Member Number", "🤝"),
        KenyanLender("United Nations SACCO", "UN SACCO", LenderCategory.SACCO, "222555", "Staff Number", "🤝"),
        KenyanLender("Ufanisi SACCO", "Ufanisi", LenderCategory.SACCO, "447447", "Member Number", "🤝"),
        KenyanLender("Winas SACCO", "Winas", LenderCategory.SACCO, "887887", "Member Number", "🤝"),
        KenyanLender("Sheria SACCO", "Sheria", LenderCategory.SACCO, "991991", "Staff Number", "🤝"),
        KenyanLender("Nyati SACCO", "Nyati", LenderCategory.SACCO, "663663", "Member Number", "🤝"),
        KenyanLender("Fariji SACCO", "Fariji", LenderCategory.SACCO, "559559", "Member Number", "🤝"),
        KenyanLender("Jamii SACCO", "Jamii", LenderCategory.SACCO, "885885", "Member Number", "🤝"),
        KenyanLender("Solution SACCO", "Solution", LenderCategory.SACCO, "993993", "Member Number", "🤝"),
        KenyanLender("Waumini SACCO", "Waumini", LenderCategory.SACCO, "774774", "Member Number", "🤝"),
    )
}

/**
 * Microfinance Institutions
 */
object KenyanMicrofinance {
    val mfis = listOf(
        KenyanLender("KWFT Microfinance Bank", "KWFT", LenderCategory.MICROFINANCE, "400500", "Account Number", "💰"),
        KenyanLender("Faulu Microfinance Bank", "Faulu", LenderCategory.MICROFINANCE, "329000", "Account Number", "💰"),
        KenyanLender("SMEP Microfinance Bank", "SMEP", LenderCategory.MICROFINANCE, "513300", "Account Number", "💰"),
        KenyanLender("Caritas Microfinance Bank", "Caritas", LenderCategory.MICROFINANCE, "599200", "Account Number", "💰"),
        KenyanLender("Century Microfinance Bank", "Century", LenderCategory.MICROFINANCE, "885100", "Account Number", "💰"),
        KenyanLender("Sumac Microfinance Bank", "Sumac", LenderCategory.MICROFINANCE, "744744", "Account Number", "💰"),
        KenyanLender("U&I Microfinance Bank", "U&I", LenderCategory.MICROFINANCE, "525252", "Account Number", "💰"),
        KenyanLender("Rafiki Microfinance Bank", "Rafiki", LenderCategory.MICROFINANCE, "800300", "Account Number", "💰"),
        KenyanLender("Maisha Microfinance Bank", "Maisha", LenderCategory.MICROFINANCE, "622622", "Account Number", "💰"),
        KenyanLender("Choice Microfinance Bank", "Choice", LenderCategory.MICROFINANCE, "991100", "Account Number", "💰"),
        KenyanLender("Daraja Microfinance Bank", "Daraja", LenderCategory.MICROFINANCE, "665665", "Account Number", "💰"),
    )
}

/**
 * Mobile Lending Apps
 */
object KenyanMobileLenders {
    val mobileLenders = listOf(
        KenyanLender("M-Shwari", "M-Shwari", LenderCategory.MOBILE_LENDER, "329901", "Phone Number", "📱"),
        KenyanLender("KCB M-Pesa", "KCB M-Pesa", LenderCategory.MOBILE_LENDER, "226600", "Phone Number", "📱"),
        KenyanLender("Tala", "Tala", LenderCategory.MOBILE_LENDER, "851900", "Phone Number", "📱"),
        KenyanLender("Branch", "Branch", LenderCategory.MOBILE_LENDER, "998800", "Phone Number", "📱"),
        KenyanLender("Zenka", "Zenka", LenderCategory.MOBILE_LENDER, "290290", "Phone Number", "📱"),
        KenyanLender("Fuliza", "Fuliza", LenderCategory.MOBILE_LENDER, "329999", "Phone Number", "📱"),
        KenyanLender("Hustler Fund", "Hustler", LenderCategory.GOVERNMENT, "553300", "ID Number", "🏛️"),
        KenyanLender("Timiza", "Timiza", LenderCategory.MOBILE_LENDER, "303030", "Phone Number", "📱"),
        KenyanLender("Berry", "Berry", LenderCategory.MOBILE_LENDER, "775500", "Phone Number", "📱"),
        KenyanLender("Okash", "Okash", LenderCategory.MOBILE_LENDER, "882200", "Phone Number", "📱"),
        KenyanLender("Opesa", "Opesa", LenderCategory.MOBILE_LENDER, "556600", "Phone Number", "📱"),
        KenyanLender("iPesa", "iPesa", LenderCategory.MOBILE_LENDER, "334400", "Phone Number", "📱"),
    )
}

/**
 * Government loan programs
 */
object KenyanGovernmentLoans {
    val governmentLoans = listOf(
        KenyanLender("HELB - Higher Education Loans", "HELB", LenderCategory.GOVERNMENT, "200800", "Registration Number", "🏛️"),
        KenyanLender("Youth Enterprise Fund", "YEF", LenderCategory.GOVERNMENT, "551100", "ID Number", "🏛️"),
        KenyanLender("Women Enterprise Fund", "WEF", LenderCategory.GOVERNMENT, "552200", "ID Number", "🏛️"),
        KenyanLender("Uwezo Fund", "Uwezo", LenderCategory.GOVERNMENT, "553100", "Group Number", "🏛️"),
        KenyanLender("AGPO Access Fund", "AGPO", LenderCategory.GOVERNMENT, "554400", "Certificate Number", "🏛️"),
        KenyanLender("Biashara Kenya Fund", "Biashara", LenderCategory.GOVERNMENT, "555500", "Application Number", "🏛️"),
    )
}

/**
 * Get all lenders by category
 */
object KenyanLenders {

    fun getAllLenders(): List<KenyanLender> {
        return KenyanBanks.banks +
               KenyanSaccos.saccos +
               KenyanMicrofinance.mfis +
               KenyanMobileLenders.mobileLenders +
               KenyanGovernmentLoans.governmentLoans
    }

    fun getLendersByCategory(category: LenderCategory): List<KenyanLender> {
        return when (category) {
            LenderCategory.BANK -> KenyanBanks.banks
            LenderCategory.SACCO -> KenyanSaccos.saccos
            LenderCategory.MICROFINANCE -> KenyanMicrofinance.mfis
            LenderCategory.MOBILE_LENDER -> KenyanMobileLenders.mobileLenders
            LenderCategory.GOVERNMENT -> KenyanGovernmentLoans.governmentLoans
            else -> emptyList()
        }
    }

    fun getLenderByName(name: String): KenyanLender? {
        return getAllLenders().find {
            it.name.equals(name, ignoreCase = true) ||
            it.shortName.equals(name, ignoreCase = true)
        }
    }

    fun getPaybillForLender(name: String): String? {
        return getLenderByName(name)?.paybillNumber
    }

    fun searchLenders(query: String): List<KenyanLender> {
        if (query.isBlank()) return getAllLenders()
        return getAllLenders().filter {
            it.name.contains(query, ignoreCase = true) ||
            it.shortName.contains(query, ignoreCase = true)
        }
    }

    // For family/friends/employer/other - no paybill
    fun getOtherLenderCategories(): List<LenderCategory> {
        return listOf(
            LenderCategory.EMPLOYER,
            LenderCategory.FAMILY_FRIENDS,
            LenderCategory.OTHER
        )
    }
}

