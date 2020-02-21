package com.feedapp.app.data.models.day

import androidx.room.*
import com.feedapp.app.data.models.Product


// meal means breakfast, lunch, snack or dinner. It's collection of Products
// day - in Standard format like "10-12-2019" "dd-mm-yyyy"

@Entity(tableName = "meals")
class Meal constructor(
    @PrimaryKey(autoGenerate = true)
    override var id: Int = 0,
    override var products: ArrayList<Product> = arrayListOf(),
    override var mealType: MealType = MealType.BREAKFAST
) : AbstractMeal() {

    override fun toString(): String {
        return "id of the meal = $id | mealType = $mealType"
    }

}

