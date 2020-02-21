package com.feedapp.app.data.repositories

import android.content.Context
import com.feedapp.app.data.databases.daos.FoodProductDao
import com.feedapp.app.data.models.FoodProduct
import javax.inject.Inject

class FoodRepository @Inject constructor(
    val context: Context,
    private val foodProductDao: FoodProductDao
) {

    fun searchById(id: Int): FoodProduct? {
        return foodProductDao.searchById(id)
    }

    fun searchByName(name: String): List<FoodProduct> {
        return foodProductDao.searchByName(name)
    }

    fun insertProduct(product: FoodProduct) {
        foodProductDao.insertProduct(product)
    }

    fun getSize(): Int {
        return foodProductDao.getSize()
    }

    fun deleteProduct(product: FoodProduct) {
        foodProductDao.deleteProduct(product)
    }

    fun getLastIndex(): Int {
        return foodProductDao.getSize()
    }

    fun getCustomProducts(): ArrayList<FoodProduct> {
        return foodProductDao.getCustomProducts() as ArrayList<FoodProduct>
    }

    fun deleteAllCustomProducts() {
        foodProductDao.deleteCustomProducts()
    }
}