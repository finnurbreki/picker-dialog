// Copyright 2017 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.example.finnur.finnursphotopicker;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;

import org.chromium.base.VisibleForTesting;

import java.util.List;

/**
 * Holds on to a {@link PickerBitmapView} that displays information about a picker bitmap.
 */
public class PickerBitmapViewHolder
        extends RecyclerView.ViewHolder implements DecoderServiceHost.ImageDecodedCallback {
    // Our parent category.
    private PickerCategoryView mCategoryView;

    // The bitmap view we are holding on to.
    private final PickerBitmapView mItemView;

    // The request we are showing the bitmap for.
    private PickerBitmap mBitmapDetails;

    /**
     * The PickerBitmapViewHolder.
     * @param itemView The {@link PickerBitmapView} view for showing the image.
     */
    public PickerBitmapViewHolder(PickerBitmapView itemView) {
        super(itemView);
        mItemView = itemView;
    }

    // DecoderServiceHost.ImageDecodedCallback

    @Override
    public void imageDecodedCallback(String filePath, Bitmap bitmap) {
        if (bitmap == null || bitmap.getWidth() == 0 || bitmap.getHeight() == 0) {
            return;
        }

        if (mCategoryView.getHighResBitmaps().get(filePath) == null) {
            mCategoryView.getHighResBitmaps().put(filePath, bitmap);
        }

        if (mCategoryView.getLowResBitmaps().get(filePath) == null) {
            // Scaling the image down takes between 0-1 ms on average (Nexus 6 phone debug build).
            Bitmap lowres = BitmapUtils.scale(bitmap, 40, false);
            mCategoryView.getLowResBitmaps().put(filePath, lowres);
        }

        if (!TextUtils.equals(mBitmapDetails.getFilePath(), filePath)) {
            return;
        }

        if (mItemView.setThumbnailBitmap(bitmap)) {
            mItemView.fadeInThumbnail();
        }
    }

    /**
     * Display a single item from |position| in the PickerCategoryView.
     * @param categoryView The PickerCategoryView to use to fetch the image.
     * @param position The position of the item to fetch.
     */
    public void displayItem(PickerCategoryView categoryView, int position) {
        mCategoryView = categoryView;

        List<PickerBitmap> pickerBitmaps = mCategoryView.getPickerBitmaps();
        mBitmapDetails = pickerBitmaps.get(position);

        String filePath = mBitmapDetails.getFilePath();
        if (mBitmapDetails.type() == PickerBitmap.TileTypes.CAMERA
                || mBitmapDetails.type() == PickerBitmap.TileTypes.GALLERY) {
            mItemView.initialize(mBitmapDetails, null, false);
            mItemView.initializeSpecialTile(mBitmapDetails);
            return;
        }

        Bitmap original = mCategoryView.getHighResBitmaps().get(filePath);
        if (original != null) {
            mItemView.initialize(mBitmapDetails, original, false);
            return;
        }

        int size = mCategoryView.getImageSize();
        Bitmap placeholder = mCategoryView.getLowResBitmaps().get(filePath);
        if (placeholder != null) {
            // Scaling the image up takes between 3-4 ms on average (Nexus 6 phone debug build).
            placeholder = BitmapUtils.scale(placeholder, size, false);
            mItemView.initialize(mBitmapDetails, placeholder, false);
        } else {
            placeholder = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
            placeholder.eraseColor(Color.argb(0, 0, 0, 0));
            mItemView.initialize(mBitmapDetails, placeholder, true);
        }

        mCategoryView.getDecoderServiceHost().decodeImage(mBitmapDetails.getFilePath(), size, this);
    }

    /**
     * Returns the file path of the current request.
     */
    public String getFilePath() {
        return mBitmapDetails == null ? null : mBitmapDetails.getFilePath();
    }

    @VisibleForTesting
    public boolean getImageLoadedForTesting() {
        return mItemView.getImageLoadedForTesting();
    }
}
