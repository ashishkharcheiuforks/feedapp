package com.feedapp.app.data.repositories

import android.app.Application
import com.feedapp.app.R
import com.feedapp.app.data.api.interfaces.RecipeApiSearch
import com.feedapp.app.data.api.models.recipedetailed.nn.RecipeDetailedResponse
import com.feedapp.app.data.api.models.recipesearch.RecipeSearchModel
import com.feedapp.app.util.StringUtils
import io.reactivex.Flowable
import retrofit2.Call
import javax.inject.Inject

class RecipeSearchRepository
@Inject internal constructor
    (
    val application: Application,
    private val recipeApiSearchResult: RecipeApiSearch
) {
    private val stringUtils = StringUtils()

    fun searchVP(
        query: String,
        intolerance: HashSet<String>? = null,
        diet: HashSet<String>? = null
    ): Call<RecipeSearchModel?>? {
        val intoleranceReady = if (!intolerance.isNullOrEmpty()) intolerance.toString().replace(
            "(])|(\\[)".toRegex(),
            ""
        ) else ""
        val dietReady = if (!diet.isNullOrEmpty()) diet.toString().replace(
            "(])|(\\[)".toRegex(),
            ""
        ) else ""

        val offset = if (intoleranceReady.isNotEmpty() || dietReady.isNotEmpty()) 0 else 30

        return recipeApiSearchResult.getRecipesVP(
            query = query,
            intolerance = intoleranceReady,
            diet = dietReady,
            offset = offset
        )
    }

    fun searchRecipes(
        query: String,
        intolerance: HashSet<String>? = null,
        diet: HashSet<String>? = null
    ): Call<RecipeSearchModel?>? {
        val intoleranceReady = if (!intolerance.isNullOrEmpty()) intolerance.toString().replace(
            "(])|(\\[)".toRegex(),
            ""
        ) else ""
        val dietReady = if (!diet.isNullOrEmpty()) diet.toString().replace(
            "(])|(\\[)".toRegex(),
            ""
        ) else ""

        return recipeApiSearchResult.getRecipesSearch(
            query = query,
            intolerance = intoleranceReady,
            diet = dietReady
        )
    }

    fun searchDetailedInfo(id: Int): Flowable<RecipeDetailedResponse?>? {
        return recipeApiSearchResult.getRecipesDetails(id = id)
    }

    fun checkTitle(title: String): String {
        return stringUtils.getCorrectRecipeTitle(title)
    }

    fun checkCredits(credits: String?, sourceName: String?, sourceUrl: String?): String {
        return if (credits.isNullOrEmpty() && sourceName != null) {
            application.getString(R.string.credits).format(sourceName)
        } else if (!sourceName.isNullOrEmpty() && credits != null) {
            application.getString(R.string.credits).format(credits)
        } else {
            val site = stringUtils.getSiteFromUrl(sourceUrl.toString())
            application.getString(R.string.credits).format(site)
        }

    }

}
