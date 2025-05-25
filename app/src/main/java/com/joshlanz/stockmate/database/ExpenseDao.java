package com.joshlanz.stockmate.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.joshlanz.stockmate.models.DailyTotal;
import com.joshlanz.stockmate.models.Expense;
import com.joshlanz.stockmate.models.Sales;

import java.util.List;

@Dao
public interface ExpenseDao {
    @Insert
    void insert(Expense expense);

    @Update
    void update(Expense expense);

    @Delete
    void delete(Expense expense);

    @Query("SELECT * FROM Expense ORDER BY id DESC")
    List<Expense> getAllExpenses();

    @Query("SELECT SUM(amount) FROM Expense")
    double getTotalExpenses();

    @Query("SELECT SUM(amount) FROM Expense WHERE category = :category")
    double getExpensesByCategory(String category);

    @Query("SELECT date, SUM(amount) as total FROM Expense GROUP BY date ORDER BY date")
    List<DailyTotal> getDailyExpenses();

    @Query("SELECT DISTINCT category FROM expense")
    List<String> getAllCategories();

    @Query("SELECT * FROM expense LIMIT :limit")
    List<Expense> getSampleExpenses(int limit);
}
