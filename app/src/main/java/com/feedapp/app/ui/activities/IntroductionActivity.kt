package com.feedapp.app.ui.activities

import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.feedapp.app.R
import com.feedapp.app.ui.fragments.intro.FirstIntroductionFragment
import com.feedapp.app.ui.fragments.intro.SecIntro
import com.feedapp.app.ui.fragments.intro.ThirdIntroductionFragment
import com.feedapp.app.viewModels.IntroductionViewModel
import com.github.paolorotolo.appintro.AppIntro
import dagger.android.AndroidInjection
import javax.inject.Inject


@Suppress("DEPRECATION")
class IntroductionActivity : AppIntro() {

    @Inject
    lateinit var modelFactory: ViewModelProvider.Factory
    private val introViewModel by lazy {
        ViewModelProvider(this, modelFactory).get(IntroductionViewModel::class.java)
    }
    private val fragFirst = FirstIntroductionFragment.newInstance()
    private val fragSecond = SecIntro.newInstance()
    private val fragThird = ThirdIntroductionFragment.newInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)

        setIntroSettings()
        setObservers()
        setStatusBar()

    }

    private fun setStatusBar() {
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        } else {
            window.statusBarColor = ContextCompat.getColor(this, R.color.black)
        }
    }

    private fun setIntroSettings() {
        addSlide(fragFirst)
        addSlide(fragSecond)
        addSlide(fragThird)

        showSkipButton(false)
        isProgressButtonEnabled = false
        setBarColor(resources.getColor(android.R.color.transparent))
        showSeparator(false)
        setIndicatorColor(
            resources.getColor(R.color.colorPrimary400),
            resources.getColor(R.color.colorPrimary800)
        )
        setFadeAnimation()
    }


    override fun onBackPressed() {
        return
    }

    private fun setObservers() {
        introViewModel.goal.observe(this, Observer {
            it?.let {
                pager.setCurrentItem(1, true)
            }
        })
        introViewModel.applied.observe(this, Observer {
            if (it) {
                pager.setCurrentItem(2, true)
            }
        })
    }


}
