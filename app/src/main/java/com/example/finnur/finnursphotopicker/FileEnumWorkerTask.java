// Copyright 2016 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

// FLIP
//package org.chromium.chrome.browser;
package com.example.finnur.finnursphotopicker;

import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

class FileEnumWorkerTask extends AsyncTask<String, Void, List<PickerBitmap>> {
    public interface FilesEnumeratedCallback {
        void filesEnumeratedCallback(List<PickerBitmap> files);
    }

    private BitmapWorkerRequest mRequest;
    private FilesEnumeratedCallback mCallback;

    public FileEnumWorkerTask(FilesEnumeratedCallback callback) {
        mCallback = callback;
    }

    // Enumerate files in background.
    @Override
    protected List<PickerBitmap> doInBackground(String... params) {
        if (isCancelled()) {
            return null;
        }

        long startTime = System.nanoTime();

        List<PickerBitmap> pickerBitmaps = new ArrayList<>();
        pickerBitmaps.add(new PickerBitmap("", PickerBitmap.TileTypes.CAMERA));
        pickerBitmaps.add(new PickerBitmap("", PickerBitmap.TileTypes.GALLERY));

        String filePath = Environment.getExternalStorageDirectory().toString() + params[0];
        File directory = new File(filePath);
        File[] files = directory.listFiles();
        for (int i = 0; i < files.length; i++) {
            if (isCancelled()) {
                return null;
            }

            //Log.e("chromium", "FileName:" + fullPath + "/" + files[i].getName() +
            //                  " size: " + files[i].length());
            pickerBitmaps.add(
                    new PickerBitmap(filePath + "/" + files[i].getName(),
                            PickerBitmap.TileTypes.NORMAL));
        }

        long endTime = System.nanoTime();
        long durationInMs =
                TimeUnit.MILLISECONDS.convert(endTime - startTime, TimeUnit.NANOSECONDS);
        Log.e("chromium", "Enumerated " + files.length + " files: " + durationInMs + " ms");

        return pickerBitmaps;
    }

    // Once complete, see if ImageView is still around and set bitmap.
    @Override
    protected void onPostExecute(List<PickerBitmap> files) {
        if (isCancelled()) {
            return;
        }

        mCallback.filesEnumeratedCallback(files);
    }
}
