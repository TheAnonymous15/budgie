package com.example.budgie.data.local

import androidx.room.*
import com.example.budgie.data.model.UtilityReading
import kotlinx.coroutines.flow.Flow

@Dao
interface UtilityReadingDao {

    @Query("SELECT * FROM utility_readings WHERE utilityName = :utilityName ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastReading(utilityName: String): UtilityReading?

    @Query("SELECT * FROM utility_readings WHERE utilityName = :utilityName ORDER BY timestamp DESC")
    fun getReadingsByUtility(utilityName: String): Flow<List<UtilityReading>>

    @Query("SELECT * FROM utility_readings ORDER BY timestamp DESC")
    fun getAllReadings(): Flow<List<UtilityReading>>

    @Query("SELECT DISTINCT utilityName FROM utility_readings ORDER BY utilityName ASC")
    fun getUtilityNames(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReading(reading: UtilityReading)

    @Delete
    suspend fun deleteReading(reading: UtilityReading)

    @Query("DELETE FROM utility_readings WHERE utilityName = :utilityName")
    suspend fun deleteReadingsByUtility(utilityName: String)
}

