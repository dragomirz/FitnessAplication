package com.fitness.fitnessapplication.DataModels;

import com.google.gson.annotations.SerializedName;

public class ProductResponse {
    private Product product;
    public Product getProduct() { return product; }

    public static class Product {
        private String product_name;
        private Nutriments nutriments;
        private String image_url;

        public String getProductName() { return product_name; }
        public Nutriments getNutriments() { return nutriments; }
        public String getImageUrl() { return image_url; }
    }

    public static class Nutriments {
        @SerializedName("energy_kcal_100g")
        private Float energyKcal100g;

        @SerializedName("energy_100g")
        private Float energy100g;

        @SerializedName("proteins_100g")
        private Float proteins_100g;

        @SerializedName("carbohydrates_100g")
        private Float carbohydrates_100g;

        @SerializedName("fat_100g")
        private Float fat_100g;

        @SerializedName("saturated-fat_100g")
        private Float saturatedFat_100g;

        @SerializedName("sugars_100g")
        private Float sugars_100g;

        public float getCalories() {
            if (energyKcal100g != null) return energyKcal100g;
            if (energy100g != null) return energy100g / 4.184f;
            return 0f;
        }

        public float getProteins() { return proteins_100g != null ? proteins_100g : 0f; }
        public float getCarbs() { return carbohydrates_100g != null ? carbohydrates_100g : 0f; }
        public float getFats() { return fat_100g != null ? fat_100g : 0f; }
        public float getSaturatedFat() { return saturatedFat_100g != null ? saturatedFat_100g : 0f; }
        public float getSugars() { return sugars_100g != null ? sugars_100g : 0f; }
    }
}