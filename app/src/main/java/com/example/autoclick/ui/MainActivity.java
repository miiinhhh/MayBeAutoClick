package com.example.autoclick.ui;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.autoclick.R;

public class MainActivity extends AppCompatActivity {

    private TextView tvStatus;
    private Button btnAccessibility;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        tvStatus = findViewById(R.id.tvStatus);
        btnAccessibility = findViewById(R.id.btnAccessibility);

        btnAccessibility.setOnClickListener(v -> {
            Intent intent = new Intent(
                    Settings.ACTION_ACCESSIBILITY_SETTINGS
            );

            startActivity(intent);
        });
    }
}