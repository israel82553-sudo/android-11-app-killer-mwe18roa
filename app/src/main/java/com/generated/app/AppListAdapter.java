package com.generated.app;

import android.content.Context;
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
            holder.tvStatusBadge.setTextColor(context.getResources().getColor(R.color.protect_blue));
            holder.tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_protected);
        } else if (item.isKilled) {
            holder.tvStatusBadge.setVisibility(View.VISIBLE);
            holder.tvStatusBadge.setText(R.string.status_killed);
            holder.tvStatusBadge.setTextColor(context.getResources().getColor(R.color.success_green));
            holder.tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_killed);
        } else if (item.isStopped) {
            holder.tvStatusBadge.setVisibility(View.VISIBLE);
            holder.tvStatusBadge.setText(R.string.status_stopped);
            holder.tvStatusBadge.setTextColor(context.getResources().getColor(R.color.text_secondary));
            holder.tvStatusBadge.setBackgroundResource(R.drawable.bg_tab_unselected);
        } else {
            holder.tvStatusBadge.setVisibility(View.GONE);
        }

        boolean canKill = !item.isKilled && !item.isStopped;
        holder.btnKillSingle.setEnabled(canKill);
        holder.btnKillSingle.setAlpha(canKill ? 1.0f : 0.45f);
        holder.ivAppIcon.setAlpha(item.isKilled || item.isStopped ? 0.65f : 1.0f);

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

    private String buildSubtitle(AppItem item) {
        if (item.lastTimeUsed > 0) {
            long diffMs = Math.max(0L, System.currentTimeMillis() - item.lastTimeUsed);
            long mins = diffMs / 60000L;
            if (mins < 2) {
                return context.getString(R.string.status_active_now);
            } else if (mins < 60) {
                return context.getString(R.string.status_used_mins, (int) mins);
            } else {
                long hours = Math.max(1L, mins / 60L);
                return context.getString(R.string.status_used_hours, (int) hours);
            }
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
