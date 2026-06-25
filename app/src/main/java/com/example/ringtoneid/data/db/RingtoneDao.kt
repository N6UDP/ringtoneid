package com.example.ringtoneid.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface RingtoneDao {
    @Query("SELECT * FROM ringtones ORDER BY contactName ASC")
    fun getAllRingtones(): Flow<List<RingtoneEntity>>

    @Query("SELECT * FROM ringtones WHERE contactId = :contactId LIMIT 1")
    suspend fun getRingtoneByContactId(contactId: Long): RingtoneEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRingtone(entity: RingtoneEntity): Long

    @Query("DELETE FROM ringtones WHERE id = :id")
    suspend fun deleteRingtone(id: Long)
}
