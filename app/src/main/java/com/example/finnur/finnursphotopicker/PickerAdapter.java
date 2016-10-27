// Copyright 2016 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

//package org.chromium.chrome.browser;
package com.example.finnur.finnursphotopicker;

import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Bitmap;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

// Chrome-specific:
/*
FLIP
import org.chromium.chrome.R;
import org.chromium.chrome.browser.download.ui.ThumbnailProvider;
import org.chromium.chrome.browser.download.ui.ThumbnailProviderImpl;
import org.chromium.chrome.browser.widget.selection.SelectionDelegate;
*/

import java.util.List;
import java.util.concurrent.TimeUnit;

public class PickerAdapter extends RecyclerView.Adapter<PickerAdapter.MyViewHolder> {

    private Resources mResources;

    // The thumbnail provider service.
    private ThumbnailProvider mThumbnailProvider;

    // The list of bitmaps to show.
    private List<PickerBitmap> mPickerBitmaps;

    // Maximum number of bitmaps to show.
    private int mMaxBitmaps;

    // The size of the bitmaps (equal length for width and height).
    private int mPhotoSize;

    private Bitmap mOverlaySelected;
    private Bitmap mOverlayUnselected;

    // Our photo selection delegate.
    private SelectionDelegate<String> mSelectionDelegate;

    public class MyViewHolder extends RecyclerView.ViewHolder implements
            ThumbnailProvider.ThumbnailRequest, View.OnClickListener {
        // The parent of this holder.
        private PickerAdapter mParent;

        // The path to the bitmap to load.
        public String mFilePath;

        // The imageview for showing the bitmap.
        public ImageView mImageView;
        public ImageView mSelectedView;
        public ImageView mUnselectedView;

        // Whether this bitmap represent the last bitmap of a set, signaling that more bitmaps are
        // available.
        public boolean mIsExpandTitle;
        private boolean mIsSelected = false;

        long mStartFetchImage;

        public MyViewHolder(View view, ThumbnailProvider provider, PickerAdapter parent) {
            super(view);
            mParent = parent;
            mImageView = (ImageView) view.findViewById(R.id.bitmap);
            mImageView.setOnClickListener(this);
            // mImageView.setScaleType(ScaleType.CENTER);
            mSelectedView = (ImageView) view.findViewById(R.id.selected);
            mSelectedView.setImageBitmap(mParent.mOverlaySelected);
            mUnselectedView = (ImageView) view.findViewById(R.id.unselected);
            mUnselectedView.setImageBitmap(mParent.mOverlayUnselected);
        }

        // ThumbnailProvider.ThumbnailRequest:

        @Override
        public String getFilePath() {
            //Log.e("chromium", "Requested file: " + mFilePath);
            return mFilePath;
        }

        @Override
        public void onThumbnailRetrieved(String filePath, Bitmap thumbnail) {
            long endTime = System.nanoTime();
            long durationInMs = TimeUnit.MILLISECONDS.convert(endTime - mStartFetchImage, TimeUnit.NANOSECONDS);
            Log.e("chromium", "Time since image fetching started: " + durationInMs + " ms");

            mParent.setBitmapWithOverlay(this, thumbnail);
        }

        @Override
        public void onClick(View view) {
            int position = getLayoutPosition();

            mParent.mSelectionDelegate.toggleSelectionForItem(mFilePath);
            mParent.notifyItemChanged(position, true);  // true=Payload (no data needed)
        }

        private void updateSelectionOverlays() {
            mSelectedView.setVisibility(mIsSelected ? View.VISIBLE : View.GONE);
            mUnselectedView.setVisibility(!mIsSelected ? View.VISIBLE : View.GONE);
        }
    }

    public PickerAdapter(List<PickerBitmap> pickerBitmaps, Resources resources, int widthPerColumn,
                         int maxBitmaps, SelectionDelegate<String> selectionDelegate) {
        mPickerBitmaps = pickerBitmaps;
        mResources = resources;
        mSelectionDelegate = selectionDelegate;
        mPhotoSize = widthPerColumn;
        mMaxBitmaps = maxBitmaps;

        mThumbnailProvider = new ThumbnailProviderImpl(widthPerColumn);

        mOverlaySelected = BitmapFactory.decodeResource(mResources, R.mipmap.selected);
        mOverlayUnselected = BitmapFactory.decodeResource(mResources, R.mipmap.unselected);
    }

    // RecyclerView.Adapter:

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.bitmap_list_row, parent, false);

        return new MyViewHolder(itemView, mThumbnailProvider, this);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        Log.e("chromium", "onBindViewHolder1 pos " + position);
        onBindViewHolder(holder, position, null);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position, List payloads) {
        Log.e("chromium", "onBindViewHolder2 pos " + position);
        PickerBitmap bitmap = mPickerBitmaps.get(position);
        holder.mFilePath = bitmap.getFilePath();
        holder.mIsExpandTitle = position == mMaxBitmaps - 1;
        holder.mImageView.setMaxHeight(mPhotoSize);
        holder.mStartFetchImage = System.nanoTime();

        if (payloads.size() == 0) {
            Bitmap newBitmap = mThumbnailProvider.getThumbnail(holder);
            setBitmapWithOverlay(holder, newBitmap);
        } else {
            holder.mIsSelected = !holder.mIsSelected;
            holder.updateSelectionOverlays();
        }
    }

    @Override
    public int getItemCount() {
        return Math.min(mPickerBitmaps.size(), mMaxBitmaps);
    }

    private void setBitmapWithOverlay(MyViewHolder holder, Bitmap thumbnailOriginal) {
        if (thumbnailOriginal == null) {
            holder.mImageView.setImageBitmap(null);
            return;
        }

        Boolean isSelected = mSelectionDelegate.isItemSelected(holder.mFilePath);

        Bitmap thumbnail = thumbnailOriginal.copy(thumbnailOriginal.getConfig(), true);

        Log.e("chromium", "onThumbnailRetrieved  " + isSelected + " " + holder.mFilePath);
        Canvas canvas = new Canvas(thumbnail);
        canvas.drawBitmap(thumbnail, new Matrix(), null);

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
        holder.updateSelectionOverlays();
    }
}
