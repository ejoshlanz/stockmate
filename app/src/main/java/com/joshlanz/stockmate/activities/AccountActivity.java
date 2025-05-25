package com.joshlanz.stockmate.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.joshlanz.stockmate.R;

public class AccountActivity extends AppCompatActivity {

    private String currentPassword = "12345678";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        int textDefault = ContextCompat.getColor(this, R.color.normal);

        // Link Button Logic
        Button linkButton = findViewById(R.id.linkButton);
        linkButton.setOnClickListener(v -> {
            boolean isLinked = linkButton.isSelected();
            linkButton.setSelected(!isLinked);
            linkButton.setText(isLinked ? "Link" : "Unlink");
            linkButton.setTextColor(isLinked ? textDefault : Color.RED);
        });

        // Logout Button Logic
        findViewById(R.id.logoutBtn).setOnClickListener(v -> {
            startActivity(new Intent(this, PresavedLoginActivity.class));
            finish(); // Close current activity
        });

        // Email Edit Button Logic
        ImageView emailEditButton = findViewById(R.id.emailEditBtn);
        emailEditButton.setOnClickListener(v -> showEditEmailDialog());

        // Phone Edit Button Logic
        ImageView phoneEditButton = findViewById(R.id.phoneEditBtn);
        phoneEditButton.setOnClickListener(v -> showEditPhoneDialog());

        // Username Edit Button Logic
        ImageView usernameEditButton = findViewById(R.id.usernameEditBtn);
        usernameEditButton.setOnClickListener(v -> showEditUsernameDialog());

        // Change Password Edit Button Logic
        ImageView passwordEditButton = findViewById(R.id.passwordEditBtn);
        passwordEditButton.setOnClickListener(v -> showChangePasswordDialog());

        // Back Arrow Button Logic
        ImageView backArrowButton = findViewById(R.id.backArrow);
        backArrowButton.setOnClickListener(v -> finish()); // Close current activity when clicked
    }

    private void showEditEmailDialog() {
        // Create the EditText for email input
        final EditText emailEditText = new EditText(this);
        emailEditText.setText("ejoshlanzuela@gmail.com"); // You can set this dynamically
        emailEditText.setSelection(emailEditText.getText().length()); // Move cursor to end

        // Create the dialog builder
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Email")
                .setMessage("Enter a new email address:")
                .setView(emailEditText)
                .setPositiveButton("Save", (dialog, which) -> {
                    // Here you can save the new email, for now we just update the text view
                    String newEmail = emailEditText.getText().toString();
                    TextView emailTextView = findViewById(R.id.emailText);
                    emailTextView.setText(newEmail);
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.cancel())
                .create()
                .show();
    }

    private void showEditPhoneDialog() {
        // Create the EditText for phone number input
        final EditText phoneEditText = new EditText(this);
        phoneEditText.setText("+63 945 320 3841"); // You can set this dynamically
        phoneEditText.setSelection(phoneEditText.getText().length()); // Move cursor to end

        // Create the dialog builder
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Phone Number")
                .setMessage("Enter a new phone number:")
                .setView(phoneEditText)
                .setPositiveButton("Save", (dialog, which) -> {
                    // Here you can save the new phone number, for now we just update the text view
                    String newPhone = phoneEditText.getText().toString();
                    TextView phoneTextView = findViewById(R.id.phoneText);
                    phoneTextView.setText(newPhone);
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.cancel())
                .create()
                .show();
    }

    private void showEditUsernameDialog() {
        // Create the EditText for username input
        final EditText usernameEditText = new EditText(this);
        usernameEditText.setText("@joshlanz"); // You can set this dynamically
        usernameEditText.setSelection(usernameEditText.getText().length()); // Move cursor to end

        // Create the dialog builder
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Username")
                .setMessage("Enter a new username:")
                .setView(usernameEditText)
                .setPositiveButton("Save", (dialog, which) -> {
                    // Here you can save the new username, for now we just update the text view
                    String newUsername = usernameEditText.getText().toString();
                    TextView usernameTextView = findViewById(R.id.usernameText);
                    usernameTextView.setText(newUsername);
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.cancel())
                .create()
                .show();
    }

    private void showChangePasswordDialog() {
        // Create the EditTexts for current password and new password input
        final EditText currentPasswordEditText = new EditText(this);
        currentPasswordEditText.setHint("Enter current password");

        final EditText newPasswordEditText = new EditText(this);
        newPasswordEditText.setHint("Enter new password");

        // Create the dialog builder
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Change Password")
                .setMessage("Enter current and new password:")
                .setView(currentPasswordEditText)
                .setView(newPasswordEditText) // Adding both EditTexts to the dialog
                .setPositiveButton("Save", (dialog, which) -> {
                    String currentPasswordInput = currentPasswordEditText.getText().toString();
                    String newPasswordInput = newPasswordEditText.getText().toString();

                    if (currentPasswordInput.equals(currentPassword)) {
                        // Proceed with password change
                        currentPassword = newPasswordInput;
                        Toast.makeText(AccountActivity.this, "Password changed successfully", Toast.LENGTH_SHORT).show();
                    } else {
                        // Show error if current password doesn't match
                        Toast.makeText(AccountActivity.this, "Incorrect current password", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.cancel())
                .create()
                .show();
    }
}
