package com.feedapp.app.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import com.feedapp.app.data.repositories.FoodRepository
import kotlinx.coroutines.Dispatchers.IO
import javax.inject.Inject

class MyMealsSearchViewModel @Inject constructor(private val foodRepository: FoodRepository) :
    ViewModel() {

    val myProducts = liveData(IO) { emit(foodRepository.getCustomProducts()) }

}