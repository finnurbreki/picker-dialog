// Copyright 2017 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.example.finnur.finnursphotopicker;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;

import org.chromium.base.BuildInfo;
import org.chromium.base.ThreadUtils;
import org.chromium.base.task.AsyncTask;
import org.chromium.net.MimeTypeFilter;
//import org.chromium.ui.base.WindowAndroid;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * A worker task to enumerate image files on disk.
 */
class FileEnumWorkerTask extends AsyncTask<List<PickerBitmap>> {
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

    //private final WindowAndroid mWindowAndroid;

    // The callback to use to communicate the results.
    private FilesEnumeratedCallback mCallback;

    // The filter to apply to the list.
    private MimeTypeFilter mFilter;

    // Whether any image MIME types were requested.
    private boolean mIncludeImages;

    // Whether any video MIME types were requested.
    private boolean mIncludeVideos;

    // The ContentResolver to use to retrieve image metadata from disk.
    private ContentResolver mContentResolver;

    // The camera directory under DCIM.
    private static final String SAMPLE_DCIM_SOURCE_SUB_DIRECTORY = "Camera";

    /**
     * A FileEnumWorkerTask constructor.
     * @param windowAndroid The window wrapper associated with the current activity.
     * @param callback The callback to use to communicate back the results.
     * @param mimeTypes The MIME type filter to apply to the list.
     * @param contentResolver The ContentResolver to use to retrieve image metadata from disk.
     */
    public FileEnumWorkerTask(/*WindowAndroid windowAndroid,*/ FilesEnumeratedCallback callback,
          List<String> mimeTypes, ContentResolver contentResolver) {
        //mWindowAndroid = windowAndroid;
        mCallback = callback;
        mFilter = new MimeTypeFilter(mimeTypes, true);
        mContentResolver = contentResolver;

        for (String mimeType : mimeTypes) {
            if (mimeType.startsWith("image/")) mIncludeImages = true;
            else if (mimeType.startsWith("video/")) mIncludeVideos = true;
            if (mIncludeImages && mIncludeVideos) break;
        }
    }

    /**
     * Retrieves the DCIM/camera directory.
     */
    private File getCameraDirectory() {
        return new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
                SAMPLE_DCIM_SOURCE_SUB_DIRECTORY);
    }

    /**
     * Enumerates (in the background) the image files on disk. Called on a non-UI thread
     * @return A sorted list of images (by last-modified first).
     */
    @Override
    protected List<PickerBitmap> doInBackground() {
        assert !ThreadUtils.runningOnUiThread();

        if (isCancelled()) return null;

        List<PickerBitmap> pickerBitmaps = new ArrayList<>();

        final String[] selectColumns = {
                MediaStore.Files.FileColumns._ID,
                MediaStore.Files.FileColumns.DATE_ADDED,
                MediaStore.Files.FileColumns.MEDIA_TYPE,
                MediaStore.Files.FileColumns.MIME_TYPE,
                MediaStore.Files.FileColumns.DATA};

        String whereClause = "";
        if (mIncludeImages) {
            whereClause += MediaStore.Files.FileColumns.MEDIA_TYPE + "="
                    + MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE;
        }
        if (mIncludeVideos) {
            if (mIncludeImages) whereClause += " OR ";
            whereClause += MediaStore.Files.FileColumns.MEDIA_TYPE + "="
                    + MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO;
        }
        String[] whereArgs = null;
        // Looks like we loose access to the filter, starting with the Q SDK.
        if (!BuildInfo.isAtLeastQ()) {
            if (!whereClause.isEmpty()) whereClause = "(" + whereClause + ") AND ";
            whereClause += "(" + MediaStore.Files.FileColumns.DATA + " LIKE ? OR "
                    + MediaStore.Files.FileColumns.DATA + " LIKE ? OR " + MediaStore.Files.FileColumns.DATA
                    + " LIKE ?) AND " + MediaStore.Files.FileColumns.DATA + " NOT LIKE ?";

            whereArgs = new String[] {
                    // Include:
                    getCameraDirectory().toString() + "%",
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                            + "%",
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                            + "%",
                    // Exclude low-quality sources, such as the screenshots directory:
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                            + "/Screenshots/"
                            + "%"};
        }

        final String orderBy = MediaStore.MediaColumns.DATE_ADDED + " DESC";

        Uri contentUri = MediaStore.Files.getContentUri("external");
        Cursor imageCursor = mContentResolver.query(contentUri,
                selectColumns, whereClause, whereArgs, orderBy);

        while (imageCursor.moveToNext()) {
            int mimeTypeIndex = imageCursor.getColumnIndex(MediaStore.Files.FileColumns.MIME_TYPE);
            String mimeType = imageCursor.getString(mimeTypeIndex);
            if (!mFilter.accept(null, mimeType)) continue;

            int dateTakenIndex = imageCursor.getColumnIndex(MediaStore.Files.FileColumns.DATE_ADDED);
            int idIndex = imageCursor.getColumnIndex(MediaStore.Files.FileColumns._ID);
            Uri uri = ContentUris.withAppendedId(contentUri, imageCursor.getInt(idIndex));
            long dateTaken = imageCursor.getLong(dateTakenIndex);

            int dataIndex = imageCursor.getColumnIndex(MediaStore.Files.FileColumns.DATA);
            String data = imageCursor.getString(dataIndex);

            String filename = uri.getPath();
            @PickerBitmap.TileTypes
            int type = PickerBitmap.TileTypes.PICTURE;
            if (mimeType.startsWith("video/")) type = PickerBitmap.TileTypes.VIDEO;

            pickerBitmaps.add(new PickerBitmap(uri, dateTaken, type));
        }
        imageCursor.close();

        pickerBitmaps.add(0, new PickerBitmap(null, 0, PickerBitmap.TileTypes.GALLERY));
        boolean hasCameraAppAvailable = true;  // Avoid mWindowAndroid outside Chromium.
        //        mWindowAndroid.canResolveActivity(new Intent(MediaStore.ACTION_IMAGE_CAPTURE));
        boolean hasOrCanRequestCameraPermission = true;  // Hard-coded in Android Studio project.
        //        mWindowAndroid.hasPermission(Manifest.permission.CAMERA)
        //        || mWindowAndroid.canRequestPermission(Manifest.permission.CAMERA);
        if (hasCameraAppAvailable && hasOrCanRequestCameraPermission) {
            pickerBitmaps.add(0, new PickerBitmap(null, 0, PickerBitmap.TileTypes.CAMERA));
        }

        return pickerBitmaps;
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
