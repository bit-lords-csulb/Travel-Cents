package com.example.travelcents.data.local.trip

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class TripLocalConverters {
    private val gson = Gson()
    private val stringListType = object : TypeToken<List<String>>() {}.type
    private val stringMapType = object : TypeToken<Map<String, String>>() {}.type

    @TypeConverter
    fun fromStringList(value: List<String>): String = gson.toJson(value)

    @TypeConverter
    fun toStringList(value: String?): List<String> {
        if (value.isNullOrBlank()) return emptyList()
        return gson.fromJson(value, stringListType)
    }

    @TypeConverter
    fun fromStringMap(value: Map<String, String>): String = gson.toJson(value)

    @TypeConverter
    fun toStringMap(value: String?): Map<String, String> {
        if (value.isNullOrBlank()) return emptyMap()
        return gson.fromJson(value, stringMapType)
    }
}
