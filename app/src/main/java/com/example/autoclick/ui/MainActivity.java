package com.example.autoclick.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.autoclick.R;
import com.example.autoclick.service.AutoClickService;

public class MainActivity extends AppCompatActivity {

    private TextView tvAccessibilityStatus;
    private TextView tvClickPointsCount;
    private TextView tvDelayInfo;
    private TextView tvRepeatInfo;

    private Button btnAccessibilityAction;
    private Button btnAddPointAction;
    private Button btnStartAction;
    private TextView tvSettingsAction;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvAccessibilityStatus = findViewById(R.id.tvAccessibilityStatus);
        tvClickPointsCount = findViewById(R.id.tvClickPointsCount);
        tvDelayInfo = findViewById(R.id.tvDelayInfo);
        tvRepeatInfo = findViewById(R.id.tvRepeatInfo);

        btnAccessibilityAction = findViewById(R.id.btnAccessibilityAction);
        btnAddPointAction = findViewById(R.id.btnAddPointAction);
        btnStartAction = findViewById(R.id.btnStartAction);
        tvSettingsAction = findViewById(R.id.tvSettingsAction);

        btnAccessibilityAction.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivity(intent);
        });

        btnAddPointAction.setOnClickListener(v -> {
            AutoClickService service = AutoClickService.getInstance();
            if (service == null) {
                Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                startActivity(intent);
                return;
            }

            if (!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivity(intent);
                return;
            }

            service.showOverlay();
        });

        btnStartAction.setOnClickListener(v -> {
            AutoClickService service = AutoClickService.getInstance();
            if (service == null) {
                Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                startActivity(intent);
                return;
            }

            service.toggleClicking();
            updateUIState();
        });

        tvSettingsAction.setOnClickListener(v -> {
            AutoClickService service = AutoClickService.getInstance();
            if (service == null) {
                Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                startActivity(intent);
                return;
            }

            if (!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivity(intent);
                return;
            }

            service.showSettingsOverlay();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUIState();
    }

    private void updateUIState() {
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
            tvAccessibilityStatus.setText("● ON");
            tvAccessibilityStatus.setTextColor(0xFF4CAF50);
            btnAccessibilityAction.setText("Accessibility đã bật");
        } else {
            tvAccessibilityStatus.setText("● OFF");
            tvAccessibilityStatus.setTextColor(0xFFF44336);
            btnAccessibilityAction.setText("Bật Accessibility");
        }

        if (service != null) {
            tvClickPointsCount.setText(String.valueOf(service.getClickPointsCount()));
            long delay = service.getDelayBetweenClicks();
            if (delay >= 1000 && delay % 1000 == 0) {
                tvDelayInfo.setText((delay / 1000) + " s");
            } else {
                tvDelayInfo.setText(delay + " ms");
            }

            int loop = service.getLoopCount();
            if (loop == -1) {
                tvRepeatInfo.setText("∞");
            } else {
                tvRepeatInfo.setText(loop + " lần");
            }

            if (service.isRunning()) {
                btnStartAction.setText("⏹ STOP");
                btnStartAction.setBackgroundColor(0xFFF44336);
            } else {
                btnStartAction.setText("▶ START");
                btnStartAction.setBackgroundColor(0xFF4CAF50);
            }
        } else {
            tvClickPointsCount.setText("0");
            tvDelayInfo.setText("500 ms");
            tvRepeatInfo.setText("∞");
            btnStartAction.setText("▶ START");
            btnStartAction.setBackgroundColor(0xFF4CAF50);
        }
    }
}
