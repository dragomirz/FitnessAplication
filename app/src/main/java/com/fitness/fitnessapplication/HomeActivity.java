package com.fitness.fitnessapplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;
import com.fitness.fitnessapplication.DataModels.DailyLogResponse;
import com.fitness.fitnessapplication.RetroFit.ApiClient;
import com.fitness.fitnessapplication.RetroFit.ApiService;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeActivity extends AppCompatActivity {
    private Spinner spinnerPeriod;
    private TextView totalsView;
    private ProgressBar caloriesProgress;
    private String[] periods = {"Daily", "Weekly", "Monthly"};
    private ActivityResultLauncher<ScanOptions> barcodeLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        spinnerPeriod = findViewById(R.id.spinner_period);
        totalsView = findViewById(R.id.totals_view);
        caloriesProgress = findViewById(R.id.calories_progress);
        Button btnHome = findViewById(R.id.btn_home);
        Button btnScanner = findViewById(R.id.btn_scanner);
        Button btnUserInfo = findViewById(R.id.btn_user_info);

        // Set up Spinner
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, periods);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPeriod.setAdapter(adapter);
        spinnerPeriod.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                fetchLogs(periods[position]);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                fetchLogs("Daily");
            }
        });

        // Set up Barcode Launcher
        barcodeLauncher = registerForActivityResult(new ScanContract(), result -> {
            if (result.getContents() == null) {
                Toast.makeText(this, "Cancelled", Toast.LENGTH_LONG).show();
            } else {
                String barcode = result.getContents();
                Intent intent = new Intent(HomeActivity.this, ProductDetailActivity.class);
                intent.putExtra("barcode", barcode);
                startActivity(intent);
            }
        });

        // Button Listeners
        btnHome.setOnClickListener(v -> fetchLogs(spinnerPeriod.getSelectedItem().toString()));
        btnScanner.setOnClickListener(v -> scanBarcode());
        btnUserInfo.setOnClickListener(v -> { /* Implement later */ });

        // Initial fetch
        fetchLogs("Daily");
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

    private void fetchLogs(String period) {
        ApiService apiService = ApiClient.getApiService(this);
        String date = new java.text.SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date());
        Call<DailyLogResponse> call;

        switch (period) {
            case "Weekly":
                call = apiService.getDailyLogs(date, "weekly");
                break;
            case "Monthly":
                call = apiService.getDailyLogs(date, "monthly");
                break;
            case "Daily":
            default:
                call = apiService.getDailyLogs(date, null);
                break;
        }

        call.enqueue(new Callback<DailyLogResponse>() {
            @Override
            public void onResponse(Call<DailyLogResponse> call, Response<DailyLogResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    DailyLogResponse.Totals totals = response.body().getTotals();
                    totalsView.setText(String.format("Calories: %.1f kcal\nProteins: %.1fg\nCarbs: %.1fg\nFats: %.1fg\nSat. Fat: %.1fg\nSugars: %.1fg",
                            totals.getCalories(), totals.getProteins(), totals.getCarbs(), totals.getFats(),
                            totals.getSaturatedFat(), totals.getSugars()));
                    int calorieGoal = period.equals("Daily") ? 2000 : period.equals("Weekly") ? 14000 : 60000;
                    caloriesProgress.setMax(calorieGoal);
                    caloriesProgress.setProgress((int) totals.getCalories());
                } else if (response.code() == 401 || response.code() == 403) {
                    Toast.makeText(HomeActivity.this, "Token expired or invalid. Please log in again.", Toast.LENGTH_LONG).show();
                    SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
                    sharedPreferences.edit().remove("token").apply();
                    startActivity(new Intent(HomeActivity.this, MainActivity.class));
                    finish();
                } else {
                    totalsView.setText("No data for this period");
                }
            }

            @Override
            public void onFailure(Call<DailyLogResponse> call, Throwable t) {
                totalsView.setText("Error: " + t.getMessage());
            }
        });
    }
}