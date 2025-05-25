package com.joshlanz.stockmate.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.joshlanz.stockmate.R;
import com.joshlanz.stockmate.models.Item;

import java.util.List;
import java.util.Locale;

public class ItemSelectorAdapter extends RecyclerView.Adapter<ItemSelectorAdapter.ViewHolder> {

    public static class SelectedItem {
        public Item item;
        public int quantity;

        public SelectedItem(Item item) {
            this.item = item;
            this.quantity = 0; // default quantity
        }
    }

    private final List<SelectedItem> selectedItems;

    // Constructor now accepts List<SelectedItem> directly (no copying)
    public ItemSelectorAdapter(List<SelectedItem> selectedItems) {
        this.selectedItems = selectedItems;
    }

    // Getter to retrieve selected items with quantity > 0
    public List<SelectedItem> getSelectedItems() {
        // Return only those with quantity > 0
        return selectedItems.stream()
                .filter(si -> si.quantity > 0)
                .toList();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_selector_row, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SelectedItem selectedItem = selectedItems.get(position);
        holder.tvItemName.setText(selectedItem.item.getTitle());
        holder.tvItemPrice.setText(String.format(Locale.getDefault(), "₱%.2f", selectedItem.item.getPrice()));
        holder.tvQuantity.setText(String.valueOf(selectedItem.quantity));

        holder.btnIncrement.setOnClickListener(v -> {            // Only increment if quantity is less than stock
            if (selectedItem.quantity < selectedItem.item.getStock()) {
                selectedItem.quantity++;
                holder.tvQuantity.setText(String.valueOf(selectedItem.quantity));

                // Optionally, enable/disable buttons depending on quantity and stock
                holder.btnIncrement.setEnabled(selectedItem.quantity < selectedItem.item.getStock());
                holder.btnDecrement.setEnabled(selectedItem.quantity > 0);
            } else {
                Toast.makeText(holder.itemView.getContext(),
                        "Cannot exceed available stock (" + selectedItem.item.getStock() + ")",
                        Toast.LENGTH_SHORT).show();
            }
        });


        holder.btnDecrement.setOnClickListener(v -> {
            if (selectedItem.quantity > 0) {
                selectedItem.quantity--;
                holder.tvQuantity.setText(String.valueOf(selectedItem.quantity));

                holder.btnIncrement.setEnabled(true);  // Enable increment if previously disabled
                holder.btnDecrement.setEnabled(selectedItem.quantity > 0);
            }
        });

    }

    @Override
    public int getItemCount() {
        return selectedItems.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvItemName, tvItemPrice, tvQuantity;
        Button btnIncrement, btnDecrement;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvItemName = itemView.findViewById(R.id.tvItemName);
            tvItemPrice = itemView.findViewById(R.id.tvItemPrice);
            tvQuantity = itemView.findViewById(R.id.tvQuantity);
            btnIncrement = itemView.findViewById(R.id.btnIncrement);
            btnDecrement = itemView.findViewById(R.id.btnDecrement);
        }
    }
}
