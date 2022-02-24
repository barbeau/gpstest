/*
 * Copyright (C) 2022 Sean J. Barbeau
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
package com.android.gpstest.model

/**
 * The [countryCode] of the user's country, for example "US", or empty String if it is unknown,
 * as well as the [countryName] for the localized country name of the address, for example "Iceland",
 * or empty String if it is unknown.
 */
data class UserCountry(val countryCode: String = "", val countryName: String = "")