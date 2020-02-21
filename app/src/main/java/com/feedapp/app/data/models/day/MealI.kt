/*
 * Copyright (c) 2020 Ruslan Potekhin
 */

package com.feedapp.app.data.models.day

import com.feedapp.app.data.models.Product
import com.feedapp.app.data.models.StatisticsNutrientType

interface MealI {
    var id: Int
    var products: ArrayList<Product>
    var mealType: MealType
    fun getTotalCalories(): Float
    fun getTotalProteins(): Float
    fun getTotalFats(): Float
    fun getTotalCarbs(): Float
    fun getTotalNutrient(nutrientType: StatisticsNutrientType): Float
    fun getTotalSugar(): Float
    fun deleteProduct(product: Product)
}