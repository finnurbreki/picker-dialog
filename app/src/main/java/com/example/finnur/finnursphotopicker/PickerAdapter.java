// Copyright 2016 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.example.finnur.finnursphotopicker;

import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.content.res.Resources;
import android.widget.ImageView;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

public class PickerAdapter extends RecyclerView.Adapter<PickerAdapter.MyViewHolder> {

    private Resources mResources;

    // The thumbnail provider service.
    private ThumbnailProvider mThumbnailProvider;

    // The list of bitmaps to show.
    private List<PickerBitmap> pickerBitmaps;

    // Maximum number of bitmaps to show.
    private int mMaxBitmaps;

    // The size of the bitmaps (equal length for width and height).
    private int mPhotoSize;

    public class MyViewHolder extends RecyclerView.ViewHolder implements ThumbnailProvider.ThumbnailRequest {
        // The parent of this holder.
        private PickerAdapter mParent;

        // The path to the bitmap to load.
        public String mFilePath;

        // The imageview for showing the bitmap.
        public ImageView mImageView;

        // Whether this bitmap represent the last bitmap of a set, signaling that more bitmaps are available.
        public Boolean mIsExpandTitle;

        public MyViewHolder(View view, ThumbnailProvider provider, PickerAdapter parent) {
            super(view);
            mParent = parent;
            mImageView = (ImageView) view.findViewById(R.id.bitmap);
            // mImageView.setScaleType(ScaleType.CENTER);
        }

        // ThumbnailProvider.ThumbnailRequest:

        @Override
        public String getFilePath() {
            return mFilePath;
        }

        @Override
        public void onThumbnailRetrieved(String filePath, Bitmap thumbnail) {
            //Log.d("***** ", "onThumbnailRetrieved " + filePath);
            Boolean isSelected = false;

            Canvas canvas = new Canvas(thumbnail);
            canvas.drawBitmap(thumbnail, new Matrix(), null);

            if (mIsExpandTitle) {
                // Start with a slightly darkened thumbnail.
                canvas.drawARGB(120, 0, 0, 0);

                // Prepare the paint object.
                Paint paint = new Paint();
                paint.setAlpha(255);
                paint.setColor(Color.WHITE);
                float textHeight = 96;
                paint.setTextSize(textHeight);

                // Calculate where to place the text overlay.
                String overlayText = Integer.toString(pickerBitmaps.size() - mMaxBitmaps) + " >";
                Rect textBounds = new Rect();
                paint.getTextBounds(overlayText, 0, overlayText.length(), textBounds);

                // Add the number.
                float x = (mParent.mPhotoSize - textBounds.width()) / 2;
                float y = (mParent.mPhotoSize - textBounds.height()) / 2;
                canvas.drawText(overlayText, x, y, paint);
            } else {
                Bitmap selectionCircle = BitmapFactory.decodeResource(
                     mResources, isSelected ? R.mipmap.selected : R.mipmap.unselected);
                canvas.drawBitmap(selectionCircle, 15, 10, null);
            }

            mImageView.setImageBitmap(thumbnail);
        }
    }


    public PickerAdapter(List<PickerBitmap> pickerBitmaps, Resources resources, int widthPerColumn, int maxBitmaps) {
        this.pickerBitmaps = pickerBitmaps;
        mResources = resources;
        mThumbnailProvider = new ThumbnailProviderImpl(widthPerColumn);
        mMaxBitmaps = maxBitmaps;
        mPhotoSize = widthPerColumn;
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
        PickerBitmap bitmap = pickerBitmaps.get(position);
        holder.mFilePath = bitmap.getFilePath();
        holder.mImageView.setMaxHeight(mPhotoSize);
        holder.mImageView.setImageBitmap(mThumbnailProvider.getThumbnail(holder));
        holder.mIsExpandTitle = position == mMaxBitmaps - 1;
    }

    @Override
    public int getItemCount() {
        return Math.min(pickerBitmaps.size(), mMaxBitmaps);
    }
}