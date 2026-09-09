package com.example.data.parser

import com.example.data.model.ExpenseCategory
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import java.util.regex.Pattern

data class ParsedTransaction(
    val title: String,
    val amount: Double,
    val type: TransactionType,
    val category: ExpenseCategory,
    val currencyCode: String,
    val accountRef: String?,
    val confidence: Float,
    val rawText: String
) {
    fun toEntity(): TransactionEntity {
        return TransactionEntity(
            title = title,
            amount = amount,
            type = type,
            category = category,
            rawSmsOrNote = rawText,
            currencyCode = currencyCode,
            accountRef = accountRef,
            isAutoParsed = true,
            timestamp = System.currentTimeMillis()
        )
    }
}

object SmsTransactionParser {

    private val CURRENCY_REGEX = Pattern.compile(
        """(?i)(?:[$€£₹¥]|USD|EUR|GBP|INR|CAD|AUD|JPY)\s*([0-9]{1,3}(?:,[0-9]{3})*(?:\.[0-9]{1,2})?|[0-9]+(?:\.[0-9]{1,2})?)"""
    )
    private val REVERSE_CURRENCY_REGEX = Pattern.compile(
        """([0-9]{1,3}(?:,[0-9]{3})*(?:\.[0-9]{1,2})?|[0-9]+(?:\.[0-9]{1,2})?)\s*(?:[$€£₹¥]|USD|EUR|GBP|INR|CAD|AUD|JPY)"""
    )

    private val DEBIT_KEYWORDS = listOf(
        "debited", "debit", "spent", "paid", "withdrawn", "purchase of", "charged", "transferred to", "sent to", "dr "
    )
    private val CREDIT_KEYWORDS = listOf(
        "credited", "credit", "received", "refund", "deposited", "salary", "added to", "cashback", "cr "
    )

    private val ACCOUNT_PATTERNS = listOf(
        Pattern.compile("""(?i)(?:a/c|acct|account|card)\s*(?:no\.?|ending)?\s*(?:[x*]+|ending\s+in\s+)?([0-9]{3,6})"""),
        Pattern.compile("""(?i)(?:xx|[*]{2,})([0-9]{3,4})""")
    )

    private val MERCHANT_PATTERNS = listOf(
        Pattern.compile("""(?i)(?:at|to|for|vpa|merchant)\s+([A-Za-z0-9&'._\- ]{2,30}?)(?:\s+on|\s+ref|\s+avail|\s+balance|\s+bal|\s+via|\.|\$)"""),
        Pattern.compile("""(?i)info:\s*([A-Za-z0-9&'._\- ]{2,30})"""),
        Pattern.compile("""(?i)towards\s+([A-Za-z0-9&'._\- ]{2,30})""")
    )

    fun parse(smsText: String, defaultCurrency: String = "USD"): ParsedTransaction? {
        val cleanText = smsText.trim()
        if (cleanText.isEmpty()) return null

        val lower = cleanText.lowercase()

        // 1. Determine Transaction Type
        var isDebit = false
        var isCredit = false
        for (kw in DEBIT_KEYWORDS) {
            if (lower.contains(kw)) {
                isDebit = true
                break
            }
        }
        for (kw in CREDIT_KEYWORDS) {
            if (lower.contains(kw)) {
                isCredit = true
                break
            }
        }

        val type = when {
            isDebit && !isCredit -> TransactionType.EXPENSE
            isCredit && !isDebit -> TransactionType.INCOME
            isCredit -> TransactionType.INCOME
            else -> TransactionType.EXPENSE
        }

        // 2. Extract Amount & Currency
        var detectedAmount = 0.0
        var detectedCurrency = defaultCurrency

        val matcher = CURRENCY_REGEX.matcher(cleanText)
        if (matcher.find()) {
            val symbolOrCode = cleanText.substring(matcher.start(), matcher.end()).trim()
            val numStr = matcher.group(1)?.replace(",", "") ?: "0"
            detectedAmount = numStr.toDoubleOrNull() ?: 0.0
            detectedCurrency = mapCurrency(symbolOrCode, defaultCurrency)
        } else {
            val revMatcher = REVERSE_CURRENCY_REGEX.matcher(cleanText)
            if (revMatcher.find()) {
                val numStr = revMatcher.group(1)?.replace(",", "") ?: "0"
                detectedAmount = numStr.toDoubleOrNull() ?: 0.0
                val symbolOrCode = cleanText.substring(revMatcher.start(), revMatcher.end()).trim()
                detectedCurrency = mapCurrency(symbolOrCode, defaultCurrency)
            }
        }

        if (detectedAmount <= 0.0) {
            // fallback: find any standalone decimal number
            val genericNumMatcher = Pattern.compile("""\b([0-9]+(?:\.[0-9]{1,2})?)\b""").matcher(cleanText)
            while (genericNumMatcher.find()) {
                val candidate = genericNumMatcher.group(1)?.toDoubleOrNull() ?: 0.0
                if (candidate > 0.99 && candidate < 500000.0) {
                    detectedAmount = candidate
                    break
                }
            }
        }

        if (detectedAmount <= 0.0) {
            return null // Unable to reliably determine transaction amount
        }

        // 3. Extract Merchant / Vendor
        var merchant = extractMerchant(cleanText)
        if (merchant.isBlank()) {
            merchant = if (type == TransactionType.INCOME) "Income Deposit" else "General Expense"
        }

        // 4. Extract Account
        val accountRef = extractAccount(cleanText)

        // 5. Categorize based on merchant & keywords
        val category = categorizeMerchant(merchant, lower, type)

        val confidence = when {
            (isDebit || isCredit) && merchant.length > 3 && detectedAmount > 0 -> 0.95f
            detectedAmount > 0 -> 0.75f
            else -> 0.50f
        }

        return ParsedTransaction(
            title = merchant,
            amount = detectedAmount,
            type = type,
            category = category,
            currencyCode = detectedCurrency,
            accountRef = accountRef,
            confidence = confidence,
            rawText = cleanText
        )
    }

    private fun mapCurrency(text: String, defaultCurrency: String): String {
        val upper = text.uppercase()
        return when {
            upper.contains("$") || upper.contains("USD") -> "USD"
            upper.contains("€") || upper.contains("EUR") -> "EUR"
            upper.contains("£") || upper.contains("GBP") -> "GBP"
            upper.contains("₹") || upper.contains("INR") -> "INR"
            upper.contains("¥") || upper.contains("JPY") -> "JPY"
            upper.contains("CAD") -> "CAD"
            upper.contains("AUD") -> "AUD"
            else -> defaultCurrency
        }
    }

    private fun extractMerchant(text: String): String {
        for (pattern in MERCHANT_PATTERNS) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                val raw = matcher.group(1)?.trim() ?: ""
                val clean = raw.replace(Regex("""(?i)(ref|avl|bal|upi|txn|acct|via).*"""), "").trim()
                if (clean.length in 2..28) {
                    return clean.split(" ").joinToString(" ") { word ->
                        word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                    }
                }
            }
        }
        return ""
    }

    private fun extractAccount(text: String): String? {
        for (pattern in ACCOUNT_PATTERNS) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                val digits = matcher.group(1)
                return "••$digits"
            }
        }
        return null
    }

    private fun categorizeMerchant(merchant: String, fullText: String, type: TransactionType): ExpenseCategory {
        if (type == TransactionType.INCOME) return ExpenseCategory.SALARY_INCOME

        val combined = "${merchant.lowercase()} $fullText"
        return when {
            combined.contains("uber") || combined.contains("lyft") || combined.contains("fuel") ||
                    combined.contains("chevron") || combined.contains("shell") || combined.contains("transit") ||
                    combined.contains("metro") || combined.contains("airline") || combined.contains("train") ->
                ExpenseCategory.TRAVEL_TRANSIT

            combined.contains("starbucks") || combined.contains("mcdonald") || combined.contains("chipotle") ||
                    combined.contains("doordash") || combined.contains("ubereats") || combined.contains("restaurant") ||
                    combined.contains("cafe") || combined.contains("pizza") || combined.contains("burger") ||
                    combined.contains("subway") || combined.contains("diner") || combined.contains("food") ->
                ExpenseCategory.FOOD_DINING

            combined.contains("walmart") || combined.contains("target") || combined.contains("trader joe") ||
                    combined.contains("whole foods") || combined.contains("kroger") || combined.contains("aldi") ||
                    combined.contains("supermarket") || combined.contains("grocery") ->
                ExpenseCategory.GROCERIES

            combined.contains("netflix") || combined.contains("spotify") || combined.contains("disney") ||
                    combined.contains("cinema") || combined.contains("theatre") || combined.contains("steam") ||
                    combined.contains("youtube") || combined.contains("game") ->
                ExpenseCategory.ENTERTAINMENT

            combined.contains("amazon") || combined.contains("apple") || combined.contains("zara") ||
                    combined.contains("nike") || combined.contains("ebay") || combined.contains("clothing") ||
                    combined.contains("store") || combined.contains("shop") ->
                ExpenseCategory.SHOPPING

            combined.contains("electric") || combined.contains("water") || combined.contains("internet") ||
                    combined.contains("verizon") || combined.contains("at&t") || combined.contains("bill") ||
                    combined.contains("utility") || combined.contains("rent") || combined.contains("insurance") ->
                ExpenseCategory.BILLS_UTILITIES

            combined.contains("gym") || combined.contains("fitness") || combined.contains("pharmacy") ||
                    combined.contains("cvs") || combined.contains("walgreens") || combined.contains("hospital") ||
                    combined.contains("doctor") ->
                ExpenseCategory.HEALTH

            combined.contains("fidelity") || combined.contains("vanguard") || combined.contains("crypto") ||
                    combined.contains("robinhood") || combined.contains("stocks") ->
                ExpenseCategory.INVESTMENTS

            else -> ExpenseCategory.OTHER
        }
    }

    // Sample bank SMS messages for quick 1-tap testing
    val SAMPLE_SMS_BANK_MESSAGES = listOf(
        "Chase: You made a $48.25 purchase at WHOLE FOODS with card ending in 4102 on 09/08. Available bal: $2,431.10.",
        "Your A/C XX8921 is debited by USD 14.99 on 08-Sep for Netflix Subscription. Bal: $1,840.50.",
        "Alert: $12.50 spent on card **7734 at Starbucks Coffee. Ref# 938210.",
        "Barclays Alert: A debit of £24.80 was made at Uber Rides London via card 9012. Avail bal: £850.00.",
        "HDFC Bank: Rs 1,450.00 debited from a/c **4392 on 09-09-26 towards SWIGGY. UPI ref 49201938.",
        "PayPal: You received $1,250.00 from Acronym Studio for Consulting Services (Salary & Income).",
        "Citi Alert: $82.40 spent at CHEVRON GAS STATION on card ending 3829 on Sep 07.",
        "Notification: €32.50 was debited for dinner at Osteria Romana on Card XX1109."
    )
}
