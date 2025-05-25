package com.joshlanz.stockmate.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.joshlanz.stockmate.R;

public class PresavedLoginActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_presaved_login); // Ensure this is the correct layout file

        // Apply window insets for proper padding
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        findViewById(R.id.loginBtn).setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class));
            finish(); // Close current activity
        });

        findViewById(R.id.switchAccBtn).setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish(); // Close current activity
        });

    }
}
