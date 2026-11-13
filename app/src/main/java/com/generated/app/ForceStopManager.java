package com.generated.app;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;

import java.util.ArrayList;
import java.util.List;

public class ForceStopManager {

    public interface ForceStopListener {
        void onProgress(int current, int total, AppItem currentItem);
        void onFinished(int forceStoppedCount);
    }

    private static final ForceStopManager INSTANCE = new ForceStopManager();
    private static final long WATCHDOG_TIMEOUT_MS = 1600L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<AppItem> queue = new ArrayList<>();

    private Context appContext;
    private ForceStopListener listener;
    private boolean isProcessing = false;
    private boolean actionHandledForCurrentIndex = false;
    private int currentIndex = 0;
    private int successCount = 0;

    private final Runnable watchdogRunnable = new Runnable() {
        @Override
        public void run() {
            if (isProcessing) {
                onCurrentAppActionCompleted(false);
            }
        }
    };

    private ForceStopManager() {
    }

    public static ForceStopManager getInstance() {
        return INSTANCE;
    }

    public static boolean isAccessibilityEnabled(Context context) {
        if (context == null) {
            return false;
        }
        if (ForceStopAccessibilityService.getInstance() != null) {
            return true;
        }
        try {
            ComponentName expectedComponentName = new ComponentName(context, ForceStopAccessibilityService.class);
            String enabledServicesSetting = Settings.Secure.getString(
                    context.getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            if (enabledServicesSetting == null) {
                return false;
            }

            String expected1 = expectedComponentName.flattenToString();
            String expected2 = expectedComponentName.flattenToShortString();

            for (String service : enabledServicesSetting.split(":")) {
                if (service.equalsIgnoreCase(expected1) || service.equalsIgnoreCase(expected2)) {
                    return true;
                }
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    public boolean isProcessing() {
        return isProcessing;
    }

    public void setListener(ForceStopListener listener) {
        this.listener = listener;
    }

    public void startBatchForceStop(Context context, List<AppItem> items) {
        if (isProcessing || items == null || items.isEmpty()) {
            return;
        }

        this.appContext = context.getApplicationContext();
        this.queue.clear();
        this.queue.addAll(items);
        this.currentIndex = 0;
        this.successCount = 0;
        this.actionHandledForCurrentIndex = false;
        this.isProcessing = true;

        ForceStopAccessibilityService service = ForceStopAccessibilityService.getInstance();
        if (service != null && !items.isEmpty()) {
            AppItem first = items.get(0);
            service.showOverlay(first.appName, first.icon, 1, items.size());
        }

        processNextPackage();
    }

    public void stopBatch() {
        if (!isProcessing) {
            return;
        }
        isProcessing = false;
        handler.removeCallbacks(watchdogRunnable);

        ForceStopAccessibilityService service = ForceStopAccessibilityService.getInstance();
        if (service != null) {
            service.hideOverlay();
        }

        int finalCount = successCount;
        queue.clear();

        if (listener != null) {
            listener.onFinished(finalCount);
        }
        bringAppToFront();
    }

    public synchronized void onCurrentAppActionCompleted(boolean success) {
        if (!isProcessing || actionHandledForCurrentIndex) {
            return;
        }

        actionHandledForCurrentIndex = true;
        handler.removeCallbacks(watchdogRunnable);
        if (success) {
            successCount++;
        }

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!isProcessing) {
                    return;
                }
                currentIndex++;
                processNextPackage();
            }
        }, 120L);
    }

    private void processNextPackage() {
        if (currentIndex >= queue.size()) {
            stopBatch();
            return;
        }

        actionHandledForCurrentIndex = false;
        AppItem item = queue.get(currentIndex);

        ForceStopAccessibilityService service = ForceStopAccessibilityService.getInstance();
        if (service != null) {
            service.updateOverlay(item.appName, item.icon, currentIndex + 1, queue.size());
        }

        if (listener != null) {
            listener.onProgress(currentIndex + 1, queue.size(), item);
        }

        try {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.fromParts("package", item.packageName, null));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NO_ANIMATION);
            appContext.startActivity(intent);
        } catch (Exception e) {
            onCurrentAppActionCompleted(false);
            return;
        }

        handler.removeCallbacks(watchdogRunnable);
        handler.postDelayed(watchdogRunnable, WATCHDOG_TIMEOUT_MS);
    }

    private void bringAppToFront() {
        if (appContext == null) {
            return;
        }
        try {
            Intent intent = new Intent(appContext, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            appContext.startActivity(intent);
        } catch (Exception ignored) {
        }
    }
}
