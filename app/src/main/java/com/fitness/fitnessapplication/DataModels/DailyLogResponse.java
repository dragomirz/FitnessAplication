package com.fitness.fitnessapplication.DataModels;

import com.google.gson.annotations.SerializedName;
import java.util.List; // Import List

public class DailyLogResponse {

    // Keep the existing 'totals' for Daily/Monthly aggregate responses
    @SerializedName("totals")
    private Totals totals;

    // Add 'dailyData' for Weekly breakdown responses
    @SerializedName("dailyData")
    private List<DailyData> dailyData;

    public Totals getTotals() {
        return totals;
    }

    public List<DailyData> getDailyData() {
        return dailyData;
    }

    // Inner class for overall totals (Daily/Monthly) OR totals for a single day within DailyData
    // Make sure variable names match the JSON from your backend EXACTLY
    public static class Totals {
        @SerializedName("calories") // Ensure these match JSON keys
        private float calories;
        @SerializedName("proteins")
        private float proteins;
        @SerializedName("carbs")
        private float carbs;
        @SerializedName("fats")
        private float fats;
        @SerializedName("saturated_fat") // Check if backend sends this key
        private float saturated_fat;
        @SerializedName("sugars")
        private float sugars;

        // Getters for all fields
        public float getCalories() { return calories; }
        public float getProteins() { return proteins; }
        public float getCarbs() { return carbs; }
        public float getFats() { return fats; }
        // Corrected getter name if backend key is saturated_fat
        public float getSaturatedFat() { return saturated_fat; }
        public float getSugars() { return sugars; }
    }

    // Inner class for the items in the weekly data array
    public static class DailyData {
        @SerializedName("date")
        private String date; // "yyyy-MM-dd"
        @SerializedName("totals")
        private Totals totals; // Contains totals for this specific day

        public String getDate() { return date; }
        public Totals getTotals() { return totals; }
    }
}