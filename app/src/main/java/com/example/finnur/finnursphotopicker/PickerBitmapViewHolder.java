// Copyright 2016 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

// FLIP
//package org.chromium.chrome.browser;
package com.example.finnur.finnursphotopicker;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/*
FLIP
import org.chromium.chrome.R;
import org.chromium.chrome.browser.download.ui.ThumbnailProvider;
*/

/** Holds onto a View that displays information about a picker bitmap. */
public class PickerBitmapViewHolder extends RecyclerView.ViewHolder
        implements ThumbnailProvider.ThumbnailRequest, BitmapWorkerTask.ImageDecodedCallback {
    // Our parent category.
    private PickerCategoryView mCategoryView;

    // The bitmap view we are holding on to.
    private final PickerBitmapView mItemView;

    // The request we are showing the bitmap for.
    private PickerBitmap mItem;

    // A worker task for asynchronously decoding images off the main thread.
    private BitmapWorkerTask mWorkerTask;

    public PickerBitmapViewHolder(View itemView) {
        super(itemView);

        assert itemView instanceof PickerBitmapView;
        mItemView = (PickerBitmapView) itemView;
    }

    // ThumbnailProvider.ThumbnailRequest:

    @Override
    public String getFilePath() {
        return mItem == null ? null : mItem.getFilePath();
    }

    @Override
    public void onThumbnailRetrieved(String filePath, Bitmap thumbnail) {
        // TODOf don't crop on the UI thread...
        if (thumbnail != null) {
            Log.e("chromium", "w x h precrop: " + thumbnail.getWidth() + " x " + thumbnail.getHeight() + " size: " + thumbnail.getByteCount());

            long startTime = System.nanoTime();

            int size = mCategoryView.getImageSize();
            Bitmap bitmap = BitmapUtils.ensureMinSize(thumbnail, size);
            bitmap = BitmapUtils.cropToSquare(bitmap, size);
            imageDecodedCallback(filePath, bitmap, 0);  // TODOf figure out 0

            long endTime = System.nanoTime();
            long durationInMs = TimeUnit.MILLISECONDS.convert(endTime - startTime, TimeUnit.NANOSECONDS);
            Log.e("chromium", "Time since image cropping started: " + durationInMs + " ms");
        }
    }

    // BitmapWorkerRequest.ImageDecodedCallback

    @Override
    public void imageDecodedCallback(String filePath, Bitmap bitmap, long requestStartTime) {
        if (bitmap == null || bitmap.getWidth() == 0 || bitmap.getHeight() == 0) {
            Log.e("chromium", "Missing bitmap");
            return;
        }

        if (mCategoryView.getPrettyBitmaps().get(filePath) == null) {
            mCategoryView.getPrettyBitmaps().put(filePath, bitmap);
        }

        if (mCategoryView.getUglyBitmaps().get(filePath) == null) {
            Bitmap ugly = BitmapUtils.scale(bitmap, 40, false);
            mCategoryView.getUglyBitmaps().put(filePath, ugly);
        }

        if (!TextUtils.equals(getFilePath(), filePath)) {
            Log.e("chromium", "Wrong holder");
            return;
        }

        Log.e("chromium", "w x h: " + bitmap.getWidth() + " x " + bitmap.getHeight() + " size: " + bitmap.getByteCount());
        long endTime = System.nanoTime();
        long durationInMs = TimeUnit.MILLISECONDS.convert(endTime - requestStartTime, TimeUnit.NANOSECONDS);
        //Log.e("chromium", "Time spent fetching images: " + durationInMs + " ms");

        mItemView.setThumbnailBitmap(bitmap);
    }

    public void displayItem(Context context, PickerCategoryView categoryView, int position) {
        mCategoryView = categoryView;

        List<PickerBitmap> pickerBitmaps = mCategoryView.getPickerBitmaps();
        mItem = pickerBitmaps.get(position);
        boolean expandTile = position == mCategoryView.getMaxImagesShown() - 1;

        Log.e("chromium", "PickerBitmapViewHolder::displayItem position: " + position + " expandTile: " + expandTile);

        String filePath = mItem.getFilePath();
        if (isImageExtension(filePath)) {
            Bitmap original = mCategoryView.getPrettyBitmaps().get(filePath);
            if (original != null) {
                mItemView.initialize(mItem, original, false);
                return;
            }

            int size = mCategoryView.getImageSize();
            Bitmap placeholder = mCategoryView.getUglyBitmaps().get(filePath);
            if (placeholder != null) {
                placeholder = BitmapUtils.scale(placeholder, size, false);
                mItemView.initialize(mItem, placeholder, false);
            } else {
                placeholder = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
                placeholder.eraseColor(Color.LTGRAY);
                mItemView.initialize(mItem, placeholder, true);
            }

            // FLIP
            boolean useThumbnailProvider = false;
            if (useThumbnailProvider) {
                Bitmap cachedBitmap = mCategoryView.getThumbnailProvider().getThumbnail(this);
                if (cachedBitmap != null)
                    imageDecodedCallback(filePath, cachedBitmap, 0);  // TODOf figure out 0
            } else {
                if (mWorkerTask != null)
                    mWorkerTask.cancel(true);

                BitmapWorkerRequest request = new BitmapWorkerRequest(context, size, this);
                mWorkerTask = new BitmapWorkerTask(request);
                mWorkerTask.execute(filePath);
            }
        } else {
            mItemView.initialize(mItem, null, false);
            if (mItem.type() == PickerBitmap.TileTypes.NORMAL)
                mItemView.setTextWithOverlay();
            else
                mItemView.initializeSpecialTile();
        }

        // TODOf implement expand tile
        /*
                if (holder.mIsExpandTitle) {
            // Start with a slightly darkened thumbnail.
            canvas.drawARGB(120, 0, 0, 0);

            // Prepare the paint object.
            Paint paint = new Paint();
            paint.setAlpha(255);
            paint.setColor(Color.WHITE);
            float textHeight = 96;
            paint.setTextSize(textHeight);

            // Calculate where to place the text overlay.
            String overlayText = Integer.toString(mPickerBitmaps.size() - mMaxBitmaps) + " >";
            Rect textBounds = new Rect();
            paint.getTextBounds(overlayText, 0, overlayText.length(), textBounds);

            // Add the number.
            float x = (mPhotoSize - textBounds.width()) / 2;
            float y = (mPhotoSize - textBounds.height()) / 2;
            canvas.drawText(overlayText, x, y, paint);
        }

        holder.mImageView.setImageBitmap(thumbnail);

         */
    }

    private boolean isImageExtension(String filePath) {
        String file = filePath.toLowerCase(Locale.US);
        return file.endsWith(".jpg") || file.endsWith(".gif") || file.endsWith(".png");
    }
}
