package com.fitness.fitnessapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.fitness.fitnessapplication.DataModels.FoodLog;
import com.fitness.fitnessapplication.DataModels.ProductResponse;
import com.fitness.fitnessapplication.RetroFit.ApiClient;
import com.fitness.fitnessapplication.RetroFit.ApiService;
import com.fitness.fitnessapplication.RetroFit.OpenFoodFactsService;
import com.squareup.picasso.Picasso;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ProductDetailActivity extends AppCompatActivity {
    private float caloriesPer100g, proteinsPer100g, carbsPer100g, fatsPer100g, saturatedFatPer100g, sugarsPer100g;
    private String productName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Hide the ActionBar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        setContentView(R.layout.activity_product_detail);

        TextView nameView = findViewById(R.id.product_name);
        TextView caloriesView = findViewById(R.id.calories);
        TextView macrosView = findViewById(R.id.macros);
        ImageView imageView = findViewById(R.id.product_image);
        EditText quantityInput = findViewById(R.id.quantity_input);
        Button logButton = findViewById(R.id.log_button);

        String barcode = getIntent().getStringExtra("barcode");
        if (barcode != null) {
            fetchProductInfo(barcode, nameView, caloriesView, macrosView, imageView);
        } else {
            Bundle extras = getIntent().getExtras();
            if (extras != null) {
                productName = extras.getString("product_name", "Unknown Product");
                caloriesPer100g = extras.getFloat("calories", 0);
                proteinsPer100g = extras.getFloat("proteins", 0);
                carbsPer100g = extras.getFloat("carbs", 0);
                fatsPer100g = extras.getFloat("fats", 0);
                saturatedFatPer100g = extras.getFloat("saturated_fat", 0);
                sugarsPer100g = extras.getFloat("sugars", 0);
                nameView.setText(productName);
                caloriesView.setText("Calories: " + caloriesPer100g + " kcal/100g");
                macrosView.setText(String.format("P: %.1fg | C: %.1fg | F: %.1fg\nSat. Fat: %.1fg | Sugars: %.1fg",
                        proteinsPer100g, carbsPer100g, fatsPer100g, saturatedFatPer100g, sugarsPer100g));
                String imageUrl = extras.getString("image_url");
                if (imageUrl != null && !imageUrl.isEmpty()) {
                    Picasso.get().load(imageUrl).into(imageView);
                }
            }
        }

        logButton.setOnClickListener(v -> {
            String qtyStr = quantityInput.getText().toString();
            if (!qtyStr.isEmpty()) {
                try {
                    float quantity = Float.parseFloat(qtyStr);
                    if (quantity <= 0) {
                        Toast.makeText(this, "Quantity must be positive", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    logConsumption(quantity);
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "Invalid quantity", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Enter quantity", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchProductInfo(String barcode, TextView nameView, TextView caloriesView, TextView macrosView, ImageView imageView) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://world.openfoodfacts.org/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        OpenFoodFactsService service = retrofit.create(OpenFoodFactsService.class);
        Call<ProductResponse> call = service.getProduct(barcode);
        call.enqueue(new Callback<ProductResponse>() {
            @Override
            public void onResponse(Call<ProductResponse> call, Response<ProductResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getProduct() != null) {
                    ProductResponse.Product product = response.body().getProduct();
                    productName = product.getProductName() != null ? product.getProductName() : "Unknown Product";
                    caloriesPer100g = product.getNutriments() != null ? product.getNutriments().getCalories() : 0f;
                    proteinsPer100g = product.getNutriments() != null ? product.getNutriments().getProteins() : 0f;
                    carbsPer100g = product.getNutriments() != null ? product.getNutriments().getCarbs() : 0f;
                    fatsPer100g = product.getNutriments() != null ? product.getNutriments().getFats() : 0f;
                    saturatedFatPer100g = product.getNutriments() != null ? product.getNutriments().getSaturatedFat() : 0f;
                    sugarsPer100g = product.getNutriments() != null ? product.getNutriments().getSugars() : 0f;

                    nameView.setText(productName);
                    caloriesView.setText("Calories: " + caloriesPer100g + " kcal/100g");
                    macrosView.setText(String.format("P: %.1fg | C: %.1fg | F: %.1fg\nSat. Fat: %.1fg | Sugars: %.1fg",
                            proteinsPer100g, carbsPer100g, fatsPer100g, saturatedFatPer100g, sugarsPer100g));
                    String imageUrl = product.getImageUrl();
                    if (imageUrl != null && !imageUrl.isEmpty()) {
                        Picasso.get().load(imageUrl).into(imageView);
                    }
                } else {
                    Toast.makeText(ProductDetailActivity.this, "Product not found", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ProductResponse> call, Throwable t) {
                Toast.makeText(ProductDetailActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void logConsumption(float quantity) {
        float factor = quantity / 100f;
        FoodLog log = new FoodLog(
                productName,
                quantity,
                caloriesPer100g * factor,
                proteinsPer100g * factor,
                carbsPer100g * factor,
                fatsPer100g * factor,
                saturatedFatPer100g * factor,
                sugarsPer100g * factor
        );

        ApiService apiService = ApiClient.getApiService(this);
        Call<Void> call = apiService.logFood(log);
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(ProductDetailActivity.this, "Logged successfully!", Toast.LENGTH_SHORT).show();
                    // Return to HomeActivity
                    Intent intent = new Intent(ProductDetailActivity.this, HomeActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(ProductDetailActivity.this, "Log failed: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(ProductDetailActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}