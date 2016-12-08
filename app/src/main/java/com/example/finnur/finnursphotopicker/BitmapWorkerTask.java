// Copyright 2016 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

// FLIP
//package org.chromium.chrome.browser;
package com.example.finnur.finnursphotopicker;

import android.graphics.Bitmap;
import android.os.AsyncTask;

class BitmapWorkerTask extends AsyncTask<String, Void, Bitmap> {
    public interface ImageDecodedCallback {
        void imageDecodedCallback(String filePath, Bitmap bitmap, long requestStartTime);
    }

    private BitmapWorkerRequest mRequest;
    private String mFilePath;
    private ImageDecodedCallback mCallback;

    public BitmapWorkerTask(BitmapWorkerRequest request) {
        mRequest = request;
        mCallback = request.getCallback();
    }

    // Decode image in background.
    @Override
    protected Bitmap doInBackground(String... params) {
        if (isCancelled()) {
            return null;
        }

        mFilePath = params[0];

        return BitmapUtils.decodeBitmapFromDisk(mFilePath, mRequest.getImageSize());
    }

    // Once complete, see if ImageView is still around and set bitmap.
    @Override
    protected void onPostExecute(Bitmap bitmap) {
        if (isCancelled()) {
            return;
        }

        mCallback.imageDecodedCallback(mFilePath, bitmap, mRequest.getRequestStartTime());
    }
}
