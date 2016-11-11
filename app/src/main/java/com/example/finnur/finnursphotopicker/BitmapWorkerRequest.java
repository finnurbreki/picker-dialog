// Copyright 2016 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

// FLIP
//package org.chromium.chrome.browser;
package com.example.finnur.finnursphotopicker;

import android.content.Context;
import android.graphics.Bitmap;

public class BitmapWorkerRequest {
    public interface ImageDecodedCallback {
        void imageDecodedCallback(String filePath, Bitmap bitmap);
    }

    private Context mContext;
    private int mImageSize;
    private ImageDecodedCallback mCallback;

    public BitmapWorkerRequest(Context context, int imageSize, ImageDecodedCallback callback) {
        mContext = context;
        mImageSize = imageSize;
        mCallback = callback;
    }

    public Context getContext() { return mContext; }
    public int getImageSize() { return mImageSize; }
    public ImageDecodedCallback getCallback() { return mCallback; }
}
