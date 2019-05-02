// Copyright 2008 Google Inc.
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

import android.content.res.Resources;
import android.opengl.GLSurfaceView;
import android.opengl.GLU;
import android.util.Log;

import com.android.gpstest.ar.util.GLBuffer;
import com.android.gpstest.ar.util.SkyRegionMap;
import com.android.gpstest.ar.util.TextureManager;
import com.android.gpstest.ar.util.UpdateClosure;
import com.android.gpstest.util.VectorUtil;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class ArRenderer implements GLSurfaceView.Renderer {
    private SkyBox mSkyBox = null;
    private OverlayManager mOverlayManager = null;

    private RenderState mRenderState = new RenderState();

    private Matrix4x4 mProjectionMatrix;
    private Matrix4x4 mViewMatrix;

    // Indicates whether the transformation matrix has changed since the last
    // time we started rendering
    private boolean mMustUpdateView = true;
    private boolean mMustUpdateProjection = true;

    private Set<UpdateClosure> mUpdateClosures = new TreeSet<UpdateClosure>();

    private RendererObjectManager.UpdateListener mUpdateListener =
            new RendererObjectManager.UpdateListener() {
                public void queueForReload(RendererObjectManager rom, boolean fullReload) {
                    mManagersToReload.add(new ManagerReloadData(rom, fullReload));
                }
            };

    // All managers - we need to reload all of these when we recreate the surface.
    private Set<RendererObjectManager> mAllManagers = new TreeSet<RendererObjectManager>();

    protected final TextureManager mTextureManager;

    private static class ManagerReloadData {
        ManagerReloadData(RendererObjectManager manager, boolean fullReload) {
            this.manager = manager;
            this.fullReload = fullReload;
        }

        public RendererObjectManager manager;
        public boolean fullReload;
    }

    // A list of managers which need to be reloaded before the next frame is rendered.  This may
    // be because they haven't ever been loaded yet, or because their objects have changed since
    // the last frame.
    private ArrayList<ManagerReloadData> mManagersToReload = new ArrayList<>();

    // Maps an integer indicating render order to a list of objects at that level.  The managers
    // will be rendered in order, with the lowest number coming first.
    private TreeMap<Integer, Set<RendererObjectManager>> mLayersToManagersMap = null;

    public ArRenderer(Resources res) {
        mRenderState.setResources(res);

        mLayersToManagersMap = new TreeMap<Integer, Set<RendererObjectManager>>();

        mTextureManager = new TextureManager(res);

        // The skybox should go behind everything.
        mSkyBox = new SkyBox(Integer.MIN_VALUE, mTextureManager);
        mSkyBox.enable(false);
        addObjectManager(mSkyBox);

        // The overlays go on top of everything.
        mOverlayManager = new OverlayManager(Integer.MAX_VALUE, mTextureManager);
        addObjectManager(mOverlayManager);

        Log.d("SkyRenderer", "SkyRenderer::SkyRenderer()");
    }

    // Returns true if the buffers should be swapped, false otherwise.
    public void onDrawFrame(GL10 gl) {
        // Initialize any of the unloaded managers.
        for (ManagerReloadData data : mManagersToReload) {
            data.manager.reload(gl, data.fullReload);
        }
        mManagersToReload.clear();

        maybeUpdateMatrices(gl);

        // Determine which sky regions should be rendered.
        mRenderState.setActiveSkyRegions(
                SkyRegionMap.getActiveRegions(
                        mRenderState.getLookDir(),
                        mRenderState.getRadiusOfView(),
                        (float) mRenderState.getScreenWidth() / mRenderState.getScreenHeight()));

        gl.glClear(GL10.GL_COLOR_BUFFER_BIT);

        for (int layer : mLayersToManagersMap.keySet()) {
            Set<RendererObjectManager> managers = mLayersToManagersMap.get(layer);
            for (RendererObjectManager rom : managers) {
                rom.draw(gl);
            }
        }
        checkForErrors(gl);

        // Queue updates for the next frame.
        for (UpdateClosure update : mUpdateClosures) {
            update.run();
        }
    }

    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        Log.d("SkyRenderer", "surfaceCreated");

        gl.glEnable(GL10.GL_DITHER);

        /*
         * Some one-time OpenGL initialization can be made here
         * probably based on features of this particular context
         */
        gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT,
                GL10.GL_FASTEST);

        gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        gl.glEnable(GL10.GL_CULL_FACE);
        gl.glShadeModel(GL10.GL_SMOOTH);
        gl.glDisable(GL10.GL_DEPTH_TEST);


        // Release references to all of the old textures.
        mTextureManager.reset();

        String extensions = gl.glGetString(GL10.GL_EXTENSIONS);
        Log.i("SkyRenderer", "GL extensions: " + extensions);

        // Determine if the phone supports VBOs or not, and set this on the GLBuffer.
        // TODO(jpowell): There are two extension strings which seem applicable.
        // There is GL_OES_vertex_buffer_object and GL_ARB_vertex_buffer_object.
        // I can't find any documentation which explains the difference between
        // these two.  Most phones which support one seem to support both,
        // except for the Nexus One, which only supports ARB but doesn't seem
        // to benefit from using VBOs anyway.  I should figure out what the
        // difference is and use ARB too, if I can.
        boolean canUseVBO = false;
        if (extensions.contains("GL_OES_vertex_buffer_object")) {
            canUseVBO = true;
        }
        // VBO support on the Cliq and Behold is broken and say they can
        // use them when they can't.  Explicitly disable it for these devices.
        final String[] badModels = {
                "MB200",
                "MB220",
                "Behold",
        };
        for (String model : badModels) {
            if (android.os.Build.MODEL.contains(model)) {
                canUseVBO = false;
            }
        }
        Log.i("SkyRenderer", "Model: " + android.os.Build.MODEL);
        Log.i("SkyRenderer", canUseVBO ? "VBOs enabled" : "VBOs disabled");
        GLBuffer.setCanUseVBO(canUseVBO);

        // Reload all of the managers.
        for (RendererObjectManager rom : mAllManagers) {
            rom.reload(gl, true);
        }
    }

    public void onSurfaceChanged(GL10 gl, int width, int height) {
        Log.d("SkyRenderer", "Starting sizeChanged, size = (" + width + ", " + height + ")");

        mRenderState.setScreenSize(width, height);
        mOverlayManager.resize(gl, width, height);

        // Need to set the matrices.
        mMustUpdateView = true;
        mMustUpdateProjection = true;

        Log.d("SkyRenderer", "Changing viewport size");

        gl.glViewport(0, 0, width, height);

        Log.d("SkyRenderer", "Done with sizeChanged");
    }

    public void setRadiusOfView(float degrees) {
        // Log.d("SkyRenderer", "setRadiusOfView(" + degrees + ")");
        mRenderState.setRadiusOfView(degrees);
        mMustUpdateProjection = true;
    }

    public void addUpdateClosure(UpdateClosure update) {
        mUpdateClosures.add(update);
    }

    public void removeUpdateCallback(UpdateClosure update) {
        mUpdateClosures.remove(update);
    }

    // Sets up from the perspective of the viewer.
    // ie, the zenith in celestial coordinates.
    public void setViewerUpDirection(GeocentricCoordinates up) {
        mOverlayManager.setViewerUpDirection(up);
    }

    public void addObjectManager(RendererObjectManager m) {
        m.setRenderState(mRenderState);
        m.setUpdateListener(mUpdateListener);
        mAllManagers.add(m);

        // It needs to be reloaded before we try to draw it.
        mManagersToReload.add(new ManagerReloadData(m, true));

        // Add it to the appropriate layer.
        Set<RendererObjectManager> managers = mLayersToManagersMap.get(m.getLayer());
        if (managers == null) {
            managers = new TreeSet<RendererObjectManager>();
            mLayersToManagersMap.put(m.getLayer(), managers);
        }
        managers.add(m);
    }

    public void removeObjectManager(RendererObjectManager m) {
        mAllManagers.remove(m);

        Set<RendererObjectManager> managers = mLayersToManagersMap.get(m.getLayer());
        // managers shouldn't ever be null, so don't bother checking.  Let it crash if it is so we
        // know there's a bug.
        managers.remove(m);
    }

    public void enableSkyGradient(GeocentricCoordinates sunPosition) {
        mSkyBox.setSunPosition(sunPosition);
        mSkyBox.enable(true);
    }

    public void disableSkyGradient() {
        mSkyBox.enable(false);
    }

    public void enableSearchOverlay(GeocentricCoordinates target, String targetName) {
        mOverlayManager.enableSearchOverlay(target, targetName);
    }

    public void disableSearchOverlay() {
        mOverlayManager.disableSearchOverlay();
    }

    public void setNightVisionMode(boolean enabled) {
        mRenderState.setNightVisionMode(enabled);
    }

    // Used to set the orientation of the text.  The angle parameter is the roll
    // of the phone.  This angle is rounded to the nearest multiple of 90 degrees
    // to keep the text readable.
    public void setTextAngle(float angleInRadians) {
        final float TWO_OVER_PI = 2.0f / (float) Math.PI;
        final float PI_OVER_TWO = (float) Math.PI / 2.0f;

        float newAngle = Math.round(angleInRadians * TWO_OVER_PI) * PI_OVER_TWO;

        mRenderState.setUpAngle(newAngle);
    }

    public void setViewOrientation(float dirX, float dirY, float dirZ,
                                   float upX, float upY, float upZ) {
        // Normalize the look direction
        float dirLen = (float) Math.sqrt(dirX * dirX + dirY * dirY + dirZ * dirZ);
        float oneOverDirLen = 1.0f / dirLen;
        dirX *= oneOverDirLen;
        dirY *= oneOverDirLen;
        dirZ *= oneOverDirLen;

        // We need up to be perpendicular to the look direction, so we subtract
        // off the projection of the look direction onto the up vector
        float lookDotUp = dirX * upX + dirY * upY + dirZ * upZ;
        upX -= lookDotUp * dirX;
        upY -= lookDotUp * dirY;
        upZ -= lookDotUp * dirZ;

        // Normalize the up vector
        float upLen = (float) Math.sqrt(upX * upX + upY * upY + upZ * upZ);
        float oneOverUpLen = 1.0f / upLen;
        upX *= oneOverUpLen;
        upY *= oneOverUpLen;
        upZ *= oneOverUpLen;

        mRenderState.setLookDir(new GeocentricCoordinates(dirX, dirY, dirZ));
        mRenderState.setUpDir(new GeocentricCoordinates(upX, upY, upZ));

        mMustUpdateView = true;

        mOverlayManager.setViewOrientation(new GeocentricCoordinates(dirX, dirY, dirZ),
                new GeocentricCoordinates(upX, upY, upZ));
    }

    protected int getWidth() {
        return mRenderState.getScreenWidth();
    }

    protected int getHeight() {
        return mRenderState.getScreenHeight();
    }


    static void checkForErrors(GL10 gl) {
        checkForErrors(gl, false);
    }

    static void checkForErrors(GL10 gl, boolean printStackTrace) {
        int error = gl.glGetError();
        if (error != 0) {
            Log.e("SkyRenderer", "GL error: " + error);
            Log.e("SkyRenderer", GLU.gluErrorString(error));
            if (printStackTrace) {
                StringWriter writer = new StringWriter();
                new Throwable().printStackTrace(new PrintWriter(writer));
                Log.e("SkyRenderer", writer.toString());
            }
        }
    }

    private void updateView(GL10 gl) {
        // Get a vector perpendicular to both, pointing to the right, by taking
        // lookDir cross up.
        Vector3 lookDir = mRenderState.getLookDir();
        Vector3 upDir = mRenderState.getUpDir();
        Vector3 right = VectorUtil.crossProduct(lookDir, upDir);

        mViewMatrix = Matrix4x4.createView(lookDir, upDir, right);

        gl.glMatrixMode(GL10.GL_MODELVIEW);
        gl.glLoadMatrixf(mViewMatrix.getFloatArray(), 0);
    }

    private void updatePerspective(GL10 gl) {
        mProjectionMatrix = Matrix4x4.createPerspectiveProjection(
                mRenderState.getScreenWidth(),
                mRenderState.getScreenHeight(),
                mRenderState.getRadiusOfView() * 3.141593f / 360.0f);

        gl.glMatrixMode(GL10.GL_PROJECTION);
        gl.glLoadMatrixf(mProjectionMatrix.getFloatArray(), 0);

        // Switch back to the model view matrix.
        gl.glMatrixMode(GL10.GL_MODELVIEW);
    }

    private void maybeUpdateMatrices(GL10 gl) {
        boolean updateTransform = mMustUpdateView || mMustUpdateProjection;
        if (mMustUpdateView) {
            updateView(gl);
            mMustUpdateView = false;
        }
        if (mMustUpdateProjection) {
            updatePerspective(gl);
            mMustUpdateProjection = false;
        }
        if (updateTransform) {
            // Device coordinates are a square from (-1, -1) to (1, 1).  Screen
            // coordinates are (0, 0) to (width, height).  Both coordinates
            // are useful in different circumstances, so we'll pre-compute
            // matrices to do the transformations from world coordinates
            // into each of these.
            Matrix4x4 transformToDevice = Matrix4x4.multiplyMM(mProjectionMatrix, mViewMatrix);

            Matrix4x4 translate = Matrix4x4.createTranslation(1, 1, 0);
            Matrix4x4 scale = Matrix4x4.createScaling(mRenderState.getScreenWidth() * 0.5f,
                    mRenderState.getScreenHeight() * 0.5f, 1);

            Matrix4x4 transformToScreen =
                    Matrix4x4.multiplyMM(Matrix4x4.multiplyMM(scale, translate),
                            transformToDevice);

            mRenderState.setTransformationMatrices(transformToDevice, transformToScreen);
        }
    }

    // WARNING!  These factory methods are invoked from another thread and
    // therefore cannot do any OpenGL operations or any nontrivial nontrivial
    // initialization.
    //
    // TODO(jpowell): This would be much safer if the renderer controller
    // schedules creation of the objects in the queue.
    public PointObjectManager createPointManager(int layer) {
        return new PointObjectManager(layer, mTextureManager);
    }

    public PolyLineObjectManager createPolyLineManager(int layer) {
        return new PolyLineObjectManager(layer, mTextureManager);
    }

    public LabelObjectManager createLabelManager(int layer) {
        return new LabelObjectManager(layer, mTextureManager);
    }

    public ImageObjectManager createImageManager(int layer) {
        return new ImageObjectManager(layer, mTextureManager);
    }
}

interface RenderStateInterface {
    public GeocentricCoordinates getCameraPos();

    public GeocentricCoordinates getLookDir();

    public GeocentricCoordinates getUpDir();

    public float getRadiusOfView();

    public float getUpAngle();

    public float getCosUpAngle();

    public float getSinUpAngle();

    public int getScreenWidth();

    public int getScreenHeight();

    public Matrix4x4 getTransformToDeviceMatrix();

    public Matrix4x4 getTransformToScreenMatrix();

    public Resources getResources();

    public boolean getNightVisionMode();

    public SkyRegionMap.ActiveRegionData getActiveSkyRegions();
}

// TODO(jpowell): RenderState is a bad name.  This class is a grab-bag of
// general state which is set once per-frame, and which individual managers
// may need to render the frame.  Come up with a better name for this.
class RenderState implements RenderStateInterface {
    public GeocentricCoordinates getCameraPos() {
        return mCameraPos;
    }

    public GeocentricCoordinates getLookDir() {
        return mLookDir;
    }

    public GeocentricCoordinates getUpDir() {
        return mUpDir;
    }

    public float getRadiusOfView() {
        return mRadiusOfView;
    }

    public float getUpAngle() {
        return mUpAngle;
    }

    public float getCosUpAngle() {
        return mCosUpAngle;
    }

    public float getSinUpAngle() {
        return mSinUpAngle;
    }

    public int getScreenWidth() {
        return mScreenWidth;
    }

    public int getScreenHeight() {
        return mScreenHeight;
    }

    public Matrix4x4 getTransformToDeviceMatrix() {
        return mTransformToDevice;
    }

    public Matrix4x4 getTransformToScreenMatrix() {
        return mTransformToScreen;
    }

    public Resources getResources() {
        return mRes;
    }

    public boolean getNightVisionMode() {
        return mNightVisionMode;
    }

    public SkyRegionMap.ActiveRegionData getActiveSkyRegions() {
        return mActiveSkyRegionSet;
    }

    public void setCameraPos(GeocentricCoordinates pos) {
        mCameraPos = pos.copy();
    }

    public void setLookDir(GeocentricCoordinates dir) {
        mLookDir = dir.copy();
    }

    public void setUpDir(GeocentricCoordinates dir) {
        mUpDir = dir.copy();
    }

    public void setRadiusOfView(float radius) {
        mRadiusOfView = radius;
    }

    public void setUpAngle(float angle) {
        mUpAngle = angle;
        mCosUpAngle = (float) Math.cos(angle);
        mSinUpAngle = (float) Math.sin(angle);
    }

    public void setScreenSize(int width, int height) {
        mScreenWidth = width;
        mScreenHeight = height;
    }

    public void setTransformationMatrices(Matrix4x4 transformToDevice,
                                          Matrix4x4 transformToScreen) {
        mTransformToDevice = transformToDevice;
        mTransformToScreen = transformToScreen;
    }

    public void setResources(Resources res) {
        mRes = res;
    }

    public void setNightVisionMode(boolean enabled) {
        mNightVisionMode = enabled;
    }

    public void setActiveSkyRegions(SkyRegionMap.ActiveRegionData set) {
        mActiveSkyRegionSet = set;
    }

    private GeocentricCoordinates mCameraPos = new GeocentricCoordinates(0, 0, 0);
    private GeocentricCoordinates mLookDir = new GeocentricCoordinates(1, 0, 0);
    private GeocentricCoordinates mUpDir = new GeocentricCoordinates(0, 1, 0);
    private float mRadiusOfView = 45;  // in degrees
    private float mUpAngle = 0;
    private float mCosUpAngle = 1;
    private float mSinUpAngle = 0;
    private int mScreenWidth = 100;
    private int mScreenHeight = 100;
    private Matrix4x4 mTransformToDevice = Matrix4x4.createIdentity();
    private Matrix4x4 mTransformToScreen = Matrix4x4.createIdentity();
    private Resources mRes;
    private boolean mNightVisionMode = false;
    private SkyRegionMap.ActiveRegionData mActiveSkyRegionSet = null;
}
