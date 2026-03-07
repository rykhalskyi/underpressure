package com.example.underpressure.data.local.converters

import androidx.room.TypeConverter

/**
 * Room TypeConverters for serializing complex data types.
 */
class Converters {
    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return value.joinToString(",")
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        return if (value.isEmpty()) emptyList() else value.split(",")
    }

    @TypeConverter
    fun fromBooleanList(value: List<Boolean>): String {
        return value.joinToString(",") { it.toString() }
    }

    @TypeConverter
    fun toBooleanList(value: String): List<Boolean> {
        return if (value.isEmpty()) emptyList() else value.split(",").map { it.toBoolean() }
    }
}
