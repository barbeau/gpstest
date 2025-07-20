package com.android.gpstest.library.di

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import com.android.gpstest.library.data.SharedAntennaManager
import com.android.gpstest.library.data.SharedGnssMeasurementManager
import com.android.gpstest.library.data.SharedGnssStatusManager
import com.android.gpstest.library.data.SharedLocationManager
import com.android.gpstest.library.data.SharedNavMessageManager
import com.android.gpstest.library.data.SharedNmeaManager
import com.android.gpstest.library.data.SharedSensorManager
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
    fun provideSharedPreferences(@ApplicationContext context: Context
    ):SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    @Provides
    @Singleton
    fun provideSharedLocationManager(
        @ApplicationContext context: Context,
        prefs: SharedPreferences
    ): SharedLocationManager =
        SharedLocationManager(context, GlobalScope, prefs)

    @Provides
    fun provideContext(
        @ApplicationContext context: Context,
    ): Context {
        return context
    }

    @Provides
    @Singleton
    fun provideSharedGnssStatusManager(
        @ApplicationContext context: Context,
        prefs: SharedPreferences,
    ): SharedGnssStatusManager =
        SharedGnssStatusManager(context, GlobalScope, prefs)

    @Provides
    @Singleton
    fun provideSharedNmeaManager(
        @ApplicationContext context: Context,
        prefs: SharedPreferences,
    ): SharedNmeaManager =
        SharedNmeaManager(context, GlobalScope, prefs)

    @Provides
    @Singleton
    fun provideSharedSensorManager(
        prefs: SharedPreferences,
        @ApplicationContext context: Context
    ): SharedSensorManager =
        SharedSensorManager(prefs, context, GlobalScope)

    @Provides
    @Singleton
    fun provideSharedNavMessageManager(
        @ApplicationContext context: Context,
        prefs: SharedPreferences,
    ): SharedNavMessageManager =
        SharedNavMessageManager(context, GlobalScope, prefs)

    @Provides
    @Singleton
    fun provideSharedMeasurementsManager(
        prefs: SharedPreferences,
        @ApplicationContext context: Context
    ): SharedGnssMeasurementManager =
        SharedGnssMeasurementManager(prefs, context, GlobalScope)

    @Provides
    @Singleton
    fun provideSharedAntennaManager(
        @ApplicationContext context: Context,
        prefs: SharedPreferences,
    ): SharedAntennaManager =
        SharedAntennaManager(context, GlobalScope,prefs)
}