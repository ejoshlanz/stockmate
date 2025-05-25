package com.joshlanz.stockmate.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.joshlanz.stockmate.R;

public class SignupActivity extends AppCompatActivity {
    private boolean isPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup); // Ensure this is the correct layout file

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

        findViewById(R.id.signupBtn).setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class));
            finish(); // Close current activity
        });

        findViewById(R.id.loginBtn).setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish(); // Close current activity
        });

        CheckBox policyCheckbox = findViewById(R.id.policyCheckbox);

        findViewById(R.id.linkPolicyTerms).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://yourdomain.com/privacy-and-terms"));
            startActivity(intent);
        });

        findViewById(R.id.signupBtn).setOnClickListener(v -> {
            if (!policyCheckbox.isChecked()) {
                Toast.makeText(this, "Please accept the privacy policy and terms to continue.", Toast.LENGTH_SHORT).show();
            } else {
                startActivity(new Intent(this, MainActivity.class));
                finish(); // Close current activity
            }
        });

        findViewById(R.id.linkPolicyTerms).setOnClickListener(v -> showPolicyDialog());


    }

    private void showPolicyDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Privacy Policy and Terms");

        builder.setMessage("By signing up, you agree to our Privacy Policy and Terms of Service. \n\n" +
                "We collect basic user information like username and email to provide a better experience. " +
                "We do not share your information with third parties. You can delete your data anytime by contacting support. " +
                "\n\nFor full details, visit our website or contact us directly.");

        builder.setPositiveButton("I Understand", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();
    }

}
