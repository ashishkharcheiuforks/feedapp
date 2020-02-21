package com.feedapp.app.data.models.day

import com.google.gson.Gson
import java.io.Serializable


data class DayDate(
    val month: String = "01",
    val day: String = "01",
    val year: String = "2020"
) : Serializable {

    fun toJson(): String? {
        return Gson().toJson(this)
    }

}
