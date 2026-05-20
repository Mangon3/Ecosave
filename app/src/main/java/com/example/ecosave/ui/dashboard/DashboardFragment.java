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
    private BudgetDao budgetDao;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);

        textAsxData = view.findViewById(R.id.text_asx_data);
        textBudgetSummary = view.findViewById(R.id.text_budget_summary);
        textBalance = view.findViewById(R.id.text_balance);
        textIncome = view.findViewById(R.id.text_income);
        textExpenses = view.findViewById(R.id.text_expenses);

        budgetDao = AppDatabase.getDatabase(requireContext()).budgetDao();

        fetchAsxData();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh budget data every time the tab becomes visible
        loadBudgetSummary();
    }

    private void fetchAsxData() {
        FinnhubApi api = RetrofitClient.getClient().create(FinnhubApi.class);
        String apiKey = BuildConfig.FINNHUB_API_KEY;

        if (apiKey == null || apiKey.isEmpty()) {
            textAsxData.setText("API key not configured");
            return;
        }

        // Major ASX companies dual-listed on NYSE (available on Finnhub free tier)
        // BHP Group, Rio Tinto, Woodside Energy - top ASX 200 constituents
        String[][] stocks = {
            {"BHP", "BHP Group"},
            {"RIO", "Rio Tinto"},
            {"WDS", "Woodside Energy"}
        };

        StringBuilder resultBuilder = new StringBuilder();
        final int[] completed = {0};

        for (String[] stock : stocks) {
            String symbol = stock[0];
            String name = stock[1];

            api.getQuote(symbol, apiKey).enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                    if (!isAdded()) return;

                    synchronized (resultBuilder) {
                        if (response.isSuccessful() && response.body() != null) {
                            JsonObject data = response.body();
                            double price = data.has("c") ? data.get("c").getAsDouble() : 0;
                            double change = data.has("d") ? data.get("d").getAsDouble() : 0;
                            double pct = data.has("dp") ? data.get("dp").getAsDouble() : 0;

                            if (price > 0) {
                                String sign = change >= 0 ? "+" : "";
                                String arrow = change >= 0 ? "  [UP]" : "  [DOWN]";
                                resultBuilder.append(String.format(Locale.US,
                                        "%s (%s): $%.2f  %s%.2f (%.2f%%)%s\n",
                                        symbol, name, price, sign, change, pct, arrow));
                            }
                        }

                        completed[0]++;
                        if (completed[0] == stocks.length) {
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> {
                                    String result = resultBuilder.toString().trim();
                                    textAsxData.setText(result.isEmpty() ? "Market data unavailable" : result);
                                });
                            }
                        }
                    }
                }

                @Override
                public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                    if (!isAdded()) return;
                    synchronized (resultBuilder) {
                        completed[0]++;
                        if (completed[0] == stocks.length) {
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> {
                                    String result = resultBuilder.toString().trim();
                                    textAsxData.setText(result.isEmpty() ? "Network error" : result);
                                });
                            }
                        }
                    }
                }
            });
        }
    }

    private void loadBudgetSummary() {
        Executors.newSingleThreadExecutor().execute(() -> {
            double totalIncome = budgetDao.getTotalIncome();
            double totalExpenses = budgetDao.getTotalExpenses();
            double balance = totalIncome - totalExpenses;

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    textBudgetSummary.setText(String.format(Locale.US,
                            "Income: $%.2f\nExpenses: $%.2f",
                            totalIncome, totalExpenses));

                    textBalance.setText(String.format(Locale.US, "$%.2f", balance));
                    textIncome.setText(String.format(Locale.US, "$%.2f", totalIncome));
                    textExpenses.setText(String.format(Locale.US, "$%.2f", totalExpenses));

                    // Color the balance based on positive/negative
                    if (balance >= 0) {
                        textBalance.setTextColor(0xFF121212); // dark on accent
                    } else {
                        textBalance.setTextColor(0xFFEF5350); // red for negative
                    }
                });
            }
        });
    }
}
