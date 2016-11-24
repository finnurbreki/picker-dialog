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
import android.graphics.Paint;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import java.lang.Math;
import java.util.List;

// Chrome-specific:
/*
FLIP
import org.chromium.chrome.R;
import org.chromium.chrome.browser.widget.selection.SelectableItemView;
import org.chromium.chrome.browser.widget.selection.SelectionDelegate;
import org.chromium.chrome.browser.widget.TintedImageView;
*/

public class PickerBitmapView extends SelectableItemView<PickerBitmap> {
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
            int borderSize = (Math.max(mStartingSize, mTargetSize) - newSize) / 2;

            mView.getLayoutParams().height = newSize;
            mView.getLayoutParams().width = newSize;
            // Create a border around the image.
            if (mView instanceof TintedImageView) {
                ViewGroup layout = (ViewGroup) mView.getParent();
                layout.setPadding(borderSize, borderSize, borderSize, borderSize);
                layout.requestLayout();
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
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mIconView = (TintedImageView) findViewById(R.id.bitmap_view);
        mSelectedView = (ImageView) findViewById(R.id.selected);
        mUnselectedView = (ImageView) findViewById(R.id.unselected);
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

    public void initialize(PickerCategoryView categoryView) {
        mCategoryView = categoryView;
        mSelectionDelegate = mCategoryView.getSelectionDelegate();
        super.setSelectionDelegate(mSelectionDelegate);
        mSelectedView.setImageBitmap(mCategoryView.getSelectionBitmap(true));
        mUnselectedView.setImageBitmap(mCategoryView.getSelectionBitmap(false));
    }

    /**
     * @param thumbnail The Bitmap to use for the icon ImageView.
     */
    public void setThumbnailBitmap(Bitmap thumbnail) {
        mIconView.setImageBitmap(thumbnail);
        mOriginalSize = thumbnail != null ? mIconView.getWidth() : 0;
        mImageLoaded = true;
        updateSelectionOverlays();
    }

    @Override
    public void onClick() {
        mSelectionDelegate.toggleSelectionForItem(mItem);
        setChecked(!super.isChecked());
    }

    @Override
    public void setChecked(boolean checked) {
        super.setChecked(checked);
        updateSelectionOverlays();
    }

    @Override
    public void onSelectionStateChange(List<PickerBitmap> selectedItems) {
        updateSelectionOverlays();

        int size = !super.isChecked() && selectedItems.contains(mItem) ? mOriginalSize - 50 : mOriginalSize;
        if (size != mIconView.getWidth()) {
            ResizeWidthAnimation animation = new ResizeWidthAnimation(mIconView, size);
            animation.setDuration(50);
            mIconView.startAnimation(animation);
        }
    }

    private void updateSelectionOverlays() {
        mSelectedView.setVisibility(super.isChecked() ? View.VISIBLE : View.GONE);

        // The visibility of the unselected image is a little more complex because we don't want
        // to show it when nothing is selected and also not on a blank canvas.
        boolean somethingSelected =
                mSelectionDelegate != null && mSelectionDelegate.isSelectionEnabled();
        if (!super.isChecked() && mImageLoaded && somethingSelected)
            mUnselectedView.setVisibility(View.VISIBLE);
        else
            mUnselectedView.setVisibility(View.GONE);
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
