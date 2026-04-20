package com.example.underpressure.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class MeasurementListType {
    DOUBLE,
    BOOLEAN,
    TEXT
}

@Entity(tableName = "measurement_lists")
data class MeasurementListEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: MeasurementListType,
    val active: Boolean = true
)
