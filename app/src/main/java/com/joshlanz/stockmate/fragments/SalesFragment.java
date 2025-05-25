package com.joshlanz.stockmate.fragments;

import android.Manifest;
import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.joshlanz.stockmate.services.LowStockAlarmReceiver;
import com.joshlanz.stockmate.services.LowStockReminderActionReceiver;
import com.joshlanz.stockmate.statics.Constants;
import com.joshlanz.stockmate.adapters.ItemSelectorAdapter;
import com.joshlanz.stockmate.database.AppDatabase;
import com.joshlanz.stockmate.database.RecentActivityDao;
import com.joshlanz.stockmate.database.SalesDao;
import com.joshlanz.stockmate.R;
import com.joshlanz.stockmate.adapters.SalesAdapter;
import com.joshlanz.stockmate.models.Item;
import com.joshlanz.stockmate.models.RecentActivity;
import com.joshlanz.stockmate.models.Sales;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SalesFragment extends Fragment implements SalesAdapter.OnSalesActionListener {
    private AppDatabase db;
    private SalesDao salesDao;
    private SalesAdapter adapter;
    private RecyclerView recyclerView;
    private final List<Sales> salesList = new ArrayList<>();
    private final List<Sales> filteredList = new ArrayList<>();
    private BarChart barChart;
    private TextView totalSalesTextView, totalTransactionsTextView, percentageTextView, currentDateTextView;
    private RecentActivityDao recentActivityDao;

    private ActivityResultLauncher<String> requestNotificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    // Permission granted — you can send the notification now if needed
                    // You might want to call sendLowStockNotification() again here if you have a pending notification
                } else {
                    Toast.makeText(requireContext(), "Notification permission denied", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        db = AppDatabase.getDatabase(requireContext());
        salesDao = db.salesDao();
        recentActivityDao = db.recentActivityDao();

        // Inflate the layout
        View view = inflater.inflate(R.layout.fragment_sales, container, false);

        // Initialize the UI elements
        totalSalesTextView = view.findViewById(R.id.totalSales);
        totalTransactionsTextView = view.findViewById(R.id.totalTransactions);
        percentageTextView = view.findViewById(R.id.percentage);
        currentDateTextView = view.findViewById(R.id.currentDate);


        // Set the current date
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault());
        String currentDate = sdf.format(Calendar.getInstance().getTime());
        currentDateTextView.setText(currentDate);

        // RecyclerView for Sales List
        recyclerView = view.findViewById(R.id.rv_sales);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new SalesAdapter(getContext(), filteredList, this);

        recyclerView.setAdapter(adapter);

        // Floating Action Button for adding sales
        FloatingActionButton fabAdd = view.findViewById(R.id.fab_add);
        fabAdd.setOnClickListener(v -> showAddSalesDialog());

        // BarChart for Sales Trend
        barChart = view.findViewById(R.id.sales_trend_chart);
        setupBarChart();

        // Load sales data
        loadSales();

        return view;
    }

    private void setupBarChart() {
        // Setup for the BarChart (using MPAndroidChart)
        barChart.setDrawBarShadow(false);
        barChart.setDrawValueAboveBar(true);
    }

    private void showEditSalesDialog(Sales sales) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Edit Sales");

        // Inflate the dialog layout
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_edit_sales, null);
        EditText saleTitle = dialogView.findViewById(R.id.et_title);
        EditText salePrice = dialogView.findViewById(R.id.et_price);
        TextView tvDate = dialogView.findViewById(R.id.tv_date); // Used for displaying date

        // Set sale time from existing sales data
        tvDate.setText(sales.getTime());

        final Calendar calendar = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        tvDate.setOnClickListener(v -> {
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(requireContext(),
                    (view, selectedYear, selectedMonth, selectedDay) -> {
                        calendar.set(selectedYear, selectedMonth, selectedDay);
                        tvDate.setText(sdf.format(calendar.getTime()));
                    }, year, month, day);
            datePickerDialog.show();
        });

        // Pre-fill title and price
        saleTitle.setText(sales.getTitle());
        salePrice.setText(String.valueOf(sales.getPrice()));

        builder.setView(dialogView);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String title = saleTitle.getText().toString();
            String date = tvDate.getText().toString();
            String priceStr = salePrice.getText().toString();

            if (TextUtils.isEmpty(title) || TextUtils.isEmpty(date) || TextUtils.isEmpty(priceStr)) {
                Toast.makeText(getContext(), "All fields must be filled", Toast.LENGTH_SHORT).show();
                return;
            }

            double price = Double.parseDouble(priceStr);
            sales.setTitle(title);
            sales.setTime(date);
            sales.setPrice(price);

            // Update adapter and database
            adapter.notifyItemChanged(salesList.indexOf(sales));

            new Thread(() -> {
                salesDao.update(sales);
                requireActivity().runOnUiThread(this::loadSales);
                logRecentActivity(R.drawable.ic_edit, "Sale Updated",  title +  " • ₱" + price);
            }).start();

            Toast.makeText(getContext(), "Sales updated successfully", Toast.LENGTH_SHORT).show();
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }


    private void showDeleteConfirmation(Sales sales) {
        new AlertDialog.Builder(getContext())
                .setTitle("Confirm Deletion")
                .setMessage("Are you sure you want to delete this sales record?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    salesList.remove(sales);
                    filteredList.remove(sales);
                    adapter.notifyDataSetChanged();

                    new Thread(() -> {
                        salesDao.delete(sales);
                        requireActivity().runOnUiThread(() -> loadSales());
                        logRecentActivity(R.drawable.ic_bin, "Sale Deleted",  sales.getTitle() +  " • ₱" + sales.getPrice());
                    }).start();

                    Toast.makeText(getContext(), "Sales deleted successfully", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void showAddSalesDialog() {
        new Thread(() -> {
            List<Item> allItems = AppDatabase.getInstance(requireContext()).itemDao().getAllItems();

            requireActivity().runOnUiThread(() -> {
                AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
                LayoutInflater inflater = requireActivity().getLayoutInflater();
                View dialogView = inflater.inflate(R.layout.dialog_add_sales, null);

                EditText etTitle = dialogView.findViewById(R.id.et_title);
                TextView tvDate = dialogView.findViewById(R.id.tv_date);
                RecyclerView rvSelectedItems = dialogView.findViewById(R.id.rvItemSelector);
                Button btnAddItem = dialogView.findViewById(R.id.btnAddItem);

                final Calendar calendar = Calendar.getInstance();
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                tvDate.setText(sdf.format(calendar.getTime()));
                tvDate.setOnClickListener(v -> {
                    int year = calendar.get(Calendar.YEAR);
                    int month = calendar.get(Calendar.MONTH);
                    int day = calendar.get(Calendar.DAY_OF_MONTH);

                    DatePickerDialog datePickerDialog = new DatePickerDialog(requireContext(),
                            (view, selectedYear, selectedMonth, selectedDay) -> {
                                calendar.set(selectedYear, selectedMonth, selectedDay);
                                tvDate.setText(sdf.format(calendar.getTime()));
                            }, year, month, day);
                    datePickerDialog.show();
                });

                List<ItemSelectorAdapter.SelectedItem> selectedItems = new ArrayList<>();

                ItemSelectorAdapter selectedAdapter = new ItemSelectorAdapter(selectedItems);
                rvSelectedItems.setLayoutManager(new LinearLayoutManager(requireContext()));
                rvSelectedItems.setAdapter(selectedAdapter);


                btnAddItem.setOnClickListener(v -> {
                    // Show dialog or bottom sheet to pick items from allItems
                    showSelectItemsDialog(allItems, selectedItems, selectedAdapter);
                });

                builder.setView(dialogView)
                        .setTitle("Add Sale")
                        .setPositiveButton("Add", null)
                        .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

                AlertDialog dialog = builder.create();
                dialog.setOnShowListener(dialogInterface -> {
                    Button btnAdd = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                    btnAdd.setOnClickListener(v -> {
                        String title = etTitle.getText().toString().trim();
                        String date = tvDate.getText().toString();

                        if (title.isEmpty()) {
                            Toast.makeText(requireContext(), "Title is required", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        if (selectedItems.isEmpty()) {
                            Toast.makeText(requireContext(), "Select at least one item", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        double totalPrice = selectedItems.stream()
                                .mapToDouble(si -> si.item.getPrice() * si.quantity)
                                .sum();

                        Sales newSale = new Sales(title, totalPrice, date);

                        new Thread(() -> {
                            salesDao.insert(newSale);

                            List<Item> lowStockItems = new ArrayList<>();

                            for (ItemSelectorAdapter.SelectedItem si : selectedItems) {
                                Item item = si.item;
                                int newQuantity = item.getStock() - si.quantity;
                                if (newQuantity < 0) newQuantity = 0;

                                item.setStock(newQuantity);
                                AppDatabase.getInstance(requireContext()).itemDao().update(item);

                                if (newQuantity < Constants.LOW_STOCK_THRESHOLD) {
                                    lowStockItems.add(item);
                                }

                            }

                            if (!lowStockItems.isEmpty()) {
                                requireActivity().runOnUiThread(() -> {
                                    notifyLowStock(requireContext(), lowStockItems);
                                });
                            }

                            requireActivity().runOnUiThread(() -> {
                                salesList.add(newSale);
                                adapter.notifyDataSetChanged();
                                loadSales();
                                logRecentActivity(R.drawable.ic_currency, "Sale Completed", title + " • ₱" + totalPrice);
                                dialog.dismiss();
                            });
                        }).start();
                    });
                });

                dialog.show();
            });
        }).start();
    }

    private void showSelectItemsDialog(List<Item> allItems, List<ItemSelectorAdapter.SelectedItem> selectedItems, ItemSelectorAdapter selectedAdapter) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Select Items");

        // Create a simple list adapter with all items for selection
        String[] itemNames = allItems.stream().map(Item::getTitle).toArray(String[]::new);

        boolean[] checkedItems = new boolean[allItems.size()];

        builder.setMultiChoiceItems(itemNames, checkedItems, (dialog, which, isChecked) -> {
            // no immediate action needed here
        });

        builder.setPositiveButton("Add Selected", (dialog, which) -> {
            AlertDialog alertDialog = (AlertDialog) dialog;
            ListView listView = alertDialog.getListView();

            for (int i = 0; i < listView.getCount(); i++) {
                if (listView.isItemChecked(i)) {
                    Item selectedItem = allItems.get(i);

                    // Check if already added; if not, add to selectedItems with default quantity 1
                    boolean exists = false;
                    for (ItemSelectorAdapter.SelectedItem si : selectedItems) {
                        if (si.item.getId() == selectedItem.getId()) {
                            exists = true;
                            break;
                        }
                    }

                    if (!exists) {
                        selectedItems.add(new ItemSelectorAdapter.SelectedItem(selectedItem));
                    }
                }
            }

            selectedAdapter.notifyDataSetChanged();
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        builder.show();
    }







    private void loadSales() {
        new Thread(() -> {
            List<Sales> dbSales = salesDao.getAllSales();
            requireActivity().runOnUiThread(() -> {
                salesList.clear();
                salesList.addAll(dbSales);
                filteredList.clear();
                filteredList.addAll(dbSales);
                adapter.notifyDataSetChanged();

                // Update the sales summary
                updateSalesSummary(dbSales);

                // Update the bar chart with the sales data
                updateSalesTrend(dbSales);
            });
        }).start();
    }

    private void updateSalesSummary(List<Sales> dbSales) {
        double[] totals = getTodayAndYesterdaySales(dbSales);
        double todaySales = totals[0];
        double yesterdaySales = totals[1];
        int totalTransactions = dbSales.size();



        // Update the TextViews with dynamic data
        totalSalesTextView.setText("₱" + todaySales);
        totalTransactionsTextView.setText(totalTransactions + " transactions");

        // Calculate the percentage change (compared to the previous day)
        if (totalTransactions > 0) {
            double percentageChange = ((todaySales - yesterdaySales) / yesterdaySales) * 100;
            percentageTextView.setText(String.format("%s%.2f%%", percentageChange > 0 ? "+" : "", percentageChange));
        } else {
            percentageTextView.setText("0.00%");
        }
    }

    private double[] getTodayAndYesterdaySales(List<Sales> dbSales) {
        double todayTotal = 0;
        double yesterdayTotal = 0;

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        // Get today's and yesterday's dates as strings
        Calendar calendar = Calendar.getInstance();
        String today = sdf.format(calendar.getTime());

        calendar.add(Calendar.DATE, -1);
        String yesterday = sdf.format(calendar.getTime());

        // Loop through sales and sum totals based on date
        for (Sales sale : dbSales) {
            String saleDate = sale.getTime();
            if (saleDate.equals(today)) {
                todayTotal += sale.getPrice();
            } else if (saleDate.equals(yesterday)) {
                yesterdayTotal += sale.getPrice();
            }
        }

        return new double[]{todayTotal, yesterdayTotal};
    }



    private void updateSalesTrend(List<Sales> dbSales) {
        List<BarEntry> salesData = new ArrayList<>();
        List<String> saleTitles = new ArrayList<>();

        for (int i = 0; i < dbSales.size(); i++) {
            Sales sale = dbSales.get(i);
            salesData.add(new BarEntry(i, (float) sale.getPrice()));
            saleTitles.add(sale.getTitle());
        }

        BarDataSet dataSet = new BarDataSet(salesData, "Sales Trend");
        dataSet.setColor(getResources().getColor(R.color.alt));
        dataSet.setValueTextColor(getResources().getColor(android.R.color.black));

        BarData barData = new BarData(dataSet);
        barChart.setData(barData);

        // Format X-axis to show sale titles
        XAxis xAxis = barChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(saleTitles));
        xAxis.setGranularity(1f);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawLabels(true);
        xAxis.setLabelRotationAngle(-45);

        // Show Y-axis labels only on the left
        barChart.getAxisRight().setEnabled(false);  // Hide right Y-axis
        barChart.getAxisLeft().setDrawLabels(true); // Show left Y-axis labels

        // Hide the legend (the label "Sales Trend")
        barChart.getLegend().setEnabled(false);

        barChart.invalidate(); // Refresh chart
    }




    @Override
    public void onEditClick(Sales salesItem) {
        showEditSalesDialog(salesItem);
    }

    @Override
    public void onDeleteClick(Sales salesItem) {
        showDeleteConfirmation(salesItem);
    }

    private void logRecentActivity(int iconResId, String title, String description) {
        String currentTime = new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date());

        RecentActivity activity = new RecentActivity(iconResId, title, description, currentTime);

        new Thread(() -> {
            recentActivityDao.insert(activity);
        }).start();
    }

    public static void sendLowStockNotification(Context context, List<Item> lowStockItems) {
        if (lowStockItems == null || lowStockItems.isEmpty()) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
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

        StringBuilder itemNames = new StringBuilder();
        ArrayList<String> itemNamesList = new ArrayList<>();
        ArrayList<Integer> itemIdsList = new ArrayList<>();
        for (int i = 0; i < lowStockItems.size(); i++) {
            String title = lowStockItems.get(i).getTitle();
            itemNames.append(title);
            itemNamesList.add(title);
            itemIdsList.add(lowStockItems.get(i).getId());
            if (i < lowStockItems.size() - 1) {
                itemNames.append(", ");
            }
        }

        String contentText = lowStockItems.size() == 1
                ? lowStockItems.get(0).getTitle() + " is low in stock."
                : lowStockItems.size() + " items are low in stock: \n" + itemNames;

        // Create intent for "Remind Me" action button
        Intent remindIntent = new Intent(context, LowStockReminderActionReceiver.class);
        remindIntent.setAction(LowStockReminderActionReceiver.ACTION_REMIND_ME);
        remindIntent.putIntegerArrayListExtra(LowStockReminderActionReceiver.EXTRA_ITEM_IDS, itemIdsList);

        PendingIntent remindPendingIntent = PendingIntent.getBroadcast(
                context,
                10001,  // Unique request code for this action
                remindIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "low_stock_channel")
                .setSmallIcon(R.drawable.ic_warning)
                .setContentTitle("Low Stock Alert")
                .setContentText(contentText)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(contentText))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .addAction(R.drawable.ic_add_alarm, "Remind Me", remindPendingIntent);  // Add button here

        NotificationManagerCompat.from(context).notify(9999, builder.build());

        // Broadcast list if needed
        Intent intent = new Intent(Constants.ACTION_LOW_STOCK);
        intent.putStringArrayListExtra("lowStockItemNames", itemNamesList);
        context.sendBroadcast(intent);
    }






    public void notifyLowStock(Context context, List<Item> lowStockItems) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                sendLowStockNotification(context, lowStockItems);
            } else {
                // Request permission
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        } else {
            // Permission not needed below Android 13
            sendLowStockNotification(context, lowStockItems);
        }
    }

    public static void scheduleLowStockReminder(Context context, Item item, long delayMillis) {
        long triggerTime = System.currentTimeMillis() + delayMillis;

        Intent intent = new Intent(context, LowStockAlarmReceiver.class);
        intent.putExtra("itemId", item.getId());
        intent.putExtra("itemTitle", item.getTitle());

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                item.getId(), // Unique ID per item
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                // Check if exact alarms are allowed
                if (!alarmManager.canScheduleExactAlarms()) {
                    // Prompt user to grant the permission via system settings
                    Intent settingsIntent = new Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                    settingsIntent.setData(android.net.Uri.parse("package:" + context.getPackageName()));
                    settingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(settingsIntent);
                    return;  // Do not schedule alarm until permission granted
                }
            }

            // Permission granted or not needed: schedule the alarm
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
        }
    }


}

