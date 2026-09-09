package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.ExpenseDao
import com.example.data.model.Expense
import com.example.data.model.ExpenseCategory
import com.example.data.model.TransactionType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExpenseDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var expenseDao: ExpenseDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        expenseDao = db.expenseDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun testInsertAndRetrieveExpense() = runBlocking {
        val expense = Expense(
            title = "Groceries Market",
            amount = 75.50,
            type = TransactionType.EXPENSE,
            category = ExpenseCategory.GROCERIES,
            timestamp = System.currentTimeMillis(),
            rawSmsOrNote = "Card payment $75.50 at Market",
            currencyCode = "USD",
            accountRef = "••1234",
            isAutoParsed = true
        )

        val id = expenseDao.insertExpense(expense)
        assertTrue(id > 0)

        val allExpenses = expenseDao.getAllExpenses().first()
        assertEquals(1, allExpenses.size)

        val retrieved = allExpenses.first()
        assertEquals("Groceries Market", retrieved.title)
        assertEquals(75.50, retrieved.amount, 0.001)
        assertEquals(ExpenseCategory.GROCERIES, retrieved.category)
        assertEquals(TransactionType.EXPENSE, retrieved.type)
        assertEquals("••1234", retrieved.accountRef)
        assertTrue(retrieved.isAutoParsed)
    }

    @Test
    fun testDeleteExpense() = runBlocking {
        val expense = Expense(
            title = "Coffee Shop",
            amount = 5.25,
            type = TransactionType.EXPENSE,
            category = ExpenseCategory.FOOD_DINING
        )
        val id = expenseDao.insertExpense(expense)
        val listBefore = expenseDao.getAllExpenses().first()
        assertEquals(1, listBefore.size)

        expenseDao.deleteById(id)
        val listAfter = expenseDao.getAllExpenses().first()
        assertTrue(listAfter.isEmpty())
    }

    @Test
    fun testGetTotalExpenseSince() = runBlocking {
        val now = System.currentTimeMillis()
        val expense1 = Expense(
            title = "Lunch",
            amount = 20.00,
            type = TransactionType.EXPENSE,
            category = ExpenseCategory.FOOD_DINING,
            timestamp = now
        )
        val expense2 = Expense(
            title = "Dinner",
            amount = 35.00,
            type = TransactionType.EXPENSE,
            category = ExpenseCategory.FOOD_DINING,
            timestamp = now + 1000
        )
        val income = Expense(
            title = "Salary",
            amount = 1000.00,
            type = TransactionType.INCOME,
            category = ExpenseCategory.SALARY_INCOME,
            timestamp = now
        )

        expenseDao.insertExpense(expense1)
        expenseDao.insertExpense(expense2)
        expenseDao.insertExpense(income)

        val totalExpense = expenseDao.getTotalExpenseSince(now - 1000).first()
        assertNotNull(totalExpense)
        assertEquals(55.00, totalExpense!!, 0.001)

        val totalIncome = expenseDao.getTotalIncomeSince(now - 1000).first()
        assertNotNull(totalIncome)
        assertEquals(1000.00, totalIncome!!, 0.001)
    }
}
