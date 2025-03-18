package com.joshlanz.stockmate;

import android.content.res.ColorStateList;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class ExpensesPage extends AppCompatActivity {
    private final List<MaterialButton> buttons = new ArrayList<>();
    private MaterialButton selectedButton = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.expenses_page);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize buttons
        MaterialButton buttonAll = findViewById(R.id.button_all);
        MaterialButton buttonOperations = findViewById(R.id.button_operations);
        MaterialButton buttonUtilities = findViewById(R.id.button_utilities);
        MaterialButton buttonEquipment = findViewById(R.id.button_equipment);
        MaterialButton buttonMarketing = findViewById(R.id.button_marketing);

        buttons.add(buttonAll);
        buttons.add(buttonOperations);
        buttons.add(buttonUtilities);
        buttons.add(buttonEquipment);
        buttons.add(buttonMarketing);


        // Set click listener for each button
        for (MaterialButton button : buttons) {
            button.setOnClickListener(v -> selectButton(button));
        }
    }

    private void selectButton(MaterialButton button) {
        if (selectedButton != null && selectedButton != button) {
            // Reset the previously selected button
            selectedButton.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.container)));
            selectedButton.setTextColor(ContextCompat.getColor(this, R.color.normal)); // Reset text color
        }

        // Highlight the newly selected button
        button.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.normal)));
        button.setTextColor(ContextCompat.getColor(this, R.color.reverse_normal)); // Fix purple tint on text

        // Update selected button
        selectedButton = button;
    }
}
