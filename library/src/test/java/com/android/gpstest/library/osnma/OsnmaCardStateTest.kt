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
package com.android.gpstest.library.osnma

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OsnmaCardStateTest {

    private fun authenticated(): OsnmaStatus {
        val engine = OsnmaEngine(
            Bits.fromHex(TestVectors.MERKLE_ROOT_1),
            listOf(TestVectors.publicKey1())
        )
        TestVectors.configuration1().forEach { engine.feedPage(it.svn, it.gst, it.page) }
        return engine.status()
    }

    private fun waitingForKroot(): OsnmaStatus {
        val engine = OsnmaEngine(
            Bits.fromHex(TestVectors.MERKLE_ROOT_1),
            listOf(TestVectors.publicKey1())
        )
        TestVectors.configuration1().take(20).forEach { engine.feedPage(it.svn, it.gst, it.page) }
        return engine.status()
    }

    @Test
    fun mainState() {
        val auth = authenticated()
        assertEquals(OsnmaMainState.NO_DATA, OsnmaCardState.of(null, 0, false, null, false).main)
        assertEquals(
            OsnmaMainState.IN_PROGRESS,
            OsnmaCardState.of(null, 10, false, null, false).main
        )
        assertEquals(
            OsnmaMainState.IN_PROGRESS,
            OsnmaCardState.of(waitingForKroot(), 20, false, 1, false).main
        )
        assertEquals(OsnmaMainState.AUTHENTICATED, OsnmaCardState.of(auth, 1, false, 1, false).main)
        assertEquals(
            OsnmaMainState.TIME_NOT_VERIFIED,
            OsnmaCardState.of(auth, 1, false, 200, false).main
        )
        // An alert takes precedence over everything else
        assertEquals(OsnmaMainState.ALERT, OsnmaCardState.of(auth, 1, true, 200, true).main)
    }

    @Test
    fun warningsCombine() {
        val auth = authenticated()
        assertTrue(OsnmaCardState.of(auth, 1, false, 1, false).warnings.isEmpty())
        val failed = auth.copy(satellites = auth.satellites.mapIndexed { i, s ->
            if (i == 0) s.copy(tagsFailed = 1, lastFailureGst = auth.gst) else s
        })
        val state = OsnmaCardState.of(failed, 1, false, 60, true)
        assertEquals(OsnmaMainState.AUTHENTICATED, state.main)
        assertEquals(
            listOf(
                OsnmaWarning.FAILED_TAGS,
                OsnmaWarning.POOR_RECEPTION,
                OsnmaWarning.SLOW_MAC_ONLY
            ),
            state.warnings.toList()
        )
    }
}
