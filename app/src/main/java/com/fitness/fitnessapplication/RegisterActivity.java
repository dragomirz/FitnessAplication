package com.fitness.fitnessapplication;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils; // Import TextUtils
import android.util.Patterns;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView; // Import AutoCompleteTextView
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.fitness.fitnessapplication.DataModels.RegisterResponse;
import com.fitness.fitnessapplication.DataModels.User;
import com.fitness.fitnessapplication.RetroFit.ApiClient;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import java.util.Objects;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

// Remove: import android.widget.Spinner; // No longer needed

public class RegisterActivity extends AppCompatActivity {

    private TextInputEditText edtName, edtEmail, edtPassword, edtHeight, edtWeight, edtAge;
    private TextInputLayout nameInputLayout, emailInputLayout, passwordInputLayout, heightInputLayout, weightInputLayout, ageInputLayout;
    // Correct declarations for the Gender dropdown
    private TextInputLayout genderSpinnerLayout;
    private AutoCompleteTextView spinnerGenderAutocomplete; // Added declaration

    private Button btnRegister;
    private TextView txtLoginAccount;
    private ProgressBar loadingSpinner;

    // Constants for validation (Keep these as they are)
    private static final int MIN_PASSWORD_LENGTH = 6;
    private static final float MIN_HEIGHT = 50f;
    private static final float MAX_HEIGHT = 280f;
    private static final float MIN_WEIGHT = 20f;
    private static final float MAX_WEIGHT = 500f;
    private static final int MIN_AGE = 5;
    private static final int MAX_AGE = 120;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.register_activity);

        // --- Find Views ---
        nameInputLayout = findViewById(R.id.name_input_layout);
        emailInputLayout = findViewById(R.id.email_input_layout);
        passwordInputLayout = findViewById(R.id.password_input_layout);
        heightInputLayout = findViewById(R.id.height_input_layout);
        weightInputLayout = findViewById(R.id.weight_input_layout);
        ageInputLayout = findViewById(R.id.age_input_layout);
        edtName = findViewById(R.id.edt_name);
        edtEmail = findViewById(R.id.edt_email);
        edtPassword = findViewById(R.id.edt_password);
        edtHeight = findViewById(R.id.edt_height);
        edtWeight = findViewById(R.id.edt_weight);
        edtAge = findViewById(R.id.edt_age);
        // Correctly find BOTH the layout and the AutoCompleteTextView
        genderSpinnerLayout = findViewById(R.id.gender_spinner_layout);
        spinnerGenderAutocomplete = findViewById(R.id.spinner_gender_autocomplete); // Added findViewById

        btnRegister = findViewById(R.id.button_register);
        txtLoginAccount = findViewById(R.id.txt_go_back);
        loadingSpinner = findViewById(R.id.register_loading_spinner);

        // --- Setup Gender Spinner ---
        // Get the array of genders from resources
        String[] genders = getResources().getStringArray(R.array.genders_array);
        // Create the adapter
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line, genders);
        // Set the adapter on the AutoCompleteTextView, NOT the TextInputLayout
        spinnerGenderAutocomplete.setAdapter(adapter);

        // --- Setup Listeners ---
        btnRegister.setOnClickListener(v -> attemptRegistration());
        txtLoginAccount.setOnClickListener(v -> {
            if (loadingSpinner.getVisibility() == View.GONE) {
                startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            }
        });
    }

    private void attemptRegistration() {
        if (!validateInputs()) {
            return; // Stop if validation fails
        }

        showLoading(true);

        // Get validated values
        String name = Objects.requireNonNull(edtName.getText()).toString().trim();
        String email = Objects.requireNonNull(edtEmail.getText()).toString().trim();
        String password = Objects.requireNonNull(edtPassword.getText()).toString().trim();
        String heightStr = Objects.requireNonNull(edtHeight.getText()).toString().trim();
        String weightStr = Objects.requireNonNull(edtWeight.getText()).toString().trim();
        // Correctly get gender from AutoCompleteTextView
        String gender = spinnerGenderAutocomplete.getText().toString();
        int age = Integer.parseInt(Objects.requireNonNull(edtAge.getText()).toString().trim());

        // Create User object (Make sure User constructor matches these types)
        User user = new User(email, password, name, heightStr, gender, weightStr, age, 0);

        // --- Perform API Call --- (Keep the Retrofit call logic as is)
        Call<RegisterResponse> call = ApiClient.getApiService(this).registerUser(user);
        call.enqueue(new Callback<RegisterResponse>() {
            @Override
            public void onResponse(Call<RegisterResponse> call, Response<RegisterResponse> response) {
                showLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    RegisterResponse registerResponse = response.body();
                    Toast.makeText(RegisterActivity.this, registerResponse.getMessage(), Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                } else {
                    String errorMessage = "Registration failed. Please try again.";
                    // Improved error message handling
                    if (response.errorBody() != null) {
                        // Try to get message from standard error body if possible
                        try {
                            // NOTE: You might need a specific error model parser here
                            errorMessage = "Registration failed: " + response.code() + " - " + response.message();
                        } catch (Exception e) { /* Ignore parsing error */ }
                    } else if (response.body() != null && response.body().getMessage() != null) {
                        errorMessage = response.body().getMessage(); // If API puts error in success body
                    } else if (!response.isSuccessful()){
                        errorMessage = "Registration failed: " + response.code() + " - " + response.message();
                    }
                    Toast.makeText(RegisterActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<RegisterResponse> call, Throwable t) {
                showLoading(false);
                Toast.makeText(RegisterActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private boolean validateInputs() {
        // Reset errors
        nameInputLayout.setError(null);
        emailInputLayout.setError(null);
        passwordInputLayout.setError(null);
        heightInputLayout.setError(null);
        weightInputLayout.setError(null);
        ageInputLayout.setError(null);
        genderSpinnerLayout.setError(null); // Reset gender error

        // Get text
        String name = Objects.requireNonNull(edtName.getText()).toString().trim();
        String email = Objects.requireNonNull(edtEmail.getText()).toString().trim();
        String password = Objects.requireNonNull(edtPassword.getText()).toString().trim();
        String heightStr = Objects.requireNonNull(edtHeight.getText()).toString().trim();
        String weightStr = Objects.requireNonNull(edtWeight.getText()).toString().trim();
        String ageStr = Objects.requireNonNull(edtAge.getText()).toString().trim();
        // Get gender text
        String gender = spinnerGenderAutocomplete.getText().toString();

        boolean isValid = true;

        // --- Validate other fields (Keep Name, Email, Password, Height, Weight, Age validation as is) ---
        // Validate Name
        if (name.isEmpty()) {
            nameInputLayout.setError("Name cannot be empty");
            isValid = false;
        }

        // Validate Email
        if (email.isEmpty()) {
            emailInputLayout.setError("Email cannot be empty");
            isValid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInputLayout.setError("Invalid email format");
            isValid = false;
        }

        // Validate Password
        if (password.isEmpty()) {
            passwordInputLayout.setError("Password cannot be empty");
            isValid = false;
        } else if (password.length() < MIN_PASSWORD_LENGTH) {
            passwordInputLayout.setError("Password must be at least " + MIN_PASSWORD_LENGTH + " characters");
            isValid = false;
        }

        // Validate Height
        if (heightStr.isEmpty()) {
            heightInputLayout.setError("Height cannot be empty");
            isValid = false;
        } else {
            try {
                float height = Float.parseFloat(heightStr);
                if (height < MIN_HEIGHT || height > MAX_HEIGHT) {
                    heightInputLayout.setError("Height must be between " + MIN_HEIGHT + " and " + MAX_HEIGHT + " cm");
                    isValid = false;
                }
            } catch (NumberFormatException e) {
                heightInputLayout.setError("Invalid number format for height");
                isValid = false;
            }
        }

        // Validate Weight
        if (weightStr.isEmpty()) {
            weightInputLayout.setError("Weight cannot be empty");
            isValid = false;
        } else {
            try {
                float weight = Float.parseFloat(weightStr);
                if (weight < MIN_WEIGHT || weight > MAX_WEIGHT) {
                    weightInputLayout.setError("Weight must be between " + MIN_WEIGHT + " and " + MAX_WEIGHT + " kg");
                    isValid = false;
                }
            } catch (NumberFormatException e) {
                weightInputLayout.setError("Invalid number format for weight");
                isValid = false;
            }
        }

        // Validate Age
        if (ageStr.isEmpty()) {
            ageInputLayout.setError("Age cannot be empty");
            isValid = false;
        } else {
            try {
                int age = Integer.parseInt(ageStr);
                if (age < MIN_AGE || age > MAX_AGE) {
                    ageInputLayout.setError("Age must be between " + MIN_AGE + " and " + MAX_AGE);
                    isValid = false;
                }
            } catch (NumberFormatException e) {
                ageInputLayout.setError("Invalid number format for age");
                isValid = false;
            }
        }

        // --- Validate Gender Spinner ---
        if (TextUtils.isEmpty(gender)) { // Check if the selection is empty
            genderSpinnerLayout.setError("Please select a gender"); // Set error on the TextInputLayout
            // Optionally request focus
            // genderSpinnerLayout.requestFocus();
            isValid = false;
        } else {
            // Optional but recommended: Check if the selected value is actually in your predefined list
            boolean found = false;
            String[] validGenders = getResources().getStringArray(R.array.genders_array);
            for(String validGender : validGenders) {
                // Use equalsIgnoreCase for case-insensitive comparison if needed
                if(gender.equals(validGender)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                genderSpinnerLayout.setError("Invalid gender selection");
                // Clear the invalid input if desired
                // spinnerGenderAutocomplete.setText("");
                isValid = false;
            }
        }

        return isValid;
    }

    private void showLoading(boolean show) {
        loadingSpinner.setVisibility(show ? View.VISIBLE : View.GONE);
        btnRegister.setEnabled(!show);
        // Correctly disable/enable all input fields
        edtName.setEnabled(!show);
        nameInputLayout.setEnabled(!show); // Also disable layout visually
        edtEmail.setEnabled(!show);
        emailInputLayout.setEnabled(!show);
        edtPassword.setEnabled(!show);
        passwordInputLayout.setEnabled(!show);
        edtHeight.setEnabled(!show);
        heightInputLayout.setEnabled(!show);
        edtWeight.setEnabled(!show);
        weightInputLayout.setEnabled(!show);
        edtAge.setEnabled(!show);
        ageInputLayout.setEnabled(!show);
        // Correctly disable/enable gender dropdown
        spinnerGenderAutocomplete.setEnabled(!show);
        genderSpinnerLayout.setEnabled(!show);
        // Disable/enable text link
        txtLoginAccount.setEnabled(!show);
    }
}