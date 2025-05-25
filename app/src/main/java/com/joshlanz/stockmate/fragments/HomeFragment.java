package com.joshlanz.stockmate.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.github.mikephil.charting.formatter.ValueFormatter;
import com.joshlanz.stockmate.statics.Constants;
import com.joshlanz.stockmate.R;
import com.joshlanz.stockmate.database.AppDatabase;
import com.joshlanz.stockmate.database.ItemDao;
import com.joshlanz.stockmate.database.SalesDao;
import com.joshlanz.stockmate.models.Item;
import com.joshlanz.stockmate.models.Sales;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.joshlanz.stockmate.database.RecentActivityDao;
import com.joshlanz.stockmate.models.RecentActivity;
import com.joshlanz.stockmate.adapters.RecentActivityAdapter;



public class HomeFragment extends Fragment {
    private AppDatabase db;
    private SalesDao salesDao;
    private ItemDao itemDao;
    private TextView totalSalesTextView, percentageTextView, totalStocksTextView, lowStocksTextView;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private LineChart weeklySalesChart;

    private RecentActivityDao recentActivityDao;
    private RecyclerView recentActivityRecycler;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        db = AppDatabase.getDatabase(requireContext());
        salesDao = db.salesDao();
        itemDao = db.itemDao();

        totalSalesTextView = view.findViewById(R.id.total_sales_count);
        percentageTextView = view.findViewById(R.id.sales_compare);
        totalStocksTextView = view.findViewById(R.id.total_stocks);
        lowStocksTextView = view.findViewById(R.id.low_stock_count);
        weeklySalesChart = view.findViewById(R.id.weekly_sales_chart);
        recentActivityDao = db.recentActivityDao();
        recentActivityRecycler = view.findViewById(R.id.rv_recent_activities);
        recentActivityRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));


        loadRecentActivities();
        loadSalesData();
        loadStocksData();
        loadWeeklySalesData();
        loadSalesData();
        loadStocksData();

        ViewCompat.setOnApplyWindowInsetsListener(view.findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        return view;
    }

    private void loadStocksData() {
        new Thread(() -> {
            List<Item> allItems = itemDao.getAllItems();
            int totalItems = allItems.size();
            int lowStockCount = (int) allItems.stream().filter(i -> i.getStock() < Constants.LOW_STOCK_THRESHOLD).count();

            requireActivity().runOnUiThread(() -> {
               totalStocksTextView.setText(String.valueOf(totalItems));
                lowStocksTextView.setText(lowStockCount + " low stock items");
                int colorRes = (lowStockCount > 0) ? R.color.red : R.color.gray;
                lowStocksTextView.setTextColor(ContextCompat.getColor(requireContext(), colorRes));
            });
        }).start();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            v.setPadding(0, 0, 0, 0);
            return insets;
        });
    }

    private void loadSalesData() {
        executor.execute(() -> {
            List<Sales> dbSales = salesDao.getAllSales();
            double[] totals = getTodayAndYesterdaySales(dbSales);
            double todaySales = totals[0];
            double yesterdaySales = totals[1];
            int totalTransactions = dbSales.size();

            mainHandler.post(() -> {
                totalSalesTextView.setText("₱" + todaySales);

                if (totalTransactions > 0 && yesterdaySales != 0) {
                    double percentageChange = ((todaySales - yesterdaySales) / yesterdaySales) * 100;
                    String formattedChange = String.format("%s%.2f%% vs yesterday", percentageChange > 0 ? "+" : "", percentageChange);
                    percentageTextView.setText(formattedChange);

                    // Set text color based on the percentage
                    int colorRes = (percentageChange >= 0) ? R.color.green : R.color.red;
                    percentageTextView.setTextColor(ContextCompat.getColor(requireContext(), colorRes));
                } else {
                    percentageTextView.setText("0.00%");
                    percentageTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray));
                }
            });
        });
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

    private void loadWeeklySalesData() {
        executor.execute(() -> {
            List<Sales> allSales = salesDao.getAllSales();

            double[] dailyTotals = new double[7];
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

            Calendar calendar = Calendar.getInstance(); // today
            calendar.add(Calendar.DATE, -6); // Move calendar back 6 days to get start of window

            // Calculate totals starting from calendar (6 days ago) to today
            for (int i = 0; i < 7; i++) {
                String date = sdf.format(calendar.getTime());
                double totalForDay = 0;
                for (Sales sale : allSales) {
                    if (sale.getTime().equals(date)) {
                        totalForDay += sale.getPrice();
                    }
                }
                dailyTotals[i] = totalForDay;
                calendar.add(Calendar.DATE, 1); // move forward one day
            }

            // Now calendar is advanced 1 day past today, so subtract 7 to get start date again
            calendar.add(Calendar.DATE, -7);
            Calendar startDate = (Calendar) calendar.clone();

            mainHandler.post(() -> setWeeklySalesChart(dailyTotals, startDate));
        });
    }


    private void setWeeklySalesChart(double[] dailyTotals, Calendar startDate) {
        // Prepare entries for the line chart
        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < dailyTotals.length; i++) {
            entries.add(new Entry(i, (float) dailyTotals[i]));
        }

        LineDataSet dataSet = new LineDataSet(entries, "Weekly Sales");
        dataSet.setColor(ContextCompat.getColor(requireContext(), R.color.normal)); // your app's primary color
        dataSet.setValueTextColor(ContextCompat.getColor(requireContext(), R.color.black));
        dataSet.setLineWidth(2f);
        dataSet.setCircleRadius(4f);
        dataSet.setCircleColor(ContextCompat.getColor(requireContext(), R.color.normal));
        dataSet.setDrawValues(false);

        LineData lineData = new LineData(dataSet);
        weeklySalesChart.setData(lineData);

        // X-axis setup (show day labels)
        XAxis xAxis = weeklySalesChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setLabelCount(7);

        // Use the passed startDate as base for labels
        xAxis.setValueFormatter(new ValueFormatter() {
            private final SimpleDateFormat dayFormat = new SimpleDateFormat("EEE", Locale.getDefault());
            @Override
            public String getFormattedValue(float value) {
                Calendar cal = (Calendar) startDate.clone();
                cal.add(Calendar.DATE, (int) value);
                return dayFormat.format(cal.getTime());
            }
        });

        // Y-axis setup
        YAxis leftAxis = weeklySalesChart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setGranularity(1f);
        leftAxis.setAxisMinimum(0f); // start y-axis at zero

        YAxis rightAxis = weeklySalesChart.getAxisRight();
        rightAxis.setEnabled(false);

        weeklySalesChart.getDescription().setEnabled(false);
        weeklySalesChart.getLegend().setEnabled(false);
        weeklySalesChart.invalidate(); // refresh
    }

    private void loadRecentActivities() {
        executor.execute(() -> {
            List<RecentActivity> activities = recentActivityDao.getAllActivities();
            mainHandler.post(() -> {
                RecentActivityAdapter adapter = new RecentActivityAdapter(getContext(), activities);
                recentActivityRecycler.setAdapter(adapter);
            });
        });
    }

}
