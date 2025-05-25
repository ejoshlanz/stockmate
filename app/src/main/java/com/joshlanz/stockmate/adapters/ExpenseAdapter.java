package com.joshlanz.stockmate.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.joshlanz.stockmate.R;
import com.joshlanz.stockmate.models.Expense;

import java.util.List;

public class ExpenseAdapter extends RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder> {

    public interface OnItemActionListener {
        void onEdit(Expense expense);
        void onDelete(Expense expense);
    }

    private final List<Expense> expenses;
    private final OnItemActionListener listener;

    public ExpenseAdapter(List<Expense> expenses, OnItemActionListener listener) {
        this.expenses = expenses;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ExpenseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_expense, parent, false);
        return new ExpenseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ExpenseViewHolder holder, int position) {
        Expense expense = expenses.get(position);

        holder.itemTitle.setText(expense.getTitle());
        holder.itemCategory.setText(expense.getCategory());
        holder.itemTime.setText(expense.getDate()); // assuming date is a string
        holder.itemPrice.setText("₱" + expense.getAmount());

        holder.ivEdit.setOnClickListener(v -> listener.onEdit(expense));
        holder.ivDelete.setOnClickListener(v -> listener.onDelete(expense));
    }

    @Override
    public int getItemCount() {
        return expenses.size();
    }

    static class ExpenseViewHolder extends RecyclerView.ViewHolder {
        TextView itemTitle, itemCategory, itemTime, itemPrice;
        ImageView ivEdit, ivDelete;

        ExpenseViewHolder(@NonNull View itemView) {
            super(itemView);
            itemTitle = itemView.findViewById(R.id.itemTitle);
            itemCategory = itemView.findViewById(R.id.itemCategory);
            itemTime = itemView.findViewById(R.id.itemTime);
            itemPrice = itemView.findViewById(R.id.itemPrice);
            ivEdit = itemView.findViewById(R.id.iv_sale_edit);
            ivDelete = itemView.findViewById(R.id.iv_sale_delete);
        }
    }
    public void updateData(List<Expense> newExpenses) {
        expenses.clear();
        if (newExpenses != null) {
            expenses.addAll(newExpenses);
        }
        notifyDataSetChanged();
    }



}
