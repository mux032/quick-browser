package com.quick.browser.di

import com.quick.browser.domain.service.*
import com.quick.browser.service.SettingsService
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class ServiceBindingModule {

    @Binds
    abstract fun bindSettingsService(settingsService: SettingsService): ISettingsService

    @Binds
    abstract fun bindEncryptedPreferencesService(encryptedPreferencesService: EncryptedPreferencesService): IEncryptedPreferencesService
}