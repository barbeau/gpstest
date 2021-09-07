package com.android.gpstest.di


import android.content.Context
import com.android.gpstest.data.SharedGnssStatusManager
import com.android.gpstest.data.SharedLocationManager
import com.android.gpstest.data.SharedNmeaManager
import com.android.gpstest.data.SharedSensorManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.GlobalScope
import javax.inject.Singleton

/**
 * Configuration for DI on the repository and shared location manager
 */
@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideSharedLocationManager(
        @ApplicationContext context: Context
    ): SharedLocationManager =
        SharedLocationManager(context, GlobalScope)

    @Provides
    @Singleton
    fun provideSharedGnssStatusManager(
        @ApplicationContext context: Context
    ): SharedGnssStatusManager =
        SharedGnssStatusManager(context, GlobalScope)

    @Provides
    @Singleton
    fun provideSharedNmeaManager(
        @ApplicationContext context: Context
    ): SharedNmeaManager =
        SharedNmeaManager(context, GlobalScope)

    @Provides
    @Singleton
    fun provideSharedSensorManager(
        @ApplicationContext context: Context
    ): SharedSensorManager =
        SharedSensorManager(context, GlobalScope)
}