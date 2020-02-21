package com.feedapp.app.di.modules.viewModelModules

import androidx.lifecycle.ViewModel
import com.feedapp.app.di.other.ViewModelKey
import com.feedapp.app.viewModels.HomeViewModel
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

@Module
abstract class HomeViewModelModule {

    @Binds
    @IntoMap
    @ViewModelKey(HomeViewModel::class)
    internal abstract fun bindHomeViewModel(homeViewModel: HomeViewModel): ViewModel

}