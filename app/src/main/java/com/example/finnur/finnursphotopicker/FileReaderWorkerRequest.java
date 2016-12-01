// Copyright 2016 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

// FLIP
//package org.chromium.chrome.browser;
package com.example.finnur.finnursphotopicker;

import android.content.Context;
import android.os.AsyncTask;

public class FileReaderWorkerRequest {
    private Context mContext;
    private int mImageSize;
    private FileReaderWorkerTask.FileReadCallback mCallback;

    // The timestap for when this class started decoding the image.
    private long mRequestStartTime;

    public FileReaderWorkerRequest(
            Context context, int imageSize, FileReaderWorkerTask.FileReadCallback callback) {
        mContext = context;
        mImageSize = imageSize;
        mCallback = callback;
        mRequestStartTime = System.nanoTime();
    }

    public Context getContext() { return mContext; }
    public int getImageSize() { return mImageSize; }
    public FileReaderWorkerTask.FileReadCallback getCallback() { return mCallback; }
    public long getRequestStartTime() { return mRequestStartTime; }
}
