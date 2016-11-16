// Copyright 2016 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

// FLIP
//package org.chromium.chrome.browser;
package com.example.finnur.finnursphotopicker;

import android.content.Context;
import android.graphics.Bitmap;

public class BitmapWorkerRequest {
    private Context mContext;
    private int mImageSize;
    private BitmapWorkerTask.ImageDecodedCallback mCallback;

    // The timestap for when this class started decoding the image.
    private long mRequestStartTime;

    public BitmapWorkerRequest(
            Context context, int imageSize, BitmapWorkerTask.ImageDecodedCallback callback) {
        mContext = context;
        mImageSize = imageSize;
        mCallback = callback;
        mRequestStartTime = System.nanoTime();
    }

    public Context getContext() { return mContext; }
    public int getImageSize() { return mImageSize; }
    public BitmapWorkerTask.ImageDecodedCallback getCallback() { return mCallback; }
    public long getRequestStartTime() { return mRequestStartTime; }
}
