package com.fitness.fitnessapplication;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.fitness.fitnessapplication.DataModels.DailyLogResponse;
import com.fitness.fitnessapplication.RetroFit.ApiClient;
import com.fitness.fitnessapplication.RetroFit.ApiService;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeActivity extends AppCompatActivity {
    private AutoCompleteTextView periodDropdown;
    private TextView caloriesValue;
    private TextView calorieGoalText;
    private TextView proteinValue;
    private TextView carbsValue;
    private TextView fatsValue;
    private TextView sugarValue;
    private CircularProgressIndicator caloriesProgress;
    private LinearProgressIndicator proteinProgress;
    private LinearProgressIndicator carbsProgress;
    private LinearProgressIndicator fatsProgress;
    private LinearProgressIndicator sugarProgress;
    private String[] periods = {"Daily", "Weekly", "Monthly"};
    private ActivityResultLauncher<ScanOptions> barcodeLauncher;

    // Nutrition goals - these could be user-configurable later
    private final int DAILY_CALORIE_GOAL = 2000;
    private final int WEEKLY_CALORIE_GOAL = 14000;
    private final int MONTHLY_CALORIE_GOAL = 60000;
    private final int PROTEIN_GOAL_DAILY = 50; // in grams
    private final int CARBS_GOAL_DAILY = 275;  // in grams
    private final int FATS_GOAL_DAILY = 65;    // in grams
    private final int SUGAR_GOAL_DAILY = 50;   // in grams

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Hide the ActionBar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        setContentView(R.layout.activity_home);

        // Initialize views
        initializeViews();

        // Set up period selector
        setupPeriodSelector();

        // Set up barcode scanner
        setupBarcodeScanner();

        // Set up button click listeners
        setupButtonListeners();

        // Initial data fetch
        fetchLogs("Daily");
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh data when returning to the activity
        String currentPeriod = periodDropdown.getText().toString();
        if (!currentPeriod.isEmpty()) {
            fetchLogs(currentPeriod);
        } else {
            fetchLogs("Daily"); // Default to Daily if no period is selected
        }
    }

    private void initializeViews() {
        periodDropdown = findViewById(R.id.period_dropdown);
        caloriesValue = findViewById(R.id.calories_value);
        calorieGoalText = findViewById(R.id.calorie_goal);
        proteinValue = findViewById(R.id.protein_value);
        carbsValue = findViewById(R.id.carbs_value);
        fatsValue = findViewById(R.id.fats_value);
        sugarValue = findViewById(R.id.sugar_value);
        caloriesProgress = findViewById(R.id.calories_progress);
        proteinProgress = findViewById(R.id.protein_progress);
        carbsProgress = findViewById(R.id.carbs_progress);
        fatsProgress = findViewById(R.id.fats_progress);
        sugarProgress = findViewById(R.id.sugar_progress);

        caloriesProgress.setMax(DAILY_CALORIE_GOAL);
        proteinProgress.setMax(PROTEIN_GOAL_DAILY);
        carbsProgress.setMax(CARBS_GOAL_DAILY);
        fatsProgress.setMax(FATS_GOAL_DAILY);
        sugarProgress.setMax(SUGAR_GOAL_DAILY);

        calorieGoalText.setText("of " + DAILY_CALORIE_GOAL + " kcal");
    }

    private void setupPeriodSelector() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, periods);
        periodDropdown.setAdapter(adapter);
        periodDropdown.setText(periods[0], false);

        periodDropdown.setOnItemClickListener((parent, view, position, id) -> {
            fetchLogs(periods[position]);
        });
    }

    private void setupBarcodeScanner() {
        barcodeLauncher = registerForActivityResult(new ScanContract(), result -> {
            if (result.getContents() == null) {
                Toast.makeText(this, "Scan cancelled", Toast.LENGTH_SHORT).show();
            } else {
                String barcode = result.getContents();
                Intent intent = new Intent(HomeActivity.this, ProductDetailActivity.class);
                intent.putExtra("barcode", barcode);
                startActivity(intent);
            }
        });
    }

    private void setupButtonListeners() {
        Button btnHome = findViewById(R.id.btn_home);
        Button btnScanner = findViewById(R.id.btn_scanner);
        Button btnUserInfo = findViewById(R.id.btn_user_info);

        btnHome.setOnClickListener(v -> refreshData());
        btnScanner.setOnClickListener(v -> scanBarcode());
        btnUserInfo.setOnClickListener(v -> {
            Toast.makeText(this, "User Info feature coming soon!", Toast.LENGTH_SHORT).show();
        });
    }

    private void refreshData() {
        String period = periodDropdown.getText().toString();
        fetchLogs(period);
        Toast.makeText(this, "Refreshing " + period + " data", Toast.LENGTH_SHORT).show();
    }

    private void scanBarcode() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 1);
        } else {
            ScanOptions options = new ScanOptions();
            options.setOrientationLocked(false);
            options.setDesiredBarcodeFormats(ScanOptions.ALL_CODE_TYPES);
            options.setPrompt("Scan a product barcode");
            options.setCameraId(0);
            options.setBeepEnabled(false);
            options.setBarcodeImageEnabled(true);
            barcodeLauncher.launch(options);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            scanBarcode();
        } else {
            Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show();
        }
    }

    private void fetchLogs(String period) {
        ApiService apiService = ApiClient.getApiService(this);
        String date = new java.text.SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date());
        Call<DailyLogResponse> call;

        int calorieGoal;
        int proteinGoal;
        int carbsGoal;
        int fatsGoal;
        int sugarGoal;

        switch (period) {
            case "Weekly":
                call = apiService.getDailyLogs(date, "weekly");
                calorieGoal = WEEKLY_CALORIE_GOAL;
                proteinGoal = PROTEIN_GOAL_DAILY * 7;
                carbsGoal = CARBS_GOAL_DAILY * 7;
                fatsGoal = FATS_GOAL_DAILY * 7;
                sugarGoal = SUGAR_GOAL_DAILY * 7;
                break;
            case "Monthly":
                call = apiService.getDailyLogs(date, "monthly");
                calorieGoal = MONTHLY_CALORIE_GOAL;
                proteinGoal = PROTEIN_GOAL_DAILY * 30;
                carbsGoal = CARBS_GOAL_DAILY * 30;
                fatsGoal = FATS_GOAL_DAILY * 30;
                sugarGoal = SUGAR_GOAL_DAILY * 30;
                break;
            case "Daily":
            default:
                call = apiService.getDailyLogs(date, null);
                calorieGoal = DAILY_CALORIE_GOAL;
                proteinGoal = PROTEIN_GOAL_DAILY;
                carbsGoal = CARBS_GOAL_DAILY;
                fatsGoal = FATS_GOAL_DAILY;
                sugarGoal = SUGAR_GOAL_DAILY;
                break;
        }

        caloriesProgress.setMax(calorieGoal);
        proteinProgress.setMax(proteinGoal);
        carbsProgress.setMax(carbsGoal);
        fatsProgress.setMax(fatsGoal);
        sugarProgress.setMax(sugarGoal);

        calorieGoalText.setText("of " + calorieGoal + " kcal");

        call.enqueue(new Callback<DailyLogResponse>() {
            @Override
            public void onResponse(Call<DailyLogResponse> call, Response<DailyLogResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    DailyLogResponse.Totals totals = response.body().getTotals();
                    if (totals != null) {
                        updateNutritionData(totals, calorieGoal, proteinGoal, carbsGoal, fatsGoal, sugarGoal);
                    } else {
                        showError("No totals data available");
                    }
                } else if (response.code() == 401 || response.code() == 403) {
                    handleAuthFailure();
                } else {
                    showError("No data available for this period");
                }
            }

            @Override
            public void onFailure(Call<DailyLogResponse> call, Throwable t) {
                showError("Network error: " + t.getMessage());
            }
        });
    }

    private void updateNutritionData(DailyLogResponse.Totals totals, int calorieGoal, int proteinGoal,
                                     int carbsGoal, int fatsGoal, int sugarGoal) {
        int calories = (int) Math.round(totals.getCalories());
        caloriesValue.setText(String.valueOf(calories));
        animateProgress(caloriesProgress, calories);

        int proteins = (int) Math.round(totals.getProteins());
        proteinValue.setText(proteins + "g");
        animateProgress(proteinProgress, proteins);

        int carbs = (int) Math.round(totals.getCarbs());
        carbsValue.setText(carbs + "g");
        animateProgress(carbsProgress, carbs);

        int fats = (int) Math.round(totals.getFats());
        fatsValue.setText(fats + "g");
        animateProgress(fatsProgress, fats);

        int sugars = (int) Math.round(totals.getSugars());
        sugarValue.setText(sugars + "g");
        animateProgress(sugarProgress, sugars);
    }

    private void animateProgress(CircularProgressIndicator indicator, int value) {
        ObjectAnimator.ofInt(indicator, "progress", indicator.getProgress(), value)
                .setDuration(1000)
                .start();
    }

    private void animateProgress(LinearProgressIndicator indicator, int value) {
        ObjectAnimator.ofInt(indicator, "progress", indicator.getProgress(), value)
                .setDuration(1000)
                .start();
    }

    private void handleAuthFailure() {
        Toast.makeText(this, "Session expired. Please log in again.", Toast.LENGTH_LONG).show();
        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        sharedPreferences.edit().remove("token").apply();
        startActivity(new Intent(HomeActivity.this, MainActivity.class));
        finish();
    }

    private void showError(String message) {
        caloriesValue.setText("0");
        caloriesProgress.setProgress(0);

        proteinValue.setText("0g");
        carbsValue.setText("0g");
        fatsValue.setText("0g");
        sugarValue.setText("0g");

        proteinProgress.setProgress(0);
        carbsProgress.setProgress(0);
        fatsProgress.setProgress(0);
        sugarProgress.setProgress(0);

        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}