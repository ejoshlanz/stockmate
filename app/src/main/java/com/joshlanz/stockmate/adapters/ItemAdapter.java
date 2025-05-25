package com.joshlanz.stockmate.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.joshlanz.stockmate.models.Item;

import com.joshlanz.stockmate.R;
import java.util.List;

public class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.ItemViewHolder> {

    private List<Item> itemList;
    private OnItemActionListener itemActionListener;

    // Constructor to initialize the item list and listener
    public ItemAdapter(List<Item> itemList, OnItemActionListener itemActionListener) {
        this.itemList = itemList;
        this.itemActionListener = itemActionListener;
    }

    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the custom layout (your layout file)
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_generic, parent, false);
        return new ItemViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
        Item item = itemList.get(position);
        // Bind data to the views
        holder.tvItemTitle.setText(item.getTitle());
        holder.tvItemCategory.setText(item.getCategory());
        holder.tvItemStock.setText(String.format("%d", item.getStock()));
        holder.tvItemPrice.setText(String.format("₱%.2f", item.getPrice()));
        holder.ivItemImage.setImageURI(item.getImageUri());  // Assuming you're using a URI for image

        // Set up click listeners for edit and delete
        holder.ivItemEdit.setOnClickListener(v -> itemActionListener.onEditClick(item));
        holder.ivItemDelete.setOnClickListener(v -> itemActionListener.onDeleteClick(item));
    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }

    public static class ItemViewHolder extends RecyclerView.ViewHolder {

        TextView tvItemTitle, tvItemCategory, tvItemStock, tvItemPrice;
        ImageView ivItemImage, ivItemEdit, ivItemDelete;

        public ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            // Initialize the views
            tvItemTitle = itemView.findViewById(R.id.tv_item_title);
            tvItemCategory = itemView.findViewById(R.id.tv_item_category);
            tvItemStock = itemView.findViewById(R.id.tv_item_stock);
            tvItemPrice = itemView.findViewById(R.id.tv_item_price);
            ivItemImage = itemView.findViewById(R.id.iv_item_image);
            ivItemEdit = itemView.findViewById(R.id.iv_item_edit);
            ivItemDelete = itemView.findViewById(R.id.iv_item_delete);
        }
    }

    // Interface for handling item edit and delete actions
    public interface OnItemActionListener {
        void onEditClick(Item item);
        void onDeleteClick(Item item);
    }
}

