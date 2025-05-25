package com.joshlanz.stockmate.services;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.joshlanz.stockmate.R;

public class LowStockAlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        int itemId = intent.getIntExtra("itemId", -1);
        String itemTitle = intent.getStringExtra("itemTitle");

        if (itemId == -1 || itemTitle == null) return;

        // Ensure notification channel exists
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "low_stock_channel",
                    "Low Stock Alerts",
                    NotificationManager.IMPORTANCE_HIGH
            );
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "low_stock_channel")
                .setSmallIcon(R.drawable.ic_warning)
                .setContentTitle("Still Low in Stock")
                .setContentText(itemTitle + " is still low in stock.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        NotificationManagerCompat.from(context).notify(itemId + 10000, builder.build());
    }
}
