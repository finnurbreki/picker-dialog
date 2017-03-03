// Copyright 2016 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.example.finnur.finnursphotopicker;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;

// Chrome-specific imports:
/*
import org.chromium.base.VisibleForTesting;
*/

import java.util.List;
import java.util.Locale;

/** Holds onto a View that displays information about a picker bitmap. */
public class PickerBitmapViewHolder
        extends RecyclerView.ViewHolder implements DecoderServiceHost.ImageDecodedCallback {
    // Our parent category.
    private PickerCategoryView mCategoryView;

    // The bitmap view we are holding on to.
    private final PickerBitmapView mItemView;

    // The request we are showing the bitmap for.
    private PickerBitmap mItem;

    public PickerBitmapViewHolder(View itemView) {
        super(itemView);

        assert itemView instanceof PickerBitmapView;
        mItemView = (PickerBitmapView) itemView;
    }

    public String getFilePath() {
        return mItem == null ? null : mItem.getFilePath();
    }

    // DecoderServiceHost.ImageDecodedCallback

    @Override
    public void imageDecodedCallback(String filePath, Bitmap bitmap) {
        if (bitmap == null || bitmap.getWidth() == 0 || bitmap.getHeight() == 0) {
            return;
        }

        if (mCategoryView.getPrettyBitmaps().get(filePath) == null) {
            mCategoryView.getPrettyBitmaps().put(filePath, bitmap);
        }

        if (mCategoryView.getLowResBitmaps().get(filePath) == null) {
            // Scaling the image down takes between 0-1 ms on average (Nexus 6 phone debug build).
            Bitmap lowres = BitmapUtils.scale(bitmap, 40, false);
            mCategoryView.getLowResBitmaps().put(filePath, lowres);
        }

        if (!TextUtils.equals(mItem.getFilePath(), filePath)) {
            return;
        }

        if (mItemView.setThumbnailBitmap(bitmap)) {
            mItemView.fadeInThumbnail();
        }
    }

    public void displayItem(PickerCategoryView categoryView, int position) {
        mCategoryView = categoryView;

        List<PickerBitmap> pickerBitmaps = mCategoryView.getPickerBitmaps();
        mItem = pickerBitmaps.get(position);

        String filePath = mItem.getFilePath();
        if (isImageExtension(filePath)) {
            Bitmap original = mCategoryView.getPrettyBitmaps().get(filePath);
            if (original != null) {
                mItemView.initialize(mItem, original, false);
                return;
            }

            int size = mCategoryView.getImageSize();
            Bitmap placeholder = mCategoryView.getLowResBitmaps().get(filePath);
            if (placeholder != null) {
                // Scaling the image up takes between 3-4 ms on average (Nexus 6 phone debug build).
                placeholder = BitmapUtils.scale(placeholder, size, false);
                mItemView.initialize(mItem, placeholder, false);
            } else {
                placeholder = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
                placeholder.eraseColor(Color.argb(0, 0, 0, 0));
                mItemView.initialize(mItem, placeholder, true);
            }

            mCategoryView.getDecoderServiceHost().decodeImage(
                    mItem.getFilePath(), size, this, System.nanoTime());
        } else {
            mItemView.initialize(mItem, null, false);
            if (mItem.type() == PickerBitmap.TileTypes.PICTURE) {
                mItemView.setTextWithOverlay();
            } else {
                mItemView.initializeSpecialTile();
            }
        }
    }

    private boolean isImageExtension(String filePath) {
        String file = filePath.toLowerCase(Locale.US);
        return file.endsWith(".jpg") || file.endsWith(".gif") || file.endsWith(".png");
    }

    @VisibleForTesting
    public boolean getImageLoadedForTesting() {
        return mItemView.getImageLoadedForTesting();
    }
}
