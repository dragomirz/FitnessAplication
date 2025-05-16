package com.fitness.fitnessapplication;
import java.util.Calendar;
import android.Manifest;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.fitness.fitnessapplication.DataModels.DailyLogResponse;
import com.fitness.fitnessapplication.DataModels.FoodLog;
import com.fitness.fitnessapplication.DataModels.User;
import com.fitness.fitnessapplication.RetroFit.ApiClient;
import com.fitness.fitnessapplication.RetroFit.ApiService;


import com.fitness.fitnessapplication.Adapters.FoodHistoryAdapter;


import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;


import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;


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
    private LinearProgressIndicator proteinProgress;
    private LinearProgressIndicator carbsProgress;
    private LinearProgressIndicator fatsProgress;
    private LinearProgressIndicator sugarProgress;
    private Button btnHome, btnScanner, btnUserInfo;


    private FrameLayout dailyMonthlyCalorieView;
    private LinearLayout weeklyBarContainer;
    private TextView nutritionSummaryTitle;


    private RecyclerView foodHistoryRecyclerView;
    private FoodHistoryAdapter foodHistoryAdapter;
    private TextView historyTitleTextView;
    private TextView emptyHistoryTextView;


    private String[] periods = {"Дневно", "Седмично"};
    private ActivityResultLauncher<ScanOptions> barcodeLauncher;
    private List<DailyLogResponse.DailyData> weeklyDataCache;
    private View currentlySelectedBarView = null;


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
        setupRecyclerView();
        setupPeriodSelector();
        setupBarcodeScanner();
        setupButtonListeners();

        getUserData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        String currentPeriod = periodDropdown.getText().toString();
        if (!currentPeriod.isEmpty()) {
            getUserData();
        } else {
            periodDropdown.setText(periods[0], false);
            getUserData();
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

        proteinProgress = findViewById(R.id.protein_progress);
        carbsProgress = findViewById(R.id.carbs_progress);
        fatsProgress = findViewById(R.id.fats_progress);
        sugarProgress = findViewById(R.id.sugar_progress);
        btnHome = findViewById(R.id.btn_home);
        btnScanner = findViewById(R.id.btn_scanner);
        btnUserInfo = findViewById(R.id.btn_user_info);

        dailyMonthlyCalorieView = findViewById(R.id.daily_monthly_calorie_view);
        weeklyBarContainer = findViewById(R.id.weekly_bar_container);
        nutritionSummaryTitle = findViewById(R.id.nutrition_summary_title);

        historyTitleTextView = findViewById(R.id.history_title);
        foodHistoryRecyclerView = findViewById(R.id.food_history_recycler_view);
        emptyHistoryTextView = findViewById(R.id.empty_history_text);

        updateUiForGoals(DAILY_CALORIE_GOAL, PROTEIN_GOAL_DAILY, CARBS_GOAL_DAILY, FATS_GOAL_DAILY, SUGAR_GOAL_DAILY);
    }

    private void setupRecyclerView() {
        foodHistoryRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        foodHistoryAdapter = new FoodHistoryAdapter(); // Create adapter instance
        foodHistoryRecyclerView.setAdapter(foodHistoryAdapter);
    }

    private void setupPeriodSelector() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, periods);
        periodDropdown.setAdapter(adapter);
        periodDropdown.setText(periods[0], false);

        periodDropdown.setOnItemClickListener((parent, view, position, id) -> {
            highlightBar(null);

            String selectedDisplayPeriod = periods[position];
            String internalKeyword;

            if (selectedDisplayPeriod.equals(periods[0])) {
                internalKeyword = "daily";
            } else if (selectedDisplayPeriod.equals(periods[1])) {
                internalKeyword = "weekly";

            } else {
                internalKeyword = "daily";
            }

            fetchLogs(internalKeyword);
        });
    }

    private void setupBarcodeScanner() {
        barcodeLauncher = registerForActivityResult(new ScanContract(), result -> {
            if (result.getContents() == null) {
                Toast.makeText(this, "Сканирането отказано", Toast.LENGTH_SHORT).show();
            } else {
                String barcode = result.getContents();
                try {
                    Intent intent = new Intent(HomeActivity.this, ProductDetailActivity.class);
                    intent.putExtra("barcode", barcode);
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(this, "Неуспешно отваряне на детайли за продукта.", Toast.LENGTH_SHORT).show();
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
        getUserData();
        Toast.makeText(this, "Опресняване на " + period + " данни", Toast.LENGTH_SHORT).show();
    }

    private void scanBarcode() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 1);
        } else {
            ScanOptions options = new ScanOptions();
            options.setOrientationLocked(false);
            options.setDesiredBarcodeFormats(ScanOptions.PRODUCT_CODE_TYPES);
            options.setPrompt("Сканирай баркод на продукт");
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
                Toast.makeText(this, "Необходимо е разрешение за камерата за сканиране на баркодове.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void getUserData() {
        ApiService apiService = ApiClient.getApiService(this);
        if (apiService == null) {
            Toast.makeText(HomeActivity.this, "Грешка в API услугата.", Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(HomeActivity.this, "Неуспешно извличане на потребителски данни: " + response.code(), Toast.LENGTH_LONG).show();
                    loadGoalsFromPrefsAndFetchLogs(); // Attempt fallback
                }
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                Toast.makeText(HomeActivity.this, "Мрежова грешка при извличане на потребителски данни.", Toast.LENGTH_SHORT).show();
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
        String currentDisplayPeriod = periodDropdown.getText().toString();
        String internalKeyword;
        if (currentDisplayPeriod.isEmpty() || !List.of(periods).contains(currentDisplayPeriod)) {
            currentDisplayPeriod = periods[0];
            periodDropdown.setText(currentDisplayPeriod, false);
            internalKeyword = "daily";
        } else {
            if (currentDisplayPeriod.equals(periods[0])) {
                internalKeyword = "daily";
            } else if (currentDisplayPeriod.equals(periods[1])) {
                internalKeyword = "weekly";

            } else {
                internalKeyword = "daily";
            }
        }

        fetchLogs(internalKeyword);
    }

    private void fetchLogs(String internalPeriodKeyword) {
        ApiService apiService = ApiClient.getApiService(this);
        if (apiService == null) {
            showError("Грешка в API услугата");
            return;
        }
        String currentDateStr = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        Call<DailyLogResponse> call;

        int summaryCalorieGoal = DAILY_CALORIE_GOAL;
        int summaryProteinGoal = PROTEIN_GOAL_DAILY;
        int summaryCarbsGoal = CARBS_GOAL_DAILY;
        int summaryFatsGoal = FATS_GOAL_DAILY;
        int summarySugarGoal = SUGAR_GOAL_DAILY;

        String dateForHistory = currentDateStr;
        boolean showHistory = false;

        // Конфигуриране на UI и цели въз основа на АНГЛИЙСКИЯ ключ
        if ("weekly".equals(internalPeriodKeyword)) { // Сравняваме с "weekly"
            dailyMonthlyCalorieView.setVisibility(View.GONE);
            weeklyBarContainer.setVisibility(View.VISIBLE);
            nutritionSummaryTitle.setText("Хранително резюме (Избери ден)");
            showHistory = false;
        } else {
            dailyMonthlyCalorieView.setVisibility(View.VISIBLE);
            weeklyBarContainer.setVisibility(View.GONE);
            nutritionSummaryTitle.setText("Хранително резюме");


            summaryCalorieGoal = DAILY_CALORIE_GOAL;
            showHistory = true;

            updateUiForGoals(summaryCalorieGoal, summaryProteinGoal, summaryCarbsGoal, summaryFatsGoal, summarySugarGoal);
        }

        setHistoryVisibility(showHistory);
        if (showHistory) {
            fetchFoodHistoryForDate(dateForHistory);
        }

        String rangeParam = internalPeriodKeyword.equalsIgnoreCase("daily") ? null : internalPeriodKeyword.toLowerCase(Locale.US);
        call = apiService.getDailyLogs(currentDateStr, rangeParam);

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
                    if ("weekly".equals(internalPeriodKeyword)) {
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
                                    setHistoryVisibility(true);
                                    fetchFoodHistoryForDate(lastDay.getDate());
                                } else {
                                    setHistoryVisibility(false);
                                }
                            } else {
                                setHistoryVisibility(false);
                            }
                        } else {
                            weeklyDataCache = new ArrayList<>();
                            weeklyBarContainer.removeAllViews();
                            showError("Няма налични седмични данни.");
                            updateNutritionSummaryForDay(null, PROTEIN_GOAL_DAILY, CARBS_GOAL_DAILY, FATS_GOAL_DAILY, SUGAR_GOAL_DAILY);
                            setHistoryVisibility(false);
                        }
                    } else {
                        if (data.getTotals() != null) {
                            updateAggregateNutritionData(data.getTotals(), finalSummaryCalorieGoal, finalSummaryProteinGoal, finalSummaryCarbsGoal, finalSummaryFatsGoal, finalSummarySugarGoal);

                        } else {
                            String displayPeriod = periodDropdown.getText().toString();
                            showError("Няма налични общи данни за " + displayPeriod);
                            updateAggregateNutritionData(null, finalSummaryCalorieGoal, finalSummaryProteinGoal, finalSummaryCarbsGoal, finalSummaryFatsGoal, finalSummarySugarGoal);
                        }
                    }
                } else if (response.code() == 401 || response.code() == 403) {
                    handleAuthFailure();
                } else {
                    String errorMsg = "Грешка при извличане на данни (" + response.code() + ")";
                    try { if (response.errorBody() != null) errorMsg += ": " + response.errorBody().string(); } catch (Exception e) { /* Ignore */ }
                    showError(errorMsg);
                    clearViewsOnError(internalPeriodKeyword, finalSummaryCalorieGoal, finalSummaryProteinGoal, finalSummaryCarbsGoal, finalSummaryFatsGoal, finalSummarySugarGoal);
                }
            }

            @Override
            public void onFailure(Call<DailyLogResponse> call, Throwable t) {
                showError("Мрежова грешка: " + t.getMessage());
                System.err.println("Network Error fetchLogs: " + t.toString());
                clearViewsOnError(internalPeriodKeyword, finalSummaryCalorieGoal, finalSummaryProteinGoal, finalSummaryCarbsGoal, finalSummaryFatsGoal, finalSummarySugarGoal);
            }
        });
    }
    private void updateAggregateNutritionData(DailyLogResponse.Totals totals, int calorieGoal, int proteinGoal,
                                              int carbsGoal, int fatsGoal, int sugarGoal) {
        if (totals != null) {
            int calories = Math.round(totals.getCalories());
            caloriesValue.setText(String.valueOf(calories));
            updateNutritionSummary(totals, proteinGoal, carbsGoal, fatsGoal, sugarGoal);
        } else {
            caloriesValue.setText("0");
            updateNutritionSummary(null, proteinGoal, carbsGoal, fatsGoal, sugarGoal);
        }
    }

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

    private void populateWeeklyBars(List<DailyLogResponse.DailyData> dailyDataList) {
        weeklyBarContainer.removeAllViews();
        currentlySelectedBarView = null;
        if (dailyDataList == null || dailyDataList.isEmpty()) return;

        LayoutInflater inflater = LayoutInflater.from(this);
        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);


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

                    Calendar cal = Calendar.getInstance();
                    cal.setTime(date);
                    int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);

                    String dayLetter;

                    switch (dayOfWeek) {
                        case Calendar.MONDAY:    dayLetter = "П"; break;
                        case Calendar.TUESDAY:   dayLetter = "В"; break;
                        case Calendar.WEDNESDAY: dayLetter = "С"; break;
                        case Calendar.THURSDAY:  dayLetter = "Ч"; break;
                        case Calendar.FRIDAY:    dayLetter = "П"; break;
                        case Calendar.SATURDAY:  dayLetter = "С"; break;
                        case Calendar.SUNDAY:    dayLetter = "Н"; break;
                        default:                 dayLetter = "?"; break;
                    }
                    dayLabel.setText(dayLetter);

                } else {
                    dayLabel.setText("?");
                }
            } catch (Exception e) {
                dayLabel.setText("?");
                System.err.println("Error parsing date or getting day letter: " + e.getMessage());
            }


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
                        setHistoryVisibility(true);
                        fetchFoodHistoryForDate(selectedDay.getDate());
                    }
                }
            });
            weeklyBarContainer.addView(barItemView);
        }
        weeklyBarContainer.requestLayout();
    }


    private void updateNutritionSummaryForDay(DailyLogResponse.DailyData dayData, int proteinGoal, int carbsGoal, int fatsGoal, int sugarGoal) {
        if (dayData != null && dayData.getDate() != null) {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            SimpleDateFormat outputFormat = new SimpleDateFormat("EEE, MMM d", Locale.US);
            try {
                Date date = inputFormat.parse(dayData.getDate());
                nutritionSummaryTitle.setText("Хранително резюме");
            } catch (Exception e) {
                nutritionSummaryTitle.setText("Хранително резюме (Избран ден)");
            }
            updateNutritionSummary(dayData.getTotals(), proteinGoal, carbsGoal, fatsGoal, sugarGoal);
        } else {
            nutritionSummaryTitle.setText("Хранително резюме");
            updateNutritionSummary(null, proteinGoal, carbsGoal, fatsGoal, sugarGoal);
        }
    }

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

    private void fetchFoodHistoryForDate(String dateString) {
        if (dateString == null || dateString.isEmpty()) {
            updateHistoryList(Collections.emptyList(), "Грешка: Невалидна дата");
            return;
        }

        ApiService apiService = ApiClient.getApiService(this);
        if (apiService == null) {
            updateHistoryList(Collections.emptyList(), "Грешка: API услугата е недостъпна");
            return;
        }


        emptyHistoryTextView.setText("Зареждане на история...");
        emptyHistoryTextView.setVisibility(View.VISIBLE);
        foodHistoryRecyclerView.setVisibility(View.GONE);


        Call<List<FoodLog>> historyCall = apiService.getLogsForDate(dateString);
        historyCall.enqueue(new Callback<List<FoodLog>>() {
            @Override
            public void onResponse(Call<List<FoodLog>> call, Response<List<FoodLog>> response) {
                if (response.isSuccessful()) {
                    List<FoodLog> logs = response.body();
                    String title = "Записани храни";
                    try {
                        SimpleDateFormat inputFmt = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                        SimpleDateFormat titleFmt = new SimpleDateFormat("EEE, MMM d", Locale.US);
                        Date parsedDate = inputFmt.parse(dateString);
                        title = "Записани храни (" + titleFmt.format(parsedDate) + ")";
                        String todayStr = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
                        if (dateString.equals(todayStr)) {
                            title = "Записани храни днес";
                        }
                    } catch (Exception e) { /* Keep default title */ }
                    updateHistoryList(logs, title);
                } else {
                    updateHistoryList(Collections.emptyList(), "Грешка при зареждане на история (" + response.code() + ")");
                }
            }

            @Override
            public void onFailure(Call<List<FoodLog>> call, Throwable t) {
                System.err.println("Network error fetching history: " + t.getMessage());
                updateHistoryList(Collections.emptyList(), "Мрежова грешка");
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
            if (title.toLowerCase().contains("грешка") || title.equalsIgnoreCase("мрежова грешка")) {
                emptyHistoryTextView.setText("Неуспешно зареждане на историята.");
            } else {
                emptyHistoryTextView.setText("Няма записани храни за този ден.");
            }
        }
    }

    private void setHistoryVisibility(boolean isVisible) {
        int visibility = isVisible ? View.VISIBLE : View.GONE;
        historyTitleTextView.setVisibility(visibility);
        if (!isVisible) {
            foodHistoryRecyclerView.setVisibility(View.GONE);
            emptyHistoryTextView.setVisibility(View.GONE);
            if (foodHistoryAdapter != null) {
                foodHistoryAdapter.updateData(Collections.emptyList());
            }
        } else {

        }
    }

    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }

    private void updateUiForGoals(int calorieGoal, int proteinGoal, int carbsGoal, int fatsGoal, int sugarGoal) {
        calorieGoalText.setText("от " + calorieGoal + " kcal");
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
        Toast.makeText(this, "Сесията изтече. Моля, влезте отново.", Toast.LENGTH_LONG).show();
        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        sharedPreferences.edit().clear().apply();
        Intent intent = new Intent(HomeActivity.this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }


    private void clearViewsOnError(String internalPeriodKeyword, int calorieGoal, int proteinGoal, int carbsGoal, int fatsGoal, int sugarGoal){

        if ("weekly".equals(internalPeriodKeyword)) {
            weeklyDataCache = new ArrayList<>();
            weeklyBarContainer.removeAllViews();
            updateNutritionSummaryForDay(null, PROTEIN_GOAL_DAILY, CARBS_GOAL_DAILY, FATS_GOAL_DAILY, SUGAR_GOAL_DAILY);
            setHistoryVisibility(false);
        } else {
            updateAggregateNutritionData(null, calorieGoal, proteinGoal, carbsGoal, fatsGoal, sugarGoal);

            if("daily".equals(internalPeriodKeyword)){
                setHistoryVisibility(true);
                updateHistoryList(Collections.emptyList(), "Грешка при зареждане на данни");
            } else {
                setHistoryVisibility(false);
            }
        }
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        System.err.println("HomeActivity Error: " + message);
        String displayPeriod = periodDropdown.getText().toString();
        String internalKeyword;

        if (displayPeriod.equals(periods[0])) {
            internalKeyword = "daily";
        } else if (displayPeriod.equals(periods[1])) {
            internalKeyword = "weekly";
        } else {

            internalKeyword = "daily";
            if (displayPeriod.isEmpty()) {
                periodDropdown.setText(periods[0], false);
            }
        }
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        System.err.println("HomeActivity Error: " + message);
        String period = periodDropdown.getText().toString();
        int calorieGoal = period.equals("Monthly") ? MONTHLY_CALORIE_GOAL : DAILY_CALORIE_GOAL;
        int proteinGoal = PROTEIN_GOAL_DAILY * (period.equals("Monthly") ? 30 : 1);
        int carbsGoal = CARBS_GOAL_DAILY * (period.equals("Monthly") ? 30 : 1);
        int fatsGoal = FATS_GOAL_DAILY * (period.equals("Monthly") ? 30 : 1);
        int sugarGoal = SUGAR_GOAL_DAILY * (period.equals("Monthly") ? 30 : 1);
        clearViewsOnError(internalKeyword, calorieGoal, proteinGoal, carbsGoal, fatsGoal, sugarGoal);
    }
}