/*
 * Copyright (c) 2020 Ruslan Potekhin
 */

package com.feedapp.app.viewModels

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.graphics.Color
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.feedapp.app.data.models.MonthEnum
import com.feedapp.app.data.models.Product
import com.feedapp.app.data.models.StatisticsNutrientType
import com.feedapp.app.data.models.day.Day
import com.feedapp.app.data.models.day.DayDate
import com.feedapp.app.data.repositories.DayRepository
import com.feedapp.app.data.repositories.StatisticsRepository
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject
import kotlin.math.roundToInt


/*
 *
 *  ViewModel is shared by StatisticsActivity, StatisticsMonthFragment and StatisticsDayFragment
 */

@SuppressLint("DefaultLocale")
class StatisticsViewModel @Inject constructor(
    application: Application,
    private val statisticsRepository: StatisticsRepository,
    private val dayRepository: DayRepository,
    calendar: Calendar
) : ViewModel() {

    var context: Context? = null

    private val monthPosition = MutableLiveData(calendar.get(Calendar.MONTH))
    private val currentYear = calendar.get(Calendar.YEAR)
    private val nutrient = MutableLiveData(StatisticsNutrientType.CALORIES)

    val barDataSet: MutableLiveData<BarDataSet> = MutableLiveData()
    val pieDataSet: MutableLiveData<PieDataSet> = MutableLiveData()
    val monthArrayList: MutableLiveData<ArrayList<String>> = MutableLiveData(getMonthArrayList())
    val nutrientArrayList: MutableLiveData<ArrayList<String>> =
        MutableLiveData(getNutrientArrayList())
    val products: MutableLiveData<ArrayList<Product>> = MutableLiveData(arrayListOf())
    // check if day data has been changed
    val dataChanged = MutableLiveData(false)


    private fun updateProducts(day: Day) {
        products.postValue(day.getAllProducts())
    }

    init {
        context = application.applicationContext
        setNewBarDataset(StatisticsNutrientType.CALORIES, getDefaultMonth())
    }


    private fun setNewPieDataset(day: Day) {
        val entriesPie = arrayListOf<PieEntry>()
        val proteins = day.getTotalNutrient(StatisticsNutrientType.PROTEINS)
        val fats = day.getTotalNutrient(StatisticsNutrientType.FATS)
        val carbs = day.getTotalNutrient(StatisticsNutrientType.CARBS)
        // return if empty
        if ((proteins + fats + carbs).roundToInt() == 0) return
        entriesPie.add(PieEntry(proteins, "Proteins"))
        entriesPie.add(PieEntry(fats, "Fats"))
        entriesPie.add(PieEntry(carbs, "Carbs"))
        val pieDataSetV = PieDataSet(entriesPie, "")
        pieDataSetV.valueTextSize = 16f
        pieDataSetV.colors = getPieColors()
        pieDataSet.postValue(pieDataSetV)
    }

    private fun getPieColors(): MutableList<Int> {
        return mutableListOf(
            Color.parseColor("#EC6B56"),
            Color.parseColor("#FFC154"),
            Color.parseColor("#47B39C")
        )
    }

    private fun getMonthArrayList(): ArrayList<String> {
        val a = arrayListOf<String>()
        for (month in MonthEnum.values()) a.add(month.toString())
        return a
    }

    private fun getNutrientArrayList(): ArrayList<String> {
        val a = arrayListOf<String>()
        for (type in StatisticsNutrientType.values()) a.add(type.toString())
        return a
    }

    fun getMonthDropdownInitialText(): String {
        return MonthEnum.values()[monthPosition.value ?: 0].toString()
    }


    fun getNutrientDropdownInitialText(): String {
        return nutrient.value?.toString() ?: StatisticsNutrientType.CALORIES.toString()
    }

    private fun getBarEntries(
        nutrientType: StatisticsNutrientType,
        monthEnum: MonthEnum
    ): MutableList<BarEntry> {
        val arr = arrayListOf<BarEntry>()
        val daysInMonth = getDaysFromCurrentMonth(monthEnum)
        // add empty entries
        // start from 1 to display columns correctly
        for (i in 1 until daysInMonth + 1) {
            arr.add(BarEntry(i.toFloat(), 0f))
        }
        // fill up with real days
        try {
            val filledDays =
                statisticsRepository.getNutrientTotalFromMonth(nutrientType, monthEnum.code)
            filledDays.forEach {
                arr[it.key - 1] = BarEntry(it.key.toFloat(), it.value.toFloat())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return arr
    }

    private fun getDaysFromCurrentMonth(monthEnum: MonthEnum): Int {
        val calendar = Calendar.getInstance()
        calendar.set(currentYear, monthEnum.code, 1)
        return calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    private fun setNewBarDataset(nutrientType: StatisticsNutrientType, monthEnum: MonthEnum) =
        viewModelScope.launch(IO) {
            nutrient.postValue(nutrientType)
            monthPosition.postValue(monthEnum.code)
            barDataSet.postValue(
                BarDataSet(
                    getBarEntries(nutrientType, monthEnum),
                    nutrientType.name
                )
            )
        }

    fun updateBarDataset(
        nutrientInt: Int = getDefaultStatisticsNutrientType().code,
        monthInt: Int = getDefaultMonth().code
    ) {
        val nutrientType = StatisticsNutrientType.values()[nutrientInt]
        val monthType = MonthEnum.values()[monthInt]
        setNewBarDataset(nutrientType, monthType)
    }


    private fun getDefaultStatisticsNutrientType(): StatisticsNutrientType {
        return StatisticsNutrientType.values()[nutrient.value?.code
            ?: StatisticsNutrientType.CALORIES.code]
    }

    private fun getDefaultMonth(): MonthEnum {
        return MonthEnum.values()[monthPosition.value ?: MonthEnum.JANUARY.code]
    }


    fun getNewPieData(date: DayDate?) =
        viewModelScope.launch(IO) {
            date ?: return@launch
            val day = dayRepository.getDayByDate(date) ?: return@launch
            setNewPieDataset(day)
            updateProducts(day)
        }


    fun deleteProduct(date: DayDate, product: Product) = viewModelScope.launch(IO) {
        // if product has been deleted, delete from it LiveData
        if (dayRepository.deleteProductFromDay(date, product)) {
            deleteProductFromProducts(product)
        }
    }

    private fun deleteProductFromProducts(product: Product) {
        try {
            val p = products.value
            p?.remove(product)
            products.postValue(p)
            if (products.value.isNullOrEmpty())
                pieDataSet.postValue(PieDataSet(listOf(), ""))
        } catch (e: IndexOutOfBoundsException) {
            e.printStackTrace()
        }
    }

    fun getDateToDisplay(dayDate: DayDate): CharSequence {
        return "${dayDate.day}/${dayDate.month}"
    }

}