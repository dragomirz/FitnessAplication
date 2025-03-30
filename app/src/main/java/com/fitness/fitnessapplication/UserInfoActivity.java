package com.fitness.fitnessapplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class UserInfoActivity extends AppCompatActivity {
    private TextView  ageView, weightView, heightView, genderView, currentCalorieGoalView, nameView;
    private EditText calorieGoalInput;
    private Button saveCalorieGoalButton, logoutButton;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Hide the ActionBar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        setContentView(R.layout.activity_user_info);

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);

        // Initialize views

        nameView = findViewById(R.id.name_view);
        ageView = findViewById(R.id.age_view);
        weightView = findViewById(R.id.weight_view);
        heightView = findViewById(R.id.height_view);
        genderView = findViewById(R.id.gender_view);
        currentCalorieGoalView = findViewById(R.id.current_calorie_goal);
        calorieGoalInput = findViewById(R.id.calorie_goal_input);
        saveCalorieGoalButton = findViewById(R.id.save_calorie_goal_button);
        logoutButton = findViewById(R.id.logout_button);

        // Load user profile data
        loadProfileData();

        // Load current calorie goal
        loadCalorieGoal();

//        // Set up save calorie goal button listener
//        saveCalorieGoalButton.setOnClickListener(v -> saveCalorieGoal());

        // Set up logout button listener
        logoutButton.setOnClickListener(v -> logout());
    }

    private void loadProfileData() {
        // Load profile data from SharedPreferences (or API if available)
        String name = sharedPreferences.getString("name","");
        int age = sharedPreferences.getInt("age", 0);
        float weight = Float.parseFloat(sharedPreferences.getString("weight", "0"));
        float height = Float.parseFloat(sharedPreferences.getString("height", "0"));
        String gender = sharedPreferences.getString("gender", "");

        nameView.setText("Name: " + (name));
        ageView.setText("Age: " + (age != 0 ? age : ""));
        weightView.setText("Weight: " + (weight != 0 ? weight + " kg" : "-- kg"));
        heightView.setText("Height: " + (height != 0 ? height + " cm" : "-- cm"));
        genderView.setText("Gender: " + gender);
    }

    private void loadCalorieGoal() {
        int calorieGoal = (int) sharedPreferences.getFloat("calorie_goal", (float) 2000.0); // Default to 2000 if not set
        currentCalorieGoalView.setText("Current Goal: " + calorieGoal + " kcal/day");
    }

    private void saveCalorieGoal() {
        String goalStr = calorieGoalInput.getText().toString();
        if (!goalStr.isEmpty()) {
            try {
                int newGoal = Integer.parseInt(goalStr);
                if (newGoal <= 0) {
                    Toast.makeText(this, "Calorie goal must be positive", Toast.LENGTH_SHORT).show();
                    return;
                }
                // Save the new calorie goal to SharedPreferences
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putInt("calorie_goal", newGoal);
                editor.apply();

                // Update the displayed goal
                currentCalorieGoalView.setText("Current Goal: " + newGoal + " kcal/day");
                calorieGoalInput.setText(""); // Clear input
                Toast.makeText(this, "Calorie goal updated!", Toast.LENGTH_SHORT).show();
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid calorie goal", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Enter a calorie goal", Toast.LENGTH_SHORT).show();
        }
    }

    private void logout() {
        // Clear the token and other user data from SharedPreferences
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove("token");
        editor.apply();

        // Navigate to MainActivity (login screen)
        Intent intent = new Intent(UserInfoActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
    }
}