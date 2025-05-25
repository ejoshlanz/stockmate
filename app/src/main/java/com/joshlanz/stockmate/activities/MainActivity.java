package com.joshlanz.stockmate.activities;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.joshlanz.stockmate.services.LowStockWorker;
import com.joshlanz.stockmate.R;
import com.joshlanz.stockmate.fragments.ExpensesFragment;
import com.joshlanz.stockmate.fragments.HomeFragment;
import com.joshlanz.stockmate.fragments.InventoryFragment;
import com.joshlanz.stockmate.fragments.SalesFragment;

import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private TextView title;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        createNotificationChannel();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        title = findViewById(R.id.title);

        findViewById(R.id.homeBtn).setOnClickListener(v -> loadFragment(new HomeFragment(), "Home"));
        findViewById(R.id.inventoryBtn).setOnClickListener(v -> loadFragment(new InventoryFragment(), "Inventory"));
        findViewById(R.id.salesBtn).setOnClickListener(v -> loadFragment(new SalesFragment(), "Sales"));
        findViewById(R.id.expenseBtn).setOnClickListener(v -> loadFragment(new ExpensesFragment(), "Expenses"));
        findViewById(R.id.exportBtn).setOnClickListener(v -> startActivity(new Intent(this, ExportActivity.class)));
        findViewById(R.id.profileBtn).setOnClickListener(v -> startActivity(new Intent(this, AccountActivity.class)));
        findViewById(R.id.notificationBtn).setOnClickListener(v -> startActivity(new Intent(this, LowStockActivity.class)));

        // Load default fragment
        if (savedInstanceState == null) {
            loadFragment(new HomeFragment(), "Home");
        }

        PeriodicWorkRequest lowStockCheckRequest =
                new PeriodicWorkRequest.Builder(LowStockWorker.class, 6, TimeUnit.HOURS)
                        .build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "low_stock_check",
                ExistingPeriodicWorkPolicy.KEEP,
                lowStockCheckRequest
        );

    }

    private void loadFragment(@NonNull Fragment fragment, @NonNull String titleText) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.content, fragment)
                .commit();
        title.setText(titleText);
    }

    private void createNotificationChannel() {
        // Create notification channel for Android 8.0 (API 26) and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "low_stock_channel",
                    "Low Stock Alerts",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notifies when an item's stock is low.");

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }

        // Request notification permission for Android 13+ (API 33)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                        100);
            }
        }
    }


}
