package com.feedapp.app.viewModels

import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.*
import com.feedapp.app.data.api.models.recipesearch.RecipeSearchModel
import com.feedapp.app.data.exceptions.NoInternetConnectionException
import com.feedapp.app.data.interfaces.SearchRecipeLoadException
import com.feedapp.app.data.models.day.MealType
import com.feedapp.app.data.repositories.RecipeSearchRepository
import com.feedapp.app.data.repositories.UserRepository
import com.feedapp.app.util.TAG
import com.feedapp.app.util.USER_RECIPES_SEARCHES_MAX
import com.feedapp.app.util.getDayDate
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*
import javax.inject.Inject
import kotlin.collections.HashSet

class RecipeSearchViewModel @Inject constructor(
    private val calendar: Calendar,
    private val sp: SharedPreferences,
    private val recipeSearchRepository: RecipeSearchRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    val isConnected = MutableLiveData(true)
    val isSearching = MutableLiveData(false)
    val hasSearched = MutableLiveData(false)
    val isRecipesEmpty = MutableLiveData(false)

    val searchRecipes = MutableLiveData<RecipeSearchModel>()
    val recipesBreakfast = MutableLiveData<RecipeSearchModel>()
    val recipesLunch = MutableLiveData<RecipeSearchModel>()
    val recipesSnack = MutableLiveData<RecipeSearchModel>()
    val recipesDinner = MutableLiveData<RecipeSearchModel>()
    var intolerance: HashSet<String>? = null
    var diet: HashSet<String>? = null

    init {
        updateIntoleranceAndDiet()
    }

    fun updateIntoleranceAndDiet() =
        viewModelScope.launch(IO) {
            val user = userRepository.getUser()
            user?.let {
                intolerance = user.intolerance
                diet = user.diet
            }
        }


    private fun searchRecipe(query: String, mealType: MealType) {
        try {
            val call = recipeSearchRepository.searchVP(query, intolerance, diet) ?: return

            call.enqueue(object : Callback<RecipeSearchModel?> {
                override fun onResponse(
                    call: Call<RecipeSearchModel?>,
                    response: Response<RecipeSearchModel?>
                ) {

                    if (response.code() == 200) {
                        setDataToRecipe(mealType, response)
                    } else {
                        Log.d(TAG, "Fail request data. Response code = ${response.code()}")
                    }
                }

                override fun onFailure(call: Call<RecipeSearchModel?>, t: Throwable) {
                    Log.d(TAG, "RecipeSearchViewModel:: Retrofit call failed")
                    t.printStackTrace()
                }
            })
        } catch (e: Exception) {
            Log.e("FeedApp", "Failed accessing API...")
            e.printStackTrace()
        }
    }

    private fun setDataToRecipe(mealType: MealType, response: Response<RecipeSearchModel?>) {
        when (mealType) {
            MealType.BREAKFAST -> {
                recipesBreakfast.postValue(response.body())
            }
            MealType.LUNCH -> {
                recipesLunch.postValue(response.body())
            }
            MealType.SNACK -> {
                recipesSnack.postValue(response.body())
            }
            MealType.DINNER -> {
                recipesDinner.postValue(response.body())
            }
        }
    }


    /*
        check if user hasn't searched too much to save API calls
     */
    fun ifRecipesLimitReached(): Boolean {
        val date = calendar.time.getDayDate()
        // check if user has searched less than max times
        val searchesInDay = sp.getInt(date.toJson(), 1)
        date.toJson()?.let { incrementSearches(it, searchesInDay) }
        if (searchesInDay <= USER_RECIPES_SEARCHES_MAX) {
            return false
        }
        return true

    }

    private fun incrementSearches(date: String, searchesInDay: Int) {
        sp.edit().putInt(date, searchesInDay + 1).apply()
    }


    fun searchRecipe(query: String, searchRecipeException: SearchRecipeLoadException) {
        try {
            if (isConnected.value == false) {
                throw NoInternetConnectionException()
            }
            val call = recipeSearchRepository.searchRecipes(query, intolerance, diet) ?: return

            call.enqueue(object : Callback<RecipeSearchModel?> {
                override fun onResponse(
                    call: Call<RecipeSearchModel?>,
                    response: Response<RecipeSearchModel?>
                ) {
                    isSearching.postValue(false)
                    if (response.code() == 200) {
                        searchRecipes.postValue(response.body())
                    } else {
                        throw Exception()
                    }
                }

                override fun onFailure(call: Call<RecipeSearchModel?>, t: Throwable) {
                    Log.d(TAG, "RecipeSearchViewModel:: Retrofit call failed")
                    t.printStackTrace()
                }
            })

        } catch (e: Exception) {
            searchRecipeException.onError()
            Log.e("FeedApp", "Failed accessing API...")
            e.printStackTrace()
        }
    }


    fun loadDefaultRecipes() {
        if (isConnected.value == true) {
            viewModelScope.launch(IO) {
                for (type in MealType.values()) searchRecipe(type.toString(), type)
            }
        }
    }

}

