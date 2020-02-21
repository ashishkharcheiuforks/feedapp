package com.feedapp.app.viewModels

import android.util.Log
import androidx.lifecycle.*
import com.feedapp.app.data.api.models.recipedetailed.nn.RecipeDetailedResponse
import com.feedapp.app.data.models.user.User
import com.feedapp.app.data.models.BasicNutrientType
import com.feedapp.app.data.models.day.MealType
import com.feedapp.app.data.models.MeasureType
import com.feedapp.app.data.repositories.DayRepository
import com.feedapp.app.data.repositories.RecipeSearchRepository
import com.feedapp.app.data.repositories.UserRepository
import com.feedapp.app.util.TAG
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject
import kotlin.collections.ArrayList
import kotlin.math.roundToInt

class DetailedRecipeViewModel @Inject constructor(
    private val recipeSearchRepository: RecipeSearchRepository,
    private val userRepository: UserRepository,
    private val dayRepository: DayRepository

) :
    ViewModel() {

    private val carbsNeeded = MutableLiveData(0)
    private val proteinsNeeded = MutableLiveData(0)
    private val fatsNeeded = MutableLiveData(0)
    val mealTypePosition = MutableLiveData(0)
    val measureSystem = MutableLiveData(MeasureType.METRIC)

    init {
        updateUserValues()
        mealTypePosition.postValue(getMealTypeByCurrentTime().code)
    }

    private fun updateUserValues() {
        fun update(user: User) {
            carbsNeeded.postValue(user.carbsNeeded)
            proteinsNeeded.postValue(user.proteinsNeeded)
            fatsNeeded.postValue(user.fatsNeeded)
            measureSystem.postValue(user.measureType)
        }
        viewModelScope.launch(IO) {
            val user = userRepository.getUser()
            user?.let { update(user) }
        }


    }


    // set dropdown item according to current time
    private fun getMealTypeByCurrentTime(): MealType {
        return when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 0..10 -> MealType.BREAKFAST
            in 11..14 -> MealType.LUNCH
            in 15..17 -> MealType.SNACK
            in 18..24 -> MealType.DINNER
            else -> MealType.BREAKFAST
        }
    }

    val isSearching = MutableLiveData(false)
    private val recipeDetailedMediator = MediatorLiveData<RecipeDetailedResponse>()
    val recipeDetailed = MutableLiveData<RecipeDetailedResponse>()

    // load details
    fun searchDetailedInfo(id: Int) {
        try {
            val source = recipeSearchRepository
                .searchDetailedInfo(id = id)?.let {
                    LiveDataReactiveStreams.fromPublisher(it.subscribeOn(Schedulers.io())
                        .doOnComplete {
                            isSearching.postValue(false)
                        })
                }
            source?.let { searchModel ->
                recipeDetailedMediator.addSource(searchModel) {
                    recipeDetailedMediator.postValue(it)
                    recipeDetailedMediator.removeSource(source)
                }
            }
        } catch (e: Exception) {
            Log.e("FeedApp", "Failed accessing API...")
            e.printStackTrace()
        }
    }


    fun observeRecipesDetailed(): MediatorLiveData<RecipeDetailedResponse> {
        return recipeDetailedMediator
    }

    fun checkCredits(credits: String?, sourceName: String?, sourceUrl: String?): String {
        return recipeSearchRepository.checkCredits(credits, sourceName, sourceUrl)
    }

    // get percentage of daily need of specific nutrient
    fun getDailyPercentage(type: BasicNutrientType): Pair<Int, Int> {
        var result: Float
        result = recipeDetailed.value?.nutrition?.getAmountByNutrient(type) ?: 0f
        when (type) {
            BasicNutrientType.CARBS ->
                result /= carbsNeeded.value?.toFloat() ?: 1f
            BasicNutrientType.PROTEINS ->
                result /= proteinsNeeded.value?.toFloat() ?: 1f
            BasicNutrientType.FATS ->
                result /= fatsNeeded.value?.toFloat() ?: 1f
            BasicNutrientType.CALORIES ->
                result = 0f
        }
        // get in percents
        result *= 100

        // percentage can be above 100
        val percentage = result
        if (result > 100f) result = 100f
        return Pair(result.roundToInt(), percentage.roundToInt())
    }

    // save recipe as consumed product
    fun trackRecipe(servings: Int?, mealType: Int?) =
        viewModelScope.launch(IO) {
            recipeDetailed.value?.let {
                dayRepository.saveRecipeToDay(
                    it,
                    servings ?: 1,
                    mealType ?: 0
                )
            }
        }


    fun isServingsCorrect(servings: String): Boolean {

        // accept only decimal
        fun onlyDigits(servings: String): Boolean {
            var onlyDigits = true
            for (i in servings.indices) {
                if (!Character.isDigit(servings[i])) {
                    onlyDigits = false
                    break
                }
            }
            return onlyDigits
        }

        // check if it's not 0 servings
        fun isOnlyZero(servings: String): Boolean {
            if ((servings.toIntOrNull() ?: 1) == 0) return true
            return false
        }
        return onlyDigits(servings) && !isOnlyZero(servings)
    }

    fun getMealList(): ArrayList<String> {
        return arrayListOf(
            MealType.BREAKFAST.toString(),
            MealType.LUNCH.toString(),
            MealType.SNACK.toString(),
            MealType.DINNER.toString()
        )
    }

    fun getDropDownInitialText(): String {
        return MealType.values()[mealTypePosition.value ?: 0].toString()
    }


}

