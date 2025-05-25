package com.joshlanz.stockmate.models;

import android.net.Uri;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "items")
public class Item {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String title;
    public String category;
    public int stock;
    public double price;
    public String imageUri;

    public Item(String title, String category, int stock, double price, String imageUri) {
        this.title = title;
        this.category = category;
        this.stock = stock;
        this.price = price;
        this.imageUri = imageUri;
    }

    public int getId() {
        return id;
    }
    public Uri getImageUri() {
        return Uri.parse(imageUri);
    }

    public String getTitle() { return title; }
    public String getCategory() { return category; }
    public int getStock() { return stock; }
    public double getPrice() { return price; }

    public void setTitle(String title) { this.title = title; }
    public void setCategory(String category) { this.category = category; }
    public void setStock(int stock) { this.stock = stock; }
    public void setPrice(double price) { this.price = price; }

    public void setImageUri(Uri imageUri) {
        this.imageUri = imageUri.toString(); // ✅ Convert to string
    }

    public void setImageUri(String imageUri) {
        this.imageUri = imageUri;
    }
}
