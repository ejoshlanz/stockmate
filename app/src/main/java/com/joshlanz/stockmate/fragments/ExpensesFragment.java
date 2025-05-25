package com.joshlanz.stockmate.fragments;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.joshlanz.stockmate.R;
import com.joshlanz.stockmate.adapters.CategoryButtonAdapter;
import com.joshlanz.stockmate.adapters.ExpenseAdapter;
import com.joshlanz.stockmate.database.AppDatabase;
import com.joshlanz.stockmate.database.ExpenseDao;
import com.joshlanz.stockmate.database.RecentActivityDao;
import com.joshlanz.stockmate.database.SalesDao;
import com.joshlanz.stockmate.models.DailyTotal;
import com.joshlanz.stockmate.models.Expense;
import com.joshlanz.stockmate.models.RecentActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

public class ExpensesFragment extends Fragment {
    private final List<String> categories = new ArrayList<>();
    private MaterialButton selectedButton = null;

    private TextView totalIncomeCountTextView;
    private TextView totalExpensesCountTextView;
    private LineChart totalIncomeChart;

    private AppDatabase db;
    private SalesDao salesDao;
    private ExpenseDao expenseDao;

    private ExpenseAdapter adapter;
    private List<Expense> allExpenses = new ArrayList<>();
    private CategoryButtonAdapter categoryButtonAdapter;

    private RecyclerView recyclerView, buttonRecyclerView;

    private RecentActivityDao recentActivityDao;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_expenses, container, false);

        recyclerView = view.findViewById(R.id.expenses_recycler_view);
        buttonRecyclerView = view.findViewById(R.id.buttonRecyclerView);
        totalIncomeCountTextView = view.findViewById(R.id.total_income_count);
        totalExpensesCountTextView = view.findViewById(R.id.total_expenses_count);
        totalIncomeChart = view.findViewById(R.id.total_income_graph);


        ViewCompat.setOnApplyWindowInsetsListener(view.findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = AppDatabase.getDatabase(requireContext());
        salesDao = db.salesDao();
        expenseDao = db.expenseDao();
        recentActivityDao = db.recentActivityDao();

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ExpenseAdapter(new ArrayList<>(), new ExpenseAdapter.OnItemActionListener() {
            @Override
            public void onEdit(Expense expense) {
                showExpenseDialog(expense);
            }

            @Override
            public void onDelete(Expense expense) {
                showDeleteConfirmation(expense);
            }
        });

        recyclerView.setAdapter(adapter);

        loadIncomeAndExpenses();
        loadGraphData();
        loadAllExpenses();
        loadCategories();
        filterExpenses("All");

        FloatingActionButton fabAdd = view.findViewById(R.id.fab_add);
        fabAdd.setOnClickListener(view1 -> showExpenseDialog(null));
    }

    private void loadAllExpenses() {
        Executors.newSingleThreadExecutor().execute(() -> {
            allExpenses = expenseDao.getAllExpenses();
            requireActivity().runOnUiThread(() -> {
                filterExpenses(selectedButton != null ? selectedButton.getText().toString() : "All");
            });
        });
    }

    private void loadIncomeAndExpenses() {
        Executors.newSingleThreadExecutor().execute(() -> {
            double totalIncome = salesDao.getTotalIncome();
            double totalExpenses = expenseDao.getTotalExpenses();

            new Handler(Looper.getMainLooper()).post(() -> {
                totalIncomeCountTextView.setText("₱" + totalIncome);
                totalExpensesCountTextView.setText("₱" + totalExpenses);
            });
        });
    }

    private void loadGraphData() {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<DailyTotal> incomeData = salesDao.getDailyIncome();
            List<DailyTotal> expensesData = expenseDao.getDailyExpenses();

            List<Entry> incomeEntries = new ArrayList<>();
            List<Entry> expensesEntries = new ArrayList<>();
            List<String> dates = new ArrayList<>();

            int index = 0;
            for (DailyTotal data : incomeData) {
                incomeEntries.add(new Entry(index, (float) data.total));
                dates.add(data.date);
                index++;
            }

            index = 0;
            for (DailyTotal data : expensesData) {
                expensesEntries.add(new Entry(index, (float) data.total));
                index++;
            }

            new Handler(Looper.getMainLooper()).post(() -> {
                showCombinedLineChart(totalIncomeChart, incomeEntries, "Income", R.color.green, expensesEntries, "Expenses", R.color.red, dates);
            });
        });
    }

    private void showCombinedLineChart(LineChart chart, List<Entry> incomeEntries, String incomeLabel, int incomeColorRes, List<Entry> expensesEntries, String expensesLabel, int expensesColorRes, List<String> dates) {
        LineDataSet incomeDataSet = new LineDataSet(incomeEntries, incomeLabel);
        incomeDataSet.setColor(ContextCompat.getColor(requireContext(), incomeColorRes));
        incomeDataSet.setCircleColor(ContextCompat.getColor(requireContext(), incomeColorRes));
        incomeDataSet.setValueTextColor(ContextCompat.getColor(requireContext(), R.color.black));
        incomeDataSet.setLineWidth(2f);
        incomeDataSet.setCircleRadius(4f);
        incomeDataSet.setDrawValues(false);

        LineDataSet expensesDataSet = new LineDataSet(expensesEntries, expensesLabel);
        expensesDataSet.setColor(ContextCompat.getColor(requireContext(), expensesColorRes));
        expensesDataSet.setCircleColor(ContextCompat.getColor(requireContext(), expensesColorRes));
        expensesDataSet.setValueTextColor(ContextCompat.getColor(requireContext(), R.color.black));
        expensesDataSet.setLineWidth(2f);
        expensesDataSet.setCircleRadius(4f);
        expensesDataSet.setDrawValues(false);

        LineData lineData = new LineData(incomeDataSet, expensesDataSet);
        chart.setData(lineData);

        XAxis xAxis = chart.getXAxis();
        xAxis.setGranularity(1f);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int index = (int) value;
                if (index >= 0 && index < dates.size()) {
                    String date = dates.get(index);
                    return date != null ? date : "";  // return empty string if null
                }
                return "";
            }

        });

        chart.getDescription().setEnabled(false);
        chart.invalidate();
    }

    private void showExpenseDialog(@Nullable Expense expenseToEdit) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle(expenseToEdit == null ? "Add New Expense" : "Edit Expense");

        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_expense, null);
        EditText etTitle = dialogView.findViewById(R.id.etTitle);
        EditText etAmount = dialogView.findViewById(R.id.etAmount);
        EditText etCategory = dialogView.findViewById(R.id.etCategory);
        TextView tvDate = dialogView.findViewById(R.id.tv_date);

        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        if (expenseToEdit != null) {
            etTitle.setText(expenseToEdit.getTitle());
            etAmount.setText(String.valueOf(expenseToEdit.getAmount()));
            etCategory.setText(expenseToEdit.getCategory());
            tvDate.setText(expenseToEdit.getDate());
        } else {
            tvDate.setText(sdf.format(calendar.getTime()));
        }

        tvDate.setOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(requireContext(),
                    (view, year, month, dayOfMonth) -> {
                        calendar.set(year, month, dayOfMonth);
                        tvDate.setText(sdf.format(calendar.getTime()));
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH));
            datePickerDialog.show();
        });

        builder.setView(dialogView)
                .setPositiveButton(expenseToEdit == null ? "Add" : "Save", (dialog, which) -> {
                    String title = etTitle.getText().toString().trim();
                    String amountStr = etAmount.getText().toString().trim();
                    String category = etCategory.getText().toString().trim();
                    String date = tvDate.getText().toString();

                    if (!title.isEmpty() && !amountStr.isEmpty() && !category.isEmpty()) {
                        double amount = Double.parseDouble(amountStr);

                        Executors.newSingleThreadExecutor().execute(() -> {
                            if (expenseToEdit == null) {
                                // Insert new expense
                                Expense newExpense = new Expense(title, amount, category, date);
                                expenseDao.insert(newExpense);
                                logRecentActivity(R.drawable.ic_expense, "New Expense",  newExpense.getTitle() +  " • ₱" + newExpense.getAmount());
                            } else {
                                // Update existing expense
                                expenseToEdit.setTitle(title);
                                expenseToEdit.setAmount(amount);
                                expenseToEdit.setCategory(category);
                                expenseToEdit.setDate(date);
                                expenseDao.update(expenseToEdit);
                                logRecentActivity(R.drawable.ic_edit, "Expense Updated",  expenseToEdit.getTitle() +  " • ₱" + expenseToEdit.getAmount());
                            }
                            // Reload data on UI thread
                            requireActivity().runOnUiThread(() -> {
                                loadAllExpenses();
                                loadIncomeAndExpenses();
                                loadGraphData();
                                loadCategories();
                            });
                        });
                    } else {
                        Toast.makeText(requireContext(), "All fields are required", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }


    private void filterExpenses(String category) {
        if (category.equalsIgnoreCase("All")) {
            adapter.updateData(allExpenses);
        } else {
            List<Expense> filtered = new ArrayList<>();
            for (Expense e : allExpenses) {
                if (e.getCategory().equalsIgnoreCase(category)) {
                    filtered.add(e);
                }
            }
            adapter.updateData(filtered);
        }
    }

    private void loadCategories() {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<String> dbCategories = expenseDao.getAllCategories();

            requireActivity().runOnUiThread(() -> {
                categories.clear();
                categories.add("All");
                categories.addAll(dbCategories);

                if (categoryButtonAdapter == null) {
                    categoryButtonAdapter = new CategoryButtonAdapter(requireContext(), categories, this::filterExpenses);
                    buttonRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
                    buttonRecyclerView.setAdapter(categoryButtonAdapter);
                } else {
                    categoryButtonAdapter.notifyDataSetChanged();
                }
            });
        });
    }
    private void showDeleteConfirmation(Expense expense) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Expense")
                .setMessage("Are you sure you want to delete this expense?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    Executors.newSingleThreadExecutor().execute(() -> {
                        expenseDao.delete(expense);
                        requireActivity().runOnUiThread(() -> {
                            allExpenses.remove(expense);
                            filterExpenses(selectedButton != null ? selectedButton.getText().toString() : "All");
                            loadIncomeAndExpenses();
                            loadGraphData();
                            loadCategories();
                            logRecentActivity(R.drawable.ic_bin, "Expense Deleted",  expense.getTitle() +  " • ₱" + expense.getAmount());
                        });
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void logRecentActivity(int iconResId, String title, String description) {
        String currentTime = new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date());

        RecentActivity activity = new RecentActivity(iconResId, title, description, currentTime);

        new Thread(() -> {
            recentActivityDao.insert(activity);
        }).start();
    }
}
