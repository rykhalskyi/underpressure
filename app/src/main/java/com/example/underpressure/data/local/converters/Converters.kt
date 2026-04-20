package com.example.underpressure.data.local.converters

import androidx.room.TypeConverter

import com.example.underpressure.data.local.entities.MeasurementListType

/**
 * Room TypeConverters for serializing complex data types.
 */
class Converters {
    @TypeConverter
    fun fromMeasurementListType(value: MeasurementListType): String {
        return value.name
    }

    @TypeConverter
    fun toMeasurementListType(value: String): MeasurementListType {
        return MeasurementListType.valueOf(value)
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
}
