package me.twocities.nonnull.library

import com.google.gson.annotations.SerializedName
import me.twocities.nonnull.NonNullValidate

@NonNullValidate
data class LibraryModel(@SerializedName("value") internal val intervalValue: String,
    private val id: Int,
    val list: List<String>,
    val map: Map<String, String>,
    var name: String?)
