package com.fitness.fitnessapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;
import com.fitness.fitnessapplication.DataModels.ProductResponse;
import com.fitness.fitnessapplication.RetroFit.OpenFoodFactsService;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ScannerActivity extends AppCompatActivity {
    private Button btnScan;
    private TextView txtResult;
    private final ActivityResultLauncher<ScanOptions> barcodeLauncher = registerForActivityResult(new ScanContract(),
            result -> {
                if (result.getContents() == null) {
                    Toast.makeText(this, "Cancelled", Toast.LENGTH_LONG).show();
                } else {
                    String barcode = result.getContents();
                    txtResult.setText(barcode);
                    getFoodInfo(barcode);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanner);

        btnScan = findViewById(R.id.btn_scan);
        txtResult = findViewById(R.id.txt_result);

        btnScan.setOnClickListener(v -> scanBarcode());
    }

    private void scanBarcode() {
        ScanOptions options = new ScanOptions();
        options.setOrientationLocked(false);
        options.setDesiredBarcodeFormats(ScanOptions.ALL_CODE_TYPES);
        options.setPrompt("Scan a barcode");
        options.setCameraId(0);
        options.setBeepEnabled(false);
        options.setBarcodeImageEnabled(true);
        barcodeLauncher.launch(options);
    }

    private void getFoodInfo(String barcode) {
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
                    Intent intent = new Intent(ScannerActivity.this, ProductDetailActivity.class);
                    intent.putExtra("product_name", product.getProductName());
                    intent.putExtra("calories", product.getNutriments() != null ? product.getNutriments().getCalories() : 0f);
                    intent.putExtra("proteins", product.getNutriments() != null ? product.getNutriments().getProteins() : 0f);
                    intent.putExtra("carbs", product.getNutriments() != null ? product.getNutriments().getCarbs() : 0f);
                    intent.putExtra("fats", product.getNutriments() != null ? product.getNutriments().getFats() : 0f);
                    intent.putExtra("saturated_fat", product.getNutriments() != null ? product.getNutriments().getSaturatedFat() : 0f);
                    intent.putExtra("sugars", product.getNutriments() != null ? product.getNutriments().getSugars() : 0f);
                    intent.putExtra("image_url", product.getImageUrl());
                    startActivity(intent);
                } else {
                    Toast.makeText(ScannerActivity.this, "Product not found", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ProductResponse> call, Throwable t) {
                Toast.makeText(ScannerActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}