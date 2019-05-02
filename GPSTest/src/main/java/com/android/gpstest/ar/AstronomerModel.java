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

import java.util.Date;

/**
 * The interface to AstronomerModelImpl.  It is not expected that there
 * will be multiple subclasses of this interface - it is purely for easy of
 * testing.
 *
 * @author John Taylor
 */
public interface AstronomerModel {
    /**
     * A POJO to hold the user's view direction.
     *
     * @author John Taylor
     */
    public static class Pointing {
        private final GeocentricCoordinates lineOfSight;
        private final GeocentricCoordinates perpendicular;

        public Pointing(GeocentricCoordinates lineOfSight,
                        GeocentricCoordinates perpendicular) {
            this.lineOfSight = lineOfSight.copy();
            this.perpendicular = perpendicular.copy();
        }

        public Pointing() {
            this(new GeocentricCoordinates(1, 0, 0),
                    new GeocentricCoordinates(0, 1, 0));
        }

        /**
         * Gets the line of sight component of the pointing.
         * Warning: creates a copy - if you can reuse your own
         * GeocentricCoordinates object it might be more efficient to
         * use {@link #getLineOfSightX()} etc.
         */
        public GeocentricCoordinates getLineOfSight() {
            return lineOfSight.copy();
        }

        /**
         * Gets the perpendicular component of the pointing.
         * Warning: creates a copy - if you can reuse your own
         * GeocentricCoordinates object it might be more efficient to
         * use {@link #getLineOfSightX()} etc.
         */
        public GeocentricCoordinates getPerpendicular() {
            return perpendicular.copy();
        }

        public float getLineOfSightX() {
            return lineOfSight.x;
        }

        public float getLineOfSightY() {
            return lineOfSight.y;
        }

        public float getLineOfSightZ() {
            return lineOfSight.z;
        }

        public float getPerpendicularX() {
            return perpendicular.x;
        }

        public float getPerpendicularY() {
            return perpendicular.y;
        }

        public float getPerpendicularZ() {
            return perpendicular.z;
        }

        /**
         * Only the AstronomerModel should change this.
         */
        void updatePerpendicular(Vector3 newPerpendicular) {
            perpendicular.assign(newPerpendicular);

        }

        /**
         * Only the AstronomerModel should change this.
         */
        void updateLineOfSight(Vector3 newLineOfSight) {
            lineOfSight.assign(newLineOfSight);
        }
    }

    /**
     * If set to false, will not update the pointing automatically.
     */
    void setAutoUpdatePointing(boolean autoUpdatePointing);

    /**
     * Gets the field of view in degrees.
     */
    float getFieldOfView();

    void setFieldOfView(float degrees);

    void setHorizontalRotation(boolean value);

    float getMagneticCorrection();

    /**
     * Returns the time, as UTC.
     */
    Date getTime();

    /**
     * Sets the clock that provides the time.
     */
    void setClock();

    /**
     * Returns the astronomer's current location on Earth.
     */
    LatLong getLocation();

    /**
     * Sets the user's current position on Earth.
     */
    void setLocation(LatLong location);

    /**
     * Gets the user's direction of view.
     */
    Pointing getPointing();

    /**
     * Sets the user's direction of view.
     */
    void setPointing(Vector3 lineOfSight, Vector3 perpendicular);

    /**
     * Gets the acceleration vector in the phone frame of reference.
     *
     * <p>The returned object should not be modified.
     */
    Vector3 getPhoneUpDirection();

    /**
     * Sets the acceleration and magnetic field in the phone frame.
     *
     * <p>The phone frame has x along the short side of the phone increasing to
     * the right, y along the long side increasing towards the top of the phone,
     * and z coming perpendicularly out of the phone increasing towards the user.
     */
    void setPhoneSensorValues(Vector3 acceleration, Vector3 magneticField);

    /**
     * Sets the phone's rotation vector from the fused gyro/mag field/accelerometer.
     * Alternative to {@link #setPhoneSensorValues(Vector3, Vector3)}
     */
    void setPhoneSensorValues(float[] rotationVector);

    /**
     * Returns the user's North in celestial coordinates.
     */
    GeocentricCoordinates getNorth();

    /**
     * Returns the user's South in celestial coordinates.
     */
    GeocentricCoordinates getSouth();

    /**
     * Returns the user's Zenith in celestial coordinates.
     */
    GeocentricCoordinates getZenith();

    /**
     * Returns the user's Nadir in celestial coordinates.
     */
    GeocentricCoordinates getNadir();

    /**
     * Returns the user's East in celestial coordinates.
     */
    GeocentricCoordinates getEast();

    /**
     * Returns the user's West in celestial coordinates.
     */
    GeocentricCoordinates getWest();

    void setMagneticDeclinationCalculator(MagneticDeclinationCalculator calculator);

    long getTimeMillis();
}
