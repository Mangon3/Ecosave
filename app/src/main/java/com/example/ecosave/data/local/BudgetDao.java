package com.example.ecosave.data.local;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.ecosave.model.BudgetEntry;

import java.util.List;

@Dao
public interface BudgetDao {
    @Insert
    void insert(BudgetEntry entry);

    @Delete
    void delete(BudgetEntry entry);

    @Query("SELECT * FROM budget_entries ORDER BY timestamp DESC")
    List<BudgetEntry> getAllEntries();

    @Query("SELECT SUM(amount) FROM budget_entries WHERE isExpense = 1")
    double getTotalExpenses();

    @Query("SELECT SUM(amount) FROM budget_entries WHERE isExpense = 0")
    double getTotalIncome();
}
