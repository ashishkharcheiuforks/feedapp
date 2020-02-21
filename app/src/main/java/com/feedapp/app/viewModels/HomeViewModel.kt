package com.feedapp.app.viewModels

import android.app.Application
import android.content.SharedPreferences
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.feedapp.app.R
import com.feedapp.app.data.models.BasicNutrientType
import com.feedapp.app.data.models.FragmentNavigationType
import com.feedapp.app.data.models.LeftNutrientCalculator
import com.feedapp.app.data.models.day.Day
import com.feedapp.app.data.repositories.DayRepository
import com.feedapp.app.data.repositories.UserRepository
import com.feedapp.app.util.DAY_FRAGMENTS_START_POSITION
import com.feedapp.app.util.SP_GUIDE_HOME
import com.feedapp.app.util.SP_INTRO_SHOWED
import com.feedapp.app.util.getDayDate
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject


/*
 *
 * ViewModel is shared by HomeActivity, HomeFragment, HomeDownFragment, HomeUpFragment and
 * DayFragment
 */

class HomeViewModel @Inject internal constructor(
    application: Application,
    private val dayRepository: DayRepository,
    private val userRepository: UserRepository,
    private val leftCalculator: LeftNutrientCalculator,
    private val sp: SharedPreferences
) : ViewModel() {

    private val left = "g left"
    private val calLeft = "cal left"
    val currentDate = MutableLiveData(dayRepository.getInitialDayDate())
    val caloriesLeft = MutableLiveData(0)
    val proteinsLeft = MutableLiveData(0)
    val calLeftOrOver = MutableLiveData(calLeft)
    val fatsLeftOrOver = MutableLiveData(left)
    val carbsLeftOrOver = MutableLiveData(left)
    val proteinsLeftOrOver = MutableLiveData(left)
    val fatsLeft = MutableLiveData(0)
    val carbsLeft = MutableLiveData(0)
    val caloriesProgress = MutableLiveData(0)
    val proteinsProgress = MutableLiveData(0)
    val carbsProgress = MutableLiveData(0)
    val fatsProgress = MutableLiveData(0)

    val isResettingDateOrSwiping = MutableLiveData(false)
    val currentDay: MutableLiveData<Day> = MutableLiveData()
    val currentBottomPosition = MutableLiveData(FragmentNavigationType.HOME)

    private val proteinsType = BasicNutrientType.PROTEINS
    private val fatsType = BasicNutrientType.FATS
    private val carbsType = BasicNutrientType.CARBS
    private val caloriesType = BasicNutrientType.CALORIES


    var proteinsNeeded: Int = 0
    var carbsNeeded: Int = 0
    var fatsNeeded: Int = 0
    var caloriesNeeded: Int = 0

    val currentPosition = MutableLiveData(DAY_FRAGMENTS_START_POSITION)

    val context = application.applicationContext!!

    internal fun introShowed() = sp.getBoolean(SP_INTRO_SHOWED, false)

    init {
        updateValues()
    }

    fun updateValues() {
        viewModelScope.launch(IO) {
            updateNeededValues()
            updateLeftValues()
        }

    }

    private suspend fun updateNeededValues() {
        val user = userRepository.getUser()
        user?.let {
            proteinsNeeded = user.proteinsNeeded
            carbsNeeded = user.carbsNeeded
            fatsNeeded = user.fatsNeeded
            caloriesNeeded = user.caloriesNeeded
        }

    }

    fun updateDayAndDate(position: Int = (currentPosition.value ?: DAY_FRAGMENTS_START_POSITION)) {
        if (isResettingDateOrSwiping.value != null
            && isResettingDateOrSwiping.value!!
        ) return
        viewModelScope.launch(IO) {
            val receivedDay = dayRepository.getPreviousOrNextDay(position)
            val receivedDate = dayRepository.getDate(position).getDayDate()
            withContext(Main) {
                currentPosition.value = (position)
                currentDay.value = receivedDay
                currentDate.value = receivedDate
            }
        }.invokeOnCompletion {
            updateLeftValues()
        }
    }

    private fun updateLeftValues() {
        updateCalories()
        updateProteins()
        updateCarbsLeft()
        updateFatsLeft()
    }


    private fun updateProteins() {
        val pair =
            leftCalculator.calculateAmount(
                proteinsNeeded,
                currentDay.value,
                proteinsType
            )
        proteinsLeft.postValue(pair.first)
        proteinsProgress.postValue(
            leftCalculator.calculateProgress(
                proteinsNeeded,
                currentDay.value,
                proteinsType
            )
        )
        if (pair.second) {
            proteinsLeftOrOver.postValue(context.getString(R.string.over))
        } else {
            proteinsLeftOrOver.postValue(context.getString(R.string.left))
        }
    }


    private fun updateCarbsLeft() {
        val pair = leftCalculator.calculateAmount(
            carbsNeeded,
            currentDay.value,
            carbsType
        )
        carbsLeft.postValue(pair.first)
        carbsProgress.postValue(
            leftCalculator.calculateProgress(
                carbsNeeded,
                currentDay.value,
                carbsType
            )
        )
        if (pair.second) {
            carbsLeftOrOver.postValue(context.getString(R.string.over))
        } else {
            carbsLeftOrOver.postValue(context.getString(R.string.left))
        }
    }

    private fun updateFatsLeft() {
        val pair = leftCalculator.calculateAmount(
            fatsNeeded,
            currentDay.value,
            fatsType
        )
        fatsLeft.postValue(pair.first)
        fatsProgress.postValue(
            leftCalculator.calculateProgress(
                fatsNeeded,
                currentDay.value,
                fatsType
            )
        )
        if (pair.second) {
            fatsLeftOrOver.postValue(context.getString(R.string.over))
        } else {
            fatsLeftOrOver.postValue(context.getString(R.string.left))
        }

    }

    private fun updateCalories() {
        val caloriesPair = leftCalculator.calculateAmount(
            caloriesNeeded,
            currentDay.value,
            caloriesType
        )
        caloriesLeft.postValue(caloriesPair.first)

        if (caloriesPair.second) {
            calLeftOrOver.postValue(context.getString(R.string.kcal_Over))
        } else {
            calLeftOrOver.postValue(context.getString(R.string.kcal_left))
        }

        caloriesProgress.postValue(
            leftCalculator.calculateProgress(
                caloriesNeeded,
                currentDay.value,
                caloriesType
            )
        )

    }

    fun resetDate() {
        currentPosition.value?.let {
            currentPosition.value = DAY_FRAGMENTS_START_POSITION
            currentDate.value = (dayRepository.getInitialDayDate())
        }
    }


    fun saveHomeUiGuideShowed() {
        sp.edit().putBoolean(SP_GUIDE_HOME, true).apply()
    }

    fun isHomeGuideShowed(): Boolean {
        return sp.getBoolean(SP_GUIDE_HOME, false)
    }

    /*
        method to get date to display in UI
     */
    fun getDateText(): CharSequence =
        "${currentDate.value?.day}.${currentDate.value?.month}"


}


