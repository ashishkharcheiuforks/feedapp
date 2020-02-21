package com.feedapp.app.data.databases.dbclasses

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.feedapp.app.data.databases.daos.DayDao
import com.feedapp.app.data.databases.daos.MealDao
import com.feedapp.app.data.databases.daos.ProductDao
import com.feedapp.app.data.databases.daos.UserDao
import com.feedapp.app.data.databases.converters.Converters
import com.feedapp.app.data.models.*
import com.feedapp.app.data.models.day.Day
import com.feedapp.app.data.models.day.Meal
import com.feedapp.app.data.models.user.User


@Database(
    entities = [User::class, Meal::class, FoodProduct::class, Product::class, Day::class, RecentProduct::class],
    version = 1
)
@TypeConverters(Converters::class)
abstract class UserDatabase : RoomDatabase() {

    abstract fun getUserDao(): UserDao
    abstract fun getMealDao(): MealDao
    abstract fun getProductDao(): ProductDao
    abstract fun getDayDao(): DayDao

    companion object {
        @Volatile
        private var instance: UserDatabase? = null
        private val LOCK = Any()

        operator fun invoke(context: Context) = instance
            ?: synchronized(LOCK) {
                instance
                    ?: buildDatabase(
                        context
                    ).also { instance = it }
            }

        private fun buildDatabase(context: Context) = Room.databaseBuilder(
            context.applicationContext, UserDatabase::class.java, "user.db"
        ).fallbackToDestructiveMigration().build()


    }


}