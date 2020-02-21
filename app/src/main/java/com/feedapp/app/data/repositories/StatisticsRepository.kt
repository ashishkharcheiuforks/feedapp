package com.feedapp.app.data.repositories

import com.feedapp.app.data.databases.daos.DayDao
import com.feedapp.app.data.models.day.Day
import com.feedapp.app.data.models.StatisticsNutrientType
import java.util.*
import javax.inject.Inject
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.math.roundToInt

class StatisticsRepository @Inject internal constructor(
    private val dayDao: DayDao,
    val calendar: Calendar
) {

    fun getNutrientTotalFromMonth(
        nutrient: StatisticsNutrientType,
        month: Int
    ): HashMap<Int, Int> {
        val monthString = getMonthStringFromInt(month)
        val days = dayDao.getAllDaysInMonth(monthString) as ArrayList<Day>
        val values = hashMapOf<Int, Int>()
        // add empty entries
        for (day in days) {
            val t = day.getTotalNutrient(nutrient).roundToInt()
            values[day.date.day.toIntOrNull() ?: 1] = t
        }
        return values
    }

    private fun getMonthStringFromInt(month: Int): String {
        val monthString = month.toString()
        val monthInt = month + 1
        return if (monthString.length == 1) "0".plus(monthInt) else monthInt.toString()
    }

}