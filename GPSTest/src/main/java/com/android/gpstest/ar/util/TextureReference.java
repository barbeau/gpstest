// Copyright 2009 Google Inc.
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

import javax.microedition.khronos.opengles.GL10;

/**
 * Represents a reference to an OpenGL texture.  You may bind the texture to
 * set it active, or delete it.  Normal Java garbage collection will not
 * reclaim this, so you must delete it when you are done with it.  Note that
 * when the OpenGL surface is re-created, any existing texture references are
 * automatically invalidated and should not be bound or deleted.
 *
 * @author James Powell
 */
public interface TextureReference {
    /**
     * Sets this as the active texture on the OpenGL context.
     *
     * @param gl The OpenGL context
     */
    void bind(GL10 gl);

    /**
     * Deletes the texture resource.  This should not be called multiple times.
     * Note that when the OpenGL surface is being re-created, all resources
     * are automatically freed, so you should not delete the textures in that
     * case.
     *
     * @param gl
     */
    void delete(GL10 gl);
}
