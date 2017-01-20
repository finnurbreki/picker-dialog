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
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

class FileEnumWorkerTask extends AsyncTask<String, Void, List<PickerBitmap>> {
    public interface FilesEnumeratedCallback {
        void filesEnumeratedCallback(List<PickerBitmap> files);
    }

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

        String paths[] = new String[3];
        paths[0] = "/DCIM/Camera";
        paths[1] = "/Pictures/Screenshots";
        paths[2] = "/Download";

        for (int path = 0; path < paths.length; ++path) {
            String filePath = Environment.getExternalStorageDirectory().toString() + paths[path];
            File directory = new File(filePath);
            File[] files = directory.listFiles();
            if (files == null) {
                continue;
            }

            for (int i = 0; i < files.length; ++i) {
                if (isCancelled()) {
                    return null;
                }

                File file = files[i];
                
                //Log.e("chromium", "FileName:" + file.getPath().toString() + "/" + file.getName() +
                //                  " size: " + file.length());
                pickerBitmaps.add(
                        new PickerBitmap(filePath + "/" + file.getName(),
                                PickerBitmap.TileTypes.PICTURE,
                                file.lastModified()));
            }
        }

        Collections.sort(pickerBitmaps);

        pickerBitmaps.add(0, new PickerBitmap("", PickerBitmap.TileTypes.GALLERY, 0));
        pickerBitmaps.add(0, new PickerBitmap("", PickerBitmap.TileTypes.CAMERA, 0));

        long endTime = System.nanoTime();
        long durationInMs =
                TimeUnit.MILLISECONDS.convert(endTime - startTime, TimeUnit.NANOSECONDS);
        Log.e("chromium", "Enumerated " + (pickerBitmaps.size() - 2) + " files: " +
                durationInMs + " ms");

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
