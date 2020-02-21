package com.feedapp.app.ui.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.feedapp.app.R
import com.feedapp.app.data.interfaces.MyProductsSearchCallback
import com.feedapp.app.data.models.FoodProduct
import com.feedapp.app.data.models.day.DayDate
import com.feedapp.app.ui.activities.DetailedFoodActivity
import com.feedapp.app.ui.viewholders.MyProductsSearchVH
import com.feedapp.app.util.intentDate
import com.feedapp.app.util.getValidLetter
import com.feedapp.app.util.intentMealType


@SuppressLint("SetTextI18n")
class MyProductsSearchRecyclerAdapter(private val myProductsSearchCallback: MyProductsSearchCallback) :
    ListAdapter<FoodProduct, MyProductsSearchVH>(DIFF_CALLBACK) {

    companion object {
        // color for image with letter
        val defaultColor = Color.rgb(253, 245, 230)
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<FoodProduct>() {
            override fun areItemsTheSame(oldItem: FoodProduct, newItem: FoodProduct): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: FoodProduct, newItem: FoodProduct): Boolean {
                return oldItem.id == newItem.id
            }

        }
    }


    override fun onBindViewHolder(holder: MyProductsSearchVH, position: Int) {
        val food = getItem(position)
        holder.textTitle.text = food.name
        holder.mainLayout.setOnClickListener {
            myProductsSearchCallback.startDetailedActivity(food.id, food.name)
        }

        // set Color to image
        holder.image.setColorFilter(defaultColor)

        // check letter
        val letter = food.name?.get(0).toString().getValidLetter()
        holder.imageLetter.text = letter
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MyProductsSearchVH {
        val holder = LayoutInflater.from(parent.context)
            .inflate(R.layout.vh_my_meals_search, parent, false)
        return MyProductsSearchVH(holder)
    }


}