package com.fitness.fitnessapplication.Adapters; // Create this package if needed

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fitness.fitnessapplication.DataModels.FoodLog; // Correct import
import com.fitness.fitnessapplication.R; // Correct import for your R file

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FoodHistoryAdapter extends RecyclerView.Adapter<FoodHistoryAdapter.FoodHistoryViewHolder> {

    private List<FoodLog> foodLogList = new ArrayList<>();

    public void updateData(List<FoodLog> newFoodLogList) {
        this.foodLogList.clear();
        if (newFoodLogList != null) {
            this.foodLogList.addAll(newFoodLogList);
        }
        notifyDataSetChanged(); // Inform the RecyclerView about the data change
    }

    @NonNull
    @Override
    public FoodHistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_food_history, parent, false);
        return new FoodHistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FoodHistoryViewHolder holder, int position) {
        FoodLog foodLog = foodLogList.get(position);
        holder.bind(foodLog);
    }

    @Override
    public int getItemCount() {
        return foodLogList.size();
    }

    // --- ViewHolder ---
    static class FoodHistoryViewHolder extends RecyclerView.ViewHolder {
        TextView nameTextView;
        TextView caloriesTextView;

        public FoodHistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.food_history_name);
            caloriesTextView = itemView.findViewById(R.id.food_history_calories);
        }

        void bind(FoodLog foodLog) {
            if (foodLog != null) {
                nameTextView.setText(foodLog.getProductName() != null ? foodLog.getProductName() : "Unknown Food");
                // Format calories nicely
                String caloriesText = String.format(Locale.getDefault(), "%.0f kcal", foodLog.getCalories());
                caloriesTextView.setText(caloriesText);

                // You could add quantity here if available:
                // String quantityText = String.format(Locale.getDefault(), "%.1f units", foodLog.getQuantity());
                // quantityTextView.setText(quantityText); // Assuming you add a quantityTextView
            } else {
                // Handle potential null item in list (shouldn't happen ideally)
                nameTextView.setText("Error");
                caloriesTextView.setText("");
            }
        }
    }
}