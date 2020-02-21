/*
 * Copyright (c) 2020 Ruslan Potekhin
 */

package com.feedapp.app.data.repositories

import android.annotation.SuppressLint
import com.feedapp.app.data.api.models.recipedetailed.nn.RecipeDetailedResponse
import com.feedapp.app.data.api.models.usdafooddetailed.USDAFoodModel
import com.feedapp.app.data.databases.daos.DayDao
import com.feedapp.app.data.models.ConverterToProduct
import com.feedapp.app.data.models.FoodProduct
import com.feedapp.app.data.models.Product
import com.feedapp.app.data.models.day.Day
import com.feedapp.app.data.models.day.DayDate
import com.feedapp.app.data.models.day.Meal
import com.feedapp.app.data.models.day.MealType
import com.feedapp.app.util.DAY_FRAGMENTS_START_POSITION
import com.feedapp.app.util.getDayDate
import java.util.*
import javax.inject.Inject

@SuppressLint("SimpleDateFormat")
class DayRepository @Inject internal constructor(
    private val dayDao: DayDao
) {

    /*
     * get date according to current position
     */
    fun getDate(position: Int): Date {
        val diff = position - DAY_FRAGMENTS_START_POSITION
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DATE, diff)
        return calendar.time
    }

    fun getInitialDayDate(): DayDate {
        val date = Calendar.getInstance().time
        return date.getDayDate()
    }


    suspend fun getPreviousOrNextDay(
        position: Int
    ): Day {
        val date = getDate(position).getDayDate()
        // if there is next or previous day in DB, return it
        // else generate new day with needed day
        return dayDao.searchByStringDate(date.month, date.day)
            ?: generateDefaultDayWithSpecificDay(date)
    }

    private fun generateMeals(): List<Meal> {
        val mutableList = mutableListOf<Meal>()
        for (value in MealType.values())
            mutableList.add(Meal(products = arrayListOf(), mealType = value))
        return mutableList
    }

    private fun generateDefaultDayWithSpecificDay(date: DayDate): Day {
        val meals = generateMeals()
        val day = Day(
            meals = meals,
            dayId = 0,
            date = date
        )
        // save generated day
        insertDay(day)
        return day
    }


    fun saveSearchProductToDay(
        dateString: DayDate,
        mealType: Int,
        apiProduct: USDAFoodModel?,
        offlineProduct: FoodProduct?,
        grams: Float
    ): Product? {
        var product: Product? = null
        try {
            val converterToProduct = ConverterToProduct()
            if (apiProduct != null) {
                product = converterToProduct.convertUSDAModel(apiProduct, grams)
            } else if (offlineProduct != null) {
                product = converterToProduct.convertFoodProduct(offlineProduct, grams)
            }
            product?.let { addProductToDay(dateString, mealType, product) }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }

        return product
    }


    /*
     * add product to the meals of given day
     */
    private fun addProductToDay(
        date: DayDate,
        mealType: Int,
        product: Product
    ) {
        // receive day by date
        var day = dayDao.searchByStringDate(date.month, date.day)


        // if day not exists, generate it
        if (day == null) {
            day = generateDefaultDayWithSpecificDay(date)

        }
        // add product to the meal of the day
        day.meals[mealType].products.add(product)
        insertDay(day)
    }

    fun deleteAllDays() {
        dayDao.deleteAllDays()
    }

    fun saveRecipeToDay(
        recipe: RecipeDetailedResponse,
        servings: Int,
        position: Int,
        date: Date = Calendar.getInstance().time
    ) {
        val product = ConverterToProduct().convertRecipe(recipe, servings)
        addProductToDay(date.getDayDate(), position, product)

    }

    fun getDayByDate(date: DayDate): Day? {
        return dayDao.searchByStringDate(date.month, date.day)
    }

    fun deleteProductFromDay(date: DayDate, product: Product): Boolean {
        val day = getDayByDate(date) ?: return false
        val isDeleted = day.deleteProduct(product)
        insertDay(day)
        return isDeleted
    }

    fun deleteDay(id: Int) {
        dayDao.deleteDay(id)
    }

    fun searchById(id: Int): Day? {
        return dayDao.searchById(id)
    }

    fun getSize(): Int {
        return dayDao.getSize()
    }

    fun getAllDays(): List<Day> {
        return dayDao.getAllDays()
    }

    fun insertDay(day: Day) {
        dayDao.insertDay(day)
    }


}