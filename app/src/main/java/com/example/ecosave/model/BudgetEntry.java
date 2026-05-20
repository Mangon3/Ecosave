package com.example.ecosave.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "budget_entries")
public class BudgetEntry {
    @PrimaryKey(autoGenerate = true)
    public int id;
    
    public String description;
    public double amount;
    public boolean isExpense;
    public String category;
    public long timestamp;

    // Data fields
    public BudgetEntry(String description, double amount, boolean isExpense, String category, long timestamp) {
        this.description = description;
        this.amount = amount;
        this.isExpense = isExpense;
        this.category = category;
        this.timestamp = timestamp;
    }
}
