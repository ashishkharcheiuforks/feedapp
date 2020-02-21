package com.feedapp.app.di.modules.viewModelModules

import androidx.lifecycle.ViewModel
import com.feedapp.app.di.other.ViewModelKey
import com.feedapp.app.viewModels.MyMealsViewModel
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

@Module
abstract class MyMealsViewModelModule {

    @Binds
    @IntoMap
    @ViewModelKey(MyMealsViewModel::class)
    internal abstract fun bindMyMealsViewModel(myMealsViewModel: MyMealsViewModel): ViewModel

}