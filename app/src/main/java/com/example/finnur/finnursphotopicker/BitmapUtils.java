// Copyright 2017 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.example.finnur.finnursphotopicker;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.FileDescriptor;

/**
 * A collection of utility functions for dealing with bitmaps.
 */
class BitmapUtils {
    /**
     * Takes a |bitmap| and returns a square thumbnail of |width| from the center of the bitmap
     * specified.
     * @param bitmap The bitmap to adjust.
     * @param width The desired width.
     * @return The new bitmap thumbnail.
     */
    public static Bitmap sizeBitmap(Bitmap bitmap, int width) {
        bitmap = ensureMinSize(bitmap, width);
        bitmap = cropToSquare(bitmap, width);
        return bitmap;
    }

    /**
     * Given a FileDescriptor, decodes the contents and returns a bitmap of
     * dimensions |size|x|size|.
     * @param descriptor The FileDescriptor for the file to read.
     * @param size The width and height of the bitmap to return.
     * @return The resulting bitmap.
     */
    public static Bitmap decodeBitmapFromFileDescriptor(FileDescriptor descriptor, int size) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFileDescriptor(descriptor, null, options);
        options.inSampleSize = calculateInSampleSize(options.outWidth, options.outHeight, size);
        options.inJustDecodeBounds = false;
        Bitmap bitmap = BitmapFactory.decodeFileDescriptor(descriptor, null, options);

        if (bitmap == null) return null;

        return sizeBitmap(bitmap, size);
    }

    /**
     * Calculates the sub-sampling factor {@link BitmapFactory#inSampleSize} option for a given
     * image dimensions, which will be used to create a bitmap of a pre-determined size (no larger
     * than |size|x|size|).
     * @param width The calculated width of the image to decode.
     * @param height The calculated height of the image to decode.
     * @param maxSize The maximum size the image should be (in either dimension).
     * @return The sub-sampling factor (1 = no change, 2 = half-size, etc).
     */
    private static int calculateInSampleSize(int width, int height, int maxSize) {
        int inSampleSize = 1;
        while ((height / inSampleSize) >= maxSize || (width / inSampleSize) >= maxSize) {
            inSampleSize *= 2;
        }
        return inSampleSize;
    }

    /**
     * Scales a |bitmap| to a certain size.
     * @param bitmap The bitmap to scale.
     * @param scaleMaxSize What to scale it to.
     * @param filter True if the source should be filtered.
     * @return The resulting scaled bitmap.
     */
    public static Bitmap scale(Bitmap bitmap, float scaleMaxSize, boolean filter) {
        float ratio = Math.min((float) scaleMaxSize / bitmap.getWidth(),
                (float) scaleMaxSize / bitmap.getHeight());
        int height = Math.round(ratio * bitmap.getHeight());
        int width = Math.round(ratio * bitmap.getWidth());

        return Bitmap.createScaledBitmap(bitmap, width, height, filter);
    }

    /**
     * Ensures a |bitmap| is at least |size| in both width and height.
     * @param bitmap The bitmap to modify.
     * @param size The minimum size (width and height).
     * @return The resulting (scaled) bitmap.
     */
    private static Bitmap ensureMinSize(Bitmap bitmap, int size) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        if (width < size) {
            float scale = (float) size / width;
            width = size;
            height *= scale;
        }

        if (height < size) {
            float scale = (float) size / height;
            height = size;
            width *= scale;
        }

        return Bitmap.createScaledBitmap(bitmap, width, height, true);
    }

    /**
     * Crops a |bitmap| to a certain square |size|
     * @param bitmap The bitmap to crop.
     * @param size The size desired (width and height).
     * @return The resulting (square) bitmap.
     */
    private static Bitmap cropToSquare(Bitmap bitmap, int size) {
        int x = 0;
        int y = 0;
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        if (width > size) x = (width - size) / 2;
        if (height > size) y = (height - size) / 2;
        return Bitmap.createBitmap(bitmap, x, y, size, size);
    }
}
