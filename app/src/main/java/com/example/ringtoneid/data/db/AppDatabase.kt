package com.example.ringtoneid.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [RingtoneEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun ringtoneDao(): RingtoneDao
}
