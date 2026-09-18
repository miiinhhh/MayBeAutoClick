package com.example.autoclick.service;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.AppCompatButton;

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

    private AppCompatButton createStyledButton(String text, int bgColor, float textSize) {
        AppCompatButton btn = new AppCompatButton(this) {
            @Override
            public boolean performClick() {
                super.performClick();
                return true;
            }
        };
        btn.setText(text);
        btn.setTextColor(0xFFFFFFFF);
        btn.setTextSize(textSize);
        btn.setTypeface(null, Typeface.BOLD);

        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(16f);
        bg.setColor(bgColor);
        btn.setBackground(bg);

        btn.setPadding(24, 12, 24, 12);
        return btn;
    }

    private View createClickPointView(int id) {
        FrameLayout container = new FrameLayout(this);

        GradientDrawable outerBg = new GradientDrawable();
        outerBg.setShape(GradientDrawable.OVAL);
        outerBg.setColor(0xCCFFFFFF);
        outerBg.setStroke(6, 0xFFFF3D00);
        container.setBackground(outerBg);

        View centerDot = new View(this);
        GradientDrawable dotBg = new GradientDrawable();
        dotBg.setShape(GradientDrawable.OVAL);
        dotBg.setColor(0xFFFF0000);
        centerDot.setBackground(dotBg);

        FrameLayout.LayoutParams dotParams = new FrameLayout.LayoutParams(24, 24);
        dotParams.gravity = Gravity.CENTER;
        container.addView(centerDot, dotParams);

        TextView tvId = new TextView(this);
        tvId.setText(String.valueOf(id));
        tvId.setTextColor(0xFF000000);
        tvId.setTextSize(15f);
        tvId.setTypeface(null, Typeface.BOLD);
        tvId.setGravity(Gravity.CENTER);

        FrameLayout.LayoutParams textParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        );
        container.addView(tvId, textParams);

        return container;
    }

    public void showSettingsOverlay() {
        if (isRunning) {
            Toast.makeText(this, "Vui lòng dừng Auto Click trước khi chỉnh cài đặt!", Toast.LENGTH_SHORT).show();
            return;
        }
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

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(24f);
        bg.setColor(0xFF212121);
        root.setBackground(bg);
        root.setPadding(35, 35, 35, 35);

        TextView tvTitle = new TextView(this);
        tvTitle.setText("⚙ Cài đặt Auto Click");
        tvTitle.setTextColor(0xFFFFFFFF);
        tvTitle.setTextSize(18f);
        tvTitle.setTypeface(null, Typeface.BOLD);
        tvTitle.setPadding(0, 0, 0, 20);
        root.addView(tvTitle);

        // 1. Delay
        TextView tvDelay = new TextView(this);
        tvDelay.setText("Delay giữa các lần click:");
        tvDelay.setTextColor(0xFFCCCCCC);
        root.addView(tvDelay);

        LinearLayout delayLayout = new LinearLayout(this);
        delayLayout.setOrientation(LinearLayout.HORIZONTAL);
        EditText etDelay = new EditText(this);
        boolean delayInSec = delayBetweenClicks >= 1000 && delayBetweenClicks % 1000 == 0;
        etDelay.setText(String.valueOf(delayInSec ? delayBetweenClicks / 1000 : delayBetweenClicks));
        etDelay.setTextColor(0xFFFFFFFF);
        etDelay.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        delayLayout.addView(etDelay, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        Spinner spinnerDelay = new Spinner(this);
        String[] units = {"ms", "s"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, units);
        spinnerDelay.setAdapter(adapter);
        spinnerDelay.setSelection(delayInSec ? 1 : 0);
        delayLayout.addView(spinnerDelay, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        root.addView(delayLayout);

        // 2. Duration
        TextView tvDur = new TextView(this);
        tvDur.setText("Thời gian giữ click:");
        tvDur.setTextColor(0xFFCCCCCC);
        tvDur.setPadding(0, 15, 0, 0);
        root.addView(tvDur);

        LinearLayout durLayout = new LinearLayout(this);
        durLayout.setOrientation(LinearLayout.HORIZONTAL);
        EditText etDur = new EditText(this);
        boolean durInSec = clickDuration >= 1000 && clickDuration % 1000 == 0;
        etDur.setText(String.valueOf(durInSec ? clickDuration / 1000 : clickDuration));
        etDur.setTextColor(0xFFFFFFFF);
        etDur.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        durLayout.addView(etDur, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        Spinner spinnerDur = new Spinner(this);
        spinnerDur.setAdapter(adapter);
        spinnerDur.setSelection(durInSec ? 1 : 0);
        durLayout.addView(spinnerDur, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        root.addView(durLayout);

        // 3. Click Mode
        TextView tvMode = new TextView(this);
        tvMode.setText("Chế độ click:");
        tvMode.setTextColor(0xFFCCCCCC);
        tvMode.setPadding(0, 15, 0, 0);
        root.addView(tvMode);

        RadioGroup rgMode = new RadioGroup(this);
        rgMode.setOrientation(RadioGroup.VERTICAL);
        RadioButton rbMulti = new RadioButton(this); rbMulti.setText("Multi Point (Tuần tự A → B → C)"); rbMulti.setTextColor(0xFFFFFFFF); rbMulti.setId(View.generateViewId()); rgMode.addView(rbMulti);
        RadioButton rbSingle = new RadioButton(this); rbSingle.setText("Single Point (Chỉ click điểm 1)"); rbSingle.setTextColor(0xFFFFFFFF); rbSingle.setId(View.generateViewId()); rgMode.addView(rbSingle);
        RadioButton rbRandom = new RadioButton(this); rbRandom.setText("Random (Ngẫu nhiên các điểm)"); rbRandom.setTextColor(0xFFFFFFFF); rbRandom.setId(View.generateViewId()); rgMode.addView(rbRandom);

        if (clickMode == 0) rgMode.check(rbMulti.getId());
        else if (clickMode == 1) rgMode.check(rbSingle.getId());
        else if (clickMode == 2) rgMode.check(rbRandom.getId());
        root.addView(rgMode);

        // 4. Loop count
        TextView tvLoop = new TextView(this);
        tvLoop.setText("Số lần lặp:");
        tvLoop.setTextColor(0xFFCCCCCC);
        tvLoop.setPadding(0, 15, 0, 0);
        root.addView(tvLoop);

        RadioGroup rgLoop = new RadioGroup(this);
        rgLoop.setOrientation(RadioGroup.VERTICAL);
        RadioButton rb1 = new RadioButton(this); rb1.setText("1 lần"); rb1.setTextColor(0xFFFFFFFF); rb1.setId(View.generateViewId()); rgLoop.addView(rb1);
        RadioButton rb10 = new RadioButton(this); rb10.setText("10 lần"); rb10.setTextColor(0xFFFFFFFF); rb10.setId(View.generateViewId()); rgLoop.addView(rb10);
        RadioButton rb100 = new RadioButton(this); rb100.setText("100 lần"); rb100.setTextColor(0xFFFFFFFF); rb100.setId(View.generateViewId()); rgLoop.addView(rb100);
        RadioButton rbInf = new RadioButton(this); rbInf.setText("Vô hạn"); rbInf.setTextColor(0xFFFFFFFF); rbInf.setId(View.generateViewId()); rgLoop.addView(rbInf);

        if (loopCount == 1) rgLoop.check(rb1.getId());
        else if (loopCount == 10) rgLoop.check(rb10.getId());
        else if (loopCount == 100) rgLoop.check(rb100.getId());
        else rgLoop.check(rbInf.getId());
        root.addView(rgLoop);

        // Buttons
        LinearLayout btnLayout = new LinearLayout(this);
        btnLayout.setOrientation(LinearLayout.HORIZONTAL);
        btnLayout.setGravity(Gravity.END);
        btnLayout.setPadding(0, 20, 0, 0);

        AppCompatButton btnCancel = createStyledButton("Hủy", 0xFF757575, 14f);
        btnCancel.setOnClickListener(v -> hideSettingsOverlay());

        AppCompatButton btnSave = createStyledButton("Lưu", 0xFF4CAF50, 14f);
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

                Toast.makeText(this, "Đã lưu cài đặt!", Toast.LENGTH_SHORT).show();
                hideSettingsOverlay();
            } catch (Exception e) {
                Toast.makeText(this, "Vui lòng nhập số hợp lệ!", Toast.LENGTH_SHORT).show();
            }
        });

        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        bp.setMargins(10, 0, 0, 0);
        btnLayout.addView(btnCancel);
        btnLayout.addView(btnSave, bp);
        root.addView(btnLayout);

        settingsParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
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

        // Create Control Panel Layout
        LinearLayout panelLayout = new LinearLayout(this);
        panelLayout.setOrientation(LinearLayout.HORIZONTAL);

        GradientDrawable panelBg = new GradientDrawable();
        panelBg.setShape(GradientDrawable.RECTANGLE);
        panelBg.setCornerRadius(24f);
        panelBg.setColor(0xEE1E1E1E);
        panelLayout.setBackground(panelBg);

        panelLayout.setPadding(16, 16, 16, 16);
        panelLayout.setGravity(Gravity.CENTER);

        AppCompatButton btnAdd = createStyledButton("+", 0xFF2196F3, 22f);
        btnAdd.setOnClickListener(v -> addClickPoint());

        AppCompatButton btnRemove = createStyledButton("-", 0xFFFF9800, 22f);
        btnRemove.setOnClickListener(v -> removeLastClickPoint());

        AppCompatButton btnSettings = createStyledButton("⚙", 0xFF607D8B, 18f);
        btnSettings.setOnClickListener(v -> showSettingsOverlay());

        btnStartStopControl = createStyledButton("Start", 0xFF4CAF50, 18f);
        btnStartStopControl.setOnClickListener(v -> toggleClicking());

        AppCompatButton btnClose = createStyledButton("X", 0xFFF44336, 18f);
        btnClose.setOnClickListener(v -> {
            stopClicking();
            hideOverlay();
        });

        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        btnParams.setMargins(6, 0, 6, 0);

        panelLayout.addView(btnAdd, btnParams);
        panelLayout.addView(btnRemove, btnParams);
        panelLayout.addView(btnSettings, btnParams);
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
            windowManager.addView(controlPanelView, controlPanelParams);
            isShowingOverlay = true;

            addClickPoint();

            Toast.makeText(this, "Đã bật bảng điều khiển quản lý điểm click", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void addClickPoint() {
        if (isRunning) {
            Toast.makeText(this, "Vui lòng dừng Auto Click trước khi thêm điểm!", Toast.LENGTH_SHORT).show();
            return;
        }

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

        WindowManager.LayoutParams pointParams = new WindowManager.LayoutParams(
                120, 120,
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
            Toast.makeText(this, "Đã thêm điểm số " + newId, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void removeLastClickPoint() {
        if (isRunning) {
            Toast.makeText(this, "Vui lòng dừng Auto Click trước khi xóa điểm!", Toast.LENGTH_SHORT).show();
            return;
        }
        if (clickPoints.isEmpty()) {
            Toast.makeText(this, "Không còn điểm nào để xóa!", Toast.LENGTH_SHORT).show();
            return;
        }

        ClickPointInfo lastPoint = clickPoints.remove(clickPoints.size() - 1);
        try {
            windowManager.removeView(lastPoint.view);
            Toast.makeText(this, "Đã xóa điểm số " + lastPoint.id, Toast.LENGTH_SHORT).show();
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
        if (clickPoints.isEmpty()) {
            Toast.makeText(this, "Vui lòng thêm ít nhất một điểm click!", Toast.LENGTH_SHORT).show();
            return;
        }
        if (isRunning) return;

        isRunning = true;
        currentIndex = 0;
        currentLoopRemaining = loopCount;

        for (ClickPointInfo point : clickPoints) {
            point.params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
            windowManager.updateViewLayout(point.view, point.params);
        }

        if (btnStartStopControl != null) {
            btnStartStopControl.setText("Stop");
            GradientDrawable stopBg = new GradientDrawable();
            stopBg.setShape(GradientDrawable.RECTANGLE);
            stopBg.setCornerRadius(16f);
            stopBg.setColor(0xFFF44336);
            btnStartStopControl.setBackground(stopBg);
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
                            Toast.makeText(AutoClickService.this, "Đã hoàn thành số lần lặp!", Toast.LENGTH_SHORT).show();
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
                            Toast.makeText(AutoClickService.this, "Đã hoàn thành số lần lặp!", Toast.LENGTH_SHORT).show();
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
                                Toast.makeText(AutoClickService.this, "Đã hoàn thành số lần lặp!", Toast.LENGTH_SHORT).show();
                                return;
                            }
                        }
                    }
                    point = clickPoints.get(currentIndex);
                    currentIndex++;
                }

                float x = point.params.x + (point.view.getWidth() > 0 ? point.view.getWidth() / 2f : 60f);
                float y = point.params.y + (point.view.getHeight() > 0 ? point.view.getHeight() / 2f : 60f);

                clickAt(x, y, clickDuration);

                handler.postDelayed(this, delayBetweenClicks);
            }
        };
        handler.post(clickRunnable);
        Toast.makeText(this, "Bắt đầu Auto Click (" + clickPoints.size() + " điểm)", Toast.LENGTH_SHORT).show();
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
            btnStartStopControl.setText("Start");
            GradientDrawable startBg = new GradientDrawable();
            startBg.setShape(GradientDrawable.RECTANGLE);
            startBg.setCornerRadius(16f);
            startBg.setColor(0xFF4CAF50);
            btnStartStopControl.setBackground(startBg);
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
