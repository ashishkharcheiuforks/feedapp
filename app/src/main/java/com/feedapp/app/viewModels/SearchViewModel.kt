package com.feedapp.app.viewModels

import android.util.Log
import androidx.lifecycle.*
import com.feedapp.app.data.api.models.usdafoodsearch.SearchMealsResultModel
import com.feedapp.app.data.exceptions.NoInternetConnectionException
import com.feedapp.app.data.models.ColorGenerator
import com.feedapp.app.data.models.FoodProduct
import com.feedapp.app.data.models.RecentProduct
import com.feedapp.app.data.repositories.SearchFoodRepository
import com.feedapp.app.data.repositories.UserRepository
import com.feedapp.app.util.TAG
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

class SearchViewModel @Inject constructor(
    private val searchRepository: SearchFoodRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    val isConnected = MutableLiveData<Boolean>(true)
    // used to avoid multiple toast in one session
    val canShowNoInternetToast = MutableLiveData<Boolean>(true)
    val recentProducts = liveData(IO) { emit(userRepository.getRecentProducts()) }

    //true if user has searched at least once
    val hasSearched = MutableLiveData<Boolean>(false)
    private val recipeMediator = MediatorLiveData<SearchMealsResultModel>()

    val mealsOnline = MutableLiveData<SearchMealsResultModel>()
    val mealsOffline = MutableLiveData<List<FoodProduct>>()

    val isSearching = MutableLiveData<Boolean>(false)

    fun searchByQuery(query: String) {
        // if connected to the Internet, search through API, else in offline db
        if (isConnected.value == true) searchOnline(query)
        else searchOffline(query)

    }

    private fun searchOffline(query: String) {
        viewModelScope.launch(IO) {
            // get result from offline DB
            val searchResult = searchFromOfflineDB(query)
            hasSearched.postValue(true)
            mealsOffline.postValue(searchResult)
            isSearching.postValue(false)
        }
    }

    private fun searchOnline(query: String) {
        try {
            val source = LiveDataReactiveStreams.fromPublisher(
                searchRepository.searchByQuery(query)!!
                    .subscribeOn(Schedulers.io())
                    .doOnComplete {
                        hasSearched.postValue(true)
                        isSearching.postValue(false)
                    }
            )
            recipeMediator.addSource(source) {
                recipeMediator.postValue(it)
                recipeMediator.removeSource(source)
            }
        } catch (e: NoInternetConnectionException) {
            e.printStackTrace()
        } catch (e: Exception) {
            isSearching.postValue(false)
            Log.e(TAG, "Failed accessing API...")
            searchOffline(query)
            e.printStackTrace()
        }
    }


    fun observeRecipe(): MediatorLiveData<SearchMealsResultModel> {
        return recipeMediator
    }


    private suspend fun searchFromOfflineDB(query: String): List<FoodProduct> {
        return withContext(IO) {
            searchRepository.searchFromOfflineDB(query)
        }
    }


    fun generateColors(size: Int?): MutableList<Int> {
        if (size == null || size == 0) return mutableListOf()
        return ColorGenerator().generateColor(size)
    }


    /*
        return 5 last recent products
     */
    fun getRecentSublist(): List<RecentProduct> = recentProducts.value ?: listOf()

}