// Copyright 2016 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.example.finnur.finnursphotopicker;

import android.os.AsyncTask;
import android.os.Environment;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class FileEnumWorkerTask extends AsyncTask<String, Void, List<PickerBitmap>> {
    public interface FilesEnumeratedCallback {
        void filesEnumeratedCallback(List<PickerBitmap> files);
    }

    private FilesEnumeratedCallback mCallback;
    private AcceptFileFilter mFilter;

    public FileEnumWorkerTask(FilesEnumeratedCallback callback) {
        this(callback, AcceptFileFilter.getDefault());
    }

    public FileEnumWorkerTask(FilesEnumeratedCallback callback, AcceptFileFilter filter) {
        mCallback = callback;
        mFilter = filter;
    }

    // Enumerate files in background.
    @Override
    protected List<PickerBitmap> doInBackground(String... params) {
        if (isCancelled()) {
            return null;
        }

        List<PickerBitmap> pickerBitmaps = new ArrayList<>();

        String paths[] = new String[3];
        paths[0] = "/DCIM/Camera";
        paths[1] = "/Pictures/Screenshots";
        paths[2] = "/Download";

        for (int path = 0; path < paths.length; ++path) {
            String filePath = Environment.getExternalStorageDirectory().toString() + paths[path];
            File directory = new File(filePath);
            File[] files = directory.listFiles(mFilter);
            if (files == null) {
                continue;
            }

            for (File file : files) {
                if (isCancelled()) {
                    return null;
                }

                pickerBitmaps.add(
                        new PickerBitmap(filePath + "/" + file.getName(),
                                PickerBitmap.TileTypes.PICTURE,
                                file.lastModified()));
            }
        }

        Collections.sort(pickerBitmaps);

        pickerBitmaps.add(0, new PickerBitmap("", PickerBitmap.TileTypes.GALLERY, 0));
        pickerBitmaps.add(0, new PickerBitmap("", PickerBitmap.TileTypes.CAMERA, 0));

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
