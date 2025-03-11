package com.fitness.fitnessapplication.DataModels;

public class DailyLogResponse {
    private Totals totals;
    public Totals getTotals() { return totals; }

    public static class Totals {
        private float calories, proteins, carbs, fats, saturated_fat, sugars;
        public float getCalories() { return calories; }
        public float getProteins() { return proteins; }
        public float getCarbs() { return carbs; }
        public float getFats() { return fats; }
        public float getSaturatedFat() { return saturated_fat; }
        public float getSugars() { return sugars; }
    }
}