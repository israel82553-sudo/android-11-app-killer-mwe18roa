package com.generated.app;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AppOpsManager;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity implements AppListAdapter.AppActionListener, ForceStopManager.ForceStopListener {

    private static final String PREFS_NAME = "app_killer_prefs";
    private static final String KEY_PROTECTED = "protected_packages";
    private static final String KEY_THEME_MODE = "theme_mode_dark";
    private static final long RECENT_WINDOW_MS = 24L * 60L * 60L * 1000L;

    private ActivityManager activityManager;
    private PackageManager packageManager;
    private SharedPreferences prefs;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private final List<AppItem> allLoadedApps = new ArrayList<>();
    private final Set<String> killedThisSession = new HashSet<>();
    private final Set<String> forceStoppedThisSession = new HashSet<>();
    private final Set<String> protectedPackages = new HashSet<>();

    private AppListAdapter adapter;

    private RelativeLayout rootView;
    private LinearLayout layoutHeader;
    private ImageButton btnThemeToggle;
    private ImageButton btnRefresh;
    private Button btnQuickBoost;
    private TextView tvRamSummary;
    private TextView tvRamFree;
    private ProgressBar progressRam;
    private TextView tvAppCountBadge;
    private LinearLayout cardUsagePermission;
    private Button btnGrantUsage;

    private LinearLayout cardAccessibilityPermission;
    private Button btnGrantAccessibility;

    private TextView btnModeForceStop;
    private TextView btnModeQuickKill;

    private LinearLayout layoutTabContainer;
    private LinearLayout layoutSearchContainer;
    private LinearLayout layoutBottomBar;
    private TextView tvFooterNote;

    private TextView btnTabRecent;
    private TextView btnTabAll;
    private EditText etSearch;
    private ImageButton btnClearSearch;
    private ListView lvApps;
    private ProgressBar progressLoading;
    private LinearLayout layoutEmpty;
    private TextView tvEmptyMessage;
    private Button btnEmptySwitchTab;
    private TextView tvResultBanner;
    private Button btnKillAll;

    private LinearLayout layoutBatchProgress;
    private TextView tvBatchProgressText;
    private ProgressBar progressBatch;
    private Button btnCancelBatch;

    private boolean isDarkMode = false;
    private boolean showRecentOnly = true;
    private boolean useForceStopMode = true;
    private boolean userSelectedTab = false;
    private boolean hasUsagePermission = false;
    private boolean hasAccessibilityPermission = false;
    private boolean isLoading = false;
    private boolean isKillingAll = false;
    private String searchQuery = "";
    private String defaultLauncherPackage = "";

    private final Runnable hideBannerRunnable = new Runnable() {
        @Override
        public void run() {
            if (tvResultBanner != null) {
                tvResultBanner.setVisibility(View.GONE);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        packageManager = getPackageManager();
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // Detect auto system dark mode or stored preference
        boolean systemDarkMode = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        isDarkMode = prefs.getBoolean(KEY_THEME_MODE, systemDarkMode);

        setContentView(R.layout.activity_main);

        detectDefaultLauncher();

        Set<String> savedProtected = prefs.getStringSet(KEY_PROTECTED, null);
        if (savedProtected != null) {
            protectedPackages.addAll(savedProtected);
        }
        if (!defaultLauncherPackage.isEmpty()) {
            protectedPackages.add(defaultLauncherPackage);
        }

        bindViews();
        adaptSmallScreenDimensions();
        applyThemeVisuals();
        setupListeners();

        adapter = new AppListAdapter(this, this);
        adapter.setDarkMode(isDarkMode);
        lvApps.setAdapter(adapter);

        ForceStopManager.getInstance().setListener(this);

        hasUsagePermission = hasUsageStatsPermission();
        hasAccessibilityPermission = ForceStopManager.isAccessibilityEnabled(this);

        showRecentOnly = hasUsagePermission;
        updateTabStyles();
        updateModeStyles();
    }

    @Override
    protected void onResume() {
        super.onResume();
        hasUsagePermission = hasUsageStatsPermission();
        hasAccessibilityPermission = ForceStopManager.isAccessibilityEnabled(this);

        if (!userSelectedTab) {
            showRecentOnly = hasUsagePermission;
            updateTabStyles();
        }

        updateAccessibilityBanner();
        updateRamInfo();
        loadAppsAsync(false);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ForceStopManager.getInstance().setListener(null);
        mainHandler.removeCallbacks(hideBannerRunnable);
        executor.shutdownNow();
    }

    private void detectDefaultLauncher() {
        try {
            Intent homeIntent = new Intent(Intent.ACTION_MAIN);
            homeIntent.addCategory(Intent.CATEGORY_HOME);
            ResolveInfo info = packageManager.resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY);
            if (info != null && info.activityInfo != null) {
                defaultLauncherPackage = info.activityInfo.packageName;
            }
        } catch (Exception ignored) {
        }
    }

    private void bindViews() {
        rootView = findViewById(R.id.rootView);
        layoutHeader = findViewById(R.id.layoutHeader);
        btnThemeToggle = findViewById(R.id.btnThemeToggle);
        btnRefresh = findViewById(R.id.btnRefresh);
        btnQuickBoost = findViewById(R.id.btnQuickBoost);
        tvRamSummary = findViewById(R.id.tvRamSummary);
        tvRamFree = findViewById(R.id.tvRamFree);
        progressRam = findViewById(R.id.progressRam);
        tvAppCountBadge = findViewById(R.id.tvAppCountBadge);
        cardUsagePermission = findViewById(R.id.cardUsagePermission);
        btnGrantUsage = findViewById(R.id.btnGrantUsage);

        cardAccessibilityPermission = findViewById(R.id.cardAccessibilityPermission);
        btnGrantAccessibility = findViewById(R.id.btnGrantAccessibility);

        btnModeForceStop = findViewById(R.id.btnModeForceStop);
        btnModeQuickKill = findViewById(R.id.btnModeQuickKill);

        layoutTabContainer = findViewById(R.id.layoutTabContainer);
        layoutSearchContainer = findViewById(R.id.layoutSearchContainer);
        layoutBottomBar = findViewById(R.id.layoutBottomBar);
        tvFooterNote = findViewById(R.id.tvFooterNote);

        btnTabRecent = findViewById(R.id.btnTabRecent);
        btnTabAll = findViewById(R.id.btnTabAll);
        etSearch = findViewById(R.id.etSearch);
        btnClearSearch = findViewById(R.id.btnClearSearch);
        lvApps = findViewById(R.id.lvApps);
        progressLoading = findViewById(R.id.progressLoading);
        layoutEmpty = findViewById(R.id.layoutEmpty);
        tvEmptyMessage = findViewById(R.id.tvEmptyMessage);
        btnEmptySwitchTab = findViewById(R.id.btnEmptySwitchTab);
        tvResultBanner = findViewById(R.id.tvResultBanner);
        btnKillAll = findViewById(R.id.btnKillAll);

        layoutBatchProgress = findViewById(R.id.layoutBatchProgress);
        tvBatchProgressText = findViewById(R.id.tvBatchProgressText);
        progressBatch = findViewById(R.id.progressBatch);
        btnCancelBatch = findViewById(R.id.btnCancelBatch);
    }

    private void adaptSmallScreenDimensions() {
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        float dpHeight = metrics.heightPixels / metrics.density;

        // If on small screen device (< 650dp height), optimize vertical padding dynamically
        if (dpHeight < 650) {
            float density = metrics.density;
            layoutHeader.setPadding((int) (12 * density), (int) (8 * density), (int) (12 * density), (int) (10 * density));
            findViewById(R.id.layoutRamCard).setPadding((int) (10 * density), (int) (8 * density), (int) (10 * density), (int) (8 * density));
            layoutBottomBar.setPadding((int) (12 * density), (int) (6 * density), (int) (12 * density), (int) (8 * density));
            tvFooterNote.setVisibility(View.GONE);
        }
    }

    private void applyThemeVisuals() {
        int statusBarColor = isDarkMode ? 0xFF090D16 : 0xFF0F172A;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            getWindow().setStatusBarColor(statusBarColor);
        }

        int bgColor = isDarkMode ? getCompatColor(R.color.bg_screen_dark) : getCompatColor(R.color.bg_screen_light);
        int cardBgColor = isDarkMode ? getCompatColor(R.color.bg_card_dark) : getCompatColor(R.color.bg_card_light);
        int borderSubtleColor = isDarkMode ? getCompatColor(R.color.border_subtle_dark) : getCompatColor(R.color.border_subtle_light);
        int textPrimary = isDarkMode ? getCompatColor(R.color.text_primary_dark) : getCompatColor(R.color.text_primary_light);
        int textSecondary = isDarkMode ? getCompatColor(R.color.text_secondary_dark) : getCompatColor(R.color.text_secondary_light);
        int headerBgColor = isDarkMode ? getCompatColor(R.color.bg_header_dark) : getCompatColor(R.color.bg_header_light);

        rootView.setBackgroundColor(bgColor);

        // Header shape
        layoutHeader.setBackground(createRoundedDrawable(headerBgColor, 0, 0, 20));

        // Tab container background
        int tabContainerColor = isDarkMode ? getCompatColor(R.color.bg_control_dark) : getCompatColor(R.color.bg_control_light);
        layoutTabContainer.setBackground(createRoundedDrawable(tabContainerColor, 0, 0, 12));

        // Search container
        layoutSearchContainer.setBackground(createRoundedDrawable(cardBgColor, borderSubtleColor, 1, 12));
        etSearch.setTextColor(textPrimary);
        etSearch.setHintTextColor(getCompatColor(R.color.text_muted));

        // Bottom bar
        layoutBottomBar.setBackgroundColor(cardBgColor);
        tvEmptyMessage.setTextColor(textSecondary);

        if (adapter != null) {
            adapter.setDarkMode(isDarkMode);
        }
    }

    private GradientDrawable createRoundedDrawable(int fillColor, int strokeColor, int strokeWidthDp, int radiusDp) {
        GradientDrawable gd = new GradientDrawable();
        gd.setShape(GradientDrawable.RECTANGLE);
        gd.setColor(fillColor);
        float density = getResources().getDisplayMetrics().density;
        gd.setCornerRadius(radiusDp * density);
        if (strokeWidthDp > 0) {
            gd.setStroke((int) (strokeWidthDp * density), strokeColor);
        }
        return gd;
    }

    private void setupListeners() {
        btnThemeToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isDarkMode = !isDarkMode;
                prefs.edit().putBoolean(KEY_THEME_MODE, isDarkMode).apply();
                applyThemeVisuals();
                updateTabStyles();
                updateModeStyles();
                Toast.makeText(MainActivity.this,
                        isDarkMode ? R.string.toast_theme_switched_dark : R.string.toast_theme_switched_light,
                        Toast.LENGTH_SHORT).show();
            }
        });

        btnRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                killedThisSession.clear();
                forceStoppedThisSession.clear();
                v.animate().rotationBy(360f).setDuration(420).start();
                updateRamInfo();
                loadAppsAsync(true);
            }
        });

        btnQuickBoost.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                killAllVisibleAppsQuick();
            }
        });

        btnGrantUsage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openUsageAccessSettings();
            }
        });

        btnGrantAccessibility.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openAccessibilitySettings();
            }
        });

        btnModeForceStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!useForceStopMode) {
                    useForceStopMode = true;
                    updateModeStyles();
                    applyFilterAndUpdateUi();
                }
            }
        });

        btnModeQuickKill.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (useForceStopMode) {
                    useForceStopMode = false;
                    updateModeStyles();
                    applyFilterAndUpdateUi();
                }
            }
        });

        btnTabRecent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                userSelectedTab = true;
                if (!showRecentOnly) {
                    showRecentOnly = true;
                    updateTabStyles();
                    applyFilterAndUpdateUi();
                }
            }
        });

        btnTabAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                userSelectedTab = true;
                if (showRecentOnly) {
                    showRecentOnly = false;
                    updateTabStyles();
                    applyFilterAndUpdateUi();
                }
            }
        });

        btnEmptySwitchTab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                userSelectedTab = true;
                showRecentOnly = false;
                updateTabStyles();
                applyFilterAndUpdateUi();
            }
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s == null ? "" : s.toString().trim().toLowerCase(Locale.getDefault());
                btnClearSearch.setVisibility(searchQuery.isEmpty() ? View.GONE : View.VISIBLE);
                applyFilterAndUpdateUi();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        btnClearSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                etSearch.setText("");
            }
        });

        btnKillAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (useForceStopMode) {
                    if (!hasAccessibilityPermission) {
                        openAccessibilitySettings();
                        Toast.makeText(MainActivity.this, R.string.toast_accessibility_required, Toast.LENGTH_LONG).show();
                    } else {
                        startForceStopBatch();
                    }
                } else {
                    killAllVisibleAppsQuick();
                }
            }
        });

        btnCancelBatch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ForceStopManager.getInstance().stopBatch();
            }
        });
    }

    private void updateModeStyles() {
        int unselectedTextColor = isDarkMode ? getCompatColor(R.color.text_secondary_dark) : getCompatColor(R.color.text_secondary_light);
        if (useForceStopMode) {
            btnModeForceStop.setBackgroundResource(R.drawable.bg_tab_selected);
            btnModeForceStop.setTextColor(0xFFFFFFFF);
            btnModeQuickKill.setBackground(null);
            btnModeQuickKill.setTextColor(unselectedTextColor);
        } else {
            btnModeQuickKill.setBackgroundResource(R.drawable.bg_tab_selected);
            btnModeQuickKill.setTextColor(0xFFFFFFFF);
            btnModeForceStop.setBackground(null);
            btnModeForceStop.setTextColor(unselectedTextColor);
        }
        updateAccessibilityBanner();
    }

    private void updateAccessibilityBanner() {
        if (useForceStopMode && !hasAccessibilityPermission) {
            cardAccessibilityPermission.setVisibility(View.VISIBLE);
        } else {
            cardAccessibilityPermission.setVisibility(View.GONE);
        }
    }

    private void updateTabStyles() {
        int unselectedTextColor = isDarkMode ? getCompatColor(R.color.text_secondary_dark) : getCompatColor(R.color.text_secondary_light);
        if (showRecentOnly) {
            btnTabRecent.setBackgroundResource(R.drawable.bg_tab_selected);
            btnTabRecent.setTextColor(0xFFFFFFFF);
            btnTabAll.setBackground(null);
            btnTabAll.setTextColor(unselectedTextColor);
        } else {
            btnTabAll.setBackgroundResource(R.drawable.bg_tab_selected);
            btnTabAll.setTextColor(0xFFFFFFFF);
            btnTabRecent.setBackground(null);
            btnTabRecent.setTextColor(unselectedTextColor);
        }
    }

    @SuppressWarnings("deprecation")
    private int getCompatColor(int colorResId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return getColor(colorResId);
        }
        return getResources().getColor(colorResId);
    }

    private void updateRamInfo() {
        if (activityManager == null) {
            return;
        }
        ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memInfo);

        long total = Math.max(1L, memInfo.totalMem);
        long avail = Math.max(0L, memInfo.availMem);
        long used = Math.max(0L, total - avail);
        int usedPercent = (int) Math.min(100L, Math.max(0L, (used * 100L) / total));
        int freePercent = Math.max(0, 100 - usedPercent);

        progressRam.setProgress(usedPercent);
        tvRamSummary.setText(getString(R.string.ram_used_format, formatBytes(used), formatBytes(total)));
        tvRamFree.setText(getString(R.string.ram_free_format, formatBytes(avail), freePercent));
    }

    private void loadAppsAsync(final boolean showSpinner) {
        if (isLoading || executor.isShutdown()) {
            return;
        }
        isLoading = true;
        hasUsagePermission = hasUsageStatsPermission();
        cardUsagePermission.setVisibility(hasUsagePermission ? View.GONE : View.VISIBLE);

        if (showSpinner || allLoadedApps.isEmpty()) {
            progressLoading.setVisibility(View.VISIBLE);
            layoutEmpty.setVisibility(View.GONE);
        }

        final boolean usageAllowed = hasUsagePermission;
        final Set<String> protectedSnapshot = new HashSet<>(protectedPackages);
        final Set<String> killedSnapshot = new HashSet<>(killedThisSession);
        final Set<String> forceStoppedSnapshot = new HashSet<>(forceStoppedThisSession);

        executor.execute(new Runnable() {
            @Override
            public void run() {
                final List<AppItem> loaded = queryInstalledAndRecentApps(
                        usageAllowed, protectedSnapshot, killedSnapshot, forceStoppedSnapshot);

                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }
                        isLoading = false;
                        progressLoading.setVisibility(View.GONE);
                        allLoadedApps.clear();
                        allLoadedApps.addAll(loaded);
                        applyFilterAndUpdateUi();
                    }
                });
            }
        });
    }

    private List<AppItem> queryInstalledAndRecentApps(
            boolean usageAllowed,
            Set<String> protectedSnapshot,
            Set<String> killedSnapshot,
            Set<String> forceStoppedSnapshot) {

        Map<String, Long> recentUsageMap = new HashMap<>();
        if (usageAllowed) {
            UsageStatsManager usm = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
            if (usm != null) {
                long endTime = System.currentTimeMillis();
                long beginTime = endTime - RECENT_WINDOW_MS;

                List<UsageStats> statsList = usm.queryUsageStats(
                        UsageStatsManager.INTERVAL_BEST, beginTime, endTime);
                if (statsList != null) {
                    for (UsageStats stats : statsList) {
                        if (stats != null) {
                            long lastUsed = stats.getLastTimeUsed();
                            if (lastUsed >= beginTime && lastUsed <= endTime + 60000L) {
                                String pkg = stats.getPackageName();
                                if (pkg != null) {
                                    Long existing = recentUsageMap.get(pkg);
                                    if (existing == null || lastUsed > existing) {
                                        recentUsageMap.put(pkg, lastUsed);
                                    }
                                }
                            }
                        }
                    }
                }

                try {
                    UsageEvents events = usm.queryEvents(beginTime, endTime);
                    if (events != null) {
                        UsageEvents.Event event = new UsageEvents.Event();
                        while (events.hasNextEvent()) {
                            events.getNextEvent(event);
                            int type = event.getEventType();
                            boolean isForeground = (type == UsageEvents.Event.MOVE_TO_FOREGROUND);
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                isForeground = isForeground || (type == UsageEvents.Event.ACTIVITY_RESUMED);
                            }
                            if (isForeground && event.getPackageName() != null) {
                                long ts = event.getTimeStamp();
                                if (ts >= beginTime && ts <= endTime + 60000L) {
                                    String pkg = event.getPackageName();
                                    Long existing = recentUsageMap.get(pkg);
                                    if (existing == null || ts > existing) {
                                        recentUsageMap.put(pkg, ts);
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception ignored) {
                }
            }
        }

        Set<String> launchablePackages = new HashSet<>();
        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> resolveInfos = packageManager.queryIntentActivities(mainIntent, 0);
        if (resolveInfos != null) {
            for (ResolveInfo ri : resolveInfos) {
                if (ri.activityInfo != null && ri.activityInfo.packageName != null) {
                    launchablePackages.add(ri.activityInfo.packageName);
                }
            }
        }

        String selfPackage = getPackageName();
        List<ApplicationInfo> installed = packageManager.getInstalledApplications(0);
        List<AppItem> result = new ArrayList<>();

        if (installed != null) {
            for (ApplicationInfo appInfo : installed) {
                String pkg = appInfo.packageName;
                if (pkg == null || pkg.equals(selfPackage) || !appInfo.enabled) {
                    continue;
                }
                boolean isPersistent = (appInfo.flags & ApplicationInfo.FLAG_PERSISTENT) != 0;
                if (isPersistent) {
                    continue;
                }
                boolean isSystem = (appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
                boolean isLaunchable = launchablePackages.contains(pkg);
                if (isSystem && !isLaunchable) {
                    continue;
                }

                boolean isLauncher = !defaultLauncherPackage.isEmpty() && defaultLauncherPackage.equals(pkg);
                boolean isStopped = (appInfo.flags & ApplicationInfo.FLAG_STOPPED) != 0;
                Long lastUsedObj = recentUsageMap.get(pkg);
                long lastUsed = lastUsedObj != null ? lastUsedObj : 0L;

                CharSequence labelSeq = appInfo.loadLabel(packageManager);
                String appName = labelSeq != null ? labelSeq.toString().trim() : pkg;
                if (appName.isEmpty()) {
                    appName = pkg;
                }

                Drawable icon;
                try {
                    icon = appInfo.loadIcon(packageManager);
                } catch (Exception e) {
                    icon = packageManager.getDefaultActivityIcon();
                }

                result.add(new AppItem(
                        pkg,
                        appName,
                        icon,
                        lastUsed,
                        isStopped,
                        isSystem,
                        isLauncher,
                        protectedSnapshot.contains(pkg) || isLauncher,
                        killedSnapshot.contains(pkg),
                        forceStoppedSnapshot.contains(pkg)));
            }
        }

        final Collator collator = Collator.getInstance(new Locale("he", "IL"));
        Collections.sort(result, new Comparator<AppItem>() {
            @Override
            public int compare(AppItem a, AppItem b) {
                if (a.lastTimeUsed != b.lastTimeUsed) {
                    return Long.compare(b.lastTimeUsed, a.lastTimeUsed);
                }
                if (a.isForceStopped != b.isForceStopped) {
                    return a.isForceStopped ? 1 : -1;
                }
                if (a.isStopped != b.isStopped) {
                    return a.isStopped ? 1 : -1;
                }
                if (a.isSystem != b.isSystem) {
                    return a.isSystem ? 1 : -1;
                }
                return collator.compare(a.appName, b.appName);
            }
        });

        return result;
    }

    private void applyFilterAndUpdateUi() {
        List<AppItem> filtered = new ArrayList<>();
        int protectedVisibleCount = 0;

        for (AppItem item : allLoadedApps) {
            if (showRecentOnly) {
                if (!hasUsagePermission || item.lastTimeUsed <= 0L) {
                    continue;
                }
            }
            if (!searchQuery.isEmpty()) {
                String nameLower = item.appName.toLowerCase(Locale.getDefault());
                String pkgLower = item.packageName.toLowerCase(Locale.getDefault());
                if (!nameLower.contains(searchQuery) && !pkgLower.contains(searchQuery)) {
                    continue;
                }
            }
            if (item.isProtected) {
                protectedVisibleCount++;
            }
            filtered.add(item);
        }

        adapter.setItems(filtered);
        if (protectedVisibleCount > 0) {
            tvAppCountBadge.setText(getString(
                    R.string.apps_count_with_protected_format,
                    filtered.size(),
                    protectedVisibleCount));
        } else {
            tvAppCountBadge.setText(getString(R.string.apps_count_format, filtered.size()));
        }

        if (filtered.isEmpty() && !isLoading) {
            layoutEmpty.setVisibility(View.VISIBLE);
            lvApps.setVisibility(View.GONE);
            if (!searchQuery.isEmpty()) {
                tvEmptyMessage.setText(R.string.empty_search);
                btnEmptySwitchTab.setVisibility(View.GONE);
            } else if (showRecentOnly && !hasUsagePermission) {
                tvEmptyMessage.setText(R.string.empty_recent_no_perm);
                btnEmptySwitchTab.setVisibility(View.VISIBLE);
            } else if (showRecentOnly) {
                tvEmptyMessage.setText(R.string.empty_recent_none);
                btnEmptySwitchTab.setVisibility(View.VISIBLE);
            } else {
                tvEmptyMessage.setText(R.string.empty_all_none);
                btnEmptySwitchTab.setVisibility(View.GONE);
            }
        } else {
            layoutEmpty.setVisibility(View.GONE);
            lvApps.setVisibility(View.VISIBLE);
        }

        updateKillAllButtonState(filtered);
    }

    private void updateKillAllButtonState(List<AppItem> visibleItems) {
        if (isKillingAll || ForceStopManager.getInstance().isProcessing()) {
            btnKillAll.setText(R.string.btn_killing);
            btnKillAll.setEnabled(false);
            return;
        }

        int targetCount = 0;
        for (AppItem item : visibleItems) {
            boolean isAlreadyStopped = item.isForceStopped || item.isKilled || item.isStopped;
            if (!item.isProtected && !isAlreadyStopped) {
                targetCount++;
            }
        }

        if (targetCount > 0) {
            if (useForceStopMode) {
                btnKillAll.setText(getString(R.string.btn_force_stop_all, targetCount));
            } else {
                btnKillAll.setText(getString(R.string.btn_kill_all, targetCount));
            }
            btnKillAll.setEnabled(true);
        } else {
            btnKillAll.setText(R.string.btn_kill_all_none);
            btnKillAll.setEnabled(false);
        }
    }

    private void startForceStopBatch() {
        List<AppItem> targets = new ArrayList<>();
        int count = adapter.getCount();
        for (int i = 0; i < count; i++) {
            AppItem item = adapter.getItem(i);
            boolean isAlreadyStopped = item.isForceStopped || item.isKilled || item.isStopped;
            if (!item.isProtected && !isAlreadyStopped) {
                targets.add(item);
            }
        }

        if (targets.isEmpty()) {
            showResultBanner(getString(R.string.result_none_to_kill));
            return;
        }

        layoutBatchProgress.setVisibility(View.VISIBLE);
        progressBatch.setMax(targets.size());
        progressBatch.setProgress(0);
        btnKillAll.setEnabled(false);

        ForceStopManager.getInstance().startBatchForceStop(this, targets);
    }

    private void killAllVisibleAppsQuick() {
        if (isKillingAll || activityManager == null || executor.isShutdown()) {
            return;
        }

        final List<AppItem> targets = new ArrayList<>();
        int count = adapter.getCount();
        for (int i = 0; i < count; i++) {
            AppItem item = adapter.getItem(i);
            boolean isAlreadyStopped = item.isForceStopped || item.isKilled || item.isStopped;
            if (!item.isProtected && !isAlreadyStopped) {
                targets.add(item);
            }
        }

        if (targets.isEmpty()) {
            showResultBanner(getString(R.string.result_none_to_kill));
            return;
        }

        isKillingAll = true;
        btnKillAll.setText(R.string.btn_killing);
        btnKillAll.setEnabled(false);

        ActivityManager.MemoryInfo beforeMem = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(beforeMem);
        final long availBefore = beforeMem.availMem;

        executor.execute(new Runnable() {
            @Override
            public void run() {
                for (AppItem item : targets) {
                    try {
                        activityManager.killBackgroundProcesses(item.packageName);
                    } catch (SecurityException ignored) {
                    }
                }

                try {
                    Thread.sleep(350L);
                } catch (InterruptedException ignored) {
                }

                ActivityManager.MemoryInfo afterMem = new ActivityManager.MemoryInfo();
                activityManager.getMemoryInfo(afterMem);
                final long freedBytes = Math.max(0L, afterMem.availMem - availBefore);

                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }
                        for (AppItem item : targets) {
                            item.isKilled = true;
                            killedThisSession.add(item.packageName);
                        }
                        isKillingAll = false;
                        adapter.notifyDataSetChanged();
                        updateRamInfo();
                        applyFilterAndUpdateUi();

                        if (freedBytes >= 1024L * 1024L) {
                            showResultBanner(getString(
                                    R.string.result_killed_freed,
                                    targets.size(),
                                    formatBytes(freedBytes)));
                        } else {
                            showResultBanner(getString(R.string.result_killed_only, targets.size()));
                        }
                    }
                });
            }
        });
    }

    @Override
    public void onProgress(final int current, final int total, final AppItem currentItem) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                layoutBatchProgress.setVisibility(View.VISIBLE);
                progressBatch.setProgress(current);
                tvBatchProgressText.setText(getString(R.string.force_stop_progress_format, currentItem.appName, current, total));
                currentItem.isForceStopped = true;
                forceStoppedThisSession.add(currentItem.packageName);
                adapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    public void onFinished(final int forceStoppedCount) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                layoutBatchProgress.setVisibility(View.GONE);
                updateRamInfo();
                applyFilterAndUpdateUi();
                showResultBanner(getString(R.string.result_force_stopped_count, forceStoppedCount));
            }
        });
    }

    @Override
    public void onKillSingle(AppItem item) {
        if (item.isProtected || item.isForceStopped || item.isKilled || item.isStopped) {
            return;
        }

        if (useForceStopMode && hasAccessibilityPermission) {
            List<AppItem> singleList = new ArrayList<>();
            singleList.add(item);
            ForceStopManager.getInstance().startBatchForceStop(this, singleList);
        } else {
            if (activityManager != null) {
                try {
                    activityManager.killBackgroundProcesses(item.packageName);
                } catch (SecurityException ignored) {
                }
            }
            item.isKilled = true;
            killedThisSession.add(item.packageName);
            adapter.notifyDataSetChanged();
            updateRamInfo();
            applyFilterAndUpdateUi();

            if (useForceStopMode && !hasAccessibilityPermission) {
                onOpenAppInfo(item);
                Toast.makeText(this, getString(R.string.toast_single_guided_force_stop, item.appName), Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, getString(R.string.toast_single_killed, item.appName), Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onToggleProtect(AppItem item) {
        if (item.isLauncher) {
            Toast.makeText(this, R.string.toast_launcher_protected, Toast.LENGTH_SHORT).show();
            return;
        }
        if (item.isProtected) {
            item.isProtected = false;
            protectedPackages.remove(item.packageName);
            Toast.makeText(this, getString(R.string.toast_unprotected, item.appName), Toast.LENGTH_SHORT).show();
        } else {
            item.isProtected = true;
            protectedPackages.add(item.packageName);
            Toast.makeText(this, getString(R.string.toast_protected, item.appName), Toast.LENGTH_SHORT).show();
        }
        prefs.edit().putStringSet(KEY_PROTECTED, new HashSet<>(protectedPackages)).apply();
        adapter.notifyDataSetChanged();
        applyFilterAndUpdateUi();
    }

    @Override
    public void onOpenAppInfo(AppItem item) {
        try {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.fromParts("package", item.packageName, null));
            startActivity(intent);
        } catch (Exception ignored) {
        }
    }

    private void openUsageAccessSettings() {
        try {
            Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
            startActivity(intent);
        } catch (Exception e) {
            try {
                startActivity(new Intent(Settings.ACTION_SETTINGS));
            } catch (Exception ignored) {
            }
        }
    }

    private void openAccessibilitySettings() {
        try {
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivity(intent);
        } catch (Exception e) {
            try {
                startActivity(new Intent(Settings.ACTION_SETTINGS));
            } catch (Exception ignored) {
            }
        }
    }

    private boolean hasUsageStatsPermission() {
        AppOpsManager appOps = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
        if (appOps == null) {
            return false;
        }
        int mode;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            mode = appOps.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(),
                    getPackageName());
        } else {
            mode = checkOpLegacy(appOps);
        }
        if (mode == AppOpsManager.MODE_DEFAULT) {
            return checkCallingOrSelfPermission(android.Manifest.permission.PACKAGE_USAGE_STATS)
                    == PackageManager.PERMISSION_GRANTED;
        }
        return mode == AppOpsManager.MODE_ALLOWED;
    }

    @SuppressWarnings("deprecation")
    private int checkOpLegacy(AppOpsManager appOps) {
        return appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                getPackageName());
    }

    private void showResultBanner(String message) {
        mainHandler.removeCallbacks(hideBannerRunnable);
        tvResultBanner.setText(message);
        tvResultBanner.setAlpha(0f);
        tvResultBanner.setVisibility(View.VISIBLE);
        tvResultBanner.animate().alpha(1f).setDuration(220).start();
        mainHandler.postDelayed(hideBannerRunnable, 5000L);
    }

    private String formatBytes(long bytes) {
        double mb = bytes / (1024.0 * 1024.0);
        if (mb >= 1024.0) {
            double gb = mb / 1024.0;
            return String.format(Locale.US, "%.1f GB", gb);
        }
        return String.format(Locale.US, "%d MB", Math.round(mb));
    }
}
