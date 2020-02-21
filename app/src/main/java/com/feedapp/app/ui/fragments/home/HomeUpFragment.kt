package com.feedapp.app.ui.fragments.home


import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider

import com.feedapp.app.R
import com.feedapp.app.databinding.FragmentHomeUpMenuBinding
import com.feedapp.app.viewModels.HomeViewModel
import dagger.android.support.DaggerFragment
import javax.inject.Inject

class HomeUpFragment @Inject constructor() : DaggerFragment() {

    @Inject
    lateinit var modelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: HomeViewModel
    private val animationDuration = 300L
    private lateinit var binding: FragmentHomeUpMenuBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        viewModel = activity?.run {
            ViewModelProvider(this, modelFactory).get(HomeViewModel::class.java)
        } ?: throw Exception("Invalid Activity")

        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_home_up_menu, container, false)
        binding.viewmodel = viewModel
        binding.lifecycleOwner = activity!!

        setObservers()

        return binding.root
    }


    private fun setObservers() {
        // when parameter changes - animate progress bar
        viewModel.caloriesProgress.observe(viewLifecycleOwner, Observer {
            viewModel.caloriesProgress.value?.apply {
                ObjectAnimator.ofInt(binding.fragmentHomeUpPBar, "progress", it)
                    .setDuration(animationDuration)
                    .start()
            }
        })

        viewModel.proteinsProgress.observe(viewLifecycleOwner, Observer {
            viewModel.proteinsProgress.value?.apply {
                ObjectAnimator.ofInt(binding.upMenuProteinsBar, "progress", it)
                    .setDuration(animationDuration)
                    .start()
            }
        })
        viewModel.carbsProgress.observe(viewLifecycleOwner, Observer {
            viewModel.carbsProgress.value?.apply {
                ObjectAnimator.ofInt(binding.upMenuCarbsBar, "progress", it)
                    .setDuration(animationDuration)
                    .start()
            }
        })

        viewModel.fatsProgress.observe(viewLifecycleOwner, Observer {
            viewModel.fatsProgress.value?.apply {
                ObjectAnimator.ofInt(binding.upMenuFatsBar, "progress", it)
                    .setDuration(animationDuration)
                    .start()
            }
        })
    }


}
