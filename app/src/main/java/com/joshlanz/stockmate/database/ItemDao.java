package com.joshlanz.stockmate.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.joshlanz.stockmate.models.Item;

import java.util.List;

@Dao
public interface ItemDao {

    @Insert
    void insert(Item item);

    @Query("SELECT * FROM items ORDER BY id DESC")
    List<Item> getAllItems();

    @androidx.room.Update
    void update(Item item);

    @androidx.room.Delete
    void delete(Item item);

    @Query("SELECT DISTINCT category FROM items")
    List<String> getAllCategories();

    @Query("SELECT * FROM items LIMIT :limit")
    List<Item> getSampleItems(int limit);

    @Query("SELECT * FROM items WHERE stock <= :threshold")
    List<Item> getItemsWithLowStock(int threshold);

    @Query("SELECT * FROM items WHERE id = :itemId LIMIT 1")
    Item getItemById(int itemId);
}

