package com.feedapp.app.data.models


/*
    Types for tracking statistics
 */
enum class StatisticsNutrientType(var code: Int = 0){
    CALORIES(0),
    FATS(1),
    CARBS(2),
    PROTEINS(3),
    SUGAR(4);

    override fun toString(): String {
        return when(this){
            CALORIES -> "Calories"
            FATS -> "Fats"
            CARBS -> "Carbs"
            PROTEINS -> "Proteins"
            SUGAR -> "Sugar"
        }
    }
}