package com.joshlanz.stockmate;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class AccountPage extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.account_page);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        int textDefault = ContextCompat.getColor(this, R.color.normal);

        Button linkButton = findViewById(R.id.linkButton);
        linkButton.setOnClickListener(v -> {
            boolean isLinked = linkButton.isSelected();
            linkButton.setSelected(!isLinked);
            linkButton.setText(isLinked ? "Link" : "Unlink");
            linkButton.setTextColor(isLinked ? textDefault : Color.RED);
        });


    }
}