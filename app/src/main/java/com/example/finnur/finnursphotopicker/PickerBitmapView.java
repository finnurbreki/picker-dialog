// Copyright 2016 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

// FLIP
//package org.chromium.chrome.browser;
package com.example.finnur.finnursphotopicker;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.util.List;

// Chrome-specific:
/*
FLIP
import org.chromium.chrome.R;
import org.chromium.chrome.browser.widget.TintedImageView;
import org.chromium.chrome.browser.widget.selection.SelectableItemView;
import org.chromium.chrome.browser.widget.selection.SelectionDelegate;
*/

public class PickerBitmapView extends SelectableItemView<PickerBitmap> {
    private Context mContext;

    // Our parent category.
    private PickerCategoryView mCategoryView;

    // The image view we are showing.
    private TintedImageView mIconView;
    private int mOriginalSize;

    // Our selection delegate.
    private SelectionDelegate<PickerBitmap> mSelectionDelegate;

    // The request we are showing the bitmap for.
    private PickerBitmap mItem;

    // The image view for signifying the image has been selected.
    public ImageView mSelectedView;

    // The image view for signifying the image has not been selected.
    public ImageView mUnselectedView;

    // Whether the image has been loaded already.
    public boolean mImageLoaded;

    private int mBorder = 50;

    private static void addPaddingToParent(View view, int padding) {
        ViewGroup layout = (ViewGroup) view.getParent();
        layout.setPadding(padding, padding, padding, padding);
        layout.requestLayout();
    }

    private class ResizeWidthAnimation extends Animation {
        private View mView;

        private int mStartingSize;
        private int mTargetSize;

        public ResizeWidthAnimation(View view, int size) {
            mView = view;
            mStartingSize = view.getWidth();
            mTargetSize = size;
        }

        @Override
        protected void applyTransformation(float interpolatedTime, Transformation transformation) {
            int newSize =
                    mStartingSize + (int) ((mTargetSize - mStartingSize) * interpolatedTime);
            int padding = (Math.max(mStartingSize, mTargetSize) - newSize) / 2;

            mView.getLayoutParams().height = newSize;
            mView.getLayoutParams().width = newSize;
            // Create a border around the image.
            if (mView instanceof TintedImageView) {
                addPaddingToParent(mView, padding);
            }
        }

        @Override
        public boolean willChangeBounds() {
            return true;
        }
    }

    /**
     * Constructor for inflating from XML.
     */
    public PickerBitmapView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mIconView = (TintedImageView) findViewById(R.id.bitmap_view);
        mSelectedView = (ImageView) findViewById(R.id.selected);
        mUnselectedView = (ImageView) findViewById(R.id.unselected);
    }

    public void initialize(PickerCategoryView categoryView) {
        mCategoryView = categoryView;
        mSelectionDelegate = mCategoryView.getSelectionDelegate();
        super.setSelectionDelegate(mSelectionDelegate);
        mSelectedView.setImageBitmap(mCategoryView.getSelectionBitmap(true));
        mUnselectedView.setImageBitmap(mCategoryView.getSelectionBitmap(false));
    }

    /**
     * Initialize the DownloadItemView. Must be called before the item can respond to click events.
     *
     * @param item      The item represented by this DownloadItemView.
     * @param thumbnail The Bitmap to use for the thumbnail or null.
     */
    public void initialize(PickerBitmap item, @Nullable Bitmap thumbnail, boolean placeholder) {
        mItem = item;
        setItem(item);
        setThumbnailBitmap(thumbnail);
        mImageLoaded = !placeholder;
        updateSelectionOverlays();

        mIconView.setOnClickListener(this);
        //mIconView.setOnLongClickListener(this);
    }

    public void initializeSpecialTile() {
        int size = mCategoryView.getImageSize();
        Bitmap tile = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        tile.eraseColor(Color.LTGRAY);
        Canvas canvas = new Canvas(tile);

        Bitmap icon;
        if (mItem.type() == PickerBitmap.TileTypes.CAMERA) {
            icon = BitmapFactory.decodeResource(
                    // FLIP
                    mContext.getResources(), R.mipmap.ic_camera_alt_black_24dp);
                    //mContext.getResources(), R.drawable.ic_photo_camera);
        } else {
            icon = BitmapFactory.decodeResource(
                    // FLIP
                    mContext.getResources(), R.mipmap.ic_collections_black_24dp);
                    //mContext.getResources(), R.drawable.ic_collections_black_24dp);
        }
        icon = Bitmap.createScaledBitmap(icon, icon.getWidth() * 2, icon.getHeight() * 2, false);
        canvas.drawBitmap(
                icon,
                (tile.getWidth() - icon.getWidth()) / 2,
                (tile.getHeight() - icon.getHeight()) / 2,
                null);

        initialize(mItem, tile, false);
    }

    /**
     * @param thumbnail The Bitmap to use for the icon ImageView.
     */
    public void setThumbnailBitmap(Bitmap thumbnail) {
        mIconView.setImageBitmap(thumbnail);
        mOriginalSize = thumbnail != null ? mIconView.getWidth() : 0;

        // If the tile has been selected before the bitmap has loaded, make sure it shows up with
        // a selection border on load.
        if (super.isChecked()) {
            mIconView.getLayoutParams().height = mOriginalSize - mBorder;
            mIconView.getLayoutParams().width = mOriginalSize - mBorder;
            addPaddingToParent(mIconView, mBorder / 2);
        }

        mImageLoaded = true;
        updateSelectionOverlays();
    }

    @Override
    public void onClick() {
        Log.e("chromium", "Tile type clicked: " + mItem.type());

        if (mItem.type() != PickerBitmap.TileTypes.NORMAL) {
            return;
        }

        mSelectionDelegate.toggleSelectionForItem(mItem);
        setChecked(!super.isChecked());
    }

    @Override
    public void setChecked(boolean checked) {
        if (mItem.type() != PickerBitmap.TileTypes.NORMAL) {
            return;
        }

        super.setChecked(checked);
        updateSelectionOverlays();
    }

    @Override
    public void onSelectionStateChange(List<PickerBitmap> selectedItems) {
        if (mItem.type() != PickerBitmap.TileTypes.NORMAL) {
            return;
        }

        updateSelectionOverlays();
        boolean selected = selectedItems.contains(mItem);
        boolean checked = super.isChecked();
        if (!mImageLoaded || selected == checked) {
            return;
        }

        int size = selected && !checked ? mOriginalSize - mBorder : mOriginalSize;
        if (size != mIconView.getWidth()) {
            ResizeWidthAnimation animation = new ResizeWidthAnimation(mIconView, size);
            animation.setDuration(50);
            mIconView.startAnimation(animation);
        }
    }

    private void updateSelectionOverlays() {
        if (mItem.type() != PickerBitmap.TileTypes.NORMAL) {
            return;
        }

        mSelectedView.setVisibility(super.isChecked() ? View.VISIBLE : View.GONE);

        // The visibility of the unselected image is a little more complex because we don't want
        // to show it when nothing is selected and also not on a blank canvas.
        boolean somethingSelected =
                mSelectionDelegate != null && mSelectionDelegate.isSelectionEnabled();
        if (!super.isChecked() && mImageLoaded && somethingSelected) {
            mUnselectedView.setVisibility(View.VISIBLE);
        } else {
            mUnselectedView.setVisibility(View.GONE);
        }
    }

    public void setTextWithOverlay() {
        int photoSize = mCategoryView.getImageSize();
        Bitmap bitmap = Bitmap.createBitmap(photoSize, photoSize, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        int textSize = 72;
        paint.setTextSize(textSize);
        paint.setTextScaleX(1);
        String filePath = mItem.getFilePath();
        int dot = filePath.lastIndexOf(".");
        String extension = dot > -1 ? filePath.substring(dot) : "(no ext)";
        float width = paint.measureText(extension);
        canvas.drawText(extension, (photoSize - width) / 2, (photoSize - textSize) / 2, paint);
        mIconView.setImageBitmap(bitmap);
        updateSelectionOverlays();
    }
}
