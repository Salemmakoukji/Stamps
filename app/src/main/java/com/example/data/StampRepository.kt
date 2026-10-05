package com.example.data

import kotlinx.coroutines.flow.Flow

class StampRepository(private val stampDao: StampDao) {
    val allStamps: Flow<List<DailyStamp>> = stampDao.getAllStamps()
    val favoriteStamps: Flow<List<DailyStamp>> = stampDao.getFavoriteStamps()

    fun getStampById(id: Long): Flow<DailyStamp?> = stampDao.getStampById(id)

    suspend fun insert(stamp: DailyStamp): Long = stampDao.insertStamp(stamp)

    suspend fun update(stamp: DailyStamp) = stampDao.updateStamp(stamp)

    suspend fun delete(stamp: DailyStamp) = stampDao.deleteStamp(stamp)

    suspend fun deleteById(id: Long) = stampDao.deleteById(id)
}
