package com.generated.app;

import android.content.Context;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class AppListAdapter extends BaseAdapter {

    public interface AppActionListener {
        void onKillSingle(AppItem item);
        void onToggleProtect(AppItem item);
        void onOpenAppInfo(AppItem item);
    }

    private final Context context;
    private final LayoutInflater inflater;
    private final AppActionListener listener;
    private final List<AppItem> items = new ArrayList<>();

    public AppListAdapter(Context context, AppActionListener listener) {
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.listener = listener;
    }

    public void setItems(List<AppItem> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public AppItem getItem(int position) {
        return items.get(position);
    }

    @Override
    public long getItemId(int position) {
        return items.get(position).packageName.hashCode();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_app, parent, false);
            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        final AppItem item = items.get(position);
        holder.ivAppIcon.setImageDrawable(item.icon);
        holder.tvAppName.setText(item.appName);
        holder.tvAppStatus.setText(buildSubtitle(item));

        if (item.isProtected) {
            holder.btnProtect.setImageResource(R.drawable.ic_shield);
        } else {
            holder.btnProtect.setImageResource(R.drawable.ic_shield_outline);
        }

        if (item.isProtected) {
            holder.tvStatusBadge.setVisibility(View.VISIBLE);
            holder.tvStatusBadge.setText(R.string.status_protected);
            holder.tvStatusBadge.setTextColor(getCompatColor(R.color.protect_blue));
            holder.tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_protected);
        } else if (item.isForceStopped) {
            holder.tvStatusBadge.setVisibility(View.VISIBLE);
            holder.tvStatusBadge.setText(R.string.status_force_stopped);
            holder.tvStatusBadge.setTextColor(getCompatColor(R.color.warning_amber));
            holder.tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_force_stopped);
        } else if (item.isKilled) {
            holder.tvStatusBadge.setVisibility(View.VISIBLE);
            holder.tvStatusBadge.setText(R.string.status_killed);
            holder.tvStatusBadge.setTextColor(getCompatColor(R.color.success_green));
            holder.tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_killed);
        } else if (item.isStopped) {
            holder.tvStatusBadge.setVisibility(View.VISIBLE);
            holder.tvStatusBadge.setText(R.string.status_stopped);
            holder.tvStatusBadge.setTextColor(getCompatColor(R.color.text_secondary));
            holder.tvStatusBadge.setBackgroundResource(R.drawable.bg_tab_unselected);
        } else {
            holder.tvStatusBadge.setVisibility(View.GONE);
        }

        boolean canKill = !item.isProtected && !item.isForceStopped && !item.isKilled && !item.isStopped;
        holder.btnKillSingle.setEnabled(canKill);
        if (item.isProtected) {
            holder.btnKillSingle.setText(R.string.btn_kill_protected);
            holder.btnKillSingle.setTextColor(getCompatColor(R.color.protect_blue));
        } else if (item.isForceStopped) {
            holder.btnKillSingle.setText(R.string.btn_force_stopped_done);
            holder.btnKillSingle.setTextColor(getCompatColor(R.color.warning_amber));
        } else if (item.isKilled) {
            holder.btnKillSingle.setText(R.string.btn_kill_done);
            holder.btnKillSingle.setTextColor(getCompatColor(R.color.success_green));
        } else if (item.isStopped) {
            holder.btnKillSingle.setText(R.string.btn_kill_stopped);
            holder.btnKillSingle.setTextColor(getCompatColor(R.color.text_secondary));
        } else {
            holder.btnKillSingle.setText(R.string.btn_kill_single);
            holder.btnKillSingle.setTextColor(getCompatColor(R.color.danger_rose));
        }
        holder.ivAppIcon.setAlpha((item.isForceStopped || item.isKilled || item.isStopped) ? 0.65f : 1.0f);

        holder.btnProtect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.onToggleProtect(item);
                }
            }
        });

        holder.btnAppInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.onOpenAppInfo(item);
                }
            }
        });

        holder.btnKillSingle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.onKillSingle(item);
                }
            }
        });

        return convertView;
    }

    @SuppressWarnings("deprecation")
    private int getCompatColor(int colorResId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return context.getColor(colorResId);
        }
        return context.getResources().getColor(colorResId);
    }

    private String buildSubtitle(AppItem item) {
        if (item.lastTimeUsed > 0L) {
            long diffMs = Math.max(0L, System.currentTimeMillis() - item.lastTimeUsed);
            long mins = diffMs / 60000L;
            if (mins < 2L) {
                return context.getString(R.string.status_active_now);
            } else if (mins < 60L) {
                return context.getString(R.string.status_used_mins, (int) mins);
            } else if (mins < 24L * 60L) {
                long hours = Math.max(1L, mins / 60L);
                return context.getString(R.string.status_used_hours, (int) hours);
            } else {
                long days = Math.max(1L, mins / (24L * 60L));
                return context.getString(R.string.status_used_days, (int) days);
            }
        }
        if (item.isSystem) {
            return context.getString(R.string.status_system_app);
        }
        return context.getString(R.string.status_installed);
    }

    private static class ViewHolder {
        final ImageView ivAppIcon;
        final TextView tvAppName;
        final TextView tvAppStatus;
        final TextView tvStatusBadge;
        final ImageButton btnProtect;
        final ImageButton btnAppInfo;
        final TextView btnKillSingle;

        ViewHolder(View root) {
            ivAppIcon = root.findViewById(R.id.ivAppIcon);
            tvAppName = root.findViewById(R.id.tvAppName);
            tvAppStatus = root.findViewById(R.id.tvAppStatus);
            tvStatusBadge = root.findViewById(R.id.tvStatusBadge);
            btnProtect = root.findViewById(R.id.btnProtect);
            btnAppInfo = root.findViewById(R.id.btnAppInfo);
            btnKillSingle = root.findViewById(R.id.btnKillSingle);
        }
    }
}
