package com.generated.app;

import android.accessibilityservice.AccessibilityService;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.Locale;

public class ForceStopAccessibilityService extends AccessibilityService {

    private static ForceStopAccessibilityService instance;
    private long lastActionTime = 0L;

    private WindowManager windowManager;
    private View overlayView;
    private ImageView ivOverlayIcon;
    private TextView tvOverlayAppName;
    private TextView tvOverlayProgressText;
    private ProgressBar progressOverlay;
    private Button btnOverlayCancel;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public static ForceStopAccessibilityService getInstance() {
        return instance;
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
    }

    public void showOverlay(final String appName, final Drawable icon, final int current, final int total) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (overlayView == null) {
                    try {
                        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
                        LayoutInflater inflater = LayoutInflater.from(ForceStopAccessibilityService.this);
                        overlayView = inflater.inflate(R.layout.layout_overlay_progress, null);

                        ivOverlayIcon = overlayView.findViewById(R.id.ivOverlayIcon);
                        tvOverlayAppName = overlayView.findViewById(R.id.tvOverlayAppName);
                        tvOverlayProgressText = overlayView.findViewById(R.id.tvOverlayProgressText);
                        progressOverlay = overlayView.findViewById(R.id.progressOverlay);
                        btnOverlayCancel = overlayView.findViewById(R.id.btnOverlayCancel);

                        if (btnOverlayCancel != null) {
                            btnOverlayCancel.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    ForceStopManager.getInstance().stopBatch();
                                }
                            });
                        }

                        int windowType;
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                            windowType = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY;
                        } else {
                            windowType = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
                        }

                        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                                WindowManager.LayoutParams.MATCH_PARENT,
                                WindowManager.LayoutParams.MATCH_PARENT,
                                windowType,
                                WindowManager.LayoutParams.FLAG_FULLSCREEN | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                                PixelFormat.OPAQUE
                        );

                        if (windowManager != null) {
                            windowManager.addView(overlayView, params);
                        }
                    } catch (Exception e) {
                        overlayView = null;
                    }
                }
                updateOverlayInternal(appName, icon, current, total);
            }
        });
    }

    public void updateOverlay(final String appName, final Drawable icon, final int current, final int total) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                updateOverlayInternal(appName, icon, current, total);
            }
        });
    }

    private void updateOverlayInternal(String appName, Drawable icon, int current, int total) {
        if (overlayView == null) return;
        if (tvOverlayAppName != null) tvOverlayAppName.setText(appName);
        if (ivOverlayIcon != null && icon != null) ivOverlayIcon.setImageDrawable(icon);
        if (tvOverlayProgressText != null) {
            tvOverlayProgressText.setText(getString(R.string.force_stop_progress_format, appName, current, total));
        }
        if (progressOverlay != null) {
            progressOverlay.setMax(total);
            progressOverlay.setProgress(current);
        }
    }

    public void hideOverlay() {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (overlayView != null && windowManager != null) {
                    try {
                        windowManager.removeView(overlayView);
                    } catch (Exception ignored) {
                    }
                    overlayView = null;
                }
            }
        });
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null) {
            return;
        }

        ForceStopManager manager = ForceStopManager.getInstance();
        if (!manager.isProcessing()) {
            hideOverlay();
            return;
        }

        CharSequence pkgCharSeq = event.getPackageName();
        if (pkgCharSeq == null) {
            return;
        }
        String pkgName = pkgCharSeq.toString();

        if (pkgName.equals("com.android.settings") ||
                pkgName.contains("settings") ||
                pkgName.equals("android") ||
                pkgName.contains("permissioncontroller") ||
                pkgName.contains("securitycenter")) {

            long now = SystemClock.uptimeMillis();
            if (now - lastActionTime < 100L) {
                return;
            }

            AccessibilityNodeInfo root = getRootInActiveWindow();
            if (root != null) {
                try {
                    processWindow(root, manager);
                } finally {
                    root.recycle();
                }
            }
        }
    }

    private void processWindow(AccessibilityNodeInfo root, ForceStopManager manager) {
        // Step 1: Check for Confirmation Dialog first
        String[] dialogIds = new String[] {
                "android:id/button1",
                "button1",
                "com.android.settings:id/right_button",
                "com.miui.securitycenter:id/confirm"
        };
        String[] dialogTexts = new String[] {
                "אישור", "עצירה כפויה", "עצור", "סגור", "כן", "OK", "Ok", "Force stop", "Yes", "Confirm"
        };

        AccessibilityNodeInfo dialogButton = findNodeByTextsOrIds(root, dialogIds, dialogTexts);
        if (dialogButton != null && dialogButton.isEnabled()) {
            if (performClickNode(dialogButton)) {
                lastActionTime = SystemClock.uptimeMillis();
                performGlobalAction(GLOBAL_ACTION_BACK);
                manager.onCurrentAppActionCompleted(true);
                return;
            }
        }

        // Step 2: Check for Force Stop button in App Info
        String[] forceStopIds = new String[] {
                "com.android.settings:id/force_stop_button",
                "force_stop_button",
                "right_button",
                "left_button",
                "button1"
        };
        String[] forceStopTexts = new String[] {
                "עצירה כפויה", "עצור כעת", "עצור תהליך", "עצירה", "סגירה כפויה", "Force stop", "FORCE STOP", "Stop"
        };

        AccessibilityNodeInfo forceStopBtn = findNodeByTextsOrIds(root, forceStopIds, forceStopTexts);
        if (forceStopBtn != null) {
            if (!forceStopBtn.isEnabled()) {
                // Already force stopped!
                lastActionTime = SystemClock.uptimeMillis();
                performGlobalAction(GLOBAL_ACTION_BACK);
                manager.onCurrentAppActionCompleted(true);
            } else {
                if (performClickNode(forceStopBtn)) {
                    lastActionTime = SystemClock.uptimeMillis();
                }
            }
        }
    }

    private AccessibilityNodeInfo findNodeByTextsOrIds(AccessibilityNodeInfo root, String[] ids, String[] texts) {
        if (root == null) {
            return null;
        }

        if (ids != null && root.getViewIdResourceName() != null) {
            String resId = root.getViewIdResourceName();
            for (String id : ids) {
                if (resId.equalsIgnoreCase(id) || resId.endsWith(":id/" + id)) {
                    return root;
                }
            }
        }

        if (texts != null && root.getText() != null) {
            String text = root.getText().toString().trim().toLowerCase(Locale.getDefault());
            for (String t : texts) {
                if (text.equals(t.toLowerCase(Locale.getDefault())) || text.contains(t.toLowerCase(Locale.getDefault()))) {
                    return root;
                }
            }
        }

        if (texts != null && root.getContentDescription() != null) {
            String desc = root.getContentDescription().toString().trim().toLowerCase(Locale.getDefault());
            for (String t : texts) {
                if (desc.contains(t.toLowerCase(Locale.getDefault()))) {
                    return root;
                }
            }
        }

        int childCount = root.getChildCount();
        for (int i = 0; i < childCount; i++) {
            AccessibilityNodeInfo child = root.getChild(i);
            if (child != null) {
                AccessibilityNodeInfo result = findNodeByTextsOrIds(child, ids, texts);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }

    private boolean performClickNode(AccessibilityNodeInfo node) {
        if (node == null) {
            return false;
        }
        if (node.isClickable()) {
            return node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        }
        AccessibilityNodeInfo parent = node.getParent();
        while (parent != null) {
            if (parent.isClickable()) {
                return parent.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            }
            parent = parent.getParent();
        }
        return false;
    }

    @Override
    public void onInterrupt() {
        hideOverlay();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        hideOverlay();
        if (instance == this) {
            instance = null;
        }
    }
}
