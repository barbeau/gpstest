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

/**
 * A base {@link UpdateClosure} that implements
 * {@link Comparable#compareTo(Object)} using hash codes so that they can
 * be used in TreeSets.
 *
 * @author Brent Bryan
 * @author John Taylor
 */
public abstract class AbstractUpdateClosure implements UpdateClosure {
    @Override
    public int compareTo(UpdateClosure that) {
        int thisHashCode = this.hashCode();
        int thatHashCode = that.hashCode();

        if (thisHashCode == thatHashCode) {
            return 0;
        }
        return (thisHashCode < thatHashCode) ? -1 : 1;
    }
}
