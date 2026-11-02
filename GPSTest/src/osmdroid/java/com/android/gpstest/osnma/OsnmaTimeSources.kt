/*
 * Copyright (C) 2026 The GPSTest contributors
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
package com.android.gpstest.osnma

import android.content.Context
import com.android.gpstest.library.data.OsnmaRepository

/**
 * Time sources for the Galileo OSNMA time check in the osmdroid build flavor: no Google Play
 * services, so Android's network time or the device clock is used (see OsnmaRepository)
 */
object OsnmaTimeSources {
    @Suppress("UNUSED_PARAMETER")
    fun install(context: Context, repository: OsnmaRepository) {
    }
}
