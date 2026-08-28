package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityDao {
    @Query("SELECT * FROM activities WHERE tripId = :tripId ORDER BY dayNumber ASC, sortOrder ASC, id ASC")
    fun getActivitiesForTrip(tripId: Long): Flow<List<ActivityEntity>>

    @Query("SELECT * FROM activities WHERE tripId = :tripId AND dayNumber = :dayNumber ORDER BY sortOrder ASC, id ASC")
    fun getActivitiesForDay(tripId: Long, dayNumber: Int): Flow<List<ActivityEntity>>

    @Query("SELECT * FROM activities WHERE tripId = :tripId")
    suspend fun getActivitiesForTripDirect(tripId: Long): List<ActivityEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivity(activity: ActivityEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllActivities(activities: List<ActivityEntity>)

    @Update
    suspend fun updateActivity(activity: ActivityEntity)

    @Query("DELETE FROM activities WHERE id = :id")
    suspend fun deleteActivityById(id: Long)

    @Query("DELETE FROM activities WHERE tripId = :tripId")
    suspend fun deleteActivitiesForTrip(tripId: Long)

    @Query("UPDATE activities SET isCompleted = :isCompleted WHERE id = :id")
    suspend fun setActivityCompleted(id: Long, isCompleted: Boolean)
}
