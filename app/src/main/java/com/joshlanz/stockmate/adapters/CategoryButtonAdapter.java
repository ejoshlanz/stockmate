package com.joshlanz.stockmate.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.joshlanz.stockmate.R;

import java.util.List;

public class CategoryButtonAdapter extends RecyclerView.Adapter<CategoryButtonAdapter.ButtonViewHolder> {

    public interface OnCategoryClickListener {
        void onCategoryClick(String category);
    }

    private final List<String> categories;
    private final Context context;
    private final OnCategoryClickListener listener;
    private int selectedPosition = 0;

    public CategoryButtonAdapter(Context context, List<String> categories, OnCategoryClickListener listener) {
        this.context = context;
        this.categories = categories;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ButtonViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_button, parent, false);
        return new ButtonViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ButtonViewHolder holder, int position) {
        String category = categories.get(position);
        holder.button.setText(category);

        // Highlight selected button
        if (position == selectedPosition) {
            holder.button.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.normal));
            holder.button.setTextColor(ContextCompat.getColor(context, R.color.reverse_normal));
        } else {
            holder.button.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.container));
            holder.button.setTextColor(ContextCompat.getColor(context, R.color.normal));
        }

        holder.button.setOnClickListener(v -> {
            int currentPosition = holder.getAdapterPosition();
            if (currentPosition == RecyclerView.NO_POSITION) return;

            int previousPosition = selectedPosition;
            selectedPosition = currentPosition;
            notifyItemChanged(previousPosition);
            notifyItemChanged(selectedPosition);

            String selectedCategory = categories.get(currentPosition);
            listener.onCategoryClick(selectedCategory);
        });
    }


    @Override
    public int getItemCount() {
        return categories.size();
    }

    public static class ButtonViewHolder extends RecyclerView.ViewHolder {
        MaterialButton button;

        public ButtonViewHolder(@NonNull View itemView) {
            super(itemView);
            button = itemView.findViewById(R.id.categoryButton);
        }
    }
}
