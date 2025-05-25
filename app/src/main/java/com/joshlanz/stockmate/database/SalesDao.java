package com.joshlanz.stockmate.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.joshlanz.stockmate.models.DailyTotal;
import com.joshlanz.stockmate.models.Sales;

import java.util.List;

@Dao
public interface SalesDao {

    @Insert
    void insert(Sales sales);

    @Update
    void update(Sales sales);

    @Delete
    void delete(Sales sales);

    @Query("SELECT * FROM sales ORDER BY id DESC")
    List<Sales> getAllSales();

    @Query("SELECT * FROM sales WHERE id = :id")
    Sales getSalesById(int id);

    @Query("SELECT SUM(price) FROM Sales")
    double getTotalIncome();

    @Query("SELECT time, SUM(price) as total FROM Sales GROUP BY time ORDER BY time")
    List<DailyTotal> getDailyIncome();

    @Query("SELECT * FROM sales LIMIT :limit")
    List<Sales> getSampleSales(int limit);
}

