package com.generated.app;

import android.accessibilityservice.AccessibilityService;
import android.os.SystemClock;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.Locale;

public class ForceStopAccessibilityService extends AccessibilityService {

    private static ForceStopAccessibilityService instance;
    private long lastActionTime = 0L;

    public static ForceStopAccessibilityService getInstance() {
        return instance;
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null) {
            return;
        }

        ForceStopManager manager = ForceStopManager.getInstance();
        if (!manager.isProcessing()) {
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
            if (now - lastActionTime < 180L) {
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
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (instance == this) {
            instance = null;
        }
    }
}
