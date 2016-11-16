// Copyright 2016 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

// FLIP
//package org.chromium.chrome.browser;
package com.example.finnur.finnursphotopicker;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.util.concurrent.TimeUnit;

class BitmapUtils {
    public static Bitmap retrieveBitmap(Context context, String filePath, int width) {
        //long startTime = System.nanoTime();

        //Runtime info = Runtime.getRuntime();
        //long diff1 = (info.maxMemory() - info.totalMemory()) / 1024;

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filePath, options);
        options.inSampleSize = calculateInSampleSize(options, width, width);
        options.inJustDecodeBounds = false;
        Bitmap bitmap = BitmapFactory.decodeFile(filePath, options);
        if (bitmap == null)
            return null;

        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        int size = bitmap.getByteCount() / 1024;
        bitmap = ensureMinSize(bitmap, width);
        int sizeEnlarged = bitmap.getByteCount() / 1024;
        bitmap = cropToSquare(bitmap, width);
        int sizeCropped = bitmap.getByteCount() / 1024;
        //Log.e("chromium", "Bitmap decoded size: " + size + " KB, enlarged: " + sizeEnlarged + " KB, cropped: " + sizeCropped + " KB");
        //long endTime = System.nanoTime();
        //long durationInMs = TimeUnit.MILLISECONDS.convert(endTime - startTime, TimeUnit.NANOSECONDS);

        //info = Runtime.getRuntime();
        //long diff2 = (info.maxMemory() - info.totalMemory()) / 1024;
        //String memory = "Mem delta: " + diff1 + " KB -> " + diff2 + " KB";

        //Log.e("chromium", "Bitmap " + w + "x" + h + " size " + size + " KB (now " + bitmap.getWidth() + "x" + bitmap.getHeight() + " size " + (bitmap.getByteCount() / 1024) + " KB) loaded in " +  durationInMs + " ms. " + memory);
        return bitmap;
    }

    private static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            while ((halfHeight / inSampleSize) >= reqHeight
                    || (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    public static Bitmap scale(Bitmap bitmap, float scaleMaxSize, boolean filter) {
        float ratio = Math.min((float) scaleMaxSize / bitmap.getWidth(),
                (float) scaleMaxSize / bitmap.getHeight());
        int height = Math.round(ratio * bitmap.getHeight());
        int width = Math.round(ratio * bitmap.getWidth());

        return Bitmap.createScaledBitmap(bitmap, width, height, filter);
    }

    // TODOf can we make this and next private?
    public static Bitmap ensureMinSize(Bitmap bitmap, int size) {
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

    public static Bitmap cropToSquare(Bitmap bitmap, int size) {
        int x = 0;
        int y = 0;
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        if (width > size)
            x = (width - size) / 2;
        if (height > size)
            y = (height - size) / 2;
        return Bitmap.createBitmap(bitmap, x, y, size, size);
    }
}
