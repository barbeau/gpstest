// Copyright 2010 Google Inc.
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

package com.android.gpstest.ar.util;

import android.util.Log;

import com.android.gpstest.ar.GeocentricCoordinates;
import com.android.gpstest.util.MathUtils;
import com.android.gpstest.util.VectorUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

/**
 * This is a utility class which divides the sky into a fixed set of regions
 * and maps each of the regions into a generic data object which contains the
 * data for rendering that region of the sky.  For a given frame, this class
 * will determine which regions are on-screen and which are totally
 * off-screen, and will return only the on-screen ones (so we can avoid paying
 * the cost of rendering the ones that aren't on-screen).  There should
 * typically be one of these objects per type of object being rendered: for
 * example, points and labels will each have their own SkyRegionMap.
 * <p>
 * Each region consists of a center (a point on the unit sphere) and an angle,
 * and should contain every object on the unit sphere within the specified
 * angle from the region's center.
 * <p>
 * This also allows for a special "catchall" region which is always rendered
 * and may contain objects from anywhere on the unit sphere.  This is useful
 * because, for small layers, it is cheaper to just render the
 * whole layer than to break it up into smaller pieces.
 * <p>
 * The center of all regions is fixed for computational reasons.  This allows
 * us to find the distance between each region and the current look direction
 * once per frame and share that between all SkyRegionMaps.  For most types
 * of objects, they can also use regions with the same radius, which means
 * that they are the same exact part of the unit sphere.  For these we can
 * compute the regions which are on screen ("active regions") once per frame,
 * and share that between all SkyRegionMaps.  These are called "standard
 * regions", as opposed to "non-standard regions", where the region's angle
 * may be greater than that of the standard region.  Non-standard regions
 * are necessary for some types of objects, such as lines, which may not be
 * fully contained within any standard region.  For lines, we can find the
 * region center which is closest to fully containing the line, and simply
 * increase the angle until it does fully contain it.
 *
 * @param <RegionRenderingData> A object which contains the data needed to
 *                              render a sky region.
 * @author James Powell
 */
public class SkyRegionMap<RegionRenderingData> {
    public static final int CATCHALL_REGION_ID = -1;

    /**
     * Interface for a factory that constructs a rendering data.
     */
    public interface RegionDataFactory<RegionRenderingData> {
        public RegionRenderingData construct();
    }

    /**
     * This stores data that we only want to compute once per frame about
     * which regions are on the screen.  We don't want to compute these
     * regions for every manager separately, since we can share them
     * between managers.
     */
    public static class ActiveRegionData {
        private ActiveRegionData(float[] regionCenterDotProducts,
                                 float screenAngle,
                                 ArrayList<Integer> activeScreenRegions) {
            if (regionCenterDotProducts.length != REGION_CENTERS.length) {
                Log.e("SkyRegionMap", "Bad regionCenterDotProducts length: " +
                        regionCenterDotProducts.length + " vs " + REGION_CENTERS.length);
            }
            this.regionCenterDotProducts = regionCenterDotProducts;
            this.screenAngle = screenAngle;
            this.activeStandardRegions = activeScreenRegions;
        }

        // Dot product of look direction with each region's center.
        // We need this for non-standard regions.  For standard regions,
        // we can compute the visible regions when we compute the
        // ActiveRegionData, so we don't need to cache this.
        private final float[] regionCenterDotProducts;

        // Angle between the look direction and the corners of the screen.
        private final float screenAngle;

        // The list of standard regions which are active given the current
        // look direction and screen angle.
        private final ArrayList<Integer> activeStandardRegions;

        /**
         * Returns true if a non-standard region is active.
         *
         * @param region        The ID of the region to check
         * @param coverageAngle the coverage angle of the region.
         * @return true if the region is active, false if not.
         */
        private boolean regionIsActive(int region, float coverageAngle) {
            // A region cannot be active if the angle between screen's center
            // and the region's center is greater than the sum of the region angle
            // and screen angle.  I make a few definitions:
            // S = screen direction (look direction)
            // s = screen angle
            // R = region center
            // r = region angle
            // If the region is active, then
            // (angle between S and R) < s + r
            // These angles are between 0 and Pi, and cos is decreasing here, so
            // cos(angle between S and R) > cos(s + r)
            // S and R are unit vectors, so S dot R = cos(angle between S and R)
            // S dot R > cos(s + r)
            // So the regions where this holds true are the visible regions.
            return regionCenterDotProducts[region] > (float) Math.cos(coverageAngle + screenAngle);
        }
    }

    /**
     * Data representing an individual object's position in a region.
     * We care about the region itself for obvious reasons, but we care about
     * the dot product with the center because it is a measure of how
     * close it is to the center of a region.
     */
    public static class ObjectRegionData {
        public int region = SkyRegionMap.CATCHALL_REGION_ID;
        public float regionCenterDotProduct = -1;
    }

    // We want to use a set of points that minimizes the maximum distance from
    // any point on the sphere to one of these points.  This is called
    // "covering" a sphere, and is a common and well-studied problem.
    // There are many links to papers on this problem at
    // http://www.ogre.nu/sphere.htm and solutions for various numbers of
    // points may be found at http://www2.research.att.com/~njas/coverings.
    // The points and cover angle used here are taken from the latter site.
    // This vim command will convert their file of points into the array below:
    // :%s/\(.*\)\n\(.*\)\n\(.*\)\n/new GeocentricCoordinates(\1f, \2f, \3f),\r

    // If the angle between the center of a region and a point on the sphere is
    // not within this angle, it cannot be in that region.
    // TODO(jpowell): We have to cite the source of these numbers in order to
    // use this.  Not sure where to do that.  Figure that out before releasing
    // a version that uses it.
  /* TODO(jpowell): I'm not sure how many regions we want.  The more we have,
   * the more setup work we need to do to figure out which ones are on screen,
   * and the more rendering calls we have (there is one per region).
   * On the other hand, each rendering call is smaller, so we do less work.
   * There's a balance here, and I need to experiment with to find the optimal
   * number of regions.
  // 16 points to cover the sphere.  Each region is about 33 degrees.
  public static final float REGION_COVERAGE_ANGLE_IN_RADIANS = 0.574193f;
  public static final GeocentricCoordinates[] REGION_CENTERS = {
      new GeocentricCoordinates(0.35286933463f, 0.74990446679f, -0.55957709252f),
      new GeocentricCoordinates(-0.39102044981f, -0.50929142069f, -0.76663241256f),
      new GeocentricCoordinates(0.62339142523f, 0.05646475870f, 0.77986848913f),
      new GeocentricCoordinates(-0.87305064313f, 0.15072732698f, 0.46374976728f),
      new GeocentricCoordinates(0.24643376097f, -0.81052424572f, 0.53133873082f),
      new GeocentricCoordinates(-0.28740443382f, -0.27010080590f, 0.91893647518f),
      new GeocentricCoordinates(-0.49803568050f, 0.86228929273f, -0.09174766950f),
      new GeocentricCoordinates(-0.92485864274f, 0.03113202471f, -0.37903467845f),
      new GeocentricCoordinates(0.52418663166f, -0.17254281044f, -0.83394085615f),
      new GeocentricCoordinates(0.93851553648f, 0.32907632924f, -0.10439040418f),
      new GeocentricCoordinates(0.19462575390f, -0.93011955358f, -0.31144571024f),
      new GeocentricCoordinates(-0.64954237639f, -0.74621124099f, 0.14582003653f),
      new GeocentricCoordinates(0.40467733983f, 0.86949976626f, 0.28320735506f),
      new GeocentricCoordinates(0.85939374845f, -0.51093567814f, 0.01967528462f),
      new GeocentricCoordinates(-0.20828264579f, 0.56991119217f, 0.79487078916f),
      new GeocentricCoordinates(-0.31189865805f, 0.33072059787f, -0.89069810416f)
  };
  */

    // 32 points to cover the sphere.  Each region is about 22.7 degrees.
    public static final float REGION_COVERAGE_ANGLE_IN_RADIANS = 0.396023592f;
    public static final GeocentricCoordinates[] REGION_CENTERS = {
            new GeocentricCoordinates(-0.850649066269f, 0.525733930059f, -0.000001851469f),
            new GeocentricCoordinates(-0.934170971625f, 0.000004098751f, -0.356825719588f),
            new GeocentricCoordinates(0.577349931933f, 0.577346773818f, 0.577354100533f),
            new GeocentricCoordinates(0.577350600623f, -0.577350601554f, -0.577349603176f),
            new GeocentricCoordinates(-0.577354427427f, -0.577349954285f, 0.577346424572f),
            new GeocentricCoordinates(-0.577346098609f, 0.577353779227f, -0.577350928448f),
            new GeocentricCoordinates(-0.577349943109f, -0.577346729115f, -0.577354134060f),
            new GeocentricCoordinates(-0.577350598760f, 0.577350586653f, 0.577349620871f),
            new GeocentricCoordinates(0.577354458161f, 0.577349932864f, -0.577346415259f),
            new GeocentricCoordinates(0.577346091159f, -0.577353793196f, 0.577350921929f),
            new GeocentricCoordinates(-0.850652559660f, -0.525728277862f, -0.000004770234f),
            new GeocentricCoordinates(-0.934173742309f, 0.000002107583f, 0.356818466447f),
            new GeocentricCoordinates(0.525734450668f, 0.000000594184f, -0.850648744032f),
            new GeocentricCoordinates(0.000002468936f, -0.356819496490f, -0.934173349291f),
            new GeocentricCoordinates(0.525727798231f, -0.000004087575f, 0.850652855821f),
            new GeocentricCoordinates(-0.000002444722f, 0.356819517910f, 0.934173340909f),
            new GeocentricCoordinates(-0.525727787986f, 0.000004113652f, -0.850652862340f),
            new GeocentricCoordinates(0.000004847534f, 0.356824675575f, -0.934171371162f),
            new GeocentricCoordinates(-0.000004885718f, -0.850652267225f, 0.525728750974f),
            new GeocentricCoordinates(-0.356825215742f, -0.934171164408f, -0.000003995374f),
            new GeocentricCoordinates(0.000000767410f, 0.850649364293f, 0.525733447634f),
            new GeocentricCoordinates(0.356825180352f, 0.934171177447f, 0.000003952533f),
            new GeocentricCoordinates(-0.000000790693f, -0.850649344735f, -0.525733478367f),
            new GeocentricCoordinates(0.356818960048f, -0.934173554182f, -0.000001195818f),
            new GeocentricCoordinates(0.850652555004f, 0.525728284381f, 0.000004773028f),
            new GeocentricCoordinates(0.934170960449f, -0.000004090369f, 0.356825748459f),
            new GeocentricCoordinates(-0.525734410621f, -0.000000609085f, 0.850648769177f),
            new GeocentricCoordinates(-0.000004815869f, -0.356824668124f, 0.934171373956f),
            new GeocentricCoordinates(0.000004877336f, 0.850652255118f, -0.525728769600f),
            new GeocentricCoordinates(-0.356819001026f, 0.934173538350f, 0.000001183711f),
            new GeocentricCoordinates(0.850649050437f, -0.525733955204f, 0.000001879409f),
            new GeocentricCoordinates(0.934173759073f, -0.000002136454f, -0.356818422675f),
    };

    // This is the coverage angle of each region.  For most sky region
    // maps, this will be null, which means that the coverage is specified by
    // REGION_COVERAGE_ANGLE_IN_RADIANS.  If some regions have a coverage
    // angle bigger than that, this must be non-NULL and should specify
    // the coverage angles for all of the regions.
    // Rather than only setting this if some regions have special angles,
    // we could just set it for everything.  The reason we don't is that
    // we can just use the standard visible regions if we don't set any
    // special coverage angles, which is a significant performance win.
    public float[] mRegionCoverageAngles = null;

    // Maps the region ID to the rendering data for the region.
    private Map<Integer, RegionRenderingData> mRegionData =
            new TreeMap<Integer, RegionRenderingData>();

    // Used to construct a new region the first time we access it.
    private RegionDataFactory<RegionRenderingData> mRegionDataFactory = null;

    /**
     * Computes the data necessary to determine which regions on the screen
     * are active.  This should be produced once per frame and passed to
     * the getDataForActiveRegions method of all SkyRegionMap objects to
     * get the active regions for each map.
     *
     * @param lookDir       The direction the user is currently facing.
     * @param fovyInDegrees The field of view (in degrees).
     * @param aspect        The aspect ratio of the screen.
     * @return A data object containing data for quickly determining the
     * active regions.
     */
    public static ActiveRegionData getActiveRegions(
            GeocentricCoordinates lookDir,
            float fovyInDegrees,
            float aspect) {
        // We effectively compute a screen "region" here.  The center of this
        // region is the look direction, and the radius is the angle between
        // the center and one of the corners.  If any region intersects the
        // screen region, we consider that region to be active.
        //
        // First, we compute the screen angle.  The angle between the vectors
        // to the top of the screen and the center of the screen is defined to
        // be fovy/2.
        // The distance between the top and center of the view plane, then, is
        // sin(fovy / 2).  The difference between the right and center must be.
        // (width / height) * sin(fovy / 2) = aspect * sin(fovy / 2)
        // This gives us a right triangle to find the distance between the center
        // and the corner of the screen.  This distance is:
        // d = sin(fovy / 2) * sqrt(1 + aspect^2).
        // The angle for the screen region is the arcsin of this value.
        float halfFovy = (fovyInDegrees * MathUtils.DEGREES_TO_RADIANS) / 2;
        float screenAngle = (float) Math.asin(
                (float) Math.sin(halfFovy) * (float) Math.sqrt(1 + aspect * aspect));

        // Next, determine whether or not the region is active.  See the
        // regionIsActive method for an explanation of the math here.
        // We don't use that method because if we did, we would repeatedly
        // compute the same cosine in that function.
        float angleThreshold = screenAngle + REGION_COVERAGE_ANGLE_IN_RADIANS;
        float dotProductThreshold = (float) Math.cos(angleThreshold);
        float[] regionCenterDotProducts = new float[REGION_CENTERS.length];
        ArrayList<Integer> activeStandardRegions = new ArrayList<Integer>();
        for (int i = 0; i < REGION_CENTERS.length; i++) {
            float dotProduct = VectorUtil.dotProduct(lookDir, REGION_CENTERS[i]);
            regionCenterDotProducts[i] = dotProduct;
            if (dotProduct > dotProductThreshold) {
                activeStandardRegions.add(i);
            }
        }

        // Log.d("SkyRegionMap", "ScreenAngle: " + screenAngle);
        // Log.d("SkyRegionMap", "Angle Threshold: " + angleThreshold);
        // Log.d("SkyRegionMap", "DP Threshold: " + dotProductThreshold);

        return new ActiveRegionData(regionCenterDotProducts, screenAngle,
                activeStandardRegions);
    }

    /**
     * Returns the region that a point belongs in.
     *
     * @param position
     * @return The region the point belongs in.
     */
    public static int getObjectRegion(GeocentricCoordinates position) {
        return getObjectRegionData(position).region;
    }

    /**
     * Returns the region a point belongs in, as well as the dot product of the
     * region center and the position.  The latter is a measure of how close it
     * is to the center of the region (1 being a perfect match).
     * <p>
     * TODO(jpowell): I think this is useful for putting lines into regions, but
     * if I don't end up using this when I implement that, I should delete this.
     *
     * @param position
     * @return The closest region and dot product with center of that region.
     */
    public static ObjectRegionData getObjectRegionData(GeocentricCoordinates position) {
        // The closest region will minimize the angle between the vectors, which
        // will maximize the dot product, so we just return the region which
        // does that.
        ObjectRegionData data = new ObjectRegionData();
        for (int i = 0; i < REGION_CENTERS.length; i++) {
            float dotProduct = VectorUtil.dotProduct(REGION_CENTERS[i], position);
            if (dotProduct > data.regionCenterDotProduct) {
                data.regionCenterDotProduct = dotProduct;
                data.region = i;
            }
        }

        // For debugging only: make sure we're within the maximum region coverage angle.
        if (data.regionCenterDotProduct < (float) Math.cos(REGION_COVERAGE_ANGLE_IN_RADIANS)) {
            Log.e("ActiveSkyRegionData",
                    "Object put in region, but outside of coverage angle. " +
                            "Angle was " + (float) Math.acos(data.regionCenterDotProduct) + " vs " +
                            REGION_COVERAGE_ANGLE_IN_RADIANS + ". Region was " + data.region);
        }

        return data;
    }

    // Clear the region map and coverage angles.
    public void clear() {
        mRegionData.clear();
        mRegionCoverageAngles = null;
    }

    /**
     * Sets a function for constructing an empty rendering data object
     * for a sky region.  This is used to create an object if getRegionData()
     * is called and none already exists.
     */
    public void setRegionDataFactory(
            RegionDataFactory<RegionRenderingData> factory) {
        mRegionDataFactory = factory;
    }

    public void setRegionData(int id, RegionRenderingData data) {
        mRegionData.put(id, data);
    }

    public float getRegionCoverageAngle(int id) {
        return mRegionCoverageAngles == null ? REGION_COVERAGE_ANGLE_IN_RADIANS
                : mRegionCoverageAngles[id];
    }

    /**
     * Sets the coverage angle for a sky region.  Needed for non-point
     * objects (see the javadoc for this class).
     *
     * @param id
     * @param angleInRadians
     */
    public void setRegionCoverageAngle(int id, float angleInRadians) {
        if (mRegionCoverageAngles == null) {
            mRegionCoverageAngles = new float[REGION_CENTERS.length];
            for (int i = 0; i < REGION_CENTERS.length; ++i) {
                mRegionCoverageAngles[i] = REGION_COVERAGE_ANGLE_IN_RADIANS;
            }
        }
        if (angleInRadians < mRegionCoverageAngles[id]) {
            Log.e("SkyRegionMap", "Reducing coverage angle of region " + id +
                    " from " + mRegionCoverageAngles[id] + " to " + angleInRadians);
        }
        mRegionCoverageAngles[id] = angleInRadians;
    }

    /**
     * Lookup the region data corresponding to a region ID.  If none exists,
     * and a region data constructor has been set (see setRegionDataConstructor),
     * that will be used to create a new region - otherwise, this will return
     * null.  This can be useful while building or updating a region, but to get
     * the region data when rendering a frame, use getDataForActiveRegions().
     *
     * @param id
     * @return The data for the specified region.
     */
    public RegionRenderingData getRegionData(int id) {
        RegionRenderingData data = mRegionData.get(id);
        if (data == null && mRegionDataFactory != null) {
            // If we have a factory, construct a new object.
            data = mRegionDataFactory.construct();
            mRegionData.put(id, data);
        }
        return data;
    }

    /**
     * Returns the rendering data for the active regions.  When using a
     * SkyRegionMap for rendering, this is the function will return the
     * data for the regions you need to render.
     * <p>
     * TODO(jpowell): I've done a little bit to verify that the regions I'm
     * computing here doesn't include regions that are obviously off screen, but
     * I should do some more work to verify that.
     *
     * @param regions
     * @return ArrayList of rendering data corresponding to the on-screen
     * regions.
     */
    public ArrayList<RegionRenderingData> getDataForActiveRegions(ActiveRegionData regions) {
        ArrayList<RegionRenderingData> data = new ArrayList<RegionRenderingData>();

        // Always add the catchall region if non-NULL.
        RegionRenderingData catchallData = mRegionData.get(CATCHALL_REGION_ID);
        if (catchallData != null) {
            data.add(catchallData);
        }

        if (mRegionCoverageAngles == null) {
            // Just return the data for the standard visible regions.
            for (int region : regions.activeStandardRegions) {
                RegionRenderingData regionData = mRegionData.get(region);
                if (regionData != null) {
                    data.add(regionData);
                }
            }
            return data;
        } else {
            for (int i = 0; i < REGION_CENTERS.length; i++) {
                // Need to specially compute the visible regions.
                if (regions.regionIsActive(i, mRegionCoverageAngles[i])) {
                    RegionRenderingData regionData = mRegionData.get(i);
                    if (regionData != null) {
                        data.add(regionData);
                    }
                }
            }
            return data;
        }
    }

    public Collection<RegionRenderingData> getDataForAllRegions() {
        return mRegionData.values();
    }
}
