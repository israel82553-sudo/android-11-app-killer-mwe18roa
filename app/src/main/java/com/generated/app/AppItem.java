package com.generated.app;

import android.graphics.drawable.Drawable;

public class AppItem {
    public final String packageName;
    public final String appName;
    public final Drawable icon;
    public final long lastTimeUsed;
    public final boolean isStopped;
    public boolean isProtected;
    public boolean isKilled;

    public AppItem(
            String packageName,
            String appName,
            Drawable icon,
            long lastTimeUsed,
            boolean isStopped,
            boolean isProtected,
            boolean isKilled) {
        this.packageName = packageName;
        this.appName = appName;
        this.icon = icon;
        this.lastTimeUsed = lastTimeUsed;
        this.isStopped = isStopped;
        this.isProtected = isProtected;
        this.isKilled = isKilled;
    }
}
