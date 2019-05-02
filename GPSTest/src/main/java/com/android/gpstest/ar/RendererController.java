// Copyright 2009 Google Inc. From StarDroid.
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

package com.android.gpstest.ar;

import android.opengl.GLSurfaceView;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Allows the rest of the program to communicate with the SkyRenderer by queueing
 * events.
 *
 * @author James Powell
 */
public class RendererController extends RendererControllerBase {
    /**
     * Used for grouping renderer calls into atomic units.
     */
    public static class AtomicSection extends RendererControllerBase {
        private Queuer mQueuer = new Queuer();
        private static int NEXT_ID = 0;
        private int mID;

        private AtomicSection(ArRenderer renderer) {
            super(renderer);
            synchronized (AtomicSection.class) {
                mID = NEXT_ID++;
            }
        }

        @Override
        protected EventQueuer getQueuer() {
            return mQueuer;
        }

        @Override
        public String toString() {
            return "AtomicSection" + mID;
        }

        private Queue<Runnable> releaseEvents() {
            Queue<Runnable> queue = mQueuer.mQueue;
            mQueuer = new Queuer();
            return queue;
        }

        private static class Queuer implements EventQueuer {
            private Queue<Runnable> mQueue = new LinkedList<Runnable>();

            public void queueEvent(Runnable r) {
                mQueue.add(r);
            }
        }
    }

    private final EventQueuer mQueuer;

    @Override
    protected EventQueuer getQueuer() {
        return mQueuer;
    }

    public RendererController(ArRenderer renderer, final GLSurfaceView view) {
        super(renderer);
        mQueuer = new EventQueuer() {
            public void queueEvent(Runnable r) {
                view.queueEvent(r);
            }
        };
    }

    @Override
    public String toString() {
        return "RendererController";
    }

    public AtomicSection createAtomic() {
        return new AtomicSection(mRenderer);
    }

    public void queueAtomic(final AtomicSection atomic) {
        String msg = "Applying " + atomic.toString();
        queueRunnable(msg, CommandType.Synchronization, new Runnable() {
            public void run() {
                Queue<Runnable> events = atomic.releaseEvents();
                for (Runnable r : events) {
                    r.run();
                }
            }
        });
    }
}
