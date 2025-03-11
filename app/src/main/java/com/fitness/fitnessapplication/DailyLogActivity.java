//package com.fitness.fitnessapplication;
//
//import android.content.Intent;
//import android.content.SharedPreferences;
//import android.os.Bundle;
//import android.widget.TextView;
//import android.widget.Toast;
//import androidx.appcompat.app.AppCompatActivity;
//import com.fitness.fitnessapplication.DataModels.DailyLogResponse;
//import com.fitness.fitnessapplication.RetroFit.ApiClient;
//import com.fitness.fitnessapplication.RetroFit.ApiService;
//import retrofit2.Call;
//import retrofit2.Callback;
//import retrofit2.Response;
//
//public class DailyLogActivity extends AppCompatActivity {
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_daily_log);
//
//        TextView totalsView = findViewById(R.id.totals_view);
//        String date = new java.text.SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date());
//
//        ApiService apiService = ApiClient.getApiService(this);
//        Call<DailyLogResponse> call = apiService.getDailyLogs(date);
//        call.enqueue(new Callback<DailyLogResponse>() {
//            @Override
//            public void onResponse(Call<DailyLogResponse> call, Response<DailyLogResponse> response) {
//                if (response.isSuccessful() && response.body() != null) {
//                    DailyLogResponse.Totals totals = response.body().getTotals();
//                    totalsView.setText(String.format("Calories: %.1f kcal\nProteins: %.1fg\nCarbs: %.1fg\nFats: %.1fg\nSat. Fat: %.1fg\nSugars: %.1fg",
//                            totals.getCalories(), totals.getProteins(), totals.getCarbs(), totals.getFats(),
//                            totals.getSaturatedFat(), totals.getSugars()));
//                } else if (response.code() == 401 || response.code() == 403) {
//                    Toast.makeText(DailyLogActivity.this, "Token expired or invalid. Please log in again.", Toast.LENGTH_LONG).show();
//                    SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
//                    sharedPreferences.edit().remove("token").apply();
//                    Intent intent = new Intent(DailyLogActivity.this, MainActivity.class);
//                    startActivity(intent);
//                    finish();
//                } else {
//                    totalsView.setText("No data for today");
//                }
//            }
//
//            @Override
//            public void onFailure(Call<DailyLogResponse> call, Throwable t) {
//                totalsView.setText("Error: " + t.getMessage());
//            }
//        });
//    }
//}