package com.example.ecosave.ui.budget;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ecosave.R;
import com.example.ecosave.data.local.AppDatabase;
import com.example.ecosave.data.local.BudgetDao;
import com.example.ecosave.model.BudgetEntry;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;
import java.util.concurrent.Executors;

public class BudgetFragment extends Fragment {

    private BudgetAdapter adapter;
    private BudgetDao budgetDao;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_budget, container, false);

        budgetDao = AppDatabase.getDatabase(requireContext()).budgetDao();
        adapter = new BudgetAdapter();

        RecyclerView recycler = view.findViewById(R.id.recycler_budget);
        recycler.setLayoutManager(new LinearLayoutManager(getContext()));
        recycler.setAdapter(adapter);

        FloatingActionButton fab = view.findViewById(R.id.fab_add_entry);
        fab.setOnClickListener(v -> showAddTransactionDialog());

        loadEntries();

        return view;
    }

    private void showAddTransactionDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_transaction, null);
        EditText editDescription = dialogView.findViewById(R.id.edit_description);
        EditText editAmount = dialogView.findViewById(R.id.edit_amount);
        EditText editCategory = dialogView.findViewById(R.id.edit_category);
        RadioGroup radioType = dialogView.findViewById(R.id.radio_type);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Add Transaction")
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    String description = editDescription.getText().toString().trim();
                    String amountStr = editAmount.getText().toString().trim();
                    String category = editCategory.getText().toString().trim();
                    boolean isExpense = radioType.getCheckedRadioButtonId() == R.id.radio_expense;

                    if (description.isEmpty() || amountStr.isEmpty()) {
                        Toast.makeText(getContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    double amount;
                    try {
                        amount = Double.parseDouble(amountStr);
                    } catch (NumberFormatException e) {
                        Toast.makeText(getContext(), "Invalid amount", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (category.isEmpty()) {
                        category = isExpense ? "Expense" : "Income";
                    }

                    BudgetEntry entry = new BudgetEntry(description, amount, isExpense, category, System.currentTimeMillis());
                    saveEntry(entry);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void saveEntry(BudgetEntry entry) {
        Executors.newSingleThreadExecutor().execute(() -> {
            budgetDao.insert(entry);
            if (getActivity() != null) {
                getActivity().runOnUiThread(this::loadEntries);
            }
        });
    }

    private void loadEntries() {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<BudgetEntry> entries = budgetDao.getAllEntries();
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> adapter.setEntries(entries));
            }
        });
    }
}
