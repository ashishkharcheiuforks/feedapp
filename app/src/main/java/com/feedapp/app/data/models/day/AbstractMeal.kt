/*
 * Copyright (c) 2020 Ruslan Potekhin
 */

package com.feedapp.app.data.models.day

import com.feedapp.app.data.models.Product
import com.feedapp.app.data.models.StatisticsNutrientType

abstract class AbstractMeal : MealI {


    override fun deleteProduct(product: Product) {
        try {
            products.remove(product)
        } catch (e: IndexOutOfBoundsException) {
            e.printStackTrace()
        }
    }

    override fun getTotalNutrient(nutrientType: StatisticsNutrientType): Float {
        when (nutrientType) {
            StatisticsNutrientType.CALORIES -> {
                return getTotalCalories()
            }
            StatisticsNutrientType.PROTEINS -> {
                return getTotalProteins()
            }
            StatisticsNutrientType.FATS -> {
                return getTotalFats()
            }
            StatisticsNutrientType.CARBS -> {
                return getTotalCarbs()
            }
            else -> {
                return 0f
            }
        }
    }

    // Get total calories from all products from meal
    override fun getTotalCalories(): Float {
        var total = 0F
        products.forEach { total += it.consumedCalories }
        return total
    }

    // Get total proteins from all products from meal
    override fun getTotalProteins(): Float {
        var total = 0F
        products.forEach { total += it.consumedProtein ?: 0f }
        return total
    }

    // Get total fats from all products from meal
    override fun getTotalFats(): Float {
        var total = 0F
        products.forEach { total += it.consumedFat ?: 0f }
        return total
    }

    // Get total carbs from all products from meal
    override fun getTotalCarbs(): Float {
        var total = 0F
        products.forEach { total += it.consumedCarbs ?: 0f }
        return total
    }

    override fun getTotalSugar(): Float {
        var total = 0F
        products.forEach { total += it.consumedSugar ?: 0f }
        return total
    }

}