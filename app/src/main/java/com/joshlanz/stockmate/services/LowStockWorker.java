package com.joshlanz.stockmate.services;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.joshlanz.stockmate.R;
import com.joshlanz.stockmate.database.AppDatabase;
import com.joshlanz.stockmate.models.Item;

import java.util.List;

public class LowStockWorker extends Worker {

    public LowStockWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        AppDatabase db = AppDatabase.getInstance(getApplicationContext());
        List<Item> lowStockItems = db.itemDao().getItemsWithLowStock(5); // threshold 5

        if (!lowStockItems.isEmpty()) {
            // Build notification message with item names
            StringBuilder message = new StringBuilder("Low stock: ");
            for (int i = 0; i < lowStockItems.size(); i++) {
                message.append(lowStockItems.get(i).getTitle());
                if (i != lowStockItems.size() - 1) {
                    message.append(", ");
                }
            }

            // Build and send the notification
            NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), "low_stock_channel")
                    .setSmallIcon(R.drawable.ic_warning) // replace with your icon
                    .setContentTitle("Low Stock Alert")
                    .setContentText(message.toString())
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(message.toString()))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true);

            NotificationManagerCompat manager = NotificationManagerCompat.from(getApplicationContext());
            manager.notify(1001, builder.build());
        }

        return Result.success();
    }

}

