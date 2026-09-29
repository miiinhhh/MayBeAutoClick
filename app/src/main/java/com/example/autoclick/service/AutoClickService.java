package com.example.autoclick.service;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;

import com.example.autoclick.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class AutoClickService extends AccessibilityService {

    public static class ClickPointInfo {
        int id;
        View view;
        WindowManager.LayoutParams params;

        public ClickPointInfo(int id, View view, WindowManager.LayoutParams params) {
            this.id = id;
            this.view = view;
            this.params = params;
        }
    }

    private static AutoClickService instance;
    private WindowManager windowManager;
    private View controlPanelView;
    private WindowManager.LayoutParams controlPanelParams;

    private View settingsView;
    private WindowManager.LayoutParams settingsParams;
    private boolean isShowingSettings = false;

    private final List<ClickPointInfo> clickPoints = new ArrayList<>();

    private boolean isShowingOverlay = false;
    private boolean isRunning = false;

    // Settings
    private long delayBetweenClicks = 500L; // default 500 ms
    private long clickDuration = 50L;     // default 50 ms
    private int loopCount = -1;           // -1 = infinite, or 1, 10, 100
    private int currentLoopRemaining = -1;
    private int clickMode = 0;            // 0 = Multi Point (Sequential), 1 = Single Point, 2 = Random

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable clickRunnable;
    private int currentIndex = 0;
    private final Random random = new Random();

    private ImageButton btnStartStopControl;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        hideOverlay();
        hideSettingsOverlay();
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

    public int getClickPointsCount() {
        return clickPoints.size();
    }

    public long getDelayBetweenClicks() {
        return delayBetweenClicks;
    }

    public int getLoopCount() {
        return loopCount;
    }

    private View createClickPointView(int id) {
        View pointView = LayoutInflater.from(this).inflate(R.layout.view_floating_target, null);
        TextView tvId = pointView.findViewById(R.id.tvPointId);
        if (tvId != null) {
            tvId.setText(String.valueOf(id));
        }
        return pointView;
    }

    public void showSettingsOverlay() {
        if (isRunning) return;
        if (isShowingSettings) return;

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        int layoutParamType;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutParamType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            @SuppressWarnings("deprecation")
            int type = WindowManager.LayoutParams.TYPE_PHONE;
            layoutParamType = type;
        }

        View root = LayoutInflater.from(this).inflate(R.layout.dialog_floating_settings, null);

        // 1. Delay
        EditText etDelay = root.findViewById(R.id.etDelay);
        Spinner spinnerDelay = root.findViewById(R.id.spinnerDelay);
        boolean delayInSec = delayBetweenClicks >= 1000 && delayBetweenClicks % 1000 == 0;
        etDelay.setText(String.valueOf(delayInSec ? delayBetweenClicks / 1000 : delayBetweenClicks));

        String[] units = {"ms", "s"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.item_spinner, units);
        spinnerDelay.setAdapter(adapter);
        spinnerDelay.setSelection(delayInSec ? 1 : 0);

        // 2. Duration
        EditText etDur = root.findViewById(R.id.etDuration);
        Spinner spinnerDur = root.findViewById(R.id.spinnerDuration);
        boolean durInSec = clickDuration >= 1000 && clickDuration % 1000 == 0;
        etDur.setText(String.valueOf(durInSec ? clickDuration / 1000 : clickDuration));

        ArrayAdapter<String> durAdapter = new ArrayAdapter<>(this, R.layout.item_spinner, units);
        spinnerDur.setAdapter(durAdapter);
        spinnerDur.setSelection(durInSec ? 1 : 0);

        // 3. Click Mode
        RadioGroup rgMode = root.findViewById(R.id.rgClickMode);
        RadioButton rbMulti = root.findViewById(R.id.rbMultiPoint);
        RadioButton rbSingle = root.findViewById(R.id.rbSinglePoint);
        RadioButton rbRandom = root.findViewById(R.id.rbRandomPoint);

        if (clickMode == 0) rgMode.check(rbMulti.getId());
        else if (clickMode == 1) rgMode.check(rbSingle.getId());
        else if (clickMode == 2) rgMode.check(rbRandom.getId());

        // 4. Loop count
        RadioGroup rgLoop = root.findViewById(R.id.rgLoopCount);
        RadioButton rb1 = root.findViewById(R.id.rbLoop1);
        RadioButton rb10 = root.findViewById(R.id.rbLoop10);
        RadioButton rb100 = root.findViewById(R.id.rbLoop100);
        RadioButton rbInf = root.findViewById(R.id.rbLoopInf);

        if (loopCount == 1) rgLoop.check(rb1.getId());
        else if (loopCount == 10) rgLoop.check(rb10.getId());
        else if (loopCount == 100) rgLoop.check(rb100.getId());
        else rgLoop.check(rbInf.getId());

        // Buttons
        View btnCancel = root.findViewById(R.id.btnCancelSettings);
        btnCancel.setOnClickListener(v -> hideSettingsOverlay());

        View btnSave = root.findViewById(R.id.btnSaveSettings);
        btnSave.setOnClickListener(v -> {
            try {
                long delayVal = Long.parseLong(etDelay.getText().toString().trim());
                delayBetweenClicks = spinnerDelay.getSelectedItem().toString().equals("s") ? delayVal * 1000 : delayVal;

                long durVal = Long.parseLong(etDur.getText().toString().trim());
                clickDuration = spinnerDur.getSelectedItem().toString().equals("s") ? durVal * 1000 : durVal;

                int checkedModeId = rgMode.getCheckedRadioButtonId();
                if (checkedModeId == rbMulti.getId()) clickMode = 0;
                else if (checkedModeId == rbSingle.getId()) clickMode = 1;
                else if (checkedModeId == rbRandom.getId()) clickMode = 2;

                int checkedId = rgLoop.getCheckedRadioButtonId();
                if (checkedId == rb1.getId()) loopCount = 1;
                else if (checkedId == rb10.getId()) loopCount = 10;
                else if (checkedId == rb100.getId()) loopCount = 100;
                else loopCount = -1;

                hideSettingsOverlay();
            } catch (Exception e) {
                // Ignore invalid input
            }
        });

        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int dialogWidth = Math.min((int) (screenWidth * 0.9), (int) (380 * getResources().getDisplayMetrics().density));

        settingsParams = new WindowManager.LayoutParams(
                dialogWidth,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutParamType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );
        settingsParams.gravity = Gravity.CENTER;

        settingsView = root;
        try {
            windowManager.addView(settingsView, settingsParams);
            isShowingSettings = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void hideSettingsOverlay() {
        if (isShowingSettings && windowManager != null && settingsView != null) {
            try {
                windowManager.removeView(settingsView);
            } catch (Exception e) {
                e.printStackTrace();
            }
            settingsView = null;
            isShowingSettings = false;
        }
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

        View panelLayout = LayoutInflater.from(this).inflate(R.layout.view_floating_panel, null);

        ImageButton btnAdd = panelLayout.findViewById(R.id.btnOverlayAdd);
        btnAdd.setOnClickListener(v -> addClickPoint());

        ImageButton btnRemove = panelLayout.findViewById(R.id.btnOverlayRemove);
        btnRemove.setOnClickListener(v -> removeLastClickPoint());

        ImageButton btnSettings = panelLayout.findViewById(R.id.btnOverlaySettings);
        btnSettings.setOnClickListener(v -> showSettingsOverlay());

        btnStartStopControl = panelLayout.findViewById(R.id.btnOverlayStartStop);
        btnStartStopControl.setOnClickListener(v -> toggleClicking());

        ImageButton btnClose = panelLayout.findViewById(R.id.btnOverlayClose);
        btnClose.setOnClickListener(v -> {
            stopClicking();
            hideOverlay();
        });

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
            windowManager.addView(controlPanelView, controlPanelParams);
            isShowingOverlay = true;

            addClickPoint();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void addClickPoint() {
        if (isRunning) return;

        int newId = clickPoints.size() + 1;
        int layoutParamType;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutParamType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            @SuppressWarnings("deprecation")
            int type = WindowManager.LayoutParams.TYPE_PHONE;
            layoutParamType = type;
        }

        View pointView = createClickPointView(newId);

        int pointSize = (int) (48 * getResources().getDisplayMetrics().density);
        WindowManager.LayoutParams pointParams = new WindowManager.LayoutParams(
                pointSize, pointSize,
                layoutParamType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );
        pointParams.gravity = Gravity.TOP | Gravity.START;
        pointParams.x = 200 + (clickPoints.size() * 50);
        pointParams.y = 400 + (clickPoints.size() * 50);

        pointView.setOnTouchListener(new View.OnTouchListener() {
            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (isRunning) return false;
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = pointParams.x;
                        initialY = pointParams.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        pointParams.x = initialX + (int) (event.getRawX() - initialTouchX);
                        pointParams.y = initialY + (int) (event.getRawY() - initialTouchY);
                        windowManager.updateViewLayout(pointView, pointParams);
                        return true;
                    case MotionEvent.ACTION_UP:
                        v.performClick();
                        return true;
                }
                return false;
            }
        });

        try {
            windowManager.addView(pointView, pointParams);
            clickPoints.add(new ClickPointInfo(newId, pointView, pointParams));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void removeLastClickPoint() {
        if (isRunning) return;
        if (clickPoints.isEmpty()) return;

        ClickPointInfo lastPoint = clickPoints.remove(clickPoints.size() - 1);
        try {
            windowManager.removeView(lastPoint.view);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void hideOverlay() {
        if (isShowingOverlay && windowManager != null) {
            try {
                for (ClickPointInfo point : clickPoints) {
                    windowManager.removeView(point.view);
                }
                clickPoints.clear();
                if (controlPanelView != null) {
                    windowManager.removeView(controlPanelView);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
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
        if (clickPoints.isEmpty()) return;
        if (isRunning) return;

        isRunning = true;
        currentIndex = 0;
        currentLoopRemaining = loopCount;

        for (ClickPointInfo point : clickPoints) {
            point.params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
            windowManager.updateViewLayout(point.view, point.params);
        }

        if (btnStartStopControl != null) {
            btnStartStopControl.setImageResource(R.drawable.ic_stop);
            btnStartStopControl.setBackgroundResource(R.drawable.bg_circle_button_stop);
        }

        clickRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isRunning || clickPoints.isEmpty()) return;

                ClickPointInfo point;
                if (clickMode == 1) {
                    // Single Point: Always click point 1 (index 0)
                    point = clickPoints.get(0);
                    if (loopCount > 0) {
                        currentLoopRemaining--;
                        if (currentLoopRemaining <= 0) {
                            stopClicking();
                            return;
                        }
                    }
                } else if (clickMode == 2) {
                    // Random Point
                    int randomIndex = random.nextInt(clickPoints.size());
                    point = clickPoints.get(randomIndex);
                    if (loopCount > 0) {
                        currentLoopRemaining--;
                        if (currentLoopRemaining <= 0) {
                            stopClicking();
                            return;
                        }
                    }
                } else {
                    // Multi Point (Sequential A -> B -> C -> ...)
                    if (currentIndex >= clickPoints.size()) {
                        currentIndex = 0;
                        if (loopCount > 0) {
                            currentLoopRemaining--;
                            if (currentLoopRemaining <= 0) {
                                stopClicking();
                                return;
                            }
                        }
                    }
                    point = clickPoints.get(currentIndex);
                    currentIndex++;
                }

                float halfWidth = (point.view.getWidth() > 0 ? point.view.getWidth() / 2f : (24 * getResources().getDisplayMetrics().density));
                float halfHeight = (point.view.getHeight() > 0 ? point.view.getHeight() / 2f : (24 * getResources().getDisplayMetrics().density));
                float x = point.params.x + halfWidth;
                float y = point.params.y + halfHeight;

                clickAt(x, y, clickDuration);

                handler.postDelayed(this, delayBetweenClicks);
            }
        };
        handler.post(clickRunnable);
    }

    public void stopClicking() {
        isRunning = false;

        for (ClickPointInfo point : clickPoints) {
            point.params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
            try {
                windowManager.updateViewLayout(point.view, point.params);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (btnStartStopControl != null) {
            btnStartStopControl.setImageResource(R.drawable.ic_play);
            btnStartStopControl.setBackgroundResource(R.drawable.bg_circle_button_start);
        }
        if (clickRunnable != null) {
            handler.removeCallbacks(clickRunnable);
            clickRunnable = null;
        }
    }

    public boolean isRunning() {
        return isRunning;
    }

    public void clickAt(float x, float y, long duration) {
        Path path = new Path();
        path.moveTo(x, y);

        GestureDescription.StrokeDescription stroke =
                new GestureDescription.StrokeDescription(
                        path,
                        0,
                        duration
                );

        GestureDescription gesture =
                new GestureDescription.Builder()
                        .addStroke(stroke)
                        .build();

        dispatchGesture(gesture, null, null);
    }
}
