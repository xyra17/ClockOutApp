package com.clockout.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkDayDao {
    @Query("SELECT * FROM work_days ORDER BY dateKey DESC")
    fun observeAll(): Flow<List<WorkDayEntity>>

    @Query("SELECT * FROM work_days WHERE dateKey = :dateKey LIMIT 1")
    fun observeByDate(dateKey: String): Flow<WorkDayEntity?>

    @Query("SELECT * FROM work_days WHERE dateKey = :dateKey LIMIT 1")
    suspend fun getByDate(dateKey: String): WorkDayEntity?

    @Query("SELECT * FROM work_days WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): WorkDayEntity?

    @Query("SELECT * FROM work_days WHERE actualClockOutEpochMillis IS NULL AND isRestDay = 0 AND clockInEpochMillis IS NOT NULL ORDER BY dateKey DESC")
    suspend fun getOpenRecords(): List<WorkDayEntity>

    @Query("SELECT * FROM work_days WHERE dateKey < :dateKey AND isRestDay = 0 AND clockInEpochMillis IS NOT NULL ORDER BY dateKey DESC LIMIT 1")
    suspend fun getLatestWorkDayBefore(dateKey: String): WorkDayEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: WorkDayEntity): Long

    @Delete
    suspend fun delete(entity: WorkDayEntity)

    @Query("DELETE FROM work_days")
    suspend fun deleteAll()
}
