package com.feedapp.app.viewModels

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.feedapp.app.data.databases.dbclasses.FoodDatabase
import com.feedapp.app.data.databases.dbclasses.UserDatabase
import com.feedapp.app.data.models.BasicNutrientType
import com.feedapp.app.data.models.DataResponseStatus
import com.feedapp.app.data.models.MeasureType
import com.feedapp.app.data.models.user.User
import com.feedapp.app.data.repositories.DayRepository
import com.feedapp.app.data.repositories.FoodRepository
import com.feedapp.app.data.repositories.UserRepository
import com.feedapp.app.util.SP_NAME_TAG
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    lateinit var userRepository: UserRepository
    lateinit var dayRepository: DayRepository
    lateinit var foodRepository: FoodRepository

    val calories = MutableLiveData(0)
    val proteins = MutableLiveData(0)
    val fats = MutableLiveData(0)
    val carbs = MutableLiveData(0)

    val nutrientValueStatus = MutableLiveData(DataResponseStatus.NONE)
    val measureSystem = MutableLiveData(MeasureType.METRIC)

    var context: Context? = null
    private val sp_caution_name = "ShouldShowCautionDialog"

    init {
        context = application.applicationContext
        loadDefaultValues(application)
    }

    private fun loadDefaultValues(application: Application) {
        viewModelScope.launch(IO) {
            val db = UserDatabase(application)
            val foodDb = FoodDatabase(application)
            userRepository =
                UserRepository(userDao = db.getUserDao())
            dayRepository = DayRepository(db.getDayDao())
            foodRepository = FoodRepository(application, foodDb.getProductDao())
            getDefaultValues()
        }
    }

    private suspend fun getDefaultValues() {
        val user: User? = userRepository.getUser()
        getCaloriesDefaultValue(user)
        getProteinsDefaultValue(user)
        getFatsDefaultValue(user)
        getCarbsDefaultValue(user)
        getMeasureSystemDefaultValue(user)

    }

    private fun getCaloriesDefaultValue(user: User?) {
        val caloriesNeeded = user?.caloriesNeeded ?: 0
        calories.postValue(caloriesNeeded)
    }

    private fun getMeasureSystemDefaultValue(user: User?) {
        measureSystem.postValue(user?.measureType ?: MeasureType.METRIC)
    }

    private fun getProteinsDefaultValue(user: User?) {
        val proteinsNeeded = user?.proteinsNeeded ?: 0
        proteins.postValue(proteinsNeeded)
    }

    private fun getFatsDefaultValue(user: User?) {
        val fatsNeeded = user?.fatsNeeded ?: 0
        fats.postValue(fatsNeeded)
    }

    private fun getCarbsDefaultValue(user: User?) {
        val carbsNeeded = user?.carbsNeeded ?: 0
        carbs.postValue(carbsNeeded)
    }

    fun saveNewValue(newValueToSave: Any?, type: BasicNutrientType) {
        viewModelScope.launch(IO) {
            var oldValue = 0
            val valueString = newValueToSave.toString()
            try {
                for (c in valueString) if (!c.isDigit()) return@launch

                val newValue = valueString.toInt()
                val user = userRepository.getUser() ?: return@launch
                oldValue = when (type) {
                    BasicNutrientType.CALORIES -> user.caloriesNeeded
                    BasicNutrientType.PROTEINS -> user.proteinsNeeded
                    BasicNutrientType.FATS -> user.fatsNeeded
                    BasicNutrientType.CARBS -> user.carbsNeeded
                }
                if (newValue !in 1..10000) throw IllegalArgumentException()
                when (type) {
                    BasicNutrientType.CALORIES -> {
                        user.caloriesNeeded = newValue
                        calories.postValue(newValue)
                    }
                    BasicNutrientType.PROTEINS -> {
                        user.proteinsNeeded = newValue
                        proteins.postValue(newValue)
                    }
                    BasicNutrientType.FATS -> {
                        user.fatsNeeded = newValue
                        fats.postValue(newValue)
                    }
                    BasicNutrientType.CARBS -> {
                        user.carbsNeeded = newValue
                        carbs.postValue(newValue)
                    }
                }
                userRepository.insertUser(user)
                nutrientValueStatus.postValue(DataResponseStatus.SUCCESS)

            } catch (e: Exception) {
                when (type) {
                    BasicNutrientType.PROTEINS -> {
                        proteins.postValue(oldValue)
                    }
                    BasicNutrientType.FATS -> {
                        fats.postValue(oldValue)
                    }
                    BasicNutrientType.CARBS -> {
                        carbs.postValue(oldValue)
                    }
                    BasicNutrientType.CALORIES -> {
                        calories.postValue(oldValue)
                    }
                }
                nutrientValueStatus.postValue(DataResponseStatus.FAILED)
                e.printStackTrace()
            }
        }
    }

    fun saveMeasure(newValue: Any) = viewModelScope.launch(IO) {
        try {
            if (newValue.toString() == "metric") {
                userRepository.saveMeasure(MeasureType.METRIC)
                measureSystem.postValue(MeasureType.METRIC)
            } else if (newValue.toString() == "us") {
                userRepository.saveMeasure(MeasureType.US)
                measureSystem.postValue(MeasureType.US)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    fun deleteAllData() = viewModelScope.launch(IO) {
        try {
            (context?.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager)
                .clearApplicationUserData()
        } catch (e: Exception) {
            e.printStackTrace()
            deleteAllSharedPrefs()
            userRepository.deleteUsers()
            dayRepository.deleteAllDays()
            foodRepository.deleteAllCustomProducts()
        }
    }


    fun saveIntolerance(newValue: Any?) {
        if (newValue == null || newValue !is HashSet<*>) return
        viewModelScope.launch(IO) {
            try {
                val hashSet = newValue as HashSet<String>
                userRepository.saveIntolerance(hashSet)
                nutrientValueStatus.postValue(DataResponseStatus.SUCCESS)
            } catch (e: java.lang.Exception) {
                nutrientValueStatus.postValue(DataResponseStatus.FAILED)
                e.printStackTrace()
            }
        }
    }

    fun saveDiet(newValue: Any?) {
        if (newValue == null || newValue !is HashSet<*>) return
        viewModelScope.launch(IO) {
            try {
                val hashSet = newValue as HashSet<String>
                userRepository.saveDiet(hashSet)
                nutrientValueStatus.postValue(DataResponseStatus.SUCCESS)
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
                nutrientValueStatus.postValue(DataResponseStatus.FAILED)
            }
        }
    }

    fun shouldShowCautionDialog(): Boolean {
        var show = false
        context?.let {
            val sharedPreference = it.getSharedPreferences(SP_NAME_TAG, Context.MODE_PRIVATE)
            show = sharedPreference.getBoolean(sp_caution_name, true)
        }
        return show
    }

    fun saveShowedCautionDialog() {
        context?.let {
            val sharedPreference = it.getSharedPreferences(SP_NAME_TAG, Context.MODE_PRIVATE)
            val editor = sharedPreference.edit()
            editor.putBoolean(sp_caution_name, false)
            editor.apply()
        }
    }



    private fun deleteAllSharedPrefs() =
        context?.getSharedPreferences(SP_NAME_TAG, Context.MODE_PRIVATE)?.edit()?.clear()?.apply()

}
