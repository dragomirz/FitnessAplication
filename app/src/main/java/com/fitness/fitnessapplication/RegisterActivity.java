package com.fitness.fitnessapplication;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.fitness.fitnessapplication.DataModels.RegisterResponse;
import com.fitness.fitnessapplication.DataModels.User;
import com.fitness.fitnessapplication.RetroFit.ApiClient;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {

    private TextInputEditText edtName, edtEmail, edtPassword, edtHeight, edtWeight;
    private TextInputLayout nameInputLayout, emailInputLayout, passwordInputLayout, heightInputLayout, weightInputLayout;
    private Spinner spinnerGender;
    private Button btnRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.register_activity);

        nameInputLayout = findViewById(R.id.name_input_layout);
        emailInputLayout = findViewById(R.id.email_input_layout);
        passwordInputLayout = findViewById(R.id.password_input_layout);
        heightInputLayout = findViewById(R.id.height_input_layout);
        weightInputLayout = findViewById(R.id.weight_input_layout);
        edtName = findViewById(R.id.edt_name);
        edtEmail = findViewById(R.id.edt_email);
        edtPassword = findViewById(R.id.edt_password);
        edtHeight = findViewById(R.id.edt_height);
        edtWeight = findViewById(R.id.edt_weight);
        spinnerGender = findViewById(R.id.spinner_gender);
        btnRegister = findViewById(R.id.button_register);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.genders_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerGender.setAdapter(adapter);

        btnRegister.setOnClickListener(v -> registerUser());
    }

    private void registerUser() {
        // Reset error states
        nameInputLayout.setError(null);
        emailInputLayout.setError(null);
        passwordInputLayout.setError(null);
        heightInputLayout.setError(null);
        weightInputLayout.setError(null);

        String name = edtName.getText().toString().trim();
        String email = edtEmail.getText().toString().trim();
        String password = edtPassword.getText().toString().trim();
        String heightStr = edtHeight.getText().toString().trim();
        String weightStr = edtWeight.getText().toString().trim();
        String gender = spinnerGender.getSelectedItem().toString();

        if (name.isEmpty()) {
            nameInputLayout.setError("Please enter your name");
            return;
        }

        if (email.isEmpty()) {
            emailInputLayout.setError("Please enter your email");
            return;
        }

        if (password.isEmpty()) {
            passwordInputLayout.setError("Please enter your password");
            return;
        }

        float height, weight;
        try {
            height = Float.parseFloat(heightStr);
            if (height <= 0) {
                heightInputLayout.setError("Height must be positive");
                return;
            }
        } catch (NumberFormatException e) {
            heightInputLayout.setError("Invalid height");
            return;
        }

        try {
            weight = Float.parseFloat(weightStr);
            if (weight <= 0) {
                weightInputLayout.setError("Weight must be positive");
                return;
            }
        } catch (NumberFormatException e) {
            weightInputLayout.setError("Invalid weight");
            return;
        }

        User user = new User(email, password, name, heightStr, gender, weightStr);

        Call<RegisterResponse> call = ApiClient.getApiService(this).registerUser(user);
        call.enqueue(new Callback<RegisterResponse>() {
            @Override
            public void onResponse(Call<RegisterResponse> call, Response<RegisterResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    RegisterResponse registerResponse = response.body();
                    Toast.makeText(RegisterActivity.this, registerResponse.getMessage(), Toast.LENGTH_SHORT).show();
                    finish(); // Return to MainActivity
                } else {
                    Toast.makeText(RegisterActivity.this, "Registration failed", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<RegisterResponse> call, Throwable t) {
                Toast.makeText(RegisterActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}