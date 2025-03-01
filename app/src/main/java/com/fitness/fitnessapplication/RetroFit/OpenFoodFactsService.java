package com.fitness.fitnessapplication.RetroFit;

import com.fitness.fitnessapplication.DataModels.ProductResponse;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface OpenFoodFactsService {
    @GET("api/v0/product/{barcode}.json")
    Call<ProductResponse> getProduct(@Path("barcode") String barcode);
}