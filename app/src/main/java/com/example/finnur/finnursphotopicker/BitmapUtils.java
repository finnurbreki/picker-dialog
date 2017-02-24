// Copyright 2016 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.example.finnur.finnursphotopicker;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.FileDescriptor;

class BitmapUtils {
    public static Bitmap sizeBitmap(Bitmap bitmap, int width) {
        bitmap = ensureMinSize(bitmap, width);
        bitmap = cropToSquare(bitmap, width);
        return bitmap;
    }

    public static Bitmap decodeBitmapFromFileDescriptor(FileDescriptor fd, int width) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFileDescriptor(fd, null, options);
        options.inSampleSize = calculateInSampleSize(options, width, width);
        options.inJustDecodeBounds = false;
        Bitmap bitmap = BitmapFactory.decodeFileDescriptor(fd, null, options);
        if (bitmap == null) {
            return null;
        }
        return sizeBitmap(bitmap, width);
    }

    public static Bitmap decodeBitmapFromDisk(String filePath, int width) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filePath, options);
        options.inSampleSize = calculateInSampleSize(options, width, width);
        options.inJustDecodeBounds = false;
        Bitmap bitmap = BitmapFactory.decodeFile(filePath, options);
        if (bitmap == null) {
            return null;
        }
        return sizeBitmap(bitmap, width);
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
