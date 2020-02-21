/*
 * Copyright (c) 2020 Ruslan Potekhin
 */

package com.feedapp.app.ui.activities

import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import androidx.core.content.res.ResourcesCompat
import androidx.core.widget.addTextChangedListener
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.feedapp.app.R
import com.feedapp.app.data.models.ColorGenerator
import com.feedapp.app.data.models.ConnectionMode
import com.feedapp.app.data.models.RegexDescriptionChecker
import com.feedapp.app.data.models.day.DayDate
import com.feedapp.app.databinding.ActivityDetailedFoodBinding
import com.feedapp.app.util.hideKeyboard
import com.feedapp.app.util.intentDate
import com.feedapp.app.util.intentMealType
import com.feedapp.app.viewModels.DetailedViewModel
import kotlinx.android.synthetic.main.activity_detailed_food.*
import java.math.RoundingMode
import javax.inject.Inject

class DetailedFoodActivity @Inject constructor() : ClassicActivity() {

    @Inject
    lateinit var modelFactory: ViewModelProvider.Factory
    private val viewModel by lazy {
        ViewModelProvider(this, modelFactory).get(DetailedViewModel::class.java)
    }
    private val regexDescriptionChecker = RegexDescriptionChecker()
    private lateinit var binding: ActivityDetailedFoodBinding

    companion object {
        private const val nutritionInformationScale = 1
    }

    private var connectionMode: ConnectionMode? = ConnectionMode.ONLINE
    private val dropdownMultipliers = arrayListOf(1.0)
    var title: String? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStatusBar()
        binding = DataBindingUtil.setContentView(this, R.layout.activity_detailed_food)
        binding.viewmodel = viewModel
        binding.lifecycleOwner = this

        prepareDetailedData()
        setUpLayout()
        setListeners()
        setObservers()
    }

    private fun loadDetailedData(fdcId: Int?, foodProductId: Int?) {
        // get data about specific product from API
        when (connectionMode) {
            ConnectionMode.ONLINE -> {
                fdcId?.let {
                    viewModel.isSearching.postValue(true)
                    viewModel.getInfoAboutProduct(it)
                }
            }
            ConnectionMode.OFFLINE -> {
                foodProductId?.let { viewModel.searchFoodProduct(it) }
            }
        }
    }


    private fun prepareDetailedData() {
        connectionMode = intent.extras?.getSerializable("connectionMode") as? ConnectionMode
        when (connectionMode) {
            ConnectionMode.ONLINE -> {
                val fdcId = intent.extras?.getInt("fdcId")
                loadDetailedData(fdcId, null)

            }
            ConnectionMode.OFFLINE -> {
                val foodProductId = intent.extras?.getInt("id")
                loadDetailedData(null, foodProductId)
            }
            null -> {
                val foodProductId: Int? = intent.extras?.getInt("id")
                connectionMode = ConnectionMode.ONLINE
                loadDetailedData(null, foodProductId)
            }
        }
        title = intent.extras?.getString("title").toString()
    }

    private fun setUpLayout() {
        val quantityList = arrayListOf<String>()
        val spinner = findViewById<AutoCompleteTextView>(R.id.detailed_quantity_dropdown)
        val adapter = ArrayAdapter(
            applicationContext, R.layout.spinner_default,
            quantityList
        )

        // set action bar
        setSupportActionBar(binding.activityDetailedToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        // set spinner
        quantityList.add("grams")
        spinner.setAdapter(adapter)
        spinner.setText(spinner.adapter.getItem(0).toString(), false)
        spinner.setOnItemClickListener { _, _, position, _ ->
            viewModel.multiplier.postValue(dropdownMultipliers[position])
        }
        // remove white space in the bottom of dropdown menu
        spinner.setDropDownBackgroundResource(R.drawable.white_background)

        //set collapsing toolbar
        title?.let { setUpCollapsingBar(it) }
    }

    private fun setListeners() {
        binding.activityDetailedToolbar.setNavigationOnClickListener {
            onBackPressed()
        }

        val editText = findViewById<EditText>(R.id.detailed_quantity_edit)
        editText.addTextChangedListener {
            if (it.isNullOrBlank()) {
                return@addTextChangedListener
            }
            if (connectionMode == ConnectionMode.ONLINE) {
                updateValuesApi()
            } else {
                updateValuesOffline()
            }
        }

        // hide keyboard if user presses OK
        editText.setOnKeyListener(View.OnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP) {
                editText.clearFocus()
                this.hideKeyboard()
                return@OnKeyListener true
            }
            false
        })

        // change multiplier according to dropdown menu
        binding.detailedQuantityDropdown.setOnItemClickListener { _, _, position, _ ->
            if (!editText.error.isNullOrEmpty()) editText.error = null
            if (position != 0 && !editText.text.isNullOrBlank() && editText.text.toString().toDouble() > 10) {
                // change editText's value to 1 if not grams
                editText.setText("1")
            }
            viewModel.multiplierPosition.value = position
            viewModel.multiplier.postValue(dropdownMultipliers[position])
            hideKeyboard()
        }


        binding.detailedAddProduct.setOnClickListener { view ->
            hideKeyboard()
            val text = editText.text
            if (text.isNullOrBlank() || text.toString() == "0") {
                editText.error = getString(R.string.error_0)
                return@setOnClickListener
            } else if (!viewModel.isMultiplierValueValid(text.toString())) {
                editText.error = getString(R.string.error_too_big)
                return@setOnClickListener
            } else if (viewModel.canSave()) {
                // if everything with numbers is ok, save to db
                view.isClickable = false
                var grams = editText.text.toString().toFloat()
                viewModel.multiplier.value?.let { grams *= it.toFloat() }
                saveConsumedFoodToDB(grams)
            }

        }
    }

    private fun saveConsumedFoodToDB(grams: Float) {
        val dateString: DayDate? = intent.extras?.getSerializable(intentDate) as DayDate?
        val mealType: Int? = intent.extras?.getInt(intentMealType)
        viewModel.saveConsumedFoodToDB(dateString, mealType, grams).invokeOnCompletion {
            finish()
        }

    }

    private fun setObservers() {

        viewModel.multiplier.observe(this, Observer {
            updateValuesApi()
        })

        viewModel.observeFoodInfo().observe(this, Observer {
            viewModel.foodInfoOnline.postValue(it)
        })

        viewModel.foodInfoOnline.observe(this, Observer {
            updateValuesApi()
        })

        if (connectionMode == ConnectionMode.OFFLINE) {
            viewModel.foodInfoOffline.observe(this, Observer {
                updateValuesOffline()
            })

        }


    }

    private fun updateValuesOffline() {
        if (binding.detailedQuantityEdit.text.isNullOrEmpty()) return
        // get model from LiveData
        val foodInfo = viewModel.foodInfoOffline.value
        val multiplier = viewModel.multiplier.value
        if (foodInfo != null && multiplier != null) {
            try {
                var multiplierBD = (multiplier / 100).toBigDecimal()
                multiplierBD *= detailed_quantity_edit.text.toString().toBigDecimal()

                val protein =
                    foodInfo.protein?.toBigDecimal()?.times(multiplierBD)
                        ?.setScale(nutritionInformationScale, RoundingMode.HALF_UP)
                        .toString()
                        .plus(getString(R.string.g))
                val fats =
                    foodInfo.fat?.toBigDecimal()?.times(multiplierBD)
                        ?.setScale(nutritionInformationScale, RoundingMode.HALF_UP).toString()
                        .plus(getString(R.string.g))

                val carbs =
                    foodInfo.carbs?.toBigDecimal()?.times(multiplierBD)
                        ?.setScale(nutritionInformationScale, RoundingMode.HALF_UP).toString()
                        .plus(getString(R.string.g))

                val calories =
                    foodInfo.calories().toBigDecimal().times(multiplierBD)
                        .setScale(nutritionInformationScale, RoundingMode.HALF_UP).toString()

                val sugars =
                    foodInfo.sugar?.toBigDecimal()?.times(multiplierBD)
                        ?.setScale(nutritionInformationScale, RoundingMode.HALF_UP).toString()
                        .plus(getString(R.string.g))
                val fiber =
                    foodInfo.total_dietary_fibre?.toBigDecimal()?.times(multiplierBD)
                        ?.setScale(nutritionInformationScale, RoundingMode.HALF_UP).toString()
                        .plus(getString(R.string.g))
                val fats_s =
                    foodInfo.sat_fats_g?.toBigDecimal()?.times(multiplierBD)
                        ?.setScale(nutritionInformationScale, RoundingMode.HALF_UP).toString()
                        .plus(getString(R.string.g))
                val fats_m =
                    foodInfo.mono_fats_g?.toBigDecimal()?.times(multiplierBD)
                        ?.setScale(nutritionInformationScale, RoundingMode.HALF_UP).toString()
                        .plus(getString(R.string.g))
                val fats_p =
                    foodInfo.o_poly_fats_g?.toBigDecimal()?.times(multiplierBD)
                        ?.setScale(nutritionInformationScale, RoundingMode.HALF_UP).toString()
                        .plus(getString(R.string.g))

                binding.detailedProteinsValue.text = protein
                binding.detailedNutritionProteinsValue.text = protein

                binding.detailedFatsValue.text = fats
                binding.detailedNutritionFatsValue.text = fats

                binding.detailedCarbsValue.text = carbs
                binding.detailedNutritionCarbsValue.text = carbs

                binding.detailedKcalValue.text = calories
                binding.detailedNutritionCaloriesValue.text = calories

                binding.detailedNutritionSugarsValue.text = sugars
                binding.detailedNutritionFiberValue.text = fiber
                binding.detailedNutritionCholesterolValue.text = "N/A"
                binding.detailedNutritionSValue.text = fats_s
                binding.detailedNutritionMonoValue.text = fats_m
                binding.detailedNutritionPolyValue.text = fats_p

                val quantityList = arrayListOf<String>()

                // default value for every entry
                quantityList.add("grams")

                binding.detailedQuantityDropdown.setAdapter(
                    ArrayAdapter(applicationContext, R.layout.spinner_default, quantityList)
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun updateValuesApi() {
        if (binding.detailedQuantityEdit.text.isNullOrEmpty()) return
        // get model from liveData
        val foodInfo = viewModel.foodInfoOnline.value
        // get multiplier with respect to 100 grams
        val multiplier = viewModel.multiplier.value
        if (foodInfo != null && multiplier != null) {
            try {
                var multiplierBD = (multiplier / 100).toBigDecimal()
                multiplierBD *= detailed_quantity_edit.text.toString().toBigDecimal()

                val protein =
                    foodInfo.foodNutrients?.find { it.nutrient?.name == getString(R.string.Protein) }
                        ?.amount?.toBigDecimal()?.times(multiplierBD)
                        ?.setScale(nutritionInformationScale, RoundingMode.HALF_UP).toString()
                        .plus(getString(R.string.g))
                val fats =
                    foodInfo.foodNutrients?.find { it.nutrient?.name == "Total lipid (fat)" }
                        ?.amount?.toBigDecimal()?.times(multiplierBD)
                        ?.setScale(nutritionInformationScale, RoundingMode.HALF_UP).toString()
                        .plus(getString(R.string.g))
                val carbs =
                    foodInfo.foodNutrients?.find { it.nutrient?.name == "Carbohydrate, by difference" }
                        ?.amount?.toBigDecimal()?.times(multiplierBD)
                        ?.setScale(nutritionInformationScale, RoundingMode.HALF_UP).toString()
                        .plus(getString(R.string.g))

                val calories =
                    foodInfo.foodNutrients?.find { it.nutrient?.name == "Energy" }
                        ?.amount?.toBigDecimal()?.times(multiplierBD)
                        ?.setScale(nutritionInformationScale, RoundingMode.HALF_UP).toString()

                val sugars =
                    foodInfo.foodNutrients?.find { it.nutrient?.name == "Sugars, total including NLEA" }
                        ?.amount?.toBigDecimal()?.times(multiplierBD)
                        ?.setScale(nutritionInformationScale, RoundingMode.HALF_UP).toString()
                        .plus(getString(R.string.g))

                val fiber =
                    foodInfo.foodNutrients?.find { it.nutrient?.name == "Fiber, total dietary" }
                        ?.amount?.toBigDecimal()?.times(multiplierBD)
                        ?.setScale(nutritionInformationScale, RoundingMode.HALF_UP).toString()
                        .plus(getString(R.string.g))

                val cholesterol =
                    foodInfo.foodNutrients?.find { it.nutrient?.name == "Cholesterol" }
                        ?.amount?.toBigDecimal()?.times(multiplierBD)
                        ?.setScale(nutritionInformationScale, RoundingMode.HALF_UP).toString()
                        .plus(getString(R.string.mg))

                val fats_s =
                    foodInfo.foodNutrients?.find { it.nutrient?.name == "Fatty acids, total saturated" }
                        ?.amount?.toBigDecimal()?.times(multiplierBD)
                        ?.setScale(nutritionInformationScale, RoundingMode.HALF_UP).toString()
                        .plus(getString(R.string.g))

                val fats_m =
                    foodInfo.foodNutrients?.find { it.nutrient?.name == "Fatty acids, total monounsaturated" }
                        ?.amount?.toBigDecimal()?.times(multiplierBD)
                        ?.setScale(nutritionInformationScale, RoundingMode.HALF_UP).toString()
                        .plus(getString(R.string.g))

                val fats_p =
                    foodInfo.foodNutrients?.find { it.nutrient?.name == "Fatty acids, total polyunsaturated" }
                        ?.amount?.toBigDecimal()?.times(multiplierBD)
                        ?.setScale(nutritionInformationScale, RoundingMode.HALF_UP).toString()
                        .plus(getString(R.string.g))


                binding.detailedProteinsValue.text = protein
                binding.detailedNutritionProteinsValue.text = protein

                binding.detailedFatsValue.text = fats
                binding.detailedNutritionFatsValue.text = fats

                binding.detailedCarbsValue.text = carbs
                binding.detailedNutritionCarbsValue.text = carbs

                binding.detailedKcalValue.text = calories
                binding.detailedNutritionCaloriesValue.text = calories

                binding.detailedNutritionSugarsValue.text = sugars
                binding.detailedNutritionFiberValue.text = fiber
                binding.detailedNutritionCholesterolValue.text = cholesterol
                binding.detailedNutritionSValue.text = fats_s
                binding.detailedNutritionMonoValue.text = fats_m
                binding.detailedNutritionPolyValue.text = fats_p

                val quantityList = arrayListOf<String>()

                // default value for every entry
                quantityList.add("grams")

                foodInfo.foodPortions?.forEach {
                    it.portionDescription?.let { description ->
                        // skip unspecified portions
                        if (!description.contains("not specified")) {
                            if (it.gramWeight != null) dropdownMultipliers.add(it.gramWeight)
                            else return@let
                            quantityList.add(
                                regexDescriptionChecker
                                    .checkValidity(description).plus("\u0020(${it.gramWeight}g)")
                            )
                        }

                    }
                }
                binding.detailedQuantityDropdown.setAdapter(
                    ArrayAdapter(
                        applicationContext, R.layout.spinner_default,
                        quantityList
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

    }


    private fun setUpCollapsingBar(title: String) = try {
        binding.activityDetailedCollapsingToolbar.apply {
            this.title = title
            setCollapsedTitleTextColor(ResourcesCompat.getColor(resources, R.color.black, theme))
            setExpandedTitleColor(
                ResourcesCompat.getColor(
                    resources,
                    R.color.colorText_desc,
                    theme
                )
            )
            setBackgroundColor(ColorGenerator().getColorWithAlpha())
            setExpandedTitleTextAppearance(R.style.CollapsingToolbar_Title)
        }
    } catch (e: java.lang.Exception) {
        e.printStackTrace()
    }

}
