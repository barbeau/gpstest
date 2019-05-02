// Copyright 2010 Google Inc. From StarDroid.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.android.gpstest.ar.touch;

import android.util.Log;

import com.android.gpstest.util.MiscUtils;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Given a flung motion event, this class pumps new Motion events out
 * to simulate an underlying object with some inertia.
 */
public class Flinger {
    private static final String TAG = MiscUtils.getTag(Flinger.class);

    public interface FlingListener {
        void fling(float distanceX, float distanceY);
    }

    private FlingListener listener;
    private int updatesPerSecond = 20;
    private int timeIntervalMillis = 1000 / updatesPerSecond;
    private ScheduledExecutorService executor;
    private ScheduledFuture<?> flingTask;

    public Flinger(FlingListener listener) {
        this.listener = listener;
        executor = Executors.newScheduledThreadPool(1);
    }

    public void fling(float velocityX, float velocityY) {
        Log.d(TAG, "Doing the fling");
        class PositionUpdater implements Runnable {
            private float myVelocityX, myVelocityY;
            private float decelFactor = 1.1f;
            private float TOL = 10;

            public PositionUpdater(float velocityX, float velocityY) {
                this.myVelocityX = velocityX;
                this.myVelocityY = velocityY;
            }

            public void run() {
                if (myVelocityX * myVelocityX + myVelocityY * myVelocityY < TOL) {
                    stop();
                }
                listener.fling(myVelocityX / updatesPerSecond,
                        myVelocityY / updatesPerSecond);
                myVelocityX /= decelFactor;
                myVelocityY /= decelFactor;
            }
        }
        flingTask = executor.scheduleAtFixedRate(new PositionUpdater(velocityX, velocityY),
                0, timeIntervalMillis, TimeUnit.MILLISECONDS);
    }

    /**
     * Brings the flinger to a dead stop.
     */
    public void stop() {
        if (flingTask != null) flingTask.cancel(true);
        Log.d(TAG, "Fling stopped");
    }
}
