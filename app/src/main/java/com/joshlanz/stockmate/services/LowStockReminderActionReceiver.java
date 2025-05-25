package com.joshlanz.stockmate.services;

import static com.joshlanz.stockmate.fragments.SalesFragment.scheduleLowStockReminder;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import com.joshlanz.stockmate.database.AppDatabase;
import com.joshlanz.stockmate.models.Item;

import java.util.ArrayList;

public class LowStockReminderActionReceiver extends BroadcastReceiver {
    public static final String ACTION_REMIND_ME = "com.joshlanz.stockmate.action.REMIND_ME";
    public static final String EXTRA_ITEM_IDS = "extra_item_ids";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null && ACTION_REMIND_ME.equals(intent.getAction())) {
            ArrayList<Integer> itemIds = intent.getIntegerArrayListExtra(EXTRA_ITEM_IDS);
            if (itemIds != null) {
                for (int itemId : itemIds) {
                    fetchItemById(context, itemId, new ItemFetchCallback() {
                        @Override
                        public void onItemFetched(Item item) {
                            if (item != null) {
                                long delayMillis = 24 * 60 * 60 * 1000; // 24 hours delay
                                scheduleLowStockReminder(context, item, delayMillis);

                                long triggerTime = System.currentTimeMillis() + delayMillis;
                                long millisLeft = triggerTime - System.currentTimeMillis();

                                // Convert millisLeft to hours and minutes
                                int hoursLeft = (int) (millisLeft / (1000 * 60 * 60));
                                int minutesLeft = (int) ((millisLeft / (1000 * 60)) % 60);

                                String timeLeftStr;
                                if (hoursLeft > 0) {
                                    timeLeftStr = hoursLeft + "h " + minutesLeft + "m";
                                } else {
                                    timeLeftStr = minutesLeft + " minutes";
                                }

                                NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                                if (notificationManager != null) {
                                    notificationManager.cancel(9999);
                                }

                                // Show toast with time left
                                String toastMsg = "Reminder set for " + item.getTitle() + " in " + timeLeftStr;
                                Toast.makeText(context, toastMsg, Toast.LENGTH_LONG).show();
                            }
                        }
                    });
                }
            }
        }
    }




    private void fetchItemById(final Context context, final int itemId, final ItemFetchCallback callback) {
        new AsyncTask<Void, Void, Item>() {
            @Override
            protected Item doInBackground(Void... voids) {
                return AppDatabase.getInstance(context).itemDao().getItemById(itemId);
            }

            @Override
            protected void onPostExecute(Item item) {
                callback.onItemFetched(item);
            }
        }.execute();
    }

    interface ItemFetchCallback {
        void onItemFetched(Item item);
    }

}
