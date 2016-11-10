// Copyright 2016 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

// FLIP
//package org.chromium.chrome.browser.download.ui;
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
*/

/** Holds onto a View that displays information about a picker bitmap. */
public class PickerBitmapViewHolder extends RecyclerView.ViewHolder
        implements ThumbnailProvider.ThumbnailRequest, BitmapWorkerRequest.ImageDecodedCallback {
    // Our parent category.
    private PickerCategoryView mCategoryView;

    // The bitmap view we are holding on to.
    private final PickerBitmapView mItemView;

    // The request we are showing the bitmap for.
    private PickerBitmap mItem;

    // A worker task for asynchronously decoding images off the main thread.
    private BitmapWorkerTask mWorkerTask;

    // The timestap for when this class started decoding the image.
    private long mStartFetchImage;

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
        // TODOf merge this with other callback
        Log.e("chromium", "PickerBitmapViewHolder onThumbnailRetrieved");
        if (TextUtils.equals(getFilePath(), filePath) && thumbnail != null
                && thumbnail.getWidth() != 0 && thumbnail.getHeight() != 0) {
            mItemView.setThumbnailBitmap(thumbnail);
        }
    }

    // BitmapWorkerRequest.ImageDecodedCallback

    @Override
    public void imageDecodedCallback(String filePath, Bitmap bitmap) {
        if (filePath != mItem.getFilePath()) {
            Log.e("chromium", "Wrong holder");
            return;
        }

        long endTime = System.nanoTime();
        long durationInMs = TimeUnit.MILLISECONDS.convert(endTime - mStartFetchImage, TimeUnit.NANOSECONDS);
        //Log.e("chromium", "Time since image fetching started: " + durationInMs + " ms");

        mItemView.setThumbnailBitmap(bitmap);
    }

    public void displayItem(Context context, PickerCategoryView categoryView, int position) {
        mCategoryView = categoryView;

        List<PickerBitmap> pickerBitmaps = mCategoryView.getPickerBitmaps();
        mItem = pickerBitmaps.get(position);
        boolean expandTile = position == mCategoryView.getMaxImagesShown() - 1;

        Log.e("chromium", "PickerBitmapViewHolder::displayItem position: " + position + " expandTile: " + expandTile);

        if (isImageExtension(mItem.getFilePath())) {
            int size = mCategoryView.getImageSize();
            Bitmap placeholder = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
            placeholder.eraseColor(Color.LTGRAY);
            mItemView.initialize(mItem, placeholder);

            boolean useThumbnailProvider = false;
            if (useThumbnailProvider) {
                // Bitmap newBitmap = mThumbnailProvider.getThumbnail(holder);
                // setBitmapWithOverlay(holder, newBitmap);
            } else {
                if (mWorkerTask != null)
                    mWorkerTask.cancel(true);

                String filePath = mItem.getFilePath();
                mStartFetchImage = System.nanoTime();
                BitmapWorkerRequest request = new BitmapWorkerRequest(context, size, this);
                mWorkerTask = new BitmapWorkerTask(request);
                mWorkerTask.execute(filePath);
            }
        } else {
            mItemView.initialize(mItem, null);
            mItemView.setTextWithOverlay();
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