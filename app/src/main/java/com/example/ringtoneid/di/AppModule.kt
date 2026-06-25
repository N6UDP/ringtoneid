package com.example.ringtoneid.di

import android.content.Context
import androidx.room.Room
import com.example.ringtoneid.data.db.AppDatabase
import com.example.ringtoneid.data.db.RingtoneDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "ringtone_id.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideRingtoneDao(db: AppDatabase): RingtoneDao = db.ringtoneDao()
}
