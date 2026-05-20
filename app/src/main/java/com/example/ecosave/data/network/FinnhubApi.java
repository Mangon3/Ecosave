package com.example.ecosave.data.network;

import com.google.gson.JsonObject;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface FinnhubApi {
    @GET("quote")
    Call<JsonObject> getQuote(@Query("symbol") String symbol, @Query("token") String token);
}
