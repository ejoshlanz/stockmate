package com.joshlanz.stockmate.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.joshlanz.stockmate.R;
import com.joshlanz.stockmate.models.RecentActivity;

import java.util.List;

public class RecentActivityAdapter extends RecyclerView.Adapter<RecentActivityAdapter.ViewHolder> {

    private final Context context;
    private List<RecentActivity> activityList;

    public RecentActivityAdapter(Context context, List<RecentActivity> activityList) {
        this.context = context;
        this.activityList = activityList;
    }

    public void setActivityList(List<RecentActivity> activityList) {
        this.activityList = activityList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RecentActivityAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_recent_activity, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecentActivityAdapter.ViewHolder holder, int position) {
        RecentActivity activity = activityList.get(position);

        holder.itemIcon.setImageResource(activity.getIconResId());
        holder.itemTitle.setText(activity.getTitle());
        holder.itemDescription.setText(activity.getDescription());
        holder.timeInfo.setText(activity.getTime());
    }

    @Override
    public int getItemCount() {
        return activityList != null ? activityList.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView itemIcon;
        TextView itemTitle, itemDescription, timeInfo;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            itemIcon = itemView.findViewById(R.id.itemIcon);
            itemTitle = itemView.findViewById(R.id.itemTitle);
            itemDescription = itemView.findViewById(R.id.itemDescription);
            timeInfo = itemView.findViewById(R.id.timeInfo);
        }
    }
}
