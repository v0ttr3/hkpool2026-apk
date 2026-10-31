package com.hkpool.app.data
import androidx.room.Entity
import androidx.room.PrimaryKey
@Entity(tableName = "hk_results")
data class ResultEntity(
    @PrimaryKey val date: String,
    val number: String,
    val asDigit: Int,
    val kop: Int,
    val kepala: Int,
    val ekor: Int,
    val timestamp: Long = System.currentTimeMillis()
)
