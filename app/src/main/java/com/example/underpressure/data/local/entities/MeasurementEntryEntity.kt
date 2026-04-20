package com.example.underpressure.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "measurement_entries",
    foreignKeys = [
        ForeignKey(
            entity = MeasurementListEntity::class,
            parentColumns = ["id"],
            childColumns = ["listId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("listId")]
)
data class MeasurementEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: String, // YYYY-MM-DD
    val slotIndex: Int, // 0-3
    val listId: Long,
    val value: String, // String representation of the value
    val updatedAt: Long = System.currentTimeMillis()
)
