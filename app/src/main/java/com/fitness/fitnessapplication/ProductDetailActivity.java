package com.fitness.fitnessapplication;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.fitness.fitnessapplication.DataModels.FoodLog;
import com.fitness.fitnessapplication.RetroFit.ApiClient;
import com.fitness.fitnessapplication.RetroFit.ApiService;
import com.squareup.picasso.Picasso;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProductDetailActivity extends AppCompatActivity {
    private float caloriesPer100g, proteinsPer100g, carbsPer100g, fatsPer100g;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_detail);

        TextView nameView = findViewById(R.id.product_name);
        TextView caloriesView = findViewById(R.id.calories);
        TextView macrosView = findViewById(R.id.macros);
        ImageView imageView = findViewById(R.id.product_image);
        EditText quantityInput = findViewById(R.id.quantity_input);
        Button logButton = findViewById(R.id.log_button);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            nameView.setText(extras.getString("product_name", "Unknown Product"));
            caloriesPer100g = extras.getFloat("calories", 0);
            proteinsPer100g = extras.getFloat("proteins", 0);
            carbsPer100g = extras.getFloat("carbs", 0);
            fatsPer100g = extras.getFloat("fats", 0);
            caloriesView.setText("Calories: " + caloriesPer100g + " kcal/100g");
            macrosView.setText(String.format("P: %.1fg | C: %.1fg | F: %.1fg", proteinsPer100g, carbsPer100g, fatsPer100g));
            String imageUrl = extras.getString("image_url");
            if (imageUrl != null && !imageUrl.isEmpty()) {
                Picasso.get().load(imageUrl).into(imageView);
            }
        }

        logButton.setOnClickListener(v -> {
            String qtyStr = quantityInput.getText().toString();
            if (!qtyStr.isEmpty()) {
                float quantity = Float.parseFloat(qtyStr);
                logConsumption(quantity);
            } else {
                Toast.makeText(this, "Enter quantity", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void logConsumption(float quantity) {
        float factor = quantity / 100f;
        FoodLog log = new FoodLog(
                getIntent().getStringExtra("product_name"),
                quantity,
                caloriesPer100g * factor,
                proteinsPer100g * factor,
                carbsPer100g * factor,
                fatsPer100g * factor
        );

        ApiService apiService = ApiClient.getApiService(this);
        Call<Void> call = apiService.logFood(log);
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(ProductDetailActivity.this, "Logged successfully!", Toast.LENGTH_SHORT).show();
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