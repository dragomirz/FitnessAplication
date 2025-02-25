package com.fitness.fitnessapplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.fitness.fitnessapplication.RetroFit.ApiClient;
import com.fitness.fitnessapplication.RetroFit.ApiService;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        Button btnHome = findViewById(R.id.btn_home);
        Button btnScanner = findViewById(R.id.btn_scanner);
        Button btnUserInfo = findViewById(R.id.btn_user_info);
        Button btnTestAuth = findViewById(R.id.btn_test_auth); // Get the test button

        btnHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // You're already on the home page
            }
        });

        btnScanner.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(HomeActivity.this, ScannerActivity.class));
            }
        });

        btnUserInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Implement user info functionality later
            }
        });

        // Add the test button's onClickListener
        btnTestAuth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                testAuthentication();
            }
        });
    }

    private void testAuthentication() {
        ApiService apiService = ApiClient.getApiService(this);
        Call<Void> call = apiService.testAuthentication();
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(HomeActivity.this, "Authentication successful!", Toast.LENGTH_SHORT).show();
                } else if (response.code() == 401 || response.code() == 403) {
                    Toast.makeText(HomeActivity.this, "Token expired or invalid. Please log in again.", Toast.LENGTH_LONG).show();
                    SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
                    sharedPreferences.edit().remove("token").apply();
                    Intent intent = new Intent(HomeActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(HomeActivity.this, "Authentication failed: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(HomeActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}