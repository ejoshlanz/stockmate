package com.joshlanz.stockmate;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.widget.ArrayAdapter;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import java.util.ArrayList;
import java.util.List;

public class ExportPage extends AppCompatActivity {
    private final List<MaterialButton> buttons = new ArrayList<>();
    private MaterialButton selectedButton = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.export_page);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Find the dropdown view
        MaterialAutoCompleteTextView autoCompleteTextView = findViewById(R.id.spinner_options);
        String[] options = getResources().getStringArray(R.array.dropdown_options);
        autoCompleteTextView.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, options));

        // Initialize buttons
        MaterialButton buttonComma = findViewById(R.id.button_comma);
        MaterialButton buttonSemicolon = findViewById(R.id.button_semicolon);
        MaterialButton buttonTab = findViewById(R.id.button_tab);

        buttons.add(buttonComma);
        buttons.add(buttonSemicolon);
        buttons.add(buttonTab);

        // Set click listener for each button
        for (MaterialButton button : buttons) {
            button.setOnClickListener(v -> selectButton(button));
        }
    }

    // Handle button selection (only one stays selected)
    private void selectButton(MaterialButton button) {
        if (selectedButton != null && selectedButton != button) {
            // Reset the previously selected button
            selectedButton.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.container)));
            selectedButton.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.normal)));
            selectedButton.setTextColor(ContextCompat.getColor(this, R.color.normal)); // Reset text color
        }

        // Highlight the newly selected button
        button.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.primaryLight)));
        button.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.normal)));
        button.setTextColor(ContextCompat.getColor(this, R.color.normal)); // Fix purple tint on text

        // Update selected button
        selectedButton = button;
    }


}
