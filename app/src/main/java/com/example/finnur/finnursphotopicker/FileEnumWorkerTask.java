// Copyright 2017 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.example.finnur.finnursphotopicker;

import android.os.AsyncTask;
import android.os.Environment;

// Chrome-specific imports:
// import org.chromium.base.ThreadUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * A worker task to enumerate image files on disk.
 */
class FileEnumWorkerTask extends AsyncTask<Void, Void, List<PickerBitmap>> {
    /**
     * An interface to use to communicate back the results to the client.
     */
    public interface FilesEnumeratedCallback {
        /**
         * A callback to define to receive the list of all images on disk.
         * @param files The list of images.
         */
        void filesEnumeratedCallback(List<PickerBitmap> files);
    }

    // The callback to use to communicate the results.
    private FilesEnumeratedCallback mCallback;

    // The filter to apply to the list.
    private AttrAcceptFileFilter mFilter;

    /**
     * A FileEnumWorkerTask constructor.
     * @param callback The callback to use to communicate back the results.
     * @param filter The file filter to apply to the list.
     */
    public FileEnumWorkerTask(FilesEnumeratedCallback callback, AttrAcceptFileFilter filter) {
        mCallback = callback;
        mFilter = filter;
    }

    /**
     * Enumerates (in the background) the image files on disk. Called on a non-UI thread
     * @param params Ignored, do not use.
     * @return A sorted list of images (by last-modified first).
     */
    @Override
    protected List<PickerBitmap> doInBackground(Void... params) {
        // assert !ThreadUtils.runningOnUiThread();

        if (isCancelled()) return null;

        List<PickerBitmap> pickerBitmaps = new ArrayList<>();

        String paths[] = new String[3];
        // TODOf look at how Google Photos and such do this.
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

                if (isImageExtension(file.getName())) {
                    pickerBitmaps.add(new PickerBitmap(filePath + "/" + file.getName(),
                            file.lastModified(), PickerBitmap.TileTypes.PICTURE));
                }
            }
        }

        Collections.sort(pickerBitmaps);

        pickerBitmaps.add(0, new PickerBitmap("", 0, PickerBitmap.TileTypes.GALLERY));
        pickerBitmaps.add(0, new PickerBitmap("", 0, PickerBitmap.TileTypes.CAMERA));

        return pickerBitmaps;
    }

    /**
     * @param filePath The file path to consider.
     * @return true if the |filePath| ends in an image extension.
     */
    private boolean isImageExtension(String filePath) {
        // TODOf This is error prone, use MimeTypeMap instead.
        String file = filePath.toLowerCase(Locale.US);
        return file.endsWith(".jpg") || file.endsWith(".gif") || file.endsWith(".png");
    }

    /**
     * Communicates the results back to the client. Called on the UI thread.
     * @param files The resulting list of files on disk.
     */
    @Override
    protected void onPostExecute(List<PickerBitmap> files) {
        if (isCancelled()) {
            return;
        }

        mCallback.filesEnumeratedCallback(files);
    }
}
