package com.example

import com.example.data.model.ExpenseCategory
import com.example.data.model.TransactionType
import com.example.data.parser.SmsTransactionParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SmsTransactionParserTest {

    @Test
    fun testParseChaseDebitSms() {
        val sms = "Chase: You spent \$48.25 at TRADER JOE'S with card ending 4102 on 09/08. Remaining balance \$2,450.00."
        val result = SmsTransactionParser.parse(sms, "USD")

        assertNotNull(result)
        result!!
        assertEquals(48.25, result.amount, 0.001)
        assertEquals(TransactionType.EXPENSE, result.type)
        assertEquals(ExpenseCategory.GROCERIES, result.category)
        assertEquals("••4102", result.accountRef)
        assertTrue(result.confidence > 0.8f)
    }

    @Test
    fun testParseSalaryCreditSms() {
        val sms = "Direct deposit received: \$3,500.00 from TECH CORP into Checking A/C ••9012."
        val result = SmsTransactionParser.parse(sms, "USD")

        assertNotNull(result)
        result!!
        assertEquals(3500.00, result.amount, 0.001)
        assertEquals(TransactionType.INCOME, result.type)
        assertEquals(ExpenseCategory.SALARY_INCOME, result.category)
        assertTrue(result.confidence >= 0.9f)
    }

    @Test
    fun testParseHdfcRupeeSms() {
        val sms = "HDFC Bank: INR 1,450.00 debited from A/C **8821 on 08-SEP at SWIGGY RESTAURANT."
        val result = SmsTransactionParser.parse(sms, "INR")

        assertNotNull(result)
        result!!
        assertEquals(1450.00, result.amount, 0.001)
        assertEquals(TransactionType.EXPENSE, result.type)
        assertEquals(ExpenseCategory.FOOD_DINING, result.category)
        assertEquals("••8821", result.accountRef)
    }
}
