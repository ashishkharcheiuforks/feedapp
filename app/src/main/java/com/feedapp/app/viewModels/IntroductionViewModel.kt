package com.feedapp.app.viewModels

import android.content.SharedPreferences
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.feedapp.app.data.models.user.User
import com.feedapp.app.data.models.CaloriesCalculator
import com.feedapp.app.data.models.CaloriesCalculator.Companion.CALORIES_MINIMUM
import com.feedapp.app.data.models.user.*
import com.feedapp.app.data.repositories.UserRepository
import com.feedapp.app.util.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import java.lang.NullPointerException
import javax.inject.Inject

class IntroductionViewModel @Inject internal constructor(
    private val userRepository: UserRepository,
    private val sp: SharedPreferences
) : ViewModel() {

    val goal = MutableLiveData(UserGoal.EAT_HEALTHIER)
    val weight = MutableLiveData<Int>(USER_BASIC_WEIGHT)
    val height = MutableLiveData<Int>(USER_BASIC_HEIGHT)
    val age = MutableLiveData<Int>(USER_BASIC_AGE)
    // false - woman, true - man
    val sex = MutableLiveData<Boolean>(USER_BASIC_SEX)
    val applied = MutableLiveData<Boolean>()
    val toastShown = MutableLiveData<Boolean>(false)
    val sexChosen = MutableLiveData<Boolean>(false)
    val activityChosen = MutableLiveData<Boolean>(false)

    private val activityLevel = MutableLiveData(UserActivityLevel.LIGHT)
    private val HEIGHT_MAXIMUM = 230
    private val WEIGHT_MAXIMUM = 350

    private fun saveIntroductionPassed() {
        sp.edit().putBoolean(SP_INTRO_SHOWED, true).apply()
    }

    private fun calculateCalories(): Int {
        return try {
            CaloriesCalculator().calculateCalories(
                true, weight.value, height.value,
                age.value, activityLevel.value,
                sex.value, goal.value
            )

        } catch (e: NullPointerException) {
            e.printStackTrace()
            CALORIES_MINIMUM
        }
    }

    fun saveUser() = viewModelScope.launch(IO) {
        if (valuesValid()) {
            val user = User(
                caloriesNeeded = calculateCalories(), proteinsNeeded = calculateProteins(),
                carbsNeeded = calculateCarbs(), fatsNeeded = calculateFats()
            )
            userRepository.insertUser(user)
            saveIntroductionPassed()
        }
    }

    private fun calculateProteins(): Int {
        val proteinsNormMultiplicand = 0.8
        val proteins = weight.value?.times(proteinsNormMultiplicand)?.toInt()
        return proteins ?: 0
    }

    private fun calculateCarbs(): Int {
        var carbsPercentage = 0.55
        if (goal.value != null) {
            // if goal is to lose weight, decrease carbs percentage
            if (goal.value == UserGoal.LOSE) carbsPercentage = 0.45
            // if goal is to gain weight, increase carbs percentage
            else if (goal.value == UserGoal.GAIN) carbsPercentage = 0.65
        }
        val carbs = (calculateCalories() * carbsPercentage) / 4
        return carbs.toInt()
    }

    private fun calculateFats(): Int {
        // 20 -35 % calories. 9 cal per gram
        var fatsPercentage = 0.27
        if (goal.value != null) {
            if (goal.value == UserGoal.LOSE) fatsPercentage = 0.22
            else if (goal.value == UserGoal.GAIN) fatsPercentage = 0.33
        }
        val carbs = (calculateCalories() * fatsPercentage) / 9
        return carbs.toInt()
    }

    private fun valuesValid(): Boolean {
        if (weight.value == null
            || activityLevel.value == null
            || height.value == null
            || sex.value == null
            || age.value == null
            || goal.value == null
        ) return false
        return true

    }

    fun canUnblockButton(length: Int, length1: Int, notEmpty: Boolean): Boolean {
        return length > 1 && length1 > 1 && notEmpty &&
                (activityChosen.value == true) && (sexChosen.value == true)
    }

    fun areHeightWeightValid(text: String, text1: String): Boolean {
        return text.toInt() > HEIGHT_MAXIMUM || text1.toInt() > WEIGHT_MAXIMUM
    }

    fun setActivityLevel(position: Int) {
        when (position) {
            0 -> {
                activityLevel.value = UserActivityLevel.LITTLE
            }
            1 -> {
                activityLevel.value = UserActivityLevel.LIGHT
            }
            2 -> {
                activityLevel.value = UserActivityLevel.MODERATE
            }
            3 -> {
                activityLevel.value = UserActivityLevel.HARD
            }
            4 -> {
                activityLevel.value = UserActivityLevel.VERY_HARD
            }
        }
    }

}
