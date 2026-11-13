package com.generated.app;

import android.graphics.drawable.Drawable;

public class AppItem {
    public final String packageName;
    public final String appName;
    public final Drawable icon;
    public final long lastTimeUsed;
    public final boolean isStopped;
    public final boolean isSystem;
    public final boolean isLauncher;
    public boolean isProtected;
    public boolean isKilled;
    public boolean isForceStopped;

    public AppItem(
            String packageName,
            String appName,
            Drawable icon,
            long lastTimeUsed,
            boolean isStopped,
            boolean isSystem,
            boolean isLauncher,
            boolean isProtected,
            boolean isKilled,
            boolean isForceStopped) {
        this.packageName = packageName;
        this.appName = appName;
        this.icon = icon;
        this.lastTimeUsed = lastTimeUsed;
        this.isStopped = isStopped;
        this.isSystem = isSystem;
        this.isLauncher = isLauncher;
        this.isProtected = isProtected;
        this.isKilled = isKilled;
        this.isForceStopped = isForceStopped;
    }
}
