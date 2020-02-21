package com.feedapp.app.viewModels

import android.content.SharedPreferences
import androidx.lifecycle.*
import com.feedapp.app.data.models.FoodProduct
import com.feedapp.app.data.repositories.FoodRepository
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import java.lang.Exception
import javax.inject.Inject

class MyMealsViewModel @Inject
internal constructor(
    private val foodRepository: FoodRepository,
    private val sp: SharedPreferences

) :
    ViewModel() {

    var myProducts = MutableLiveData(liveData { emit(foodRepository.getCustomProducts()) }.value)

    private val _textNoMeals = MutableLiveData<String>().apply {
        value = "There is no products yet"
    }
    val textNoMeals: LiveData<String> = _textNoMeals
    val isTextNoMealsVisible = MutableLiveData(false)
    val isProgressBarVisible = MutableLiveData(true)


    fun deleteCustomProduct(
        foodProduct: FoodProduct
    ) {
        viewModelScope.launch(IO) {
            myProducts.postValue(myProducts.value?.apply {
                try {
                    remove(foodProduct)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            })
            foodRepository.deleteProduct(foodProduct)
        }
    }

    fun refreshCustomProducts() {
        viewModelScope.launch(IO) {
            myProducts.postValue(foodRepository.getCustomProducts())
        }
    }

    fun isProductsEmpty(): Boolean {
        return myProducts.value != null && myProducts.value!!.isEmpty()
    }

    fun isProductsUiGuideShowed(): Boolean {
        return sp.getBoolean("productsUi", false)
    }

    fun saveProductsUiGuideShowed() {
        sp.edit().putBoolean("productsUi", true).apply()
    }

}