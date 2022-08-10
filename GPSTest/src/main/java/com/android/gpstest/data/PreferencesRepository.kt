/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.gpstest.data

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import com.android.gpstest.model.AppPreferences
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

/**
 * Class that handles saving and retrieving user preferences
 *
 * Patterned after Google codelab https://developer.android.com/codelabs/android-preferences-datastore
 * with https://github.com/googlecodelabs/android-datastore/blob/preferences_datastore/.
 *
 * TODO: All preference access should be migrated to this class ([PreferenceUtil], [PreferenceUtils])
 */
class PreferencesRepository @Inject constructor(private val dataStore: DataStore<Preferences>) {

  private object PreferencesKeys {
    val IS_TRACKING_STARTED = booleanPreferencesKey("is_tracking_started")
  }

  /**
   * Get the user preferences flow.
   */
  val userPreferencesFlow: Flow<AppPreferences> = dataStore.data.catch { exception ->
      // dataStore.data throws an IOException when an error is encountered when reading data
      if (exception is IOException) {
        Log.e(TAG, "Error reading preferences.", exception)
        emit(emptyPreferences())
      } else {
        throw exception
      }
    }.map { preferences ->
      mapUserPreferences(preferences)
    }

  suspend fun updateTracking(isTrackingStarted: Boolean) {
    dataStore.edit { preferences ->
      preferences[PreferencesKeys.IS_TRACKING_STARTED] = isTrackingStarted
    }
  }

  // TODO - do we need to use this to set initial preferences? It's currently unused.
  suspend fun fetchInitialPreferences() = mapUserPreferences(dataStore.data.first().toPreferences())

  @Deprecated("All preferences should ideally be observed from [userPreferencesFlow]. " +
                "This method is provided for backwards compatibility with code that accesses " +
                "preferences synchronously.")
  fun prefs() = runBlocking { mapUserPreferences(dataStore.data.first()) }

  private fun mapUserPreferences(preferences: Preferences): AppPreferences {
    // Get the preferences and convert them to a [AppPreferences] object
    val isTrackingStarted =
      preferences[PreferencesKeys.IS_TRACKING_STARTED] ?: true
    return AppPreferences(isTrackingStarted)
  }

  companion object {
    private const val TAG = "PreferencesRepo"
  }
}