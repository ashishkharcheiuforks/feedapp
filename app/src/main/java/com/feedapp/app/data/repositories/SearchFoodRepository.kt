package com.feedapp.app.data.repositories

import com.feedapp.app.data.api.interfaces.USDAApiServiceFood
import com.feedapp.app.data.api.models.usdafoodsearch.SearchMealsResultModel
import com.feedapp.app.data.api.models.usdafooddetailed.USDAFoodModel
import com.feedapp.app.data.databases.daos.FoodProductDao
import com.feedapp.app.data.models.FoodProduct
import io.reactivex.Flowable
import javax.inject.Inject

class SearchFoodRepository
@Inject constructor(
    private val searchApi: USDAApiServiceFood,
    private val foodProductDao: FoodProductDao
)
{
    fun searchFromOfflineDB(query: String):List<FoodProduct>{
        return foodProductDao.searchByName(query)
    }

    fun searchOfflineByExactName(name:String): FoodProduct {
        return foodProductDao.searchByNameExact(name)
    }


    fun searchByQuery(query: String)
            : Flowable<SearchMealsResultModel?>? {
        val body = HashMap<String, Any>()
        val searchOptions = HashMap<String, Any>()
        searchOptions["Survey (FNDDS)"] = true
        searchOptions["Foundation"] = true
        searchOptions["Branded"] = false
        body["includeDataTypes"] = searchOptions
        body["generalSearchInput"] = query

        return searchApi.getMealsByQuery(body = body)
    }

    fun getInfoAboutProduct(id:Int): Flowable<USDAFoodModel?>? {
        return searchApi.getMealsByQuery(id)
    }

    fun searchOffline(id: Int): FoodProduct? {
        return foodProductDao.searchById(id)
    }
}