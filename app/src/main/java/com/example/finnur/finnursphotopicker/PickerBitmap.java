// Copyright 2016 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

//  FLIP
//package org.chromium.chrome.browser;
package com.example.finnur.finnursphotopicker;

// TODO(finnur): If this class doesn't get more complex then we can reduce it to a String.
public class PickerBitmap {
    // The file path to the bitmap to show.
    private String mFilePath;
    private TileTypes mType;

    public enum TileTypes {
        NORMAL, CAMERA, GALLERY
    }

    public PickerBitmap(String filePath, TileTypes type) {
        mFilePath = filePath;
        mType = type;
    }

    public String getFilePath() {
        return mFilePath;
    }

    public void setFilePath(String filePath) {
        mFilePath = filePath;
    }

    public TileTypes type() { return mType; }
}
