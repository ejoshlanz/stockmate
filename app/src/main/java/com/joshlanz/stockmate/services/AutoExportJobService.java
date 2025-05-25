package com.joshlanz.stockmate.services;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.core.content.FileProvider;

import com.joshlanz.stockmate.R;
import com.joshlanz.stockmate.database.AppDatabase;
import com.joshlanz.stockmate.models.Expense;
import com.joshlanz.stockmate.models.Item;
import com.joshlanz.stockmate.models.Sales;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

public class AutoExportJobService extends JobService {

    private static final String CHANNEL_ID = "export_channel";
    private static final int NOTIFICATION_ID = 1001;

    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    public boolean onStartJob(JobParameters params) {
        new Thread(() -> {
            try {
                AppDatabase db = AppDatabase.getDatabase(getApplicationContext());

                List<Sales> salesList = db.salesDao().getAllSales();
                List<Item> itemList = db.itemDao().getAllItems();
                List<Expense> expenseList = db.expenseDao().getAllExpenses();

                // Create export file in app-specific documents folder
                File exportFile = new File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "AutoExport_StockMate.json");

                FileOutputStream outputStream = new FileOutputStream(exportFile);
                String json = buildJson(salesList, itemList, expenseList);
                outputStream.write(json.getBytes());
                outputStream.flush();
                outputStream.close();

                showNotification("Auto Export Complete", "Data was exported to: " + exportFile.getName(), exportFile);

                handler.post(() -> Toast.makeText(getApplicationContext(), "Auto-export completed", Toast.LENGTH_SHORT).show());
            } catch (Exception e) {
                e.printStackTrace();
                handler.post(() -> Toast.makeText(getApplicationContext(), "Export failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }

            jobFinished(params, false);
        }).start();

        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return true;
    }

    private String buildJson(List<Sales> salesList, List<Item> itemList, List<Expense> expenseList) {
        StringBuilder jsonBuilder = new StringBuilder();
        jsonBuilder.append("{\n");

        // Sales
        jsonBuilder.append("  \"Sales\": [\n");
        for (int i = 0; i < salesList.size(); i++) {
            Sales s = salesList.get(i);
            jsonBuilder.append("    {\n");
            jsonBuilder.append("      \"ID\": ").append(s.getId()).append(",\n");
            jsonBuilder.append("      \"Title\": \"").append(s.getTitle().replace("\"", "\\\"")).append("\",\n");
            jsonBuilder.append("      \"Price\": ").append(s.getPrice()).append(",\n");
            jsonBuilder.append("      \"Time\": \"").append(s.getTime().replace("\"", "\\\"")).append("\"\n");
            jsonBuilder.append("    }").append(i < salesList.size() - 1 ? "," : "").append("\n");
        }
        jsonBuilder.append("  ],\n");

        // Items
        jsonBuilder.append("  \"Items\": [\n");
        for (int i = 0; i < itemList.size(); i++) {
            Item item = itemList.get(i);
            jsonBuilder.append("    {\n");
            jsonBuilder.append("      \"ID\": ").append(item.getId()).append(",\n");
            jsonBuilder.append("      \"Title\": \"").append(item.getTitle().replace("\"", "\\\"")).append("\",\n");
            jsonBuilder.append("      \"Category\": \"").append(item.getCategory().replace("\"", "\\\"")).append("\",\n");
            jsonBuilder.append("      \"Stock\": ").append(item.getStock()).append(",\n");
            jsonBuilder.append("      \"Price\": ").append(item.getPrice()).append("\n");
            jsonBuilder.append("    }").append(i < itemList.size() - 1 ? "," : "").append("\n");
        }
        jsonBuilder.append("  ],\n");

        // Expenses
        jsonBuilder.append("  \"Expenses\": [\n");
        for (int i = 0; i < expenseList.size(); i++) {
            Expense e = expenseList.get(i);
            jsonBuilder.append("    {\n");
            jsonBuilder.append("      \"ID\": ").append(e.getId()).append(",\n");
            jsonBuilder.append("      \"Title\": \"").append(e.getTitle().replace("\"", "\\\"")).append("\",\n");
            jsonBuilder.append("      \"Amount\": ").append(e.getAmount()).append(",\n");
            jsonBuilder.append("      \"Category\": \"").append(e.getCategory().replace("\"", "\\\"")).append("\",\n");
            jsonBuilder.append("      \"Date\": \"").append(e.getDate().replace("\"", "\\\"")).append("\"\n");
            jsonBuilder.append("    }").append(i < expenseList.size() - 1 ? "," : "").append("\n");
        }
        jsonBuilder.append("  ]\n");

        jsonBuilder.append("}\n");
        return jsonBuilder.toString();
    }

    private void showNotification(String title, String content, File file) {
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Auto Export Notifications",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            manager.createNotificationChannel(channel);
        }

        Uri fileUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
        Intent openIntent = new Intent(Intent.ACTION_VIEW);
        openIntent.setDataAndType(fileUri, "application/json");
        openIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(content)
                .setSmallIcon(R.drawable.ic_export) // make sure this icon exists
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        manager.notify(NOTIFICATION_ID, builder.build());
    }
}
