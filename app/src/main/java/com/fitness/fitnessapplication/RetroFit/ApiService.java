package com.fitness.fitnessapplication.RetroFit;

import com.fitness.fitnessapplication.DataModels.LoginResponse;
import com.fitness.fitnessapplication.DataModels.RegisterResponse;
import com.fitness.fitnessapplication.DataModels.User;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

public interface ApiService {
    @POST("/register")
    Call<RegisterResponse> registerUser(@Body User user);

    @POST("/login")
    Call<LoginResponse> loginUser(@Body User user);

    @GET("/test-auth")
    Call<Void> testAuthentication();
}