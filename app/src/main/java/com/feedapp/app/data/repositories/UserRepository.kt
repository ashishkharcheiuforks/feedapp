package com.feedapp.app.data.repositories

import com.feedapp.app.data.databases.daos.UserDao
import com.feedapp.app.data.models.user.User
import com.feedapp.app.data.models.MeasureType
import com.feedapp.app.data.models.RecentProduct
import javax.inject.Inject
import kotlin.collections.HashSet


class UserRepository @Inject internal constructor(
    private val userDao: UserDao
) {

    fun getCaloriesNeeded(): Int? = userDao.getCalories()

    fun insertUser(user: User) {
        userDao.insertUser(user)
    }

    suspend fun getUser(): User? {
        return userDao.getUser()
    }

    fun deleteUsers() {
        userDao.deleteAllUsers()
    }

    fun getRecentProducts() = userDao.getRecentProducts()

    // saving recent searched products to db
    fun saveToRecent(recentProduct: RecentProduct) {
        userDao.insertRecentProducts(recentProduct)
    }


    suspend fun saveMeasure(metric: MeasureType) {
        val user = userDao.getUser()
        user?.run {
            measureType = metric
            userDao.insertUser(user)
        }
    }

    fun saveIntolerance(hashSet: HashSet<String>) {
        val user = userDao.getUser()
        user?.run {
            intolerance = hashSet
            userDao.insertUser(user)
        }
    }

    fun saveDiet(hashSet: HashSet<String>) {
        userDao.getUser()?.apply {
            diet = hashSet
            userDao.insertUser(user = this)
        }
    }

    fun deleteAllUsers() {
        userDao.deleteAllUsers()
    }


}