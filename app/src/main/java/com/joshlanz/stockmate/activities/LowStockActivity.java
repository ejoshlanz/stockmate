package com.joshlanz.stockmate.activities;

import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.joshlanz.stockmate.R;
import com.joshlanz.stockmate.database.AppDatabase;
import com.joshlanz.stockmate.models.Item;
import com.joshlanz.stockmate.database.ItemDao;

import java.util.ArrayList;
import java.util.List;

public class LowStockActivity extends AppCompatActivity {

    private ListView lowStockListView;
    private ItemDao itemDao;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_low_stock);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        lowStockListView = findViewById(R.id.lowStockListView);
        itemDao = AppDatabase.getInstance(this).itemDao();

        ImageView backArrowButton = findViewById(R.id.backArrow);
        backArrowButton.setOnClickListener(v -> finish());
        // Run the DB query on background thread using AsyncTask
        new LoadLowStockItemsTask().execute(5); // pass threshold = 5
    }

    private class LoadLowStockItemsTask extends AsyncTask<Integer, Void, List<Item>> {

        @Override
        protected List<Item> doInBackground(Integer... params) {
            int threshold = params[0];
            return itemDao.getItemsWithLowStock(threshold);
        }

        @Override
        protected void onPostExecute(List<Item> lowStockItems) {
            List<String> displayList = new ArrayList<>();
            for (Item item : lowStockItems) {
                displayList.add(item.getTitle() + " - Stock: " + item.getStock());
            }

            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                    LowStockActivity.this,
                    android.R.layout.simple_list_item_1,
                    displayList);

            lowStockListView.setAdapter(adapter);
        }
    }
}
