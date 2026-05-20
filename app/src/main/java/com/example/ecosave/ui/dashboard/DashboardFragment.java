package com.example.ecosave.ui.dashboard;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.ecosave.BuildConfig;
import com.example.ecosave.R;
import com.example.ecosave.data.local.AppDatabase;
import com.example.ecosave.data.local.BudgetDao;
import com.example.ecosave.data.network.FinnhubApi;
import com.example.ecosave.data.network.RetrofitClient;
import com.google.gson.JsonObject;

import java.util.Locale;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DashboardFragment extends Fragment {

    private static final String TAG = "DashboardFragment";
    private TextView textAsxData;
    private TextView textBudgetSummary;
    private TextView textBalance;
    private TextView textIncome;
    private TextView textExpenses;
    private TextView textDashboardTitle;
    private BudgetDao budgetDao;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Set up UI components and time greeting
        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);

        textAsxData = view.findViewById(R.id.text_asx_data);
        textBudgetSummary = view.findViewById(R.id.text_budget_summary);
        textBalance = view.findViewById(R.id.text_balance);
        textIncome = view.findViewById(R.id.text_income);
        textExpenses = view.findViewById(R.id.text_expenses);
        textDashboardTitle = view.findViewById(R.id.text_dashboard_title);

        java.util.Calendar calendar = java.util.Calendar.getInstance();
        int hour = calendar.get(java.util.Calendar.HOUR_OF_DAY);
        String greeting;
        if (hour < 12) {
            greeting = "Good Morning!";
        } else if (hour < 18) {
            greeting = "Good Afternoon";
        } else {
            greeting = "Good Evening";
        }
        textDashboardTitle.setText(greeting);

        budgetDao = AppDatabase.getDatabase(requireContext()).budgetDao();

        fetchAsxData();

        return view;
    }

    @Override
    public void onResume() {
        // Load budget summary when screen resumes
        super.onResume();
        loadBudgetSummary();
    }

    private static class StockResult {
        String symbol;
        String name;
        double price;
        double change;
        double pct;
        boolean success;

        StockResult(String symbol, String name) {
            // Initialize the stock result model
            this.symbol = symbol;
            this.name = name;
            this.success = false;
        }
    }

    private void fetchAsxData() {
        // Fetch market stock quotes from remote API
        FinnhubApi api = RetrofitClient.getClient().create(FinnhubApi.class);
        String apiKey = BuildConfig.FINNHUB_API_KEY;

        if (apiKey == null || apiKey.isEmpty()) {
            textAsxData.setText("API key not configured");
            return;
        }

        String[][] stocks = {
            {"BHP", "BHP Group"},
            {"RIO", "Rio Tinto"},
            {"WDS", "Woodside Energy"}
        };

        final StockResult[] results = new StockResult[stocks.length];
        for (int i = 0; i < stocks.length; i++) {
            results[i] = new StockResult(stocks[i][0], stocks[i][1]);
        }
        final int[] completed = {0};

        for (int i = 0; i < stocks.length; i++) {
            final int index = i;
            String symbol = stocks[i][0];

            api.getQuote(symbol, apiKey).enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                    if (!isAdded()) return;

                    synchronized (results) {
                        if (response.isSuccessful() && response.body() != null) {
                            JsonObject data = response.body();
                            double price = data.has("c") ? data.get("c").getAsDouble() : 0;
                            double change = data.has("d") ? data.get("d").getAsDouble() : 0;
                            double pct = data.has("dp") ? data.get("dp").getAsDouble() : 0;

                            if (price > 0) {
                                results[index].price = price;
                                results[index].change = change;
                                results[index].pct = pct;
                                results[index].success = true;
                            }
                        }

                        completed[0]++;
                        if (completed[0] == stocks.length) {
                            updateMarketUi(results);
                        }
                    }
                }

                @Override
                public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                    if (!isAdded()) return;
                    synchronized (results) {
                        completed[0]++;
                        if (completed[0] == stocks.length) {
                            updateMarketUi(results);
                        }
                    }
                }
            });
        }
    }

    private void updateMarketUi(StockResult[] results) {
        // Render results on screen with colors
        if (getActivity() == null) return;
        getActivity().runOnUiThread(() -> {
            android.text.SpannableStringBuilder ssb = new android.text.SpannableStringBuilder();
            for (int i = 0; i < results.length; i++) {
                StockResult r = results[i];
                String line;
                if (r.success) {
                    String sign = r.change >= 0 ? "+" : "";
                    String arrow = r.change >= 0 ? "  [UP]" : "  [DOWN]";
                    line = String.format(Locale.US, "%s (%s): $%.2f  %s%.2f (%.2f%%)%s",
                            r.symbol, r.name, r.price, sign, r.change, r.pct, arrow);
                } else {
                    line = String.format(Locale.US, "%s (%s): Unavailable", r.symbol, r.name);
                }

                int start = ssb.length();
                ssb.append(line);
                int end = ssb.length();

                if (r.success) {
                    if (r.pct >= 1.0) {
                        ssb.setSpan(new android.text.style.ForegroundColorSpan(0xFF43A047), start, end, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    } else if (r.pct <= -1.0) {
                        ssb.setSpan(new android.text.style.ForegroundColorSpan(0xFFE53935), start, end, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                } else {
                    ssb.setSpan(new android.text.style.ForegroundColorSpan(0xFF757575), start, end, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }

                if (i < results.length - 1) {
                    ssb.append("\n");
                }
            }
            textAsxData.setText(ssb);
        });
    }

    private void loadBudgetSummary() {
        // Read database context on background thread
        Executors.newSingleThreadExecutor().execute(() -> {
            double totalIncome = budgetDao.getTotalIncome();
            double totalExpenses = budgetDao.getTotalExpenses();
            double balance = totalIncome - totalExpenses;
            java.util.List<com.example.ecosave.model.BudgetEntry> recent = budgetDao.getRecentEntries();

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    android.text.SpannableStringBuilder ssb = new android.text.SpannableStringBuilder();
                    int count = Math.min(recent.size(), 3);
                    if (count == 0) {
                        ssb.append("No recent transactions");
                    } else {
                        for (int i = 0; i < count; i++) {
                            com.example.ecosave.model.BudgetEntry entry = recent.get(i);
                            String sign = entry.isExpense ? "-" : "+";
                            String amountText = String.format(Locale.US, "%s$%.2f", sign, entry.amount);

                            String prefix = String.format(Locale.US, "%s (%s)  ", entry.description, entry.category);
                            ssb.append(prefix);

                            int start = ssb.length();
                            ssb.append(amountText);
                            int end = ssb.length();

                            int color = entry.isExpense ? 0xFFE53935 : 0xFF43A047;
                            ssb.setSpan(new android.text.style.ForegroundColorSpan(color), start, end, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                            ssb.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD), start, end, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                            if (i < count - 1) {
                                ssb.append("\n");
                            }
                        }
                    }
                    textBudgetSummary.setText(ssb);

                    textBalance.setText(String.format(Locale.US, "$%.2f", balance));
                    textIncome.setText(String.format(Locale.US, "$%.2f", totalIncome));
                    textExpenses.setText(String.format(Locale.US, "$%.2f", totalExpenses));

                    if (balance >= 0) {
                        textBalance.setTextColor(0xFF121212);
                    } else {
                        textBalance.setTextColor(0xFFEF5350);
                    }
                });
            }
        });
    }
}
