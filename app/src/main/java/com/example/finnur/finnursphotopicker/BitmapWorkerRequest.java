// Copyright 2016 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.example.finnur.finnursphotopicker;

import android.content.Context;

public class BitmapWorkerRequest {
    private Context mContext;
    private int mImageSize;
    private BitmapWorkerTask.ImageDecodedCallback mCallback;

    public BitmapWorkerRequest(
            Context context, int imageSize, BitmapWorkerTask.ImageDecodedCallback callback) {
        mContext = context;
        mImageSize = imageSize;
        mCallback = callback;
    }

    public Context getContext() {
        return mContext;
    }
    public int getImageSize() {
        return mImageSize;
    }
    public BitmapWorkerTask.ImageDecodedCallback getCallback() {
        return mCallback;
    }
}
