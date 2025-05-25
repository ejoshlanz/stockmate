package com.joshlanz.stockmate.activities;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.joshlanz.stockmate.R;
import com.joshlanz.stockmate.database.AppDatabase;
import com.joshlanz.stockmate.database.ExpenseDao;
import com.joshlanz.stockmate.database.ItemDao;
import com.joshlanz.stockmate.database.SalesDao;
import com.joshlanz.stockmate.models.Expense;
import com.joshlanz.stockmate.models.Item;
import com.joshlanz.stockmate.models.Sales;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ExportActivity extends AppCompatActivity {
    private static final int CREATE_FILE_REQUEST_CODE = 1001;

    private final List<MaterialButton> buttons = new ArrayList<>();
    private MaterialButton selectedButton = null;
    private String selectedDelimiter = ",";

    private String selectedFormat = "";


    private AppDatabase db;
    private SalesDao salesDao;
    private ItemDao itemDao;
    private ExpenseDao expenseDao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_export);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = AppDatabase.getDatabase(this);
        salesDao = db.salesDao();
        itemDao = db.itemDao();
        expenseDao = db.expenseDao();

        // UI references
        MaterialAutoCompleteTextView formatSpinner = findViewById(R.id.spinner_options);
        MaterialAutoCompleteTextView entitySpinner = findViewById(R.id.entity_spinner_options);
        LinearLayout checkboxContainer = findViewById(R.id.checkbox_container);
        TextView previewText = findViewById(R.id.preview_text);

        // Format options
        String[] formats = getResources().getStringArray(R.array.dropdown_options);
        formatSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, formats));

        // Delimiter buttons
        MaterialButton buttonComma = findViewById(R.id.button_comma);
        MaterialButton buttonSemicolon = findViewById(R.id.button_semicolon);
        MaterialButton buttonTab = findViewById(R.id.button_tab);

        buttons.add(buttonComma);
        buttons.add(buttonSemicolon);
        buttons.add(buttonTab);

        for (MaterialButton button : buttons) {
            button.setOnClickListener(v -> selectButton(button));
        }

        // Back button
        ImageView backArrowButton = findViewById(R.id.backArrow);
        backArrowButton.setOnClickListener(v -> finish());

        // Entity dropdown setup
        String[] entities = {"Sales", "Item", "Expense"};
        entitySpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, entities));

        entitySpinner.setOnItemClickListener((parent, view, position, id) -> {
            String selectedEntity = entities[position];
            checkboxContainer.removeAllViews();

            String[] columns = getColumnsForEntity(selectedEntity);
            for (String column : columns) {
                CheckBox checkBox = new CheckBox(this);
                checkBox.setText(column);

                // Add listener to update preview when checkbox state changes
                checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> updatePreview());

                checkboxContainer.addView(checkBox);
            }

            updatePreview(); // Also update preview immediately after entity change
        });


        // Export button logic
        MaterialButton exportButton = findViewById(R.id.button_export);
        exportButton.setOnClickListener(v -> {
            String selectedFormat = formatSpinner.getText().toString();
            if (selectedFormat.isEmpty()) {
                // Don't show preview or proceed if no format selected
                previewText.setText("");
                return;
            }

            // Launch file picker to create a file
            String mimeType = getMimeTypeForFormat(selectedFormat);
            String defaultFileName = getDefaultFileNameForFormat(selectedFormat);

            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType(mimeType);
            intent.putExtra(Intent.EXTRA_TITLE, defaultFileName);

            startActivityForResult(intent, CREATE_FILE_REQUEST_CODE);
        });



        MaterialAutoCompleteTextView formatDropdown = findViewById(R.id.spinner_options);
        LinearLayout delimiterContainer = findViewById(R.id.delimiter_container);

        formatDropdown.setOnItemClickListener((parent, view, position, id) -> {
            selectedFormat = (String) parent.getItemAtPosition(position);
            if ("Comma-Separated Values (csv)".equalsIgnoreCase(selectedFormat)) {
                delimiterContainer.setVisibility(View.VISIBLE);
            } else {
                delimiterContainer.setVisibility(View.GONE);
            }

            updatePreview();  // Update preview when format changes
        });


    }

    private void selectButton(MaterialButton button) {
        if (selectedButton != null && selectedButton != button) {
            selectedButton.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.container)));
            selectedButton.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.normal)));
            selectedButton.setTextColor(ContextCompat.getColor(this, R.color.normal));
        }

        button.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.primaryLight)));
        button.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.normal)));
        button.setTextColor(ContextCompat.getColor(this, R.color.normal));

        selectedButton = button;

        selectedDelimiter = button.getText().toString().contains("Comma") ? "," :
                button.getText().toString().contains("Semicolon") ? ";" : "\t";

        updatePreview();  // Update preview after delimiter changes
    }


    private String[] getColumnsForEntity(String entity) {
        switch (entity) {
            case "Sales":
                return new String[]{"ID", "Title", "Price", "Time"};
            case "Item":
                return new String[]{"ID", "Title", "Category", "Stock", "Price"};
            case "Expense":
                return new String[]{"ID", "Title", "Amount", "Category", "Date"};
            default:
                return new String[]{};
        }
    }

    private String getFieldValue(Object obj, String entity, String columnName) {
        if (entity.equals("Sales") && obj instanceof Sales) {
            Sales sale = (Sales) obj;
            switch (columnName) {
                case "ID": return String.valueOf(sale.getId());
                case "Title": return sale.getTitle();
                case "Price": return String.valueOf(sale.getPrice());
                case "Time": return sale.getTime();
            }
        } else if (entity.equals("Item") && obj instanceof Item) {
            Item item = (Item) obj;
            switch (columnName) {
                case "ID": return String.valueOf(item.getId());
                case "Title": return item.getTitle();
                case "Category": return item.getCategory();
                case "Stock": return String.valueOf(item.getStock());
                case "Price": return String.valueOf(item.getPrice());
            }
        } else if (entity.equals("Expense") && obj instanceof Expense) {
            Expense expense = (Expense) obj;
            switch (columnName) {
                case "ID": return String.valueOf(expense.getId());
                case "Title": return expense.getTitle();
                case "Amount": return String.valueOf(expense.getAmount());
                case "Category": return expense.getCategory();
                case "Date": return expense.getDate();
            }
        }
        return "";
    }

    private void updatePreview() {
        MaterialAutoCompleteTextView entitySpinner = findViewById(R.id.entity_spinner_options);
        LinearLayout checkboxContainer = findViewById(R.id.checkbox_container);
        TextView previewText = findViewById(R.id.preview_text);

        String selectedEntity = entitySpinner.getText().toString();
        List<String> selectedColumns = new ArrayList<>();

        for (int i = 0; i < checkboxContainer.getChildCount(); i++) {
            CheckBox cb = (CheckBox) checkboxContainer.getChildAt(i);
            if (cb.isChecked()) {
                selectedColumns.add(cb.getText().toString());
            }
        }

        // If no format or no entity or no columns, clear preview and return
        if (selectedFormat.isEmpty() || selectedEntity.isEmpty() || selectedColumns.isEmpty()) {
            previewText.setText("");
            return;
        }

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            List<?> dataRows = new ArrayList<>();

            // Run DB query off the main thread
            switch (selectedEntity) {
                case "Sales":
                    dataRows = salesDao.getSampleSales(3);
                    break;
                case "Item":
                    dataRows = itemDao.getSampleItems(3);
                    break;
                case "Expense":
                    dataRows = expenseDao.getSampleExpenses(3);
                    break;
            }

            String previewTextStr;

            if (selectedFormat.equalsIgnoreCase("JavaScript Object Notation (json)")) {
                // Build JSON preview
                StringBuilder jsonBuilder = new StringBuilder();
                jsonBuilder.append("[\n");
                for (int i = 0; i < dataRows.size(); i++) {
                    Object row = dataRows.get(i);
                    jsonBuilder.append("  {\n");
                    for (int col = 0; col < selectedColumns.size(); col++) {
                        String colName = selectedColumns.get(col);
                        String cellValue = getFieldValue(row, selectedEntity, colName);
                        jsonBuilder.append("    \"").append(colName).append("\": ");
                        jsonBuilder.append("\"").append(cellValue.replace("\"", "\\\"")).append("\"");
                        if (col < selectedColumns.size() - 1) {
                            jsonBuilder.append(",");
                        }
                        jsonBuilder.append("\n");
                    }
                    jsonBuilder.append("  }");
                    if (i < dataRows.size() - 1) {
                        jsonBuilder.append(",");
                    }
                    jsonBuilder.append("\n");
                }
                jsonBuilder.append("]");
                previewTextStr = jsonBuilder.toString();
            } else {
                // Build CSV preview as before
                StringBuilder previewBuilder = new StringBuilder();

                // Header row
                for (int i = 0; i < selectedColumns.size(); i++) {
                    previewBuilder.append(selectedColumns.get(i));
                    if (i < selectedColumns.size() - 1) {
                        previewBuilder.append(selectedDelimiter);
                    }
                }
                previewBuilder.append("\n");

                // Data rows
                for (Object row : dataRows) {
                    for (int col = 0; col < selectedColumns.size(); col++) {
                        String colName = selectedColumns.get(col);
                        String cellValue = getFieldValue(row, selectedEntity, colName);
                        previewBuilder.append(cellValue);
                        if (col < selectedColumns.size() - 1) {
                            previewBuilder.append(selectedDelimiter);
                        }
                    }
                    previewBuilder.append("\n");
                }

                previewTextStr = previewBuilder.toString();
            }

            // Update UI on main thread
            handler.post(() -> previewText.setText(previewTextStr));
        });
    }

    private String getMimeTypeForFormat(String format) {
        switch (format) {
            case "Comma-Separated Values (csv)":
                return "text/csv";
            case "JavaScript Object Notation (json)":
                return "application/json";
            default:
                return "*/*";
        }
    }

    private String getDefaultFileNameForFormat(String format) {
        switch (format) {
            case "Comma-Separated Values (csv)":
                return "export.csv";
            case "JavaScript Object Notation (json)":
                return "export.json";
            default:
                return "export.txt";
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CREATE_FILE_REQUEST_CODE && resultCode == RESULT_OK) {
            if (data != null) {
                final Uri uri = data.getData();
                if (uri != null) {
                    saveExportedDataToUri(uri);
                }
            }
        }
    }

    private void saveExportedDataToUri(Uri uri) {
        MaterialAutoCompleteTextView entitySpinner = findViewById(R.id.entity_spinner_options);
        LinearLayout checkboxContainer = findViewById(R.id.checkbox_container);
        MaterialAutoCompleteTextView formatSpinner = findViewById(R.id.spinner_options);

        String selectedEntity = entitySpinner.getText().toString();
        String selectedFormat = formatSpinner.getText().toString();

        List<String> selectedColumns = new ArrayList<>();
        for (int i = 0; i < checkboxContainer.getChildCount(); i++) {
            CheckBox cb = (CheckBox) checkboxContainer.getChildAt(i);
            if (cb.isChecked()) {
                selectedColumns.add(cb.getText().toString());
            }
        }

        if (selectedColumns.isEmpty() || selectedEntity.isEmpty() || selectedFormat.isEmpty()) {
            // Nothing to export
            return;
        }

        // Run DB query off main thread
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            List<?> dataRows = new ArrayList<>();
            switch (selectedEntity) {
                case "Sales":
                    dataRows = salesDao.getSampleSales(1000); // You can increase number to export full data
                    break;
                case "Item":
                    dataRows = itemDao.getSampleItems(1000);
                    break;
                case "Expense":
                    dataRows = expenseDao.getSampleExpenses(1000);
                    break;
            }

            String exportContent = "";

            if ("Comma-Separated Values (csv)".equalsIgnoreCase(selectedFormat)) {
                exportContent = generateCSV(selectedColumns, dataRows);
            } else if ("JavaScript Object Notation (json)".equalsIgnoreCase(selectedFormat)) {
                exportContent = generateJSON(selectedColumns, dataRows, selectedEntity);
            }

            // Write to file
            try {
                OutputStream outputStream = getContentResolver().openOutputStream(uri);
                if (outputStream != null) {
                    outputStream.write(exportContent.getBytes());
                    outputStream.close();

                    handler.post(() -> {
                        // Show success message or toast
                        Toast.makeText(this, "Export successful!", Toast.LENGTH_SHORT).show();
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                handler.post(() -> Toast.makeText(this, "Export failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
    }

    private String generateCSV(List<String> columns, List<?> dataRows) {
        StringBuilder builder = new StringBuilder();

        // Header
        for (int i = 0; i < columns.size(); i++) {
            builder.append(columns.get(i));
            if (i < columns.size() - 1) builder.append(selectedDelimiter);
        }
        builder.append("\n");

        // Rows
        for (Object row : dataRows) {
            for (int i = 0; i < columns.size(); i++) {
                builder.append(getFieldValue(row, row.getClass().getSimpleName(), columns.get(i)));
                if (i < columns.size() - 1) builder.append(selectedDelimiter);
            }
            builder.append("\n");
        }

        return builder.toString();
    }

    private String generateJSON(List<String> columns, List<?> dataRows, String entity) {
        StringBuilder builder = new StringBuilder();
        builder.append("[\n");

        for (int i = 0; i < dataRows.size(); i++) {
            Object row = dataRows.get(i);
            builder.append("  {\n");
            for (int j = 0; j < columns.size(); j++) {
                String col = columns.get(j);
                String value = getFieldValue(row, entity, col);

                builder.append("    \"").append(col).append("\": ");
                // Add quotes if string
                if (isNumeric(value)) {
                    builder.append(value);
                } else {
                    builder.append("\"").append(value.replace("\"", "\\\"")).append("\"");
                }

                if (j < columns.size() - 1) builder.append(",");
                builder.append("\n");
            }
            builder.append("  }");
            if (i < dataRows.size() - 1) builder.append(",");
            builder.append("\n");
        }

        builder.append("]");
        return builder.toString();
    }

    private boolean isNumeric(String str) {
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

}
