package com.fitness.fitnessapplication;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources; // Added
import android.graphics.drawable.ColorDrawable; // Added
import android.os.Bundle;
import android.util.TypedValue; // Added
import android.view.LayoutInflater; // Added
import android.view.View; // Added
import android.view.ViewGroup; // Added
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.FrameLayout; // Added
import android.widget.LinearLayout; // Added
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

// Updated import for the modified response structure
import com.fitness.fitnessapplication.DataModels.DailyLogResponse;
import com.fitness.fitnessapplication.DataModels.User;
import com.fitness.fitnessapplication.RetroFit.ApiClient;
import com.fitness.fitnessapplication.RetroFit.ApiService;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import java.text.ParseException; // Added
import java.text.SimpleDateFormat; // Added
import java.util.ArrayList; // Added
import java.util.Date; // Added
import java.util.List; // Added
import java.util.Locale; // Added

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeActivity extends AppCompatActivity {

    // Existing Views
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
    private Button btnHome, btnScanner, btnUserInfo; // Assuming IDs are btn_home, btn_scanner, btn_user_info

    // New/Modified Views
    private FrameLayout dailyMonthlyCalorieView; // Container for circular progress
    private LinearLayout weeklyBarContainer;     // Container for weekly bars
    private TextView nutritionSummaryTitle;     // Title text view inside the card

    // Other Variables
    private String[] periods = {"Daily", "Weekly", "Monthly"};
    private ActivityResultLauncher<ScanOptions> barcodeLauncher;
    private List<DailyLogResponse.DailyData> weeklyDataCache; // Cache for weekly data
    private View currentlySelectedBarView = null; // To track the selected bar view

    // Nutrition goals
    private int DAILY_CALORIE_GOAL = 2000;
    private int WEEKLY_CALORIE_GOAL = 14000;
    private int MONTHLY_CALORIE_GOAL = 60000;
    private final int PROTEIN_GOAL_DAILY = 50;
    private final int CARBS_GOAL_DAILY = 275;
    private final int FATS_GOAL_DAILY = 65;
    private final int SUGAR_GOAL_DAILY = 50;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        setContentView(R.layout.activity_home);

        initializeViews();
        setupPeriodSelector();
        setupBarcodeScanner();
        setupButtonListeners();

        // Fetch user data first to set goals, which then triggers the initial fetchLogs
        getUserData();
        // Initial fetchLogs("Daily") will happen inside updateCalorieGoal after getUserData finishes
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh data when returning - use the current selection
        String currentPeriod = periodDropdown.getText().toString();
        if (!currentPeriod.isEmpty()) {
            // Re-fetch user data in case goals changed elsewhere, then fetch logs
            getUserData();
        } else {
            periodDropdown.setText(periods[0], false); // Set default if empty
            getUserData(); // Fetch goals then logs
        }
    }

    private void initializeViews() {
        // Existing Views
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
        btnHome = findViewById(R.id.btn_home);
        btnScanner = findViewById(R.id.btn_scanner);
        btnUserInfo = findViewById(R.id.btn_user_info);


        // New/Modified Views
        dailyMonthlyCalorieView = findViewById(R.id.daily_monthly_calorie_view);
        weeklyBarContainer = findViewById(R.id.weekly_bar_container);
        nutritionSummaryTitle = findViewById(R.id.nutrition_summary_title);

        // Initial UI Goal Setup (will be updated by getUserData)
        updateUiForGoals(DAILY_CALORIE_GOAL, PROTEIN_GOAL_DAILY, CARBS_GOAL_DAILY, FATS_GOAL_DAILY, SUGAR_GOAL_DAILY);
    }

    private void setupPeriodSelector() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, periods);
        periodDropdown.setAdapter(adapter);
        // Set initial text, but fetchLogs will be driven by getUserData->updateCalorieGoal
        periodDropdown.setText(periods[0], false);

        periodDropdown.setOnItemClickListener((parent, view, position, id) -> {
            // Clear previous selection highlight when period changes
            highlightBar(null);
            fetchLogs(periods[position]);
        });
    }

    private void setupBarcodeScanner() {
        barcodeLauncher = registerForActivityResult(new ScanContract(), result -> {
            if (result.getContents() == null) {
                Toast.makeText(this, "Scan cancelled", Toast.LENGTH_SHORT).show();
            } else {
                String barcode = result.getContents();
                // Assuming ProductDetailActivity exists and handles the barcode
                Intent intent = new Intent(HomeActivity.this, ProductDetailActivity.class);
                intent.putExtra("barcode", barcode);
                startActivity(intent);
            }
        });
    }

    private void setupButtonListeners() {
        btnHome.setOnClickListener(v -> refreshData());
        btnScanner.setOnClickListener(v -> scanBarcode());
        btnUserInfo.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, UserInfoActivity.class);
            startActivity(intent);
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
            options.setOrientationLocked(false); // Prefer false for easier scanning
            options.setDesiredBarcodeFormats(ScanOptions.PRODUCT_CODE_TYPES); // Be more specific
            options.setPrompt("Scan a product barcode");
            options.setCameraId(0); // Use default camera
            options.setBeepEnabled(true); // Enable beep on scan
            options.setBarcodeImageEnabled(false); // No need for image usually
            barcodeLauncher.launch(options);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                scanBarcode();
            } else {
                Toast.makeText(this, "Camera permission is required to scan barcodes.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void getUserData() {
        ApiService apiService = ApiClient.getApiService(this);
        if (apiService == null) {
            Toast.makeText(HomeActivity.this, "Error initializing API service.", Toast.LENGTH_SHORT).show();
            // Handle error appropriately, maybe redirect to login or show retry option
            handleAuthFailure(); // Or a more specific error handling
            return;
        }

        Call<User> userDataCall = apiService.getUserData();
        userDataCall.enqueue(new Callback<User>() { // Use User directly if User class has all fields
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (response.isSuccessful() && response.body() != null) {
                    User userData = response.body();
                    // Save user data to SharedPreferences
                    SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString("name", userData.getName());
                    editor.putInt("age", userData.getAge());
                    editor.putString("weight", userData.getWeight());
                    editor.putString("height", userData.getHeight());
                    editor.putString("gender", userData.getGender());
                    // Make sure getCalories() exists in User model and returns float or double
                    editor.putFloat("calorie_goal", (float) userData.getCalories());
                    editor.apply();

                    // Update UI with user's calorie goal, which triggers fetchLogs
                    updateCalorieGoal(userData.getCalories());
                } else if (response.code() == 401 || response.code() == 403){
                    handleAuthFailure(); // Handle auth failure specifically
                } else {
                    Toast.makeText(HomeActivity.this, "Failed to get user data: " + response.message(), Toast.LENGTH_LONG).show();
                    // Attempt to load from SharedPreferences as fallback?
                    loadGoalsFromPrefsAndFetchLogs();
                }
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                Toast.makeText(HomeActivity.this, "Network error getting user data: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                System.err.println("Network error getting user data: " + t.getMessage());
                // Attempt to load from SharedPreferences as fallback
                loadGoalsFromPrefsAndFetchLogs();
            }
        });
    }

    // Fallback loading mechanism
    private void loadGoalsFromPrefsAndFetchLogs() {
        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        float savedGoal = sharedPreferences.getFloat("calorie_goal", 2000f); // Default to 2000 if not found
        updateCalorieGoal(savedGoal);
    }

    // Method to update the calorie goal variables and UI elements
    private void updateCalorieGoal(double calorieGoal) {
        DAILY_CALORIE_GOAL = (int) Math.round(calorieGoal); // Use round
        WEEKLY_CALORIE_GOAL = DAILY_CALORIE_GOAL * 7;
        // Monthly is approximate, consider using average days in month if needed later
        MONTHLY_CALORIE_GOAL = DAILY_CALORIE_GOAL * 30;

        // Fetch logs based on the *currently selected* period in the dropdown
        String currentPeriod = periodDropdown.getText().toString();
        if (currentPeriod.isEmpty()) { // If dropdown is empty somehow, default to Daily
            currentPeriod = periods[0];
            periodDropdown.setText(currentPeriod, false);
        }
        fetchLogs(currentPeriod); // Trigger log fetching AFTER goals are updated
    }

    // Central method to fetch data based on selected period
    private void fetchLogs(String period) {
        ApiService apiService = ApiClient.getApiService(this);
        if (apiService == null) {
            showError("API Service Error");
            return;
        }
        // Use current date for fetching
        String date = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        Call<DailyLogResponse> call;

        // Goals for the summary card (initially set based on period, adjusted for weekly later)
        int summaryCalorieGoal = DAILY_CALORIE_GOAL; // Default to daily
        int summaryProteinGoal = PROTEIN_GOAL_DAILY;
        int summaryCarbsGoal = CARBS_GOAL_DAILY;
        int summaryFatsGoal = FATS_GOAL_DAILY;
        int summarySugarGoal = SUGAR_GOAL_DAILY;

        // Adjust visibility and goals based on period
        if ("Weekly".equals(period)) {
            dailyMonthlyCalorieView.setVisibility(View.GONE);
            weeklyBarContainer.setVisibility(View.VISIBLE);
            nutritionSummaryTitle.setText("Nutrition Summary (Select a Day)");
            // Goals for summary card remain daily when viewing weekly details
        } else {
            dailyMonthlyCalorieView.setVisibility(View.VISIBLE);
            weeklyBarContainer.setVisibility(View.GONE);
            nutritionSummaryTitle.setText("Nutrition Summary");
            // Set aggregate goals for Daily/Monthly views
            if ("Monthly".equals(period)) {
                summaryCalorieGoal = MONTHLY_CALORIE_GOAL;
                summaryProteinGoal *= 30; // Approximate monthly goal
                summaryCarbsGoal *= 30;
                summaryFatsGoal *= 30;
                summarySugarGoal *= 30;
            } else { // Daily
                summaryCalorieGoal = DAILY_CALORIE_GOAL;
                // Daily goals are already set
            }
            // Update the circular progress goal text and max value for Daily/Monthly
            updateUiForGoals(summaryCalorieGoal, summaryProteinGoal, summaryCarbsGoal, summaryFatsGoal, summarySugarGoal);
        }

        // Make the API call
        // Pass lowercase range "daily", "weekly", "monthly" or null for default (daily)
        String rangeParam = period.equalsIgnoreCase("Daily") ? null : period.toLowerCase(Locale.US);
        call = apiService.getDailyLogs(date, rangeParam);

        int finalSummaryCalorieGoal = summaryCalorieGoal;
        int finalSummaryProteinGoal = summaryProteinGoal;
        int finalSummaryCarbsGoal = summaryCarbsGoal;
        int finalSummaryFatsGoal = summaryFatsGoal;
        int finalSummarySugarGoal = summarySugarGoal;
        call.enqueue(new Callback<DailyLogResponse>() {
            @Override
            public void onResponse(Call<DailyLogResponse> call, Response<DailyLogResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    DailyLogResponse data = response.body();
                    if ("Weekly".equals(period)) {
                        if (data.getDailyData() != null && !data.getDailyData().isEmpty()) {
                            weeklyDataCache = data.getDailyData();
                            populateWeeklyBars(weeklyDataCache);
                            // Show last day's summary initially and highlight bar
                            if (!weeklyDataCache.isEmpty()) {
                                // Pass daily goals for the initial summary view
                                updateNutritionSummaryForDay(weeklyDataCache.get(weeklyDataCache.size() - 1),
                                        PROTEIN_GOAL_DAILY, CARBS_GOAL_DAILY, FATS_GOAL_DAILY, SUGAR_GOAL_DAILY);
                                if (weeklyBarContainer.getChildCount() > 0) {
                                    highlightBar(weeklyBarContainer.getChildAt(weeklyBarContainer.getChildCount() - 1).findViewById(R.id.bar_view));
                                }
                            } else {
                                // Handle case where dailyData exists but is empty
                                showError("No log data found for this week.");
                                updateNutritionSummaryForDay(null, PROTEIN_GOAL_DAILY, CARBS_GOAL_DAILY, FATS_GOAL_DAILY, SUGAR_GOAL_DAILY); // Clear summary
                            }
                        } else {
                            weeklyDataCache = new ArrayList<>();
                            weeklyBarContainer.removeAllViews();
                            showError("No weekly data available.");
                            updateNutritionSummaryForDay(null, PROTEIN_GOAL_DAILY, CARBS_GOAL_DAILY, FATS_GOAL_DAILY, SUGAR_GOAL_DAILY); // Clear summary
                        }
                    } else { // Daily or Monthly Aggregate View
                        if (data.getTotals() != null) {
                            // Pass the calculated aggregate goals for this period
                            updateAggregateNutritionData(data.getTotals(), finalSummaryCalorieGoal, finalSummaryProteinGoal, finalSummaryCarbsGoal, finalSummaryFatsGoal, finalSummarySugarGoal);
                        } else {
                            showError("No totals data available for " + period);
                            // Clear aggregate view and summary
                            updateAggregateNutritionData(null, finalSummaryCalorieGoal, finalSummaryProteinGoal, finalSummaryCarbsGoal, finalSummaryFatsGoal, finalSummarySugarGoal);
                        }
                    }
                } else if (response.code() == 401 || response.code() == 403) {
                    handleAuthFailure();
                } else {
                    // Handle other non-successful responses (e.g., 404, 500)
                    String errorMsg = "Error fetching data (" + response.code() + ")";
                    try { // Try to get error body
                        if (response.errorBody() != null) errorMsg += ": " + response.errorBody().string();
                    } catch (Exception e) { /* Ignore */ }
                    showError(errorMsg);
                    // Clear relevant views based on period
                    if ("Weekly".equals(period)) {
                        weeklyDataCache = new ArrayList<>();
                        weeklyBarContainer.removeAllViews();
                        updateNutritionSummaryForDay(null, PROTEIN_GOAL_DAILY, CARBS_GOAL_DAILY, FATS_GOAL_DAILY, SUGAR_GOAL_DAILY);
                    } else {
                        updateAggregateNutritionData(null, finalSummaryCalorieGoal, finalSummaryProteinGoal, finalSummaryCarbsGoal, finalSummaryFatsGoal, finalSummarySugarGoal);
                    }
                }
            }

            @Override
            public void onFailure(Call<DailyLogResponse> call, Throwable t) {
                showError("Network error: " + t.getMessage());
                System.err.println("Network Error fetching logs: " + t.toString());
                // Clear relevant views based on period
                if ("Weekly".equals(period)) {
                    weeklyDataCache = new ArrayList<>();
                    weeklyBarContainer.removeAllViews();
                    updateNutritionSummaryForDay(null, PROTEIN_GOAL_DAILY, CARBS_GOAL_DAILY, FATS_GOAL_DAILY, SUGAR_GOAL_DAILY);
                } else {
                    // Clear aggregate view and summary
                    updateAggregateNutritionData(null, finalSummaryCalorieGoal, finalSummaryProteinGoal, finalSummaryCarbsGoal, finalSummaryFatsGoal, finalSummarySugarGoal);
                }
            }
        });
    }

    // Updates Aggregate View (Daily/Monthly Circular Progress)
    private void updateAggregateNutritionData(DailyLogResponse.Totals totals, int calorieGoal, int proteinGoal,
                                              int carbsGoal, int fatsGoal, int sugarGoal) {

        if (totals != null) {
            int calories = Math.round(totals.getCalories());
            caloriesValue.setText(String.valueOf(calories));
            animateProgress(caloriesProgress, calories);
            // Update the nutrition summary card using the aggregate totals and goals
            updateNutritionSummary(totals, proteinGoal, carbsGoal, fatsGoal, sugarGoal);
        } else {
            // Clear the view if totals are null
            caloriesValue.setText("0");
            animateProgress(caloriesProgress, 0);
            // Clear the summary card as well
            updateNutritionSummary(null, proteinGoal, carbsGoal, fatsGoal, sugarGoal);
        }
    }

    // Reusable method to update ONLY the Nutrition Summary Card's linear bars and text
    private void updateNutritionSummary(DailyLogResponse.Totals totals, int proteinGoal, int carbsGoal, int fatsGoal, int sugarGoal) {
        // Ensure goals are positive for setting max progress
        proteinProgress.setMax(Math.max(1, proteinGoal));
        carbsProgress.setMax(Math.max(1, carbsGoal));
        fatsProgress.setMax(Math.max(1, fatsGoal));
        sugarProgress.setMax(Math.max(1, sugarGoal));

        if (totals != null) {
            int proteins = Math.round(totals.getProteins());
            proteinValue.setText(proteins + "g");
            animateProgress(proteinProgress, proteins);

            int carbs = Math.round(totals.getCarbs());
            carbsValue.setText(carbs + "g");
            animateProgress(carbsProgress, carbs);

            int fats = Math.round(totals.getFats());
            fatsValue.setText(fats + "g");
            animateProgress(fatsProgress, fats);

            // Use getSaturatedFat() if that's the correct getter based on DailyLogResponse
            // float saturated = totals.getSaturatedFat();

            int sugars = Math.round(totals.getSugars());
            sugarValue.setText(sugars + "g");
            animateProgress(sugarProgress, sugars);
        } else {
            // Clear values if totals are null
            proteinValue.setText("0g");
            carbsValue.setText("0g");
            fatsValue.setText("0g");
            sugarValue.setText("0g");
            animateProgress(proteinProgress, 0);
            animateProgress(carbsProgress, 0);
            animateProgress(fatsProgress, 0);
            animateProgress(sugarProgress, 0);
        }
    }

    // --- Weekly Bar Population and Interaction ---

    private void populateWeeklyBars(List<DailyLogResponse.DailyData> dailyDataList) {
        weeklyBarContainer.removeAllViews(); // Clear previous bars
        currentlySelectedBarView = null; // Reset selection

        if (dailyDataList == null || dailyDataList.isEmpty()) {
            // Optionally show a message here if needed
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(this);
        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        SimpleDateFormat outputFormat = new SimpleDateFormat("EEE", Locale.US); // For day initial

        // 1. Find max calorie value for scaling
        double maxCalories = 0;
        for (DailyLogResponse.DailyData dayData : dailyDataList) {
            if (dayData != null && dayData.getTotals() != null && dayData.getTotals().getCalories() > maxCalories) {
                maxCalories = dayData.getTotals().getCalories();
            }
        }
        // Use daily goal or a default if max is 0, prevents division by zero
        if (maxCalories <= 0) {
            maxCalories = Math.max(1.0, DAILY_CALORIE_GOAL); // Ensure max is at least 1
        }

        // 2. Define max bar height in dp and convert to pixels
        int maxBarHeightDp = 140; // Adjust as needed (should be less than container height)
        int maxBarHeightPx = dpToPx(maxBarHeightDp);
        int minBarHeightPx = dpToPx(2); // Min height for bars with >0 calories


        // 3. Inflate and configure bars
        for (int i = 0; i < dailyDataList.size(); i++) {
            final DailyLogResponse.DailyData dayData = dailyDataList.get(i);
            if (dayData == null) continue; // Skip if data for a day is somehow null

            View barItemView = inflater.inflate(R.layout.item_weekly_bar, weeklyBarContainer, false);

            View barView = barItemView.findViewById(R.id.bar_view);
            TextView dayLabel = barItemView.findViewById(R.id.day_label_text);
            TextView calorieLabel = barItemView.findViewById(R.id.calorie_text);

            // Set Day Label
            try {
                if (dayData.getDate() != null) {
                    Date date = inputFormat.parse(dayData.getDate());
                    dayLabel.setText(outputFormat.format(date).substring(0, 1)); // Get first letter
                } else {
                    dayLabel.setText("?");
                }
            } catch (ParseException | NullPointerException | IndexOutOfBoundsException e) {
                dayLabel.setText("?");
                System.err.println("Error parsing date for label: " + dayData.getDate() + " | " + e.getMessage());
            }

            // Calculate and Set Bar Height
            double currentCalories = (dayData.getTotals() != null) ? dayData.getTotals().getCalories() : 0;
            int barHeight;
            if (currentCalories > 0) {
                barHeight = (int) ((currentCalories / maxCalories) * maxBarHeightPx);
                barHeight = Math.max(minBarHeightPx, barHeight); // Ensure minimum visible height
                calorieLabel.setText(String.valueOf(Math.round(currentCalories)));
                calorieLabel.setVisibility(View.VISIBLE);
            } else {
                barHeight = 0; // No height for zero calories
                calorieLabel.setVisibility(View.INVISIBLE);
            }

            ViewGroup.LayoutParams params = barView.getLayoutParams();
            params.height = barHeight;
            barView.setLayoutParams(params);

            // Set Click Listener for the whole column
            final int index = i; // Need final index for listener
            barItemView.setOnClickListener(v -> {
                if (weeklyDataCache != null && index < weeklyDataCache.size()) {
                    // Pass daily goals when updating summary from a selected day
                    updateNutritionSummaryForDay(weeklyDataCache.get(index),
                            PROTEIN_GOAL_DAILY, CARBS_GOAL_DAILY, FATS_GOAL_DAILY, SUGAR_GOAL_DAILY);
                    highlightBar(barView); // Highlight the clicked bar's view
                }
            });

            // Add the configured item to the container
            weeklyBarContainer.addView(barItemView);
        }
        // Ensure layout updates after adding views
        weeklyBarContainer.requestLayout();
    }

    // Updates the Nutrition Summary Card based on selected day's data
    private void updateNutritionSummaryForDay(DailyLogResponse.DailyData dayData, int proteinGoal, int carbsGoal, int fatsGoal, int sugarGoal) {
        if (dayData != null && dayData.getDate() != null) {
            // Update the title
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            SimpleDateFormat outputFormat = new SimpleDateFormat("EEE, MMM d", Locale.US); // e.g., Mon, Mar 24
            try {
                Date date = inputFormat.parse(dayData.getDate());
                nutritionSummaryTitle.setText("Nutrition Summary (" + outputFormat.format(date) + ")");
            } catch (ParseException | NullPointerException e) {
                nutritionSummaryTitle.setText("Nutrition Summary (Selected Day)");
                System.err.println("Error parsing date for title: " + dayData.getDate());
            }
            // Update the summary card using the selected day's totals and the provided (daily) goals
            updateNutritionSummary(dayData.getTotals(), proteinGoal, carbsGoal, fatsGoal, sugarGoal);
        } else {
            // Clear the summary if data is null
            nutritionSummaryTitle.setText("Nutrition Summary");
            updateNutritionSummary(null, proteinGoal, carbsGoal, fatsGoal, sugarGoal); // Pass null totals
        }
    }


    // Helper to highlight the selected bar and unhighlight the previous one
    private void highlightBar(View selectedBar) {
        // Reset previous selection (using default colorPrimary)
        if (currentlySelectedBarView != null && currentlySelectedBarView.getBackground() instanceof ColorDrawable) {
            currentlySelectedBarView.setBackgroundColor(ContextCompat.getColor(this, R.color.colorPrimary));
        } else if (currentlySelectedBarView != null) {
            // If background wasn't a simple color, you might need a different way to store/reset
            currentlySelectedBarView.setBackgroundColor(ContextCompat.getColor(this, R.color.colorPrimary)); // Attempt reset
        }


        // Apply selection highlight
        if (selectedBar != null) {
            int selectedColor = ContextCompat.getColor(this, R.color.colorSecondary); // Make sure you have this color
            // Basic fallback if colorSecondary isn't defined (consider a better fallback)
            if (selectedColor == 0) selectedColor = ContextCompat.getColor(this, android.R.color.holo_blue_light);

            selectedBar.setBackgroundColor(selectedColor);
            currentlySelectedBarView = selectedBar;
        } else {
            currentlySelectedBarView = null; // No bar selected
        }
    }


    // Helper method to convert dp to pixels
    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                getResources().getDisplayMetrics()
        );
    }

    // Helper to set max values and text for goals (used for Daily/Monthly circular view)
    private void updateUiForGoals(int calorieGoal, int proteinGoal, int carbsGoal, int fatsGoal, int sugarGoal) {
        // Set max for circular progress
        caloriesProgress.setMax(Math.max(1, calorieGoal));
        calorieGoalText.setText("of " + calorieGoal + " kcal");

        // Max values for linear bars are set within updateNutritionSummary based on context
    }

    // --- Animation Methods ---

    private void animateProgress(CircularProgressIndicator indicator, int value) {
        int max = indicator.getMax();
        int progressValue = Math.max(0, Math.min(value, max)); // Ensure 0 <= value <= max
        ObjectAnimator.ofInt(indicator, "progress", indicator.getProgress(), progressValue)
                .setDuration(800) // Adjust duration as needed
                .start();
    }

    private void animateProgress(LinearProgressIndicator indicator, int value) {
        int max = indicator.getMax();
        int progressValue = Math.max(0, Math.min(value, max)); // Ensure 0 <= value <= max
        ObjectAnimator.ofInt(indicator, "progress", indicator.getProgress(), progressValue)
                .setDuration(800)
                .start();
    }

    // --- Error Handling and Auth Failure ---

    private void handleAuthFailure() {
        Toast.makeText(this, "Session expired. Please log in again.", Toast.LENGTH_LONG).show();
        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        sharedPreferences.edit().remove("token").apply(); // Clear token
        // Optional: Clear other sensitive user data from prefs
        Intent intent = new Intent(HomeActivity.this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // Clear back stack
        startActivity(intent);
        finish(); // Finish HomeActivity
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        System.err.println("HomeActivity Error: " + message); // Log error for debugging

        // Determine current period to clear correctly
        String period = periodDropdown.getText().toString();

        if ("Weekly".equals(period)) {
            weeklyBarContainer.removeAllViews();
            updateNutritionSummaryForDay(null, PROTEIN_GOAL_DAILY, CARBS_GOAL_DAILY, FATS_GOAL_DAILY, SUGAR_GOAL_DAILY); // Clear summary
        } else {
            // Clear aggregate view and summary
            int currentCalorieGoal = period.equals("Monthly") ? MONTHLY_CALORIE_GOAL : DAILY_CALORIE_GOAL;
            int currentProteinGoal = PROTEIN_GOAL_DAILY * (period.equals("Monthly") ? 30 : 1);
            int currentCarbsGoal = CARBS_GOAL_DAILY * (period.equals("Monthly") ? 30 : 1);
            int currentFatsGoal = FATS_GOAL_DAILY * (period.equals("Monthly") ? 30 : 1);
            int currentSugarGoal = SUGAR_GOAL_DAILY * (period.equals("Monthly") ? 30 : 1);
            updateAggregateNutritionData(null, currentCalorieGoal, currentProteinGoal, currentCarbsGoal, currentFatsGoal, currentSugarGoal);
        }
    }
}