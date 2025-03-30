package com.fitness.fitnessapplication.RetroFit;

import com.fitness.fitnessapplication.DataModels.FoodLog; // Make sure this import is correct
import com.fitness.fitnessapplication.DataModels.LoginResponse;
import com.fitness.fitnessapplication.DataModels.RegisterResponse;
import com.fitness.fitnessapplication.DataModels.User;
import com.fitness.fitnessapplication.DataModels.DailyLogResponse;

import java.util.List; // Import List

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface ApiService {
    @POST("/register")
    Call<RegisterResponse> registerUser(@Body User user);

    @POST("/login")
    Call<LoginResponse> loginUser(@Body User user);

    @GET("/get-user-data")
    Call<User> getUserData();

    @POST("/log-food")
    Call<Void> logFood(@Body FoodLog log); // Assuming this matches your log structure

    // Existing endpoint for aggregate data
    @GET("/daily-logs")
    Call<DailyLogResponse> getDailyLogs(@Query("date") String date, @Query("range") String range);


    @GET("/logs-by-date")
    Call<List<FoodLog>> getLogsForDate(@Query("date") String date); // Returns a List of FoodLog objects
}