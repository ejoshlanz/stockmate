package com.joshlanz.stockmate.database;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.joshlanz.stockmate.models.Expense;
import com.joshlanz.stockmate.models.Item;
import com.joshlanz.stockmate.models.RecentActivity;
import com.joshlanz.stockmate.models.Sales;

@Database(entities = {Item.class, Sales.class, Expense.class, RecentActivity.class}, version = 7)

public abstract class AppDatabase extends RoomDatabase {

    public abstract ItemDao itemDao();
    public abstract SalesDao salesDao();
    public abstract ExpenseDao expenseDao();
    public abstract RecentActivityDao recentActivityDao();
   private static volatile AppDatabase INSTANCE;;

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "inventory_database")
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
    public static AppDatabase getDatabase(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    // Use the RoomDatabase.Builder instead of databaseBuilder
                    INSTANCE = new RoomDatabase.Builder<>(context.getApplicationContext(), AppDatabase.class, "inventory_database")
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
