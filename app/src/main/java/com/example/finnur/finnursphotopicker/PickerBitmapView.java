// Copyright 2016 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.example.finnur.finnursphotopicker;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.ImageView;
import android.widget.TextView;

// Chrome-specific imports:
/*
import org.chromium.chrome.R;
import org.chromium.chrome.browser.widget.selection.SelectableItemView;
import org.chromium.chrome.browser.widget.selection.SelectionDelegate;
*/

import java.util.List;

public class PickerBitmapView extends SelectableItemView<PickerBitmap> {
    private Context mContext;

    // Our parent category.
    private PickerCategoryView mCategoryView;

    // The image view we are showing.
    private ImageView mIconView;
    private View mScrim;
    private View mBorderView;

    // Our selection delegate.
    private SelectionDelegate<PickerBitmap> mSelectionDelegate;

    // The request we are showing the bitmap for.
    private PickerBitmap mItem;

    // The control that signifies the image has been selected.
    public ImageView mSelectedView;

    // The control that signifies the image has not been selected.
    public ImageView mUnselectedView;

    // The camera/gallery special tile (with icon as drawable).
    public TextView mSpecialTile;

    // Whether the image has been loaded already.
    public boolean mImageLoaded;

    // The amount to use for the border.
    private int mBorder;

    /**
     * Resets the view to its starting state, which is necessary when the view is about to be
     * re-used.
     */
    private void resetTile() {
        mUnselectedView.setVisibility(View.GONE);
        mSelectedView.setVisibility(View.GONE);
        mScrim.setVisibility(View.GONE);
        mSpecialTile.setVisibility(View.GONE);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        if (mCategoryView == null) return;  // Android studio calls onMeasure to draw the widget.
        int width = mCategoryView.getImageSize();
        int height = mCategoryView.getImageSize();
        setMeasuredDimension(width, height);
    }

    private static void addPaddingToParent(View view, int padding) {
        ViewGroup layout = (ViewGroup) view.getParent();
        layout.setPadding(padding, padding, padding, padding);
        layout.requestLayout();
    }

    private class ResizeWidthAnimation extends Animation {
        private View mView;

        private int mStartingSize;
        private int mTargetSize;

        public ResizeWidthAnimation(View view, View scrim, int size) {
            mView = view;
            mScrim = scrim;
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
            if (mView instanceof ImageView) {
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
        mIconView = (ImageView) findViewById(R.id.bitmap_view);
        mScrim = findViewById(R.id.scrim);
        mBorderView = findViewById(R.id.border);
        mSelectedView = (ImageView) findViewById(R.id.selected);
        mUnselectedView = (ImageView) findViewById(R.id.unselected);
        mSpecialTile = (TextView) findViewById(R.id.special_tile);
    }

    public void initialize(PickerCategoryView categoryView) {
        mCategoryView = categoryView;
        mSelectionDelegate = mCategoryView.getSelectionDelegate();
        super.setSelectionDelegate(mSelectionDelegate);

        mSelectedView.setImageBitmap(mCategoryView.getSelectionBitmap(true));
        mUnselectedView.setImageBitmap(mCategoryView.getSelectionBitmap(false));

        mBorder = (int) getResources().getDimension(R.dimen.file_picker_selected_padding);
    }

    /**
     * Initialize the DownloadItemView. Must be called before the item can respond to click events.
     *
     * @param item      The item represented by this DownloadItemView.
     * @param thumbnail The Bitmap to use for the thumbnail or null.
     */
    public void initialize(PickerBitmap item, @Nullable Bitmap thumbnail, boolean placeholder) {
        resetTile();

        mItem = item;
        setItem(item);
        setThumbnailBitmap(thumbnail);
        mImageLoaded = !placeholder;

        updateSelectionState();

        setOnClickListener(this);
    }

    public boolean getImageLoadedForTesting() {
        return mImageLoaded;
    }

    public void initializeSpecialTile() {
        int size = mCategoryView.getImageSize();
        Bitmap tile = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        tile.eraseColor(Color.argb(0, 0, 0, 0));

        int iconBitmapId, labelStringId;
        if (mItem.type() == PickerBitmap.TileTypes.CAMERA) {
            iconBitmapId = R.drawable.ic_photo_camera;
            labelStringId = R.string.file_picker_camera;
        } else {
            iconBitmapId = R.drawable.ic_collections_black_24dp;
            labelStringId = R.string.file_picker_browse;
        }

        Resources resources = mContext.getResources();
        mSpecialTile.setText(labelStringId);
        Bitmap icon = BitmapFactory.decodeResource(resources, iconBitmapId);
        DisplayMetrics metrics = resources.getDisplayMetrics();
        float pixels = 48 * ((float) metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT);
        BitmapDrawable drawable = new BitmapDrawable(
                resources, Bitmap.createScaledBitmap(icon, (int) pixels, (int) pixels, true));
        mSpecialTile.setCompoundDrawablesRelativeWithIntrinsicBounds(null, drawable, null, null);

        initialize(mItem, tile, false);

        mSpecialTile.setVisibility(View.VISIBLE);
    }

    /**
     * @param thumbnail The Bitmap to use for the icon ImageView.
     * @return true if no image was loaded before (e.g. not even a low-res image).
     */
    public boolean setThumbnailBitmap(Bitmap thumbnail) {
        mIconView.setImageBitmap(thumbnail);

        // If the tile has been selected before the bitmap has loaded, make sure it shows up with
        // a selection border and scrim on load.
        if (super.isChecked()) {
            mIconView.getLayoutParams().height = mCategoryView.getImageSize() - 2 * mBorder;
            mIconView.getLayoutParams().width = mCategoryView.getImageSize() - 2 * mBorder;
            addPaddingToParent(mIconView, mBorder);
            mScrim.setVisibility(View.VISIBLE);
        }

        boolean noImageWasLoaded = !mImageLoaded;
        mImageLoaded = true;
        updateSelectionState();

        return noImageWasLoaded;
    }

    public void fadeInThumbnail() {
        mIconView.setAlpha(0.0f);
        mIconView.animate().alpha(1.0f).setDuration(200).start();
    }

    @Override
    public void onClick() {
        if (mItem.type() == PickerBitmap.TileTypes.GALLERY) {
            mCategoryView.showGallery();
            return;
        } else if (mItem.type() == PickerBitmap.TileTypes.CAMERA) {
            mCategoryView.showCamera();
            return;
        }

        mSelectionDelegate.toggleSelectionForItem(mItem);
        setChecked(!super.isChecked());
    }

    @Override
    public void setChecked(boolean checked) {
        if (mItem.type() != PickerBitmap.TileTypes.PICTURE) {
            return;
        }

        super.setChecked(checked);
        updateSelectionState();
    }

    @Override
    public void onSelectionStateChange(List<PickerBitmap> selectedItems) {
        boolean selected = selectedItems.contains(mItem);

        if (mItem.type() != PickerBitmap.TileTypes.PICTURE) {
            if (selected) mSelectionDelegate.toggleSelectionForItem(mItem);
            updateSelectionState();
            return;
        }

        boolean checked = super.isChecked();

        if (!mCategoryView.isMultiSelect() && !selected && checked) {
            super.toggle();
        }

        updateSelectionState();

        if (!mImageLoaded || selected == checked) {
            return;
        }

        int size = selected && !checked
                ? mCategoryView.getImageSize() - 2 * mBorder : mCategoryView.getImageSize();
        if (size != mIconView.getWidth()) {
            ResizeWidthAnimation animation = new ResizeWidthAnimation(mIconView, mScrim, size);
            animation.setDuration(100);
            // TODO: Add MD interpolator
            // animation.setInterpolator((mContext, R.interpolator.fast_out_linear_in);
            mIconView.startAnimation(animation);
        }
    }

    private void updateSelectionState() {
        boolean special = mItem.type() != PickerBitmap.TileTypes.PICTURE;
        boolean checked = super.isChecked();
        boolean anySelection = mSelectionDelegate != null
                && mSelectionDelegate.isSelectionEnabled();
        boolean multiSelect = mCategoryView.isMultiSelect();
        int bgColorId, fgColorId;
        if (!special) {
            bgColorId = R.color.file_picker_tile_bg_color;
            fgColorId = R.color.file_picker_special_tile_color;
        } else if (!anySelection || !multiSelect) {
            bgColorId = R.color.file_picker_special_tile_bg_color;
            fgColorId = R.color.file_picker_special_tile_color;
        } else {
            bgColorId = R.color.file_picker_special_tile_disabled_bg_color;
            fgColorId = R.color.file_picker_special_tile_disabled_color;
        }

        mBorderView.setBackgroundColor(ContextCompat.getColor(mContext, bgColorId));
        mSpecialTile.setTextColor(ContextCompat.getColor(mContext, fgColorId));
        Drawable[] drawables = mSpecialTile.getCompoundDrawables();
        // The textview only has a top compound drawable (2nd element).
        if (drawables[1] != null) {
            drawables[1].setTintList(createSimpleColorStateList(
                    ContextCompat.getColor(mContext, fgColorId)));
        }

        // The visibility of the unselected image is a little more complex because we don't want
        // to show it when nothing is selected and also not on a blank canvas.
        mSelectedView.setVisibility(!special && checked ? View.VISIBLE : View.GONE);
        mUnselectedView.setVisibility(!special && !checked && anySelection
                ? View.VISIBLE : View.GONE);
        mScrim.setVisibility(!special && !checked && anySelection
                ? View.VISIBLE : View.GONE);
    }

    private static ColorStateList createSimpleColorStateList(int color) {
        int[][] states = new int[][] {
            new int[] { android.R.attr.state_enabled },
            new int[] { -android.R.attr.state_enabled },
        };

        int[] colors = new int[] { color, color };
        return new ColorStateList(states, colors);
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
        updateSelectionState();
    }
}
