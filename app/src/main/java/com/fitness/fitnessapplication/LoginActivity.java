package com.fitness.fitnessapplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.fitness.fitnessapplication.DataModels.LoginResponse;
import com.fitness.fitnessapplication.DataModels.User;
import com.fitness.fitnessapplication.RetroFit.ApiClient;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText edtEmail, edtPassword;
    private TextInputLayout emailInputLayout, passwordInputLayout;
    private Button btnLogin;
    private TextView txtCreateAccount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        String token = sharedPreferences.getString("token", null);

        if (token != null) {
            Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        setContentView(R.layout.activity_login);

        emailInputLayout = findViewById(R.id.email_input_layout);
        passwordInputLayout = findViewById(R.id.password_input_layout);
        edtEmail = findViewById(R.id.edt_mail);
        edtPassword = findViewById(R.id.edt_password);
        btnLogin = findViewById(R.id.button_login);
        txtCreateAccount = findViewById(R.id.txt_create_account);

        btnLogin.setOnClickListener(v -> loginUser());

        txtCreateAccount.setOnClickListener(v -> startActivity(new Intent(LoginActivity.this, RegisterActivity.class)));
    }

    private void loginUser() {
        String email = edtEmail.getText().toString().trim();
        String password = edtPassword.getText().toString().trim();

        // Нулиране на грешките
        emailInputLayout.setError(null);
        passwordInputLayout.setError(null);

        // Валидация с директно вмъкнати български съобщения
        if (email.isEmpty()) {
            emailInputLayout.setError("Моля, въведете вашия имейл"); // <-- Преведено
            return;
        }

        if (password.isEmpty()) {
            passwordInputLayout.setError("Моля, въведете вашата парола"); // <-- Преведено
            return;
        }

        User user = new User(email, password);

        Call<LoginResponse> call = ApiClient.getApiService(this).loginUser(user);
        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    LoginResponse loginResponse = response.body();
                    // Показване на съобщението от сървъра (това НЕ Е преведено тук, идва от API)
                    Toast.makeText(LoginActivity.this, loginResponse.getMessage(), Toast.LENGTH_SHORT).show();
                    if (loginResponse.getToken() != null) {
                        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putString("token", loginResponse.getToken());
                        editor.apply();

                        Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                        startActivity(intent);
                        finish();
                    }
                } else {
                    // Показване на преведено съобщение за неуспешен вход
                    Toast.makeText(LoginActivity.this, "Неуспешен вход", Toast.LENGTH_SHORT).show(); // <-- Преведено
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                // Показване на преведено съобщение за мрежова грешка
                String errorMessage = t.getMessage() != null ? t.getMessage() : "Неизвестна грешка";
                Toast.makeText(LoginActivity.this, "Мрежова грешка: " + errorMessage, Toast.LENGTH_LONG).show(); // <-- Преведено (частично)
            }
        });
    }
}