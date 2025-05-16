package com.fitness.fitnessapplication; // Use your actual package name

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log; // Import Log
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar; // Import ProgressBar
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.fitness.fitnessapplication.DataModels.UpdateResponse; // Нов модел за отговор от update
import com.fitness.fitnessapplication.DataModels.User; // Може да е нужен за тип
import com.fitness.fitnessapplication.RetroFit.ApiClient;
import com.fitness.fitnessapplication.RetroFit.ApiService;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout; // Import TextInputLayout

import java.util.HashMap; // Import HashMap
import java.util.Map; // Import Map
import java.util.Objects; // Import Objects

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UserInfoActivity extends AppCompatActivity {

    // Profile Views
    private TextView nameView, genderView; // Оставяме ги TextView
    // Променяме на полета за редакция
    private TextInputLayout weightInputLayoutProfile, heightInputLayoutProfile, ageInputLayoutProfile;
    private TextInputEditText weightInputProfile, heightInputProfile, ageInputProfile;
    private Button saveProfileButton; // Нов бутон

    // Calorie Goal Views
    private TextView currentCalorieGoalView;
    private TextInputEditText calorieGoalInput;
    private TextInputLayout calorieGoalInputLayout; // Добавяме Layout за грешки
    private Button saveCalorieGoalButton;

    // Action Buttons
    private Button logoutButton;
    private Button btnHome, btnScanner, btnUserInfo; // Bottom Nav Buttons

    // Other
    private SharedPreferences sharedPreferences;
    private ProgressBar loadingSpinnerProfile, loadingSpinnerGoal; // Отделни спинери? Или един общ?

    // Константи за валидация (копирани от RegisterActivity)
    private static final float MIN_HEIGHT = 50f;
    private static final float MAX_HEIGHT = 280f;
    private static final float MIN_WEIGHT = 20f;
    private static final float MAX_WEIGHT = 500f;
    private static final int MIN_AGE = 5;
    private static final int MAX_AGE = 120;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        setContentView(R.layout.activity_user_info);

        sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);

        initializeViews();
        loadProfileData();
        setupActionListeners();
        setupBottomNavigation();
    }

    private void initializeViews() {
        // Profile Stats (TextViews за нередактируеми)
        nameView = findViewById(R.id.name_view);
        genderView = findViewById(R.id.gender_view);

        // Profile Stats (EditTexts за редактируеми)
        weightInputLayoutProfile = findViewById(R.id.weight_input_layout_profile);
        weightInputProfile = findViewById(R.id.weight_input_profile);
        heightInputLayoutProfile = findViewById(R.id.height_input_layout_profile);
        heightInputProfile = findViewById(R.id.height_input_profile);
        ageInputLayoutProfile = findViewById(R.id.age_input_layout_profile);
        ageInputProfile = findViewById(R.id.age_input_profile);
        saveProfileButton = findViewById(R.id.save_profile_button); // Намираме новия бутон

        // Calorie Goal


        // Logout Button
        logoutButton = findViewById(R.id.logout_button);

        // Bottom Navigation Buttons
        btnHome = findViewById(R.id.btn_home);
        btnScanner = findViewById(R.id.btn_scanner);
        btnUserInfo = findViewById(R.id.btn_user_info);
    }

    private void setupActionListeners() {
        // Listener за запазване на ПРОФИЛА
        if (saveProfileButton != null) {
            saveProfileButton.setOnClickListener(v -> saveProfileChanges());
        } else {
            System.err.println("UserInfoActivity: save_profile_button not found!");
        }

        // Listener за изход
        if (logoutButton != null) {
            logoutButton.setOnClickListener(v -> logout());
        } else {
            System.err.println("UserInfoActivity: logout_button not found!");
        }
    }

    private void setupBottomNavigation() {
        // ... (логиката остава същата, бутоните са преведени в XML) ...
        if (btnHome != null) {
            btnHome.setOnClickListener(v -> {
                Intent intent = new Intent(UserInfoActivity.this, HomeActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish(); // Затваряме текущия екран
            });
        }
        if (btnScanner != null) {
            // Ако искате Scan бутонът да води към Home, логиката е ОК.
            // Ако трябва да води към ScannerActivity:
            // Intent intent = new Intent(UserInfoActivity.this, ScannerActivity.class);
            // startActivity(intent);
            // Не затваряйте UserInfoActivity непременно тук.
            // Засега оставям да води към Home:
            btnScanner.setOnClickListener(v -> {
                Intent intent = new Intent(UserInfoActivity.this, HomeActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish(); // Затваряме текущия екран
            });
        }

        if (btnUserInfo != null) {
            btnUserInfo.setEnabled(false); // Вече сме тук
        }
    }

    private void loadProfileData() {
        // Име (нередактируемо)
        String name = sharedPreferences.getString("name", "--");
        if (nameView != null) nameView.setText("Име: " + name); // Преведено

        // Пол (нередактируемо) - Превеждаме показаната стойност
        String gender = sharedPreferences.getString("gender", "--"); // "Male" или "Female"
        String displayGender = gender;
        if ("Male".equalsIgnoreCase(gender)) {
            displayGender = "Мъж";
        } else if ("Female".equalsIgnoreCase(gender)) {
            displayGender = "Жена";
        }
        if (genderView != null) genderView.setText("Пол: " + displayGender); // Преведено

        // Попълваме полетата за редакция
        String weightStr = sharedPreferences.getString("weight", ""); // "" за празно поле
        if (weightInputProfile != null) weightInputProfile.setText(weightStr);

        String heightStr = sharedPreferences.getString("height", "");
        if (heightInputProfile != null) heightInputProfile.setText(heightStr);

        int age = sharedPreferences.getInt("age", 0);
        if (ageInputProfile != null)
            ageInputProfile.setText(age != 0 ? String.valueOf(age) : ""); // "" ако е 0
    }

    private void loadCalorieGoal() {
        // Зарежда ЗАДАДЕНАТА цел от SharedPreferences
        if (currentCalorieGoalView != null) {
            float calorieGoalFloat = sharedPreferences.getFloat("calorie_goal", 2000f);
            int calorieGoal = Math.round(calorieGoalFloat);
            // Преведено
            currentCalorieGoalView.setText("Текуща цел: " + calorieGoal + " ккал/ден");
        }
        // Изчистваме полето за въвеждане
        if (calorieGoalInput != null) {
            calorieGoalInput.setText("");
        }
        // Нулираме грешката
        if (calorieGoalInputLayout != null) {
            calorieGoalInputLayout.setError(null);
        }
    }

    // Запазва само локално и показва съобщение


    // Нов метод за запазване на промените в профила (тегло, височина, възраст)
    private void saveProfileChanges() {
        if (!validateProfileInputs()) {
            return; // Спираме, ако валидацията е неуспешна
        }

        // Може да добавите спинър за зареждане тук
        // showLoadingProfile(true);

        String weightStr = Objects.requireNonNull(weightInputProfile.getText()).toString().trim();
        String heightStr = Objects.requireNonNull(heightInputProfile.getText()).toString().trim();
        String ageStr = Objects.requireNonNull(ageInputProfile.getText()).toString().trim();

        // Създаваме обект (Map) с данните за изпращане
        Map<String, Object> updatePayload = new HashMap<>();
        try {
            updatePayload.put("weight", Float.parseFloat(weightStr));
            updatePayload.put("height", Float.parseFloat(heightStr));
            updatePayload.put("age", Integer.parseInt(ageStr));
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Грешка при обработка на данните.", Toast.LENGTH_SHORT).show(); // Преведено
            return;
        }


        ApiService apiService = ApiClient.getApiService(this);
        if (apiService == null) {
            Toast.makeText(this, "Грешка: API услугата е недостъпна", Toast.LENGTH_SHORT).show(); // Преведено
            // showLoadingProfile(false);
            return;
        }

        Call<UpdateResponse> call = apiService.updateUserData(updatePayload); // Извикваме новия метод от ApiService
        call.enqueue(new Callback<UpdateResponse>() {
            @Override
            public void onResponse(Call<UpdateResponse> call, Response<UpdateResponse> response) {
                // Скриваме спинъра
                // showLoadingProfile(false);

                if (response.isSuccessful() && response.body() != null) {
                    Toast.makeText(UserInfoActivity.this, response.body().getMessage(), Toast.LENGTH_LONG).show(); // Показваме съобщението от сървъра

                    // --- Актуализираме локалните SharedPreferences СЛЕД успешен запис ---
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString("weight", weightStr);
                    editor.putString("height", heightStr);
                    editor.putInt("age", Integer.parseInt(ageStr));

                    // Ако бекендът връща преизчислените калории, ги записваме
                    if(response.body().getUpdatedFields() != null && response.body().getUpdatedFields().containsKey("calories")) {
                        Object caloriesValue = response.body().getUpdatedFields().get("calories");
                        if (caloriesValue instanceof Number) {
                            editor.putFloat("calorie_goal", ((Number) caloriesValue).floatValue());
                            // Актуализираме и показването на ТЕКУЩАТА цел (изчислената)
                            loadCalorieGoal(); // Това ще вземе новата стойност от prefs
                        }
                    }
                    editor.apply();
                    // --------------------------------------------------------------------

                } else {
                    String errorMsg = "Неуспешна актуализация."; // Преведено default
                    if (response.errorBody() != null) {
                        try {
                            // Опит за четене на грешката (може да се нуждае от JSON парсер)
                            errorMsg = "Грешка: " + response.code(); // Опростено
                        } catch (Exception e) { /* ignore */ }
                    } else if (response.body() != null && response.body().getMessage() != null) {
                        errorMsg = response.body().getMessage(); // Съобщение от сървъра
                    }
                    Toast.makeText(UserInfoActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<UpdateResponse> call, Throwable t) {
                // Скриваме спинъра
                // showLoadingProfile(false);
                Toast.makeText(UserInfoActivity.this, "Мрежова грешка: " + t.getMessage(), Toast.LENGTH_LONG).show(); // Преведено
            }
        });
    }


    // Валидация за полетата на профила
    private boolean validateProfileInputs() {
        // Нулираме грешките
        weightInputLayoutProfile.setError(null);
        heightInputLayoutProfile.setError(null);
        ageInputLayoutProfile.setError(null);

        String weightStr = Objects.requireNonNull(weightInputProfile.getText()).toString().trim();
        String heightStr = Objects.requireNonNull(heightInputProfile.getText()).toString().trim();
        String ageStr = Objects.requireNonNull(ageInputProfile.getText()).toString().trim();

        boolean isValid = true;

        // Валидация на Тегло
        if (weightStr.isEmpty()) {
            weightInputLayoutProfile.setError("Теглото не може да бъде празно"); // Преведено
            isValid = false;
        } else {
            try {
                float weight = Float.parseFloat(weightStr);
                if (weight < MIN_WEIGHT || weight > MAX_WEIGHT) {
                    weightInputLayoutProfile.setError("Теглото трябва да е между " + MIN_WEIGHT + " и " + MAX_WEIGHT + " кг"); // Преведено
                    isValid = false;
                }
            } catch (NumberFormatException e) {
                weightInputLayoutProfile.setError("Невалиден числов формат за тегло"); // Преведено
                isValid = false;
            }
        }

        // Валидация на Височина
        if (heightStr.isEmpty()) {
            heightInputLayoutProfile.setError("Височината не може да бъде празна"); // Преведено
            isValid = false;
        } else {
            try {
                float height = Float.parseFloat(heightStr);
                if (height < MIN_HEIGHT || height > MAX_HEIGHT) {
                    heightInputLayoutProfile.setError("Височината трябва да е между " + MIN_HEIGHT + " и " + MAX_HEIGHT + " см"); // Преведено
                    isValid = false;
                }
            } catch (NumberFormatException e) {
                heightInputLayoutProfile.setError("Невалиден числов формат за височина"); // Преведено
                isValid = false;
            }
        }

        // Валидация на Възраст
        if (ageStr.isEmpty()) {
            ageInputLayoutProfile.setError("Възрастта не може да бъде празна"); // Преведено
            isValid = false;
        } else {
            try {
                int age = Integer.parseInt(ageStr);
                if (age < MIN_AGE || age > MAX_AGE) {
                    ageInputLayoutProfile.setError("Възрастта трябва да е между " + MIN_AGE + " и " + MAX_AGE); // Преведено
                    isValid = false;
                }
            } catch (NumberFormatException e) {
                ageInputLayoutProfile.setError("Невалиден числов формат за възраст"); // Преведено
                isValid = false;
            }
        }

        return isValid;
    }


    private void logout() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove("token");
        // editor.clear(); // Помислете дали искате да изтриете ВСИЧКИ настройки
        editor.apply();

        Intent intent = new Intent(UserInfoActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
        // Преведено
        Toast.makeText(this, "Успешен изход", Toast.LENGTH_SHORT).show();
    }

    // TODO: Добавете методи showLoadingProfile(boolean show) и showLoadingGoal(boolean show), ако използвате отделни спинери.
}