package com.feedapp.app.ui.fragments.home

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.feedapp.app.R
import com.feedapp.app.data.models.BasicNutrientType
import com.feedapp.app.data.models.DataResponseStatus
import com.feedapp.app.data.models.MeasureType
import com.feedapp.app.ui.activities.IntroductionActivity
import com.feedapp.app.util.toast
import com.feedapp.app.viewModels.SettingsViewModel

class SettingsFragment : PreferenceFragmentCompat() {

    private lateinit var viewModel: SettingsViewModel
    private var caloriesPreference: EditTextPreference? = null
    private var proteinsPreference: EditTextPreference? = null
    private var carbsPreference: EditTextPreference? = null
    private var fatsPreference: EditTextPreference? = null
    private var metricPreference: ListPreference? = null
    private var deletePreference: Preference? = null
    private var intolerancePreference: Preference? = null
    private var dietPreference: Preference? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity?.let {
            viewModel = ViewModelProvider(this).get(SettingsViewModel::class.java)
        }
        caloriesPreference = preferenceManager.findPreference("calories")
        carbsPreference = preferenceManager.findPreference("carbs")
        proteinsPreference = preferenceManager.findPreference("proteins")
        fatsPreference = preferenceManager.findPreference("fats")
        metricPreference = preferenceManager.findPreference("measure")
        deletePreference = preferenceManager.findPreference("delete")
        intolerancePreference = preferenceManager.findPreference("intolerance")
        dietPreference = preferenceManager.findPreference("diet")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpView()
    }

    private fun setUpView() {
        setUpObservers()
        setUpListeners()
    }

    private fun setUpListeners() {

        intolerancePreference?.setOnPreferenceChangeListener { _, newValue ->
            viewModel.saveIntolerance(newValue)
            if (viewModel.shouldShowCautionDialog()) showCautionDialog()
            return@setOnPreferenceChangeListener true
        }

        dietPreference?.setOnPreferenceChangeListener { _, newValue ->
            viewModel.saveDiet(newValue)
            if (viewModel.shouldShowCautionDialog()) showCautionDialog()
            return@setOnPreferenceChangeListener true
        }

        deletePreference?.setOnPreferenceClickListener {
            // show dialog
            activity?.let {
                AlertDialog.Builder(activity!!)
                    .setTitle(getString(R.string.delete_all))
                    .setMessage(getString(R.string.dialog_delete_data))
                    .setPositiveButton(getString(R.string.delete)) { _, _ ->
                        viewModel.deleteAllData()
                            // when deleted, start intro activity
                            .invokeOnCompletion {
                                val introIntent = Intent(activity, IntroductionActivity::class.java)
                                startActivity(introIntent)
                            }
                    }
                    .setNegativeButton(R.string.cancel, null)
                    .show()
            }
            true
        }
        // measure
        metricPreference?.setOnPreferenceChangeListener { _, newValue ->
            viewModel.saveMeasure(newValue)
            true
        }
        //calories
        caloriesPreference?.setOnBindEditTextListener {
            it.inputType = InputType.TYPE_CLASS_NUMBER
            it.selectAll()
        }
        caloriesPreference?.setOnPreferenceChangeListener { _, newValue ->
            viewModel.saveNewValue(newValue, BasicNutrientType.CALORIES)
            true
        }

        //proteins
        proteinsPreference?.setOnBindEditTextListener {
            it.inputType = InputType.TYPE_CLASS_NUMBER
            it.selectAll()
        }
        proteinsPreference?.setOnPreferenceChangeListener { _, newValue ->
            viewModel.saveNewValue(newValue, BasicNutrientType.PROTEINS)
            true
        }

        //carbs
        carbsPreference?.setOnBindEditTextListener {
            it.inputType = InputType.TYPE_CLASS_NUMBER
            it.selectAll()
        }
        carbsPreference?.setOnPreferenceChangeListener { _, newValue ->
            viewModel.saveNewValue(newValue, BasicNutrientType.CARBS)
            true
        }

        //fats
        fatsPreference?.setOnBindEditTextListener {
            it.inputType = InputType.TYPE_CLASS_NUMBER
            it.selectAll()
        }
        fatsPreference?.setOnPreferenceChangeListener { _, newValue ->
            viewModel.saveNewValue(newValue, BasicNutrientType.FATS)
            true
        }


    }

    private fun showCautionDialog() {
        activity?.let {
            AlertDialog.Builder(activity!!)
                .setTitle(getString(R.string.dialog_caution_diet_title))
                .setMessage(getString(R.string.dialog_caution_diet_message))
                .setPositiveButton(getString(R.string.ok))
                { _, _ -> viewModel.saveShowedCautionDialog() }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }
    }


    private fun setUpObservers() {

        viewModel.measureSystem.observe(viewLifecycleOwner, Observer { measureType ->
            when (measureType) {
                MeasureType.METRIC -> metricPreference?.setValueIndex(0)
                MeasureType.US -> metricPreference?.setValueIndex(1)
                else -> {
                }
            }
        })

        viewModel.nutrientValueStatus.observe(viewLifecycleOwner, Observer {
            when (it) {
                DataResponseStatus.SUCCESS -> {
                    toastSuccess()
                    viewModel.nutrientValueStatus.postValue(DataResponseStatus.NONE)
                }
                DataResponseStatus.FAILED -> {
                    toastFail()
                    viewModel.nutrientValueStatus.postValue(DataResponseStatus.NONE)
                }
                else -> {
                }
            }
        })

        // calories
        viewModel.calories.observe(viewLifecycleOwner, Observer {
            caloriesPreference?.text = it?.toString() ?: ""
        })

        // proteins
        viewModel.proteins.observe(viewLifecycleOwner, Observer {
            proteinsPreference?.text = it?.toString() ?: ""
        })

        // proteins
        viewModel.carbs.observe(viewLifecycleOwner, Observer {
            carbsPreference?.text = it?.toString() ?: ""
        })

        // proteins
        viewModel.fats.observe(viewLifecycleOwner, Observer {
            fatsPreference?.text = it?.toString() ?: ""
        })

    }

    private fun toastSuccess() {
        activity?.toast("Saved successfully!");
    }

    private fun toastFail() {
        activity?.toast("Failed to save")
    }

}
