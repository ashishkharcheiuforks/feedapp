package com.feedapp.app.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.feedapp.app.data.models.FoodProduct
import com.feedapp.app.data.repositories.FoodRepository
import com.feedapp.app.util.caloriesToEnergy
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import java.math.RoundingMode
import javax.inject.Inject

class AddCustomProductViewModel @Inject internal constructor(private val foodRepository: FoodRepository) :
    ViewModel() {

    private fun saveCustomProduct(foodProduct: FoodProduct) {
        foodRepository.insertProduct(foodProduct)

    }

    private fun getLastIndex(): Int {
        return foodRepository.getLastIndex()
    }

    fun exceeds(gramsInOnePortion: Float, nutrient: Float): Boolean {
        return gramsInOnePortion < nutrient && gramsInOnePortion != 0f
    }

    fun getMultiplier(gramsInOnePortion: Float): Float {
        return 100 / gramsInOnePortion
    }

    fun getCalories(caloriesInOnePortion: Float, hundredMultiplier: Float): Float {
        return (caloriesInOnePortion * hundredMultiplier)
            .toBigDecimal()
            .setScale(2, RoundingMode.UP).toFloat()
    }

    fun getEnergy(caloriesInHundredGrams: Float): Float {
        return caloriesToEnergy(caloriesInHundredGrams)
    }

    fun saveProduct(
        name: String,
        energy: Float,
        proteinsInHundred: Float,
        fatsInHundred: Float,
        carbsInHundred: Float,
        sugar: Float,
        sFats: Float,
        uFats: Float,
        tFats: Float
    ) = viewModelScope.launch(IO) {
            val lastId = getLastIndex()
            val product = FoodProduct(
                id = lastId + 1,
                name = name,
                energy = energy,
                protein = proteinsInHundred,
                fat = fatsInHundred,
                carbs = carbsInHundred,
                sugar = sugar,
                sat_fats_g = sFats,
                mono_fats_g = uFats,
                o_poly_fats_g = tFats
            )
            saveCustomProduct(product)
        }

}
