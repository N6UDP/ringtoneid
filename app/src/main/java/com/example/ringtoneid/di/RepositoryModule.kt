package com.example.ringtoneid.di

import com.example.ringtoneid.data.contacts.ContactsRepositoryImpl
import com.example.ringtoneid.data.ringtone.RingtoneRepositoryImpl
import com.example.ringtoneid.domain.repository.ContactsRepository
import com.example.ringtoneid.domain.repository.RingtoneRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindContactsRepository(impl: ContactsRepositoryImpl): ContactsRepository

    @Binds
    @Singleton
    abstract fun bindRingtoneRepository(impl: RingtoneRepositoryImpl): RingtoneRepository
}
