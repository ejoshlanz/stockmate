package com.joshlanz.stockmate;

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

public class SignupPage extends AppCompatActivity {
    private boolean isPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.signup_page); // Ensure this is the correct layout file

        // Apply window insets for proper padding
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        EditText passwordInput = findViewById(R.id.passwordInput);
        ImageView togglePassword = findViewById(R.id.togglePassword); // Ensure this ImageView exists in XML

        togglePassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isPasswordVisible) {
                    passwordInput.setTransformationMethod(PasswordTransformationMethod.getInstance());
                    togglePassword.setImageResource(R.drawable.ic_password_hidden); // Ensure this drawable exists
                } else {
                    passwordInput.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                    togglePassword.setImageResource(R.drawable.ic_password_visible); // Ensure this drawable exists
                }
                isPasswordVisible = !isPasswordVisible;
                passwordInput.setSelection(passwordInput.getText().length()); // Keep cursor at the end
            }
        });
    }
}
