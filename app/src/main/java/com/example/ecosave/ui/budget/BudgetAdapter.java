package com.example.ecosave.ui.budget;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ecosave.R;
import com.example.ecosave.model.BudgetEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class BudgetAdapter extends RecyclerView.Adapter<BudgetAdapter.EntryViewHolder> {

    private final List<BudgetEntry> entries = new ArrayList<>();

    public void setEntries(List<BudgetEntry> newEntries) {
        entries.clear();
        entries.addAll(newEntries);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public EntryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_budget_entry, parent, false);
        return new EntryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EntryViewHolder holder, int position) {
        holder.bind(entries.get(position));
    }

    @Override
    public int getItemCount() {
        return entries.size();
    }

    static class EntryViewHolder extends RecyclerView.ViewHolder {
        TextView description, category, amount;

        EntryViewHolder(View itemView) {
            super(itemView);
            description = itemView.findViewById(R.id.text_entry_description);
            category = itemView.findViewById(R.id.text_entry_category);
            amount = itemView.findViewById(R.id.text_entry_amount);
        }

        void bind(BudgetEntry entry) {
            description.setText(entry.description);
            category.setText(entry.category);
            if (entry.isExpense) {
                amount.setText(String.format(Locale.US, "-$%.2f", entry.amount));
                amount.setTextColor(0xFFE53935); // red
            } else {
                amount.setText(String.format(Locale.US, "+$%.2f", entry.amount));
                amount.setTextColor(0xFF43A047); // green
            }
        }
    }
}
