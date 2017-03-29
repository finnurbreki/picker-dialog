// Copyright 2016 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.example.finnur.finnursphotopicker;

/**
 * A class to keep track of the meta data associated with a an image in the
 * photo picker.
 */
public class PickerBitmap implements Comparable<PickerBitmap> {
    // The possible types of tiles involved in the viewer.
    public enum TileTypes { PICTURE, CAMERA, GALLERY }

    // The file path to the bitmap to show.
    private String mFilePath;

    // When the bitmap was last modified on disk.
    private long mLastModified;

    // The type of tile involved.
    private TileTypes mType;

    /**
     * The PickerBitmap constructor.
     * @param filePath The file path to the bitmap to show.
     * @param lastModified When the bitmap was last modified on disk.
     * @param type The type of tile involved.
     */
    public PickerBitmap(String filePath, long lastModified, TileTypes type) {
        mFilePath = filePath;
        mLastModified = lastModified;
        mType = type;
    }

    /**
     * Accessor for the filepath.
     * @return The file path for this PickerBitmap object.
     */
    public String getFilePath() {
        return mFilePath;
    }

    /**
     * Accessor for the tile type.
     * @return The type of tile involved for this bitmap object.
     */
    public TileTypes type() {
        return mType;
    }

    /**
     * A comparison function for PickerBitmaps (results in a last-modified first sort).
     * @param other The PickerBitmap to compare it to.
     * @return 0, 1, or -1, depending on which is bigger.
     */
    @Override
    public int compareTo(PickerBitmap other) {
        if (mLastModified < other.mLastModified) {
            return 1;
        } else if (mLastModified > other.mLastModified) {
            return -1;
        }
        return 0;
    }

    @Override
    public final int hashCode() {
        return (mFilePath + mLastModified).hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (other instanceof PickerBitmap) {
            return compareTo((PickerBitmap) other) == 0;
        }
        return false;
    }
}
