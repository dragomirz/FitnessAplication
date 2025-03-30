package com.fitness.fitnessapplication.DataModels;

// Keep import for SerializedName IF your JSON keys differ from field names
// import com.google.gson.annotations.SerializedName;

public class FoodLog {

    // Assuming your backend JSON keys match these variable names (e.g., "product_name")
    // If keys are different (e.g., "productName"), add @SerializedName("productName") before the field
    private String product_name;
    private float quantity;
    private float calories;
    private float proteins;
    private float carbs;
    private float fats;
    private float saturated_fat;
    private float sugars;

    // Keep your existing constructor
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

    // *** ADD GETTER METHODS ***

    public String getProductName() { // Use standard Java naming convention (camelCase) for getters
        return product_name;
    }

    public float getQuantity() {
        return quantity;
    }

    public float getCalories() {
        return calories;
    }

    public float getProteins() {
        return proteins;
    }

    public float getCarbs() {
        return carbs;
    }

    public float getFats() {
        return fats;
    }

    public float getSaturatedFat() { // Getter for saturated_fat
        return saturated_fat;
    }

    public float getSugars() {
        return sugars;
    }

    // No setters needed if you only construct it once and read from it
}