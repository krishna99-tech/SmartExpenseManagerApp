package com.expensemanager.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.expensemanager.data.model.Expense
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY date DESC")
    fun observeAll(): Flow<List<Expense>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(expense: Expense): Long

    @Delete
    suspend fun delete(expense: Expense)

    @Query("SELECT * FROM expenses WHERE date >= :start AND date < :end ORDER BY date DESC")
    suspend fun getBetween(start: Long, end: Long): List<Expense>

    @Query("SELECT * FROM expenses WHERE isRecurring = 1")
    suspend fun getRecurringExpenses(): List<Expense>
}
