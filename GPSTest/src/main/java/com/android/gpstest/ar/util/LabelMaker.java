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

package com.android.gpstest.ar.util;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.opengl.GLUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

import javax.microedition.khronos.opengles.GL10;

public class LabelMaker {
    private int mStrikeWidth;
    private int mStrikeHeight;
    private boolean mFullColor;
    private Bitmap mBitmap;
    private Canvas mCanvas;
    private Resources mRes;

    private TextureReference mTexture = null;

    private float mTexelWidth; // Convert texel to U
    private float mTexelHeight; // Convert texel to V

    /**
     * A class which contains data that describes a label and its position in the texture.
     */
    public static class LabelData {
        public LabelData(String text, int color, int fontSize) {
            mText = text;
            mColor = color;
            mFontSize = fontSize;
        }

        // Sets data about the label's position in the texture.
        public void setTextureData(int widthInPixels, int heightInPixels,
                                   int cropU, int cropV, int cropW, int cropH,
                                   float texelWidth, float texelHeight) {
            mWidthInPixels = widthInPixels;
            mHeightInPixels = heightInPixels;

            int[] texCoords = new int[8];
            // lower left
            texCoords[0] = FixedPoint.floatToFixedPoint(cropU * texelWidth);
            texCoords[1] = FixedPoint.floatToFixedPoint(cropV * texelHeight);

            // upper left
            texCoords[2] = FixedPoint.floatToFixedPoint(cropU * texelWidth);
            texCoords[3] = FixedPoint.floatToFixedPoint((cropV + cropH) * texelHeight);

            // lower right
            texCoords[4] = FixedPoint.floatToFixedPoint((cropU + cropW) * texelWidth);
            texCoords[5] = FixedPoint.floatToFixedPoint(cropV * texelHeight);

            // upper right
            texCoords[6] = FixedPoint.floatToFixedPoint((cropU + cropW) * texelWidth);
            texCoords[7] = FixedPoint.floatToFixedPoint((cropV + cropH) * texelHeight);

            mTexCoords = ByteBuffer.allocateDirect(8 * 4).order(ByteOrder.nativeOrder()).asIntBuffer();
            mTexCoords.put(texCoords);
            mTexCoords.position(0);

            mCrop = new int[]{cropU, cropV, cropW, cropH};
        }

        public String getText() {
            return mText;
        }

        public int getColor() {
            return mColor;
        }

        public int getFontSize() {
            return mFontSize;
        }

        public int getWidthInPixels() {
            return mWidthInPixels;
        }

        public int getHeightInPixels() {
            return mHeightInPixels;
        }

        public IntBuffer getTexCoords() {
            return mTexCoords;
        }

        public int[] getCrop() {
            return mCrop;
        }

        private String mText = "";
        private int mColor = 0xffffffff;
        private int mFontSize = 24;
        private int mWidthInPixels = 0;
        private int mHeightInPixels = 0;
        private IntBuffer mTexCoords = null;
        private int[] mCrop = null;
    }

    /**
     * Create a label maker or maximum compatibility with various OpenGL ES
     * implementations, the strike width and height must be powers of two, We want
     * the strike width to be at least as wide as the widest window.
     *
     * @param fullColor true if we want a full color backing store (4444),
     *                  otherwise we generate a grey L8 backing store.
     */
    public LabelMaker(boolean fullColor) {
        mFullColor = fullColor;
        mStrikeWidth = 512;
        mStrikeHeight = -1;
    }

    /**
     * Call to initialize the class. Call whenever the surface has been created.
     *
     * @param gl
     */
    public TextureReference initialize(GL10 gl, Paint textPaint, LabelData[] labels,
                                       Resources res, TextureManager textureManager) {
        mRes = res;
        mTexture = textureManager.createTexture(gl);
        mTexture.bind(gl);

        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER,
                GL10.GL_NEAREST);
        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER,
                GL10.GL_NEAREST);

        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S,
                GL10.GL_CLAMP_TO_EDGE);
        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T,
                GL10.GL_CLAMP_TO_EDGE);

        gl.glTexEnvf(GL10.GL_TEXTURE_ENV, GL10.GL_TEXTURE_ENV_MODE, GL10.GL_REPLACE);

        int minHeight = addLabelsInternal(gl, textPaint, false, labels);

        // Round up to the nearest power of two, since textures have to be a power of two in size.
        int roundedHeight = 1;
        while (roundedHeight < minHeight)
            roundedHeight <<= 1;

        mStrikeHeight = roundedHeight;

        mTexelWidth = (float) (1.0 / mStrikeWidth);
        mTexelHeight = (float) (1.0 / mStrikeHeight);

        beginAdding(gl);
        addLabelsInternal(gl, textPaint, true, labels);
        endAdding(gl);

        return mTexture;
    }

    /**
     * Call when the surface has been destroyed
     */
    public void shutdown(GL10 gl) {
        if (mTexture != null) {
            mTexture.delete(gl);
        }
    }

    /**
     * Call to add a list of labels
     *
     * @param gl
     * @param textPaint the paint of the label
     * @param labels    the array of labels being added
     * @return the required height
     */
    private int addLabelsInternal(GL10 gl, Paint textPaint, boolean drawToCanvas,
                                  LabelData[] labels) {
        int u = 0;
        int v = 0;
        int lineHeight = 0;
        for (LabelData label : labels) {
            int ascent = 0;
            int descent = 0;
            int measuredTextWidth = 0;

            int height = 0;
            int width = 0;

            // TODO(jpowell): This is a hack to deal with text that's too wide to
            // fit on the screen.  We should really split this up among multiple lines,
            // but just making the text smaller is much easier.

            int fontSize = label.getFontSize();
            do {
                textPaint.setColor(0xff000000 | label.getColor());
                textPaint.setTextSize(fontSize * mRes.getDisplayMetrics().density);

                // Paint.ascent is negative, so negate it.
                ascent = (int) Math.ceil(-textPaint.ascent());
                descent = (int) Math.ceil(textPaint.descent());
                measuredTextWidth = (int) Math.ceil(textPaint.measureText(label.getText()));

                height = ascent + descent;
                width = measuredTextWidth;

                // If it's wider than the screen, try it again with a font size of 1
                // smaller.
                fontSize--;
            } while (fontSize > 0 && width > mRes.getDisplayMetrics().widthPixels);

            int nextU;

            // Is there room for this string on the current line?
            if (u + width > mStrikeWidth) {
                // No room, go to the next line:
                u = 0;
                nextU = width;
                v += lineHeight;
                lineHeight = 0;
            } else {
                nextU = u + width;
            }

            lineHeight = Math.max(lineHeight, height);
            if (v + lineHeight > mStrikeHeight && drawToCanvas) {
                throw new IllegalArgumentException("Out of texture space.");
            }

            int vBase = v + ascent;

            if (drawToCanvas) {
                mCanvas.drawText(label.getText(), u, vBase, textPaint);

                label.setTextureData(width, height, u, v + height, width, -height,
                        mTexelWidth, mTexelHeight);
            }

            u = nextU;
        }

        return v + lineHeight;
    }

    private void beginAdding(GL10 gl) {
        Bitmap.Config config = mFullColor ? Bitmap.Config.ARGB_4444 : Bitmap.Config.ALPHA_8;
        mBitmap = Bitmap.createBitmap(mStrikeWidth, mStrikeHeight, config);
        mCanvas = new Canvas(mBitmap);
        mBitmap.eraseColor(0);
    }

    private void endAdding(GL10 gl) {
        mTexture.bind(gl);
        GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, mBitmap, 0);
        // Reclaim storage used by bitmap and canvas.
        mBitmap.recycle();
        mBitmap = null;
        mCanvas = null;
    }
}