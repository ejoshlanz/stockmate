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
import com.joshlanz.stockmate.models.Sales;

import java.text.DecimalFormat;
import java.util.List;

public class SalesAdapter extends RecyclerView.Adapter<SalesAdapter.SalesViewHolder> {

    private Context context;
    private List<Sales> salesList;
    private OnSalesActionListener onSaleClickListener;

    public interface OnSalesActionListener {
        void onEditClick(Sales salesItem);
        void onDeleteClick(Sales salesItem);
    }


    public SalesAdapter(Context context, List<Sales> salesList, OnSalesActionListener listener) {
        this.context = context;
        this.salesList = salesList;
        this.onSaleClickListener = listener;
    }


    @Override
    public SalesViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_sales, parent, false);
        return new SalesViewHolder(view);
    }

    @Override
    public void onBindViewHolder(SalesViewHolder holder, int position) {
        Sales item = salesList.get(position);

        // Set the title
        holder.saleTitle.setText(item.getTitle());

        // Format price as currency
        DecimalFormat decimalFormat = new DecimalFormat("#,###.00");
        holder.salePrice.setText("₱" + decimalFormat.format(item.getPrice()));

        // Set sale time
        holder.saleTime.setText(item.getTime());


        // Handle edit button click
        holder.ivSaleEdit.setOnClickListener(v -> {onSaleClickListener.onEditClick(item);});

        // Handle delete button click
        holder.ivSaleDelete.setOnClickListener(v -> {onSaleClickListener.onDeleteClick(item);});
    }

    @Override
    public int getItemCount() {
        return salesList.size();
    }

    public void updateData(List<Sales> newSalesList) {
        this.salesList = newSalesList;
        notifyDataSetChanged();
    }

    public static class SalesViewHolder extends RecyclerView.ViewHolder {
        TextView saleTitle, salePrice, saleTime;
        ImageView ivSaleEdit, ivSaleDelete;

        public SalesViewHolder(View itemView) {
            super(itemView);
            saleTitle = itemView.findViewById(R.id.saleTitle);
            salePrice = itemView.findViewById(R.id.salePrice);
            saleTime = itemView.findViewById(R.id.saleTime);
            ivSaleEdit = itemView.findViewById(R.id.iv_sale_edit);
            ivSaleDelete = itemView.findViewById(R.id.iv_sale_delete);
        }
    }
}
