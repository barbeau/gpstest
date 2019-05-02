// Copyright 2008 Google Inc. From StarDroid.
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

import android.os.ConditionVariable;
import android.util.Log;

import com.android.gpstest.ar.util.UpdateClosure;

import java.util.EnumSet;
import java.util.List;

public abstract class RendererControllerBase {
    /**
     * Base class for all renderer managers.
     */
    public static abstract class RenderManager<E> {
        protected RendererObjectManager mManager;

        private RenderManager(RendererObjectManager mgr) {
            mManager = mgr;
        }

        public void queueEnabled(final boolean enable, RendererControllerBase controller) {
            final String msg = (enable ? "Enabling" : "Disabling") + " manager " + mManager;
            controller.queueRunnable(msg, CommandType.Data, new Runnable() {
                public void run() {
                    mManager.enable(enable);
                }
            });
        }

        public void queueMaxFieldOfView(final float fov, RendererControllerBase controller) {
            final String msg = "Setting manager max field of view: " + fov;
            controller.queueRunnable(msg, CommandType.Data, new Runnable() {
                public void run() {
                    mManager.setMaxRadiusOfView(fov);
                }
            });
        }

        public abstract void queueObjects(
                final List<E> objects,
                final EnumSet<RendererObjectManager.UpdateType> updateType,
                RendererControllerBase controller);
    }

    // TODO(brent): collapse these into a single class?

    /**
     * Class for managing a set of point objects.
     */
    public static class PointManager extends RenderManager<PointSource> {
        private PointManager(PointObjectManager manager) {
            super(manager);
        }

        @Override
        public void queueObjects(final List<PointSource> points,
                                 final EnumSet<RendererObjectManager.UpdateType> updateType,
                                 RendererControllerBase controller) {
            String msg = "Setting point objects";
            controller.queueRunnable(msg, CommandType.Data, new Runnable() {
                public void run() {
                    ((PointObjectManager) mManager).updateObjects(points, updateType);
                }
            });
        }
    }

    /**
     * Class for managing a set of polyline objects.
     */
    public static class LineManager extends RenderManager<LineSource> {
        private LineManager(PolyLineObjectManager manager) {
            super(manager);
        }

        @Override
        public void queueObjects(final List<LineSource> lines,
                                 final EnumSet<RendererObjectManager.UpdateType> updateType,
                                 RendererControllerBase controller) {
            String msg = "Setting line objects";
            controller.queueRunnable(msg, CommandType.Data, new Runnable() {
                public void run() {
                    ((PolyLineObjectManager) mManager).updateObjects(lines, updateType);
                }
            });
        }
    }

    /**
     * Class for managing a set of text label objects.
     */
    public static class LabelManager extends RenderManager<TextSource> {
        private LabelManager(LabelObjectManager manager) {
            super(manager);
        }

        @Override
        public void queueObjects(final List<TextSource> labels,
                                 final EnumSet<RendererObjectManager.UpdateType> updateType,
                                 RendererControllerBase controller) {
            String msg = "Setting label objects";
            controller.queueRunnable(msg, CommandType.Data, new Runnable() {
                public void run() {
                    ((LabelObjectManager) mManager).updateObjects(labels, updateType);
                }
            });
        }
    }

    /**
     * Class for managing a set of image objects.
     */
    public static class ImageManager extends RenderManager<ImageSource> {
        private ImageManager(ImageObjectManager manager) {
            super(manager);
        }

        @Override
        public void queueObjects(final List<ImageSource> images,
                                 final EnumSet<RendererObjectManager.UpdateType> updateType,
                                 RendererControllerBase controller) {
            String msg = "Setting image objects";
            controller.queueRunnable(msg, CommandType.Data, new Runnable() {
                public void run() {
                    ((ImageObjectManager) mManager).updateObjects(images, updateType);
                }
            });
        }
    }

    protected static interface EventQueuer {
        void queueEvent(Runnable r);
    }

    public RendererControllerBase(ArRenderer renderer) {
        mRenderer = renderer;
    }

    // Used only to allow logging different types of events.  The distinction
    // can be somewhat ambiguous at times, so when in doubt, I tend to use
    // "view" for those things that change all the time (like the direction
    // the user is looking) and "data" for those that change less often
    // (like whether a layer is visible or not).
    protected enum CommandType {
        View,  // The command only changes the user's view.
        Data,  // The command changes what is actually rendered.
        Synchronization  // The command relates to synchronization.
    }

    private static final boolean SHOULD_LOG_QUEUE = false;
    private static final boolean SHOULD_LOG_RUN = false;
    private static final boolean SHOULD_LOG_FINISH = false;

    protected final ArRenderer mRenderer;

    public PointManager createPointManager(int layer) {
        PointManager manager = new PointManager(mRenderer.createPointManager(layer));
        queueAddManager(manager);
        return manager;
    }

    public LineManager createLineManager(int layer) {
        LineManager manager = new LineManager(mRenderer.createPolyLineManager(layer));
        queueAddManager(manager);
        return manager;
    }

    public LabelManager createLabelManager(int layer) {
        LabelManager manager = new LabelManager(mRenderer.createLabelManager(layer));
        queueAddManager(manager);
        return manager;
    }

    public ImageManager createImageManager(int layer) {
        ImageManager manager = new ImageManager(mRenderer.createImageManager(layer));
        queueAddManager(manager);
        return manager;
    }

    public void queueNightVisionMode(final boolean enable) {
        final String msg = "Setting night vision mode: " + enable;
        queueRunnable(msg, CommandType.View, new Runnable() {
            public void run() {
                mRenderer.setNightVisionMode(enable);
            }
        });
    }

    public void queueFieldOfView(final float fov) {
        final String msg = "Setting fov: " + fov;
        queueRunnable(msg, CommandType.View, new Runnable() {
            public void run() {
                mRenderer.setRadiusOfView(fov);
            }
        });
    }

    public void queueTextAngle(final float angleInRadians) {
        final String msg = "Setting text angle: " + angleInRadians;
        queueRunnable(msg, CommandType.View, new Runnable() {
            public void run() {
                mRenderer.setTextAngle(angleInRadians);
            }
        });
    }

    public void queueViewerUpDirection(final GeocentricCoordinates up) {
        final String msg = "Setting up direction: " + up;
        queueRunnable(msg, CommandType.View, new Runnable() {
            public void run() {
                mRenderer.setViewerUpDirection(up);
            }
        });
    }

    public void queueSetViewOrientation(final float dirX, final float dirY, final float dirZ,
                                        final float upX, final float upY, final float upZ) {
        final String msg = "Setting view orientation";
        queueRunnable(msg, CommandType.Data, new Runnable() {
            public void run() {
                mRenderer.setViewOrientation(dirX, dirY, dirZ, upX, upY, upZ);
            }
        });
    }

    public void queueEnableSkyGradient(final GeocentricCoordinates sunPosition) {
        final String msg = "Enabling sky gradient at: " + sunPosition;
        queueRunnable(msg, CommandType.Data, new Runnable() {
            public void run() {
                mRenderer.enableSkyGradient(sunPosition);
            }
        });
    }

    public void queueDisableSkyGradient() {
        final String msg = "Disabling sky gradient";
        queueRunnable(msg, CommandType.Data, new Runnable() {
            public void run() {
                mRenderer.disableSkyGradient();
            }
        });
    }

    public void queueEnableSearchOverlay(final GeocentricCoordinates target,
                                         final String targetName) {
        final String msg = "Enabling search overlay";
        queueRunnable(msg, CommandType.Data, new Runnable() {
            public void run() {
                mRenderer.enableSearchOverlay(target, targetName);
            }
        });
    }

    public void queueDisableSearchOverlay() {
        final String msg = "Disabling search overlay";
        queueRunnable(msg, CommandType.Data, new Runnable() {
            public void run() {
                mRenderer.disableSearchOverlay();
            }
        });
    }

    public void addUpdateClosure(final UpdateClosure runnable) {
        final String msg = "Setting update callback";
        queueRunnable(msg, CommandType.Data, new Runnable() {
            @Override
            public void run() {
                mRenderer.addUpdateClosure(runnable);
            }
        });
    }

    public void removeUpdateCallback(final UpdateClosure update) {
        final String msg = "Removing update callback";
        queueRunnable(msg, CommandType.Data, new Runnable() {
            @Override
            public void run() {
                mRenderer.removeUpdateCallback(update);
            }
        });
    }

    /**
     * Must be called once to register an object manager to the renderer.
     *
     * @param rom
     */
    public <E> void queueAddManager(final RenderManager<E> rom) {
        String msg = "Adding manager: " + rom;
        queueRunnable(msg, CommandType.Data, new Runnable() {
            public void run() {
                mRenderer.addObjectManager(rom.mManager);
            }
        });
    }

    public void waitUntilFinished() {
        final ConditionVariable cv = new ConditionVariable();
        String msg = "Waiting until operations have finished";
        queueRunnable(msg, CommandType.Synchronization, new Runnable() {
            public void run() {
                cv.open();
            }
        });
        cv.block();
    }

    abstract protected EventQueuer getQueuer();

    protected void queueRunnable(String msg, final CommandType type, final Runnable r) {
        EventQueuer queuer = getQueuer();
        String fullMessage = toString() + " - " + msg;
        RendererControllerBase.queueRunnable(queuer, fullMessage, type, r);
    }

    protected static void queueRunnable(EventQueuer queuer, final String msg,
                                        final CommandType type, final Runnable r) {
        // If we're supposed to log something, then wrap the runnable with the
        // appropriate logging statements.  Otherwise, just queue it.
        if (SHOULD_LOG_QUEUE || SHOULD_LOG_RUN || SHOULD_LOG_FINISH) {
            logQueue(msg, type);
            queuer.queueEvent(new Runnable() {
                public void run() {
                    logRun(msg, type);
                    r.run();
                    logFinish(msg, type);
                }
            });
        } else {
            queuer.queueEvent(r);
        }
    }

    protected static void logQueue(String description, CommandType type) {
        if (SHOULD_LOG_QUEUE) {
            Log.d("RendererController-" + type.toString(), "Queuing: " + description);
        }
    }

    protected static void logRun(String description, CommandType type) {
        if (SHOULD_LOG_RUN) {
            Log.d("RendererController-" + type.toString(), "Running: " + description);
        }
    }

    protected static void logFinish(String description, CommandType type) {
        if (SHOULD_LOG_FINISH) {
            Log.d("RendererController-" + type.toString(), "Finished: " + description);
        }
    }
}
