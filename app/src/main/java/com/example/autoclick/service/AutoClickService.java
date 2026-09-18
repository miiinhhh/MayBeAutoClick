package com.example.autoclick.service;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.widget.AppCompatButton;

public class AutoClickService extends AccessibilityService {

    private static AutoClickService instance;
    private WindowManager windowManager;
    private View clickPointView;
    private View controlPanelView;
    private WindowManager.LayoutParams clickPointParams;
    private WindowManager.LayoutParams controlPanelParams;
    private boolean isShowingOverlay = false;
    private boolean isRunning = false;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable clickRunnable;

    private AppCompatButton btnStartStopControl;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        hideOverlay();
        stopClicking();
        instance = null;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
    }

    @Override
    public void onInterrupt() {
    }

    public static AutoClickService getInstance() {
        return instance;
    }

    public void showOverlay() {
        if (isShowingOverlay) return;

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        int layoutParamType;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutParamType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            @SuppressWarnings("deprecation")
            int type = WindowManager.LayoutParams.TYPE_PHONE;
            layoutParamType = type;
        }

        // 1. Create Click Point View (Draggable circle)
        AppCompatButton pointBtn = new AppCompatButton(this) {
            @Override
            public boolean performClick() {
                super.performClick();
                return true;
            }
        };
        pointBtn.setText("1");
        pointBtn.setBackgroundColor(0xCCF50057);
        pointBtn.setTextColor(0xFFFFFFFF);

        clickPointParams = new WindowManager.LayoutParams(
                120, 120,
                layoutParamType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );
        clickPointParams.gravity = Gravity.TOP | Gravity.START;
        clickPointParams.x = 200;
        clickPointParams.y = 400;

        pointBtn.setOnTouchListener(new View.OnTouchListener() {
            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = clickPointParams.x;
                        initialY = clickPointParams.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        clickPointParams.x = initialX + (int) (event.getRawX() - initialTouchX);
                        clickPointParams.y = initialY + (int) (event.getRawY() - initialTouchY);
                        windowManager.updateViewLayout(clickPointView, clickPointParams);
                        return true;
                    case MotionEvent.ACTION_UP:
                        v.performClick();
                        return true;
                }
                return false;
            }
        });
        clickPointView = pointBtn;

        // 2. Create Control Panel (Bar with Start/Stop and Close buttons)
        LinearLayout panelLayout = new LinearLayout(this);
        panelLayout.setOrientation(LinearLayout.HORIZONTAL);
        panelLayout.setBackgroundColor(0xCC212121);
        panelLayout.setPadding(10, 10, 10, 10);
        panelLayout.setGravity(Gravity.CENTER);

        btnStartStopControl = new AppCompatButton(this) {
            @Override
            public boolean performClick() {
                super.performClick();
                return true;
            }
        };
        btnStartStopControl.setText("Start");
        btnStartStopControl.setBackgroundColor(0xFF4CAF50);
        btnStartStopControl.setTextColor(0xFFFFFFFF);
        btnStartStopControl.setOnClickListener(v -> toggleClicking());

        AppCompatButton btnClose = new AppCompatButton(this) {
            @Override
            public boolean performClick() {
                super.performClick();
                return true;
            }
        };
        btnClose.setText("X");
        btnClose.setBackgroundColor(0xFFF44336);
        btnClose.setTextColor(0xFFFFFFFF);
        btnClose.setOnClickListener(v -> {
            stopClicking();
            hideOverlay();
        });

        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        btnParams.setMargins(5, 0, 5, 0);

        panelLayout.addView(btnStartStopControl, btnParams);
        panelLayout.addView(btnClose, btnParams);

        controlPanelParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutParamType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );
        controlPanelParams.gravity = Gravity.TOP | Gravity.START;
        controlPanelParams.x = 100;
        controlPanelParams.y = 150;

        panelLayout.setOnTouchListener(new View.OnTouchListener() {
            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = controlPanelParams.x;
                        initialY = controlPanelParams.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        controlPanelParams.x = initialX + (int) (event.getRawX() - initialTouchX);
                        controlPanelParams.y = initialY + (int) (event.getRawY() - initialTouchY);
                        windowManager.updateViewLayout(controlPanelView, controlPanelParams);
                        return true;
                    case MotionEvent.ACTION_UP:
                        v.performClick();
                        return true;
                }
                return false;
            }
        });
        controlPanelView = panelLayout;

        try {
            windowManager.addView(clickPointView, clickPointParams);
            windowManager.addView(controlPanelView, controlPanelParams);
            isShowingOverlay = true;
            Toast.makeText(this, "Đã bật bảng điều khiển nổi", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void hideOverlay() {
        if (isShowingOverlay && windowManager != null) {
            try {
                if (clickPointView != null) windowManager.removeView(clickPointView);
                if (controlPanelView != null) windowManager.removeView(controlPanelView);
            } catch (Exception e) {
                e.printStackTrace();
            }
            clickPointView = null;
            controlPanelView = null;
            isShowingOverlay = false;
        }
    }

    public void toggleClicking() {
        if (isRunning) {
            stopClicking();
        } else {
            startClicking();
        }
    }

    public void startClicking() {
        if (!isShowingOverlay || clickPointView == null) {
            Toast.makeText(this, "Vui lòng thêm điểm click trước!", Toast.LENGTH_SHORT).show();
            return;
        }
        if (isRunning) return;

        isRunning = true;
        if (btnStartStopControl != null) {
            btnStartStopControl.setText("Stop");
            btnStartStopControl.setBackgroundColor(0xFFF44336);
        }

        clickRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isRunning) return;
                float x = clickPointParams.x + (clickPointView.getWidth() > 0 ? clickPointView.getWidth() / 2f : 60f);
                float y = clickPointParams.y + (clickPointView.getHeight() > 0 ? clickPointView.getHeight() / 2f : 60f);
                clickAt(x, y);

                handler.postDelayed(this, 1000);
            }
        };
        handler.post(clickRunnable);
        Toast.makeText(this, "Bắt đầu Auto Click", Toast.LENGTH_SHORT).show();
    }

    public void stopClicking() {
        isRunning = false;
        if (btnStartStopControl != null) {
            btnStartStopControl.setText("Start");
            btnStartStopControl.setBackgroundColor(0xFF4CAF50);
        }
        if (clickRunnable != null) {
            handler.removeCallbacks(clickRunnable);
            clickRunnable = null;
        }
        Toast.makeText(this, "Đã dừng Auto Click", Toast.LENGTH_SHORT).show();
    }

    public boolean isRunning() {
        return isRunning;
    }

    public void clickAt(float x, float y) {
        Path path = new Path();
        path.moveTo(x, y);

        GestureDescription.StrokeDescription stroke =
                new GestureDescription.StrokeDescription(
                        path,
                        0,
                        50
                );

        GestureDescription gesture =
                new GestureDescription.Builder()
                        .addStroke(stroke)
                        .build();

        dispatchGesture(gesture, new GestureResultCallback() {
            @Override
            public void onCompleted(GestureDescription gestureDescription) {
                super.onCompleted(gestureDescription);
            }

            @Override
            public void onCancelled(GestureDescription gestureDescription) {
                super.onCancelled(gestureDescription);
            }
        }, null);
    }
}
