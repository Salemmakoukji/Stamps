package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface StampDao {
    @Query("SELECT * FROM daily_stamps ORDER BY dateMillis DESC")
    fun getAllStamps(): Flow<List<DailyStamp>>

    @Query("SELECT * FROM daily_stamps WHERE isFavorite = 1 ORDER BY dateMillis DESC")
    fun getFavoriteStamps(): Flow<List<DailyStamp>>

    @Query("SELECT * FROM daily_stamps WHERE id = :id LIMIT 1")
    fun getStampById(id: Long): Flow<DailyStamp?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStamp(stamp: DailyStamp): Long

    @Update
    suspend fun updateStamp(stamp: DailyStamp)

    @Delete
    suspend fun deleteStamp(stamp: DailyStamp)

    @Query("DELETE FROM daily_stamps WHERE id = :id")
    suspend fun deleteById(id: Long)
}
