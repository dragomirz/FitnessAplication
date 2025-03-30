package com.fitness.fitnessapplication; // Use your actual package name

import android.Manifest;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager; // Added
import androidx.recyclerview.widget.RecyclerView; // Added

// Data Models (ensure correct package)
import com.fitness.fitnessapplication.DataModels.DailyLogResponse;
import com.fitness.fitnessapplication.DataModels.FoodLog; // Added
import com.fitness.fitnessapplication.DataModels.User;

// Retrofit (ensure correct package)
import com.fitness.fitnessapplication.RetroFit.ApiClient;
import com.fitness.fitnessapplication.RetroFit.ApiService;

// Adapter (ensure correct package)
import com.fitness.fitnessapplication.Adapters.FoodHistoryAdapter; // Added

// Other components
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

// Java utils
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections; // Added
import java.util.Date;
import java.util.List;
import java.util.Locale;

// Retrofit
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
    private Button btnHome, btnScanner, btnUserInfo;

    // New/Modified Views
    private FrameLayout dailyMonthlyCalorieView;
    private LinearLayout weeklyBarContainer;
    private TextView nutritionSummaryTitle;

    // History Section Views
    private RecyclerView foodHistoryRecyclerView;
    private FoodHistoryAdapter foodHistoryAdapter;
    private TextView historyTitleTextView;
    private TextView emptyHistoryTextView;

    // Other Variables
    private String[] periods = {"Daily", "Weekly", "Monthly"};
    private ActivityResultLauncher<ScanOptions> barcodeLauncher;
    private List<DailyLogResponse.DailyData> weeklyDataCache;
    private View currentlySelectedBarView = null;

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
        setupRecyclerView(); // Setup history RecyclerView
        setupPeriodSelector();
        setupBarcodeScanner();
        setupButtonListeners();

        getUserData(); // Fetches user data -> updates goals -> fetches logs & potentially history
    }

    @Override
    protected void onResume() {
        super.onResume();
        String currentPeriod = periodDropdown.getText().toString();
        if (!currentPeriod.isEmpty()) {
            getUserData(); // Re-fetch goals and then data
        } else {
            periodDropdown.setText(periods[0], false);
            getUserData();
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

        // Initialize History Views
        historyTitleTextView = findViewById(R.id.history_title);
        foodHistoryRecyclerView = findViewById(R.id.food_history_recycler_view);
        emptyHistoryTextView = findViewById(R.id.empty_history_text);

        // Initial UI Goal Setup
        updateUiForGoals(DAILY_CALORIE_GOAL, PROTEIN_GOAL_DAILY, CARBS_GOAL_DAILY, FATS_GOAL_DAILY, SUGAR_GOAL_DAILY);
    }

    // Setup History RecyclerView
    private void setupRecyclerView() {
        foodHistoryRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        foodHistoryAdapter = new FoodHistoryAdapter(); // Create adapter instance
        foodHistoryRecyclerView.setAdapter(foodHistoryAdapter);
    }

    private void setupPeriodSelector() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, periods);
        periodDropdown.setAdapter(adapter);
        periodDropdown.setText(periods[0], false); // Set initial value

        periodDropdown.setOnItemClickListener((parent, view, position, id) -> {
            highlightBar(null); // Clear weekly selection
            fetchLogs(periods[position]); // Fetch data for newly selected period
        });
    }

    private void setupBarcodeScanner() {
        barcodeLauncher = registerForActivityResult(new ScanContract(), result -> {
            if (result.getContents() == null) {
                Toast.makeText(this, "Scan cancelled", Toast.LENGTH_SHORT).show();
            } else {
                String barcode = result.getContents();
                // Ensure ProductDetailActivity exists and can handle the intent
                try {
                    Intent intent = new Intent(HomeActivity.this, ProductDetailActivity.class);
                    intent.putExtra("barcode", barcode);
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(this, "Could not open product details.", Toast.LENGTH_SHORT).show();
                    System.err.println("Error launching ProductDetailActivity: " + e.getMessage());
                }
            }
        });
    }

    private void setupButtonListeners() {
        btnHome.setOnClickListener(v -> refreshData());
        btnScanner.setOnClickListener(v -> scanBarcode());
        btnUserInfo.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(HomeActivity.this, UserInfoActivity.class);
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(this, "Could not open user info.", Toast.LENGTH_SHORT).show();
                System.err.println("Error launching UserInfoActivity: " + e.getMessage());
            }
        });
    }

    private void refreshData() {
        String period = periodDropdown.getText().toString();
        // Re-fetch user data to ensure goals are up-to-date, then fetch logs
        getUserData();
        Toast.makeText(this, "Refreshing " + period + " data", Toast.LENGTH_SHORT).show();
    }

    private void scanBarcode() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 1);
        } else {
            ScanOptions options = new ScanOptions();
            options.setOrientationLocked(false);
            options.setDesiredBarcodeFormats(ScanOptions.PRODUCT_CODE_TYPES);
            options.setPrompt("Scan a product barcode");
            options.setCameraId(0);
            options.setBeepEnabled(true);
            options.setBarcodeImageEnabled(false);
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
            Toast.makeText(HomeActivity.this, "API Service Error.", Toast.LENGTH_SHORT).show();
            loadGoalsFromPrefsAndFetchLogs(); // Attempt fallback
            return;
        }

        Call<User> userDataCall = apiService.getUserData();
        userDataCall.enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (response.isSuccessful() && response.body() != null) {
                    User userData = response.body();
                    SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString("name", userData.getName());
                    editor.putInt("age", userData.getAge());
                    editor.putString("weight", userData.getWeight());
                    editor.putString("height", userData.getHeight());
                    editor.putString("gender", userData.getGender());
                    // Ensure getCalories() exists and returns a number
                    try {
                        editor.putFloat("calorie_goal", (float) userData.getCalories());
                    } catch (Exception e) {
                        editor.putFloat("calorie_goal", 2000f); // Default if parsing fails
                        System.err.println("Error reading calorie goal from user data: " + e.getMessage());
                    }
                    editor.apply();
                    updateCalorieGoal(userData.getCalories());
                } else if (response.code() == 401 || response.code() == 403) {
                    handleAuthFailure();
                } else {
                    Toast.makeText(HomeActivity.this, "Failed to get user data: " + response.code(), Toast.LENGTH_LONG).show();
                    loadGoalsFromPrefsAndFetchLogs(); // Attempt fallback
                }
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                Toast.makeText(HomeActivity.this, "Network error getting user data.", Toast.LENGTH_SHORT).show();
                System.err.println("Network error getUserData: " + t.getMessage());
                loadGoalsFromPrefsAndFetchLogs(); // Attempt fallback
            }
        });
    }

    private void loadGoalsFromPrefsAndFetchLogs() {
        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        float savedGoal = sharedPreferences.getFloat("calorie_goal", 2000f);
        updateCalorieGoal(savedGoal);
    }

    private void updateCalorieGoal(double calorieGoal) {
        DAILY_CALORIE_GOAL = (int) Math.round(calorieGoal);
        WEEKLY_CALORIE_GOAL = DAILY_CALORIE_GOAL * 7;
        MONTHLY_CALORIE_GOAL = DAILY_CALORIE_GOAL * 30;

        String currentPeriod = periodDropdown.getText().toString();
        if (currentPeriod.isEmpty()) {
            currentPeriod = periods[0];
            periodDropdown.setText(currentPeriod, false);
        }
        fetchLogs(currentPeriod); // Fetch logs *after* goals are set
    }

    private void fetchLogs(String period) {
        ApiService apiService = ApiClient.getApiService(this);
        if (apiService == null) {
            showError("API Service Error");
            return;
        }
        String currentDateStr = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        Call<DailyLogResponse> call;

        int summaryCalorieGoal = DAILY_CALORIE_GOAL;
        int summaryProteinGoal = PROTEIN_GOAL_DAILY;
        int summaryCarbsGoal = CARBS_GOAL_DAILY;
        int summaryFatsGoal = FATS_GOAL_DAILY;
        int summarySugarGoal = SUGAR_GOAL_DAILY;

        String dateForHistory = currentDateStr; // Today's date by default
        boolean showHistory = false; // Flag to control history section visibility

        // Configure UI based on period
        if ("Weekly".equals(period)) {
            dailyMonthlyCalorieView.setVisibility(View.GONE);
            weeklyBarContainer.setVisibility(View.VISIBLE);
            nutritionSummaryTitle.setText("Nutrition Summary (Select a Day)");
            showHistory = false; // History shown on bar click
        } else {
            dailyMonthlyCalorieView.setVisibility(View.VISIBLE);
            weeklyBarContainer.setVisibility(View.GONE);
            nutritionSummaryTitle.setText("Nutrition Summary");

            if ("Monthly".equals(period)) {
                summaryCalorieGoal = MONTHLY_CALORIE_GOAL;
                summaryProteinGoal *= 30;
                summaryCarbsGoal *= 30;
                summaryFatsGoal *= 30;
                summarySugarGoal *= 30;
                showHistory = false; // No history for monthly
            } else { // Daily
                summaryCalorieGoal = DAILY_CALORIE_GOAL;
                showHistory = true; // Show history for daily
            }
            updateUiForGoals(summaryCalorieGoal, summaryProteinGoal, summaryCarbsGoal, summaryFatsGoal, summarySugarGoal);
        }

        // Set history visibility *before* making the call
        setHistoryVisibility(showHistory);
        if (showHistory) {
            fetchFoodHistoryForDate(dateForHistory); // Fetch history if needed (i.e., for Daily view)
        }


        // Prepare and make the API call for aggregate/weekly data
        String rangeParam = period.equalsIgnoreCase("Daily") ? null : period.toLowerCase(Locale.US);
        call = apiService.getDailyLogs(currentDateStr, rangeParam); // Always use today's date for this call context

        // Store final goals for use inside callback
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
                            // Show last day's summary & fetch its history initially
                            if (!weeklyDataCache.isEmpty()) {
                                DailyLogResponse.DailyData lastDay = weeklyDataCache.get(weeklyDataCache.size() - 1);
                                updateNutritionSummaryForDay(lastDay, PROTEIN_GOAL_DAILY, CARBS_GOAL_DAILY, FATS_GOAL_DAILY, SUGAR_GOAL_DAILY);
                                if (weeklyBarContainer.getChildCount() > 0) {
                                    View lastBar = weeklyBarContainer.getChildAt(weeklyBarContainer.getChildCount() - 1);
                                    if(lastBar != null) highlightBar(lastBar.findViewById(R.id.bar_view));
                                }
                                // Fetch history for the initially displayed day
                                if (lastDay != null && lastDay.getDate() != null) {
                                    setHistoryVisibility(true); // Show history section now
                                    fetchFoodHistoryForDate(lastDay.getDate());
                                } else {
                                    setHistoryVisibility(false); // Hide if date is bad
                                }
                            } else {
                                setHistoryVisibility(false); // Hide if no data
                            }
                        } else {
                            weeklyDataCache = new ArrayList<>();
                            weeklyBarContainer.removeAllViews();
                            showError("No weekly data available.");
                            updateNutritionSummaryForDay(null, PROTEIN_GOAL_DAILY, CARBS_GOAL_DAILY, FATS_GOAL_DAILY, SUGAR_GOAL_DAILY);
                            setHistoryVisibility(false); // Hide history section
                        }
                    } else { // Daily or Monthly Aggregate View
                        if (data.getTotals() != null) {
                            updateAggregateNutritionData(data.getTotals(), finalSummaryCalorieGoal, finalSummaryProteinGoal, finalSummaryCarbsGoal, finalSummaryFatsGoal, finalSummarySugarGoal);
                            // History fetch for Daily was already triggered before the call
                        } else {
                            showError("No totals data available for " + period);
                            updateAggregateNutritionData(null, finalSummaryCalorieGoal, finalSummaryProteinGoal, finalSummaryCarbsGoal, finalSummaryFatsGoal, finalSummarySugarGoal);
                        }
                    }
                } else if (response.code() == 401 || response.code() == 403) {
                    handleAuthFailure();
                } else {
                    String errorMsg = "Error fetching data (" + response.code() + ")";
                    try { if (response.errorBody() != null) errorMsg += ": " + response.errorBody().string(); } catch (Exception e) { /* Ignore */ }
                    showError(errorMsg);
                    clearViewsOnError(period, finalSummaryCalorieGoal, finalSummaryProteinGoal, finalSummaryCarbsGoal, finalSummaryFatsGoal, finalSummarySugarGoal);
                }
            }

            @Override
            public void onFailure(Call<DailyLogResponse> call, Throwable t) {
                showError("Network error: " + t.getMessage());
                System.err.println("Network Error fetchLogs: " + t.toString());
                clearViewsOnError(period, finalSummaryCalorieGoal, finalSummaryProteinGoal, finalSummaryCarbsGoal, finalSummaryFatsGoal, finalSummarySugarGoal);
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
            updateNutritionSummary(totals, proteinGoal, carbsGoal, fatsGoal, sugarGoal);
        } else {
            caloriesValue.setText("0");
            animateProgress(caloriesProgress, 0);
            updateNutritionSummary(null, proteinGoal, carbsGoal, fatsGoal, sugarGoal);
        }
    }

    // Reusable method to update ONLY the Nutrition Summary Card's linear bars and text
    private void updateNutritionSummary(DailyLogResponse.Totals totals, int proteinGoal, int carbsGoal, int fatsGoal, int sugarGoal) {
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

            int sugars = Math.round(totals.getSugars());
            sugarValue.setText(sugars + "g");
            animateProgress(sugarProgress, sugars);
        } else {
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
        weeklyBarContainer.removeAllViews();
        currentlySelectedBarView = null;
        if (dailyDataList == null || dailyDataList.isEmpty()) return;

        LayoutInflater inflater = LayoutInflater.from(this);
        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        SimpleDateFormat outputFormat = new SimpleDateFormat("EEE", Locale.US);

        double maxCalories = 0;
        for (DailyLogResponse.DailyData dayData : dailyDataList) {
            if (dayData != null && dayData.getTotals() != null && dayData.getTotals().getCalories() > maxCalories) {
                maxCalories = dayData.getTotals().getCalories();
            }
        }
        if (maxCalories <= 0) maxCalories = Math.max(1.0, DAILY_CALORIE_GOAL);

        int maxBarHeightDp = 140;
        int maxBarHeightPx = dpToPx(maxBarHeightDp);
        int minBarHeightPx = dpToPx(2);

        for (int i = 0; i < dailyDataList.size(); i++) {
            final DailyLogResponse.DailyData dayData = dailyDataList.get(i);
            if (dayData == null) continue;

            View barItemView = inflater.inflate(R.layout.item_weekly_bar, weeklyBarContainer, false);
            View barView = barItemView.findViewById(R.id.bar_view);
            TextView dayLabel = barItemView.findViewById(R.id.day_label_text);
            TextView calorieLabel = barItemView.findViewById(R.id.calorie_text);

            try {
                if (dayData.getDate() != null) {
                    Date date = inputFormat.parse(dayData.getDate());
                    dayLabel.setText(outputFormat.format(date).substring(0, 1));
                } else { dayLabel.setText("?"); }
            } catch (Exception e) { dayLabel.setText("?"); System.err.println("Error parsing date for label: " + e.getMessage()); }

            double currentCalories = (dayData.getTotals() != null) ? dayData.getTotals().getCalories() : 0;
            int barHeight = (currentCalories > 0) ? Math.max(minBarHeightPx, (int) ((currentCalories / maxCalories) * maxBarHeightPx)) : 0;
            ViewGroup.LayoutParams params = barView.getLayoutParams(); params.height = barHeight; barView.setLayoutParams(params);
            if (currentCalories > 0) { calorieLabel.setText(String.valueOf(Math.round(currentCalories))); calorieLabel.setVisibility(View.VISIBLE); } else { calorieLabel.setVisibility(View.INVISIBLE); }

            final int index = i;
            barItemView.setOnClickListener(v -> {
                if (weeklyDataCache != null && index < weeklyDataCache.size()) {
                    DailyLogResponse.DailyData selectedDay = weeklyDataCache.get(index);
                    if (selectedDay != null && selectedDay.getDate() != null) {
                        updateNutritionSummaryForDay(selectedDay, PROTEIN_GOAL_DAILY, CARBS_GOAL_DAILY, FATS_GOAL_DAILY, SUGAR_GOAL_DAILY);
                        highlightBar(barView);
                        setHistoryVisibility(true); // Show history section
                        fetchFoodHistoryForDate(selectedDay.getDate()); // Fetch history for clicked day
                    }
                }
            });
            weeklyBarContainer.addView(barItemView);
        }
        weeklyBarContainer.requestLayout();
    }

    // Updates the Nutrition Summary Card based on selected day's data
    private void updateNutritionSummaryForDay(DailyLogResponse.DailyData dayData, int proteinGoal, int carbsGoal, int fatsGoal, int sugarGoal) {
        if (dayData != null && dayData.getDate() != null) {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            SimpleDateFormat outputFormat = new SimpleDateFormat("EEE, MMM d", Locale.US);
            try {
                Date date = inputFormat.parse(dayData.getDate());
                nutritionSummaryTitle.setText("Nutrition Summary (" + outputFormat.format(date) + ")");
            } catch (Exception e) {
                nutritionSummaryTitle.setText("Nutrition Summary (Selected Day)");
            }
            updateNutritionSummary(dayData.getTotals(), proteinGoal, carbsGoal, fatsGoal, sugarGoal);
        } else {
            nutritionSummaryTitle.setText("Nutrition Summary");
            updateNutritionSummary(null, proteinGoal, carbsGoal, fatsGoal, sugarGoal);
        }
    }

    // Helper to highlight the selected bar
    private void highlightBar(View selectedBar) {
        if (currentlySelectedBarView != null) {
            currentlySelectedBarView.setBackgroundColor(ContextCompat.getColor(this, R.color.colorPrimary));
        }
        if (selectedBar != null) {
            int selectedColor = ContextCompat.getColor(this, R.color.colorSecondary);
            if (selectedColor == 0) selectedColor = ContextCompat.getColor(this, android.R.color.holo_blue_light); // Fallback
            selectedBar.setBackgroundColor(selectedColor);
            currentlySelectedBarView = selectedBar;
        } else {
            currentlySelectedBarView = null;
        }
    }

    // --- History Fetching and Display ---
    private void fetchFoodHistoryForDate(String dateString) {
        if (dateString == null || dateString.isEmpty()) {
            updateHistoryList(Collections.emptyList(), "Error: Invalid date");
            return;
        }

        ApiService apiService = ApiClient.getApiService(this);
        if (apiService == null) {
            updateHistoryList(Collections.emptyList(), "Error: API Service unavailable");
            return;
        }

        // Optional: Show loading indicator for history
        emptyHistoryTextView.setText("Loading history...");
        emptyHistoryTextView.setVisibility(View.VISIBLE);
        foodHistoryRecyclerView.setVisibility(View.GONE);


        Call<List<FoodLog>> historyCall = apiService.getLogsForDate(dateString);
        historyCall.enqueue(new Callback<List<FoodLog>>() {
            @Override
            public void onResponse(Call<List<FoodLog>> call, Response<List<FoodLog>> response) {
                if (response.isSuccessful()) {
                    List<FoodLog> logs = response.body();
                    String title = "Logged Foods";
                    try {
                        SimpleDateFormat inputFmt = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                        SimpleDateFormat titleFmt = new SimpleDateFormat("EEE, MMM d", Locale.US);
                        Date parsedDate = inputFmt.parse(dateString);
                        title = "Logged Foods (" + titleFmt.format(parsedDate) + ")";
                        String todayStr = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
                        if (dateString.equals(todayStr)) {
                            title = "Logged Foods Today";
                        }
                    } catch (Exception e) { /* Keep default title */ }
                    updateHistoryList(logs, title);
                } else {
                    updateHistoryList(Collections.emptyList(), "Error loading history (" + response.code() + ")");
                }
            }

            @Override
            public void onFailure(Call<List<FoodLog>> call, Throwable t) {
                System.err.println("Network error fetching history: " + t.getMessage());
                updateHistoryList(Collections.emptyList(), "Network Error");
            }
        });
    }

    private void updateHistoryList(List<FoodLog> logs, String title) {
        historyTitleTextView.setText(title);
        if (logs != null && !logs.isEmpty()) {
            foodHistoryAdapter.updateData(logs);
            foodHistoryRecyclerView.setVisibility(View.VISIBLE);
            emptyHistoryTextView.setVisibility(View.GONE);
        } else {
            foodHistoryAdapter.updateData(Collections.emptyList());
            foodHistoryRecyclerView.setVisibility(View.GONE);
            emptyHistoryTextView.setVisibility(View.VISIBLE);
            if (title.toLowerCase().contains("error") || title.equalsIgnoreCase("Network Error")) {
                emptyHistoryTextView.setText("Could not load history.");
            } else {
                emptyHistoryTextView.setText("No foods logged for this day.");
            }
        }
    }

    private void setHistoryVisibility(boolean isVisible) {
        int visibility = isVisible ? View.VISIBLE : View.GONE;
        historyTitleTextView.setVisibility(visibility); // Title always matches section visibility

        // RecyclerView and Empty text visibility is handled by updateHistoryList OR when hiding
        if (!isVisible) {
            foodHistoryRecyclerView.setVisibility(View.GONE);
            emptyHistoryTextView.setVisibility(View.GONE);
            if (foodHistoryAdapter != null) { // Clear adapter data when hiding
                foodHistoryAdapter.updateData(Collections.emptyList());
            }
        } else {
            // If becoming visible, updateHistoryList will handle showing list or empty text
        }
    }

    // --- Utility and Helper Methods ---
    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }

    private void updateUiForGoals(int calorieGoal, int proteinGoal, int carbsGoal, int fatsGoal, int sugarGoal) {
        caloriesProgress.setMax(Math.max(1, calorieGoal));
        calorieGoalText.setText("of " + calorieGoal + " kcal");
        // Linear bar max values are set in updateNutritionSummary
    }

    private void animateProgress(CircularProgressIndicator indicator, int value) {
        int max = indicator.getMax();
        int progressValue = Math.max(0, Math.min(value, max));
        ObjectAnimator.ofInt(indicator, "progress", indicator.getProgress(), progressValue).setDuration(800).start();
    }

    private void animateProgress(LinearProgressIndicator indicator, int value) {
        int max = indicator.getMax();
        int progressValue = Math.max(0, Math.min(value, max));
        ObjectAnimator.ofInt(indicator, "progress", indicator.getProgress(), progressValue).setDuration(800).start();
    }

    private void handleAuthFailure() {
        Toast.makeText(this, "Session expired. Please log in again.", Toast.LENGTH_LONG).show();
        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        sharedPreferences.edit().clear().apply(); // Clear all prefs on auth failure
        Intent intent = new Intent(HomeActivity.this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void clearViewsOnError(String period, int calorieGoal, int proteinGoal, int carbsGoal, int fatsGoal, int sugarGoal){
        if ("Weekly".equals(period)) {
            weeklyDataCache = new ArrayList<>();
            weeklyBarContainer.removeAllViews();
            updateNutritionSummaryForDay(null, PROTEIN_GOAL_DAILY, CARBS_GOAL_DAILY, FATS_GOAL_DAILY, SUGAR_GOAL_DAILY);
            setHistoryVisibility(false); // Hide history on error
        } else {
            updateAggregateNutritionData(null, calorieGoal, proteinGoal, carbsGoal, fatsGoal, sugarGoal);
            if("Daily".equals(period)){
                setHistoryVisibility(true); // Keep section visible but show error
                updateHistoryList(Collections.emptyList(), "Error loading data");
            } else { // Monthly
                setHistoryVisibility(false);
            }
        }
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        System.err.println("HomeActivity Error: " + message);
        String period = periodDropdown.getText().toString();
        int calorieGoal = period.equals("Monthly") ? MONTHLY_CALORIE_GOAL : DAILY_CALORIE_GOAL;
        int proteinGoal = PROTEIN_GOAL_DAILY * (period.equals("Monthly") ? 30 : 1);
        int carbsGoal = CARBS_GOAL_DAILY * (period.equals("Monthly") ? 30 : 1);
        int fatsGoal = FATS_GOAL_DAILY * (period.equals("Monthly") ? 30 : 1);
        int sugarGoal = SUGAR_GOAL_DAILY * (period.equals("Monthly") ? 30 : 1);
        clearViewsOnError(period, calorieGoal, proteinGoal, carbsGoal, fatsGoal, sugarGoal);
    }
}