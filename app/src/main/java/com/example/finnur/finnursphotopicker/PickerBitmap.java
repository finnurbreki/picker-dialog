// Copyright 2016 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.example.finnur.finnursphotopicker;

/**
 * A class to keep track of the meta data associated with a an image in the
 * photo picker.
 */
public class PickerBitmap implements Comparable<PickerBitmap> {
    // The file path to the bitmap to show.
    private String mFilePath;
    private TileTypes mType;
    private long mLastModified;

    public enum TileTypes { PICTURE, CAMERA, GALLERY }

    public PickerBitmap(String filePath, TileTypes type, long lastModified) {
        mFilePath = filePath;
        mType = type;
        mLastModified = lastModified;
    }

    public String getFilePath() {
        return mFilePath;
    }

    public long getLastModified() {
        return mLastModified;
    }

    public TileTypes type() {
        return mType;
    }

    @Override
    public int compareTo(PickerBitmap f) {
        if (mLastModified < f.mLastModified) {
            return 1;
        } else if (mLastModified > f.mLastModified) {
            return -1;
        }
        return 0;
    }
}
