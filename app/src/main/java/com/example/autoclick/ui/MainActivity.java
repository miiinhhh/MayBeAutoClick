package com.example.autoclick.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.autoclick.R;
import com.example.autoclick.service.AutoClickService;

public class MainActivity extends AppCompatActivity {

    private TextView tvStatus;
    private Button btnAccessibility;
    private Button btnAddPoint;
    private Button btnStartStop;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvStatus = findViewById(R.id.tvStatus);
        btnAccessibility = findViewById(R.id.btnAccessibility);
        btnAddPoint = findViewById(R.id.btnAddPoint);
        btnStartStop = findViewById(R.id.btnStartStop);

        btnAccessibility.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivity(intent);
        });

        btnAddPoint.setOnClickListener(v -> {
            AutoClickService service = AutoClickService.getInstance();
            if (service == null) {
                Toast.makeText(this, "Vui lòng bật dịch vụ Accessibility trước!", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "Vui lòng cấp quyền hiển thị trên ứng dụng khác!", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivity(intent);
                return;
            }

            service.showOverlay();
            Toast.makeText(this, "Đã hiển thị điểm click. Hãy kéo đến vị trí muốn click.", Toast.LENGTH_SHORT).show();
        });

        btnStartStop.setOnClickListener(v -> {
            AutoClickService service = AutoClickService.getInstance();
            if (service == null) {
                Toast.makeText(this, "Vui lòng bật dịch vụ Accessibility trước!", Toast.LENGTH_SHORT).show();
                return;
            }

            service.toggleClicking();
            updateStartStopButton();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkAccessibilityStatus();
        updateStartStopButton();
    }

    private void updateStartStopButton() {
        AutoClickService service = AutoClickService.getInstance();
        if (service != null && service.isRunning()) {
            btnStartStop.setText("Stop");
            btnStartStop.setBackgroundColor(0xFFFF5722);
        } else {
            btnStartStop.setText("Start");
            btnStartStop.setBackgroundColor(0xFF4CAF50);
        }
    }

    private void checkAccessibilityStatus() {
        AutoClickService service = AutoClickService.getInstance();
        boolean isServiceRunning = (service != null);

        String enabledServices = Settings.Secure.getString(
                getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        );

        String serviceName1 = getPackageName() + "/" + AutoClickService.class.getName();
        String serviceName2 = getPackageName() + "/.service.AutoClickService";

        boolean isEnabledInSettings = enabledServices != null &&
                (enabledServices.contains(serviceName1) || enabledServices.contains(serviceName2));

        if (isServiceRunning || isEnabledInSettings) {
            tvStatus.setText("Accessibility: ON");
            btnAccessibility.setText("Accessibility đã bật");
        } else {
            tvStatus.setText("Accessibility: OFF");
            btnAccessibility.setText("Bật Accessibility");
        }
    }
}
