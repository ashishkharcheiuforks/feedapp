package com.feedapp.app.di.modules.viewModelModules

import androidx.lifecycle.ViewModel
import com.feedapp.app.di.other.ViewModelKey
import com.feedapp.app.viewModels.MyMealsViewModel
import com.feedapp.app.viewModels.RecipeSearchViewModel
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

@Module
abstract class RecipeSearchViewModelModule {

    @Binds
    @IntoMap
    @ViewModelKey(RecipeSearchViewModel::class)
    internal abstract fun bindrecipeSearchViewModelModule(recipeSearchViewModel: RecipeSearchViewModel): ViewModel

}