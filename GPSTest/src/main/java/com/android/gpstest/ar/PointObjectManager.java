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

import android.util.Log;

import com.android.gpstest.R;
import com.android.gpstest.ar.util.IndexBuffer;
import com.android.gpstest.ar.util.NightVisionColorBuffer;
import com.android.gpstest.ar.util.SkyRegionMap;
import com.android.gpstest.ar.util.TexCoordBuffer;
import com.android.gpstest.ar.util.TextureManager;
import com.android.gpstest.ar.util.TextureReference;
import com.android.gpstest.ar.util.VertexBuffer;
import com.android.gpstest.util.MathUtils;
import com.android.gpstest.util.VectorUtil;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import javax.microedition.khronos.opengles.GL10;

public class PointObjectManager extends RendererObjectManager {
    private static final int NUM_STARS_IN_TEXTURE = 2;
    // Small sets of point aren't worth breaking up into regions.
    // Right now, I'm arbitrarily setting the threshold to 200.
    private static final int MINIMUM_NUM_POINTS_FOR_REGIONS = 200;

    private class RegionData {
        // TODO(jpowell): This is a convenient hack until the catalog tells us the
        // region for all of its sources.  Remove this once we add that.
        List<PointSource> sources = new ArrayList<PointSource>();

        private VertexBuffer mVertexBuffer = new VertexBuffer(true);
        private NightVisionColorBuffer mColorBuffer = new NightVisionColorBuffer(true);
        private TexCoordBuffer mTexCoordBuffer = new TexCoordBuffer(true);
        private IndexBuffer mIndexBuffer = new IndexBuffer(true);
    }

    // Should we compute the regions for the points?
    // If false, we just put them in the catchall region.
    private static final boolean COMPUTE_REGIONS = true;
    private int mNumPoints = 0;

    private SkyRegionMap<RegionData> mSkyRegions = new SkyRegionMap<RegionData>();

    private TextureReference mTextureRef = null;

    public PointObjectManager(int layer, TextureManager textureManager) {
        super(layer, textureManager);
        // We want to initialize the labels of a sky region to an empty set of data.
        mSkyRegions.setRegionDataFactory(
                new SkyRegionMap.RegionDataFactory<RegionData>() {
                    public RegionData construct() {
                        return new RegionData();
                    }
                });
    }

    public void updateObjects(List<PointSource> points, EnumSet<UpdateType> updateType) {
        boolean onlyUpdatePoints = true;
        // We only care about updates to positions, ignore any other updates.
        if (updateType.contains(UpdateType.Reset)) {
            onlyUpdatePoints = false;
        } else if (updateType.contains(UpdateType.UpdatePositions)) {
            // Sanity check: make sure the number of points is unchanged.
            if (points.size() != mNumPoints) {
                Log.e("PointObjectManager",
                        "Updating PointObjectManager a different number of points: update had " +
                                points.size() + " vs " + mNumPoints + " before");
                return;
            }
        } else {
            return;
        }

        mNumPoints = points.size();

        mSkyRegions.clear();

        if (COMPUTE_REGIONS) {
            // Find the region for each point, and put it in a separate list
            // for that region.
            for (PointSource point : points) {
                int region = points.size() < MINIMUM_NUM_POINTS_FOR_REGIONS
                        ? SkyRegionMap.CATCHALL_REGION_ID
                        : SkyRegionMap.getObjectRegion(point.getLocation());
                mSkyRegions.getRegionData(region).sources.add(point);
            }
        } else {
            mSkyRegions.getRegionData(SkyRegionMap.CATCHALL_REGION_ID).sources = points;
        }

        // Generate the resources for all of the regions.
        for (RegionData data : mSkyRegions.getDataForAllRegions()) {
            int numVertices = 4 * data.sources.size();
            int numIndices = 6 * data.sources.size();

            data.mVertexBuffer.reset(numVertices);
            data.mColorBuffer.reset(numVertices);
            data.mTexCoordBuffer.reset(numVertices);
            data.mIndexBuffer.reset(numIndices);

            Vector3 up = new Vector3(0, 1, 0);

            // By inspecting the perspective projection matrix, you can show that,
            // to have a quad at the center of the screen to be of size k by k
            // pixels, the width and height are both:
            // k * tan(fovy / 2) / screenHeight
            // This is not difficult to derive.  Look at the transformation matrix
            // in SkyRenderer if you're interested in seeing why this is true.
            // I'm arbitrarily deciding that at a 60 degree field of view, and 480
            // pixels high, a size of 1 means "1 pixel," so calculate sizeFactor
            // based on this.  These numbers mostly come from the fact that that's
            // what I think looks reasonable.
            float fovyInRadians = 60 * MathUtils.PI / 180.0f;
            float sizeFactor = (float) Math.tan(fovyInRadians * 0.5f) / 480;

            Vector3 bottomLeftPos = new Vector3(0, 0, 0);
            Vector3 topLeftPos = new Vector3(0, 0, 0);
            Vector3 bottomRightPos = new Vector3(0, 0, 0);
            Vector3 topRightPos = new Vector3(0, 0, 0);

            Vector3 su = new Vector3(0, 0, 0);
            Vector3 sv = new Vector3(0, 0, 0);

            short index = 0;

            float starWidthInTexels = 1.0f / NUM_STARS_IN_TEXTURE;

            for (PointSource p : data.sources) {
                int color = 0xff000000 | p.getColor();  // Force alpha to 0xff
                short bottomLeft = index++;
                short topLeft = index++;
                short bottomRight = index++;
                short topRight = index++;

                // First triangle
                data.mIndexBuffer.addIndex(bottomLeft);
                data.mIndexBuffer.addIndex(topLeft);
                data.mIndexBuffer.addIndex(bottomRight);

                // Second triangle
                data.mIndexBuffer.addIndex(topRight);
                data.mIndexBuffer.addIndex(bottomRight);
                data.mIndexBuffer.addIndex(topLeft);

                int starIndex = p.getPointShape().getImageIndex();

                float texOffsetU = starWidthInTexels * starIndex;

                data.mTexCoordBuffer.addTexCoords(texOffsetU, 1);
                data.mTexCoordBuffer.addTexCoords(texOffsetU, 0);
                data.mTexCoordBuffer.addTexCoords(texOffsetU + starWidthInTexels, 1);
                data.mTexCoordBuffer.addTexCoords(texOffsetU + starWidthInTexels, 0);

                Vector3 pos = p.getLocation();
                Vector3 u = VectorUtil.normalized(VectorUtil.crossProduct(pos, up));
                Vector3 v = VectorUtil.crossProduct(u, pos);

                float s = p.getSize() * sizeFactor;

                su.assign(s * u.x, s * u.y, s * u.z);
                sv.assign(s * v.x, s * v.y, s * v.z);

                bottomLeftPos.assign(pos.x - su.x - sv.x, pos.y - su.y - sv.y, pos.z - su.z - sv.z);
                topLeftPos.assign(pos.x - su.x + sv.x, pos.y - su.y + sv.y, pos.z - su.z + sv.z);
                bottomRightPos.assign(pos.x + su.x - sv.x, pos.y + su.y - sv.y, pos.z + su.z - sv.z);
                topRightPos.assign(pos.x + su.x + sv.x, pos.y + su.y + sv.y, pos.z + su.z + sv.z);

                // Add the vertices
                data.mVertexBuffer.addPoint(bottomLeftPos);
                data.mColorBuffer.addColor(color);

                data.mVertexBuffer.addPoint(topLeftPos);
                data.mColorBuffer.addColor(color);

                data.mVertexBuffer.addPoint(bottomRightPos);
                data.mColorBuffer.addColor(color);

                data.mVertexBuffer.addPoint(topRightPos);
                data.mColorBuffer.addColor(color);
            }
            //Log.i("PointObjectManager",
            //      "Vertices: " + data.mVertexBuffer.size() + ", Indices: " + data.mIndexBuffer.size());
            data.sources = null;
        }
    }

    @Override
    public void reload(GL10 gl, boolean fullReload) {
        mTextureRef = textureManager().getTextureFromResource(gl, R.drawable.stars_texture);
        for (RegionData data : mSkyRegions.getDataForAllRegions()) {
            data.mVertexBuffer.reload();
            data.mColorBuffer.reload();
            data.mTexCoordBuffer.reload();
            data.mIndexBuffer.reload();
        }
    }

    @Override
    protected void drawInternal(GL10 gl) {
        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glEnableClientState(GL10.GL_COLOR_ARRAY);
        gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);

        gl.glEnable(GL10.GL_CULL_FACE);
        gl.glFrontFace(GL10.GL_CW);
        gl.glCullFace(GL10.GL_BACK);

        gl.glEnable(GL10.GL_ALPHA_TEST);
        gl.glAlphaFunc(GL10.GL_GREATER, 0.5f);

        gl.glEnable(GL10.GL_TEXTURE_2D);

        mTextureRef.bind(gl);

        gl.glTexEnvf(GL10.GL_TEXTURE_ENV, GL10.GL_TEXTURE_ENV_MODE, GL10.GL_MODULATE);

        // Render all of the active sky regions.
        SkyRegionMap.ActiveRegionData activeRegions = getRenderState().getActiveSkyRegions();
        ArrayList<RegionData> activeRegionData = mSkyRegions.getDataForActiveRegions(activeRegions);
        for (RegionData data : activeRegionData) {
            if (data.mVertexBuffer.size() == 0) {
                continue;
            }

            data.mVertexBuffer.set(gl);
            data.mColorBuffer.set(gl, getRenderState().getNightVisionMode());
            data.mTexCoordBuffer.set(gl);
            data.mIndexBuffer.draw(gl, GL10.GL_TRIANGLES);
        }

        gl.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
        gl.glDisable(GL10.GL_TEXTURE_2D);
        gl.glDisable(GL10.GL_ALPHA_TEST);
    }
}
