package com.fitness.fitnessapplication.DataModels;

public class FoodLog {
    private String product_name;
    private float quantity, calories, proteins, carbs, fats, saturated_fat, sugars;

    public FoodLog(String product_name, float quantity, float calories, float proteins, float carbs, float fats, float saturated_fat, float sugars) {
        this.product_name = product_name;
        this.quantity = quantity;
        this.calories = calories;
        this.proteins = proteins;
        this.carbs = carbs;
        this.fats = fats;
        this.saturated_fat = saturated_fat;
        this.sugars = sugars;
    }
}