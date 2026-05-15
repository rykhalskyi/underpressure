package com.otakeessen.underpressure.data.local.converters

import androidx.room.TypeConverter

import com.otakeessen.underpressure.domain.BpGuidelines

/**
 * Room TypeConverters for serializing complex data types.
 */
class Converters {
    @TypeConverter
    fun fromBpGuidelines(value: BpGuidelines): String {
        return value.name
    }

    @TypeConverter
    fun toBpGuidelines(value: String): BpGuidelines {
        return BpGuidelines.valueOf(value)
    }

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

    @TypeConverter
    fun fromIntList(value: List<Int>): String {
        return value.joinToString(",") { it.toString() }
    }

    @TypeConverter
    fun toIntList(value: String): List<Int> {
        return if (value.isEmpty()) emptyList() else value.split(",").map { it.toInt() }
    }
}

