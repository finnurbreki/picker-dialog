// Copyright 2017 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.example.finnur.finnursphotopicker;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.support.graphics.drawable.VectorDrawableCompat;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.ImageView;
import android.widget.TextView;

import org.chromium.base.ApiCompatibilityUtils;
import org.chromium.base.VisibleForTesting;
//import org.chromium.chrome.R;
import org.chromium.chrome.browser.widget.selection.SelectableItemView;
import org.chromium.chrome.browser.widget.selection.SelectionDelegate;

import java.util.List;

/**
 * A container class for a view showing a photo in the Photo Picker.
 */
public class PickerBitmapView extends SelectableItemView<PickerBitmap> {
    // The length of the image selection animation (in ms).
    private int ANIMATION_DURATION = 100;

    // The length of the fade in animation (in ms).
    private int IMAGE_FADE_IN_DURATION = 200;

    // Our context.
    private Context mContext;

    // Our parent category.
    private PickerCategoryView mCategoryView;

    // Our selection delegate.
    private SelectionDelegate<PickerBitmap> mSelectionDelegate;

    // The request details (meta-data) for the bitmap shown.
    private PickerBitmap mBitmapDetails;

    // The image view containing the bitmap.
    private ImageView mIconView;

    // The little shader in the top left corner (provides backdrop for selection ring on
    // unfavorable image backgrounds).
    private View mScrim;

    // The view behind the image, representing the selection border.
    private View mBorderView;

    // The control that signifies the image has been selected.
    private ImageView mSelectedView;

    // The control that signifies the image has not been selected.
    private View mUnselectedView;

    // The camera/gallery special tile (with icon as drawable).
    private TextView mSpecialTile;

    // Whether the image has been loaded already.
    private boolean mImageLoaded;

    // The amount to use for the border.
    private int mBorder;

    /**
     * A resize animation class for the images (shrinks the image on selection).
     */
    private static class ResizeWidthAnimation extends Animation {
        // The view to animate size changes for.
        private View mView;

        // The starting size of the view.
        private int mStartingSize;

        // The target size we want to achieve.
        private int mTargetSize;

        /**
         * The ResizeWidthAnimation constructor.
         * @param view The view to animate size changes for.
         * @param size The target size we want to achieve.
         */
        public ResizeWidthAnimation(View view, int size) {
            mView = view;
            mStartingSize = view.getWidth();
            mTargetSize = size;
        }

        @Override
        protected void applyTransformation(float interpolatedTime, Transformation transformation) {
            int newSize = mStartingSize + (int) ((mTargetSize - mStartingSize) * interpolatedTime);
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
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        if (mCategoryView == null) return;

        int width = mCategoryView.getImageSize();
        int height = mCategoryView.getImageSize();
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mIconView = (ImageView) findViewById(R.id.bitmap_view);
        mScrim = findViewById(R.id.scrim);
        mBorderView = findViewById(R.id.border);
        mSelectedView = (ImageView) findViewById(R.id.selected);
        mUnselectedView = findViewById(R.id.unselected);
        mSpecialTile = (TextView) findViewById(R.id.special_tile);
    }

    @Override
    public void onClick() {
        if (isGalleryTile()) {
            mCategoryView.showGallery();
            return;
        } else if (isCameraTile()) {
            mCategoryView.showCamera();
            return;
        }

        // The SelectableItemView expects long press to be the selection event, but this class wants
        // that to happen on click instead.
        super.onLongClick(this);
    }

    @Override
    protected boolean toggleSelectionForItem(PickerBitmap item) {
        if (isGalleryTile() || isCameraTile()) return false;
        return super.toggleSelectionForItem(item);
    }

    @Override
    public void setChecked(boolean checked) {
        if (!isPictureTile()) {
            return;
        }

        super.setChecked(checked);
        updateSelectionState();
    }

    @Override
    public void onSelectionStateChange(List<PickerBitmap> selectedItems) {
/*
        boolean selected = selectedItems.contains(mRequest);

        if (mRequest.type() != PickerBitmap.TileTypes.PICTURE) {
            if (selected) mSelectionDelegate.toggleSelectionForItem(mRequest);
            updateSelectionState();
            return;
        }

        boolean checked = super.isChecked();

        if (!mCategoryView.isMultiSelect() && !selected && checked) {
            super.toggle();
        }
*/
        updateSelectionState();

/*
        boolean checked = super.isChecked();
        if (!mImageLoaded || selected == checked) {
            return;
        }

        int size = selected && !checked ? mCategoryView.getImageSize() - 2 * mBorder
                                        : mCategoryView.getImageSize();
        if (size != mIconView.getWidth()) {
            ResizeWidthAnimation animation = new ResizeWidthAnimation(mIconView, size);
            animation.setDuration(ANIMATION_DURATION);
            // TODO: Add MD interpolator
            // animation.setInterpolator((mContext, R.interpolator.fast_out_linear_in);
            mIconView.startAnimation(animation);
        }
*/
    }

    /**
     * Sets the {@link PickerCategoryView} for this PickerBitmapView.
     * @param categoryView The category view showing the images. Used to access
     *     common functionality and sizes and retrieve the {@link SelectionDelegate}.
     */
    public void setCategoryView(PickerCategoryView categoryView) {
        mCategoryView = categoryView;
        mSelectionDelegate = mCategoryView.getSelectionDelegate();
        super.setSelectionDelegate(mSelectionDelegate);

        mBorder = (int) getResources().getDimension(R.dimen.photo_picker_selected_padding);
    }

    /**
     * Completes the initialization of the PickerBitmapView. Must be called before the image can
     * respond to click events.
     * @param bitmapDetails The details about the bitmap represented by this PickerBitmapView.
     * @param thumbnail The Bitmap to use for the thumbnail (or null).
     * @param placeholder Whether the image given is a placeholder or the actual image.
     */
    public void initialize(
            PickerBitmap bitmapDetails, @Nullable Bitmap thumbnail, boolean placeholder) {
        resetTile();

        mBitmapDetails = bitmapDetails;
        setItem(bitmapDetails);
        setThumbnailBitmap(thumbnail);
        mImageLoaded = !placeholder;

        updateSelectionState();
    }

    /**
     * Initialization for the special tiles (camera/gallery icon).
     * @param bitmapDetails The details about the bitmap represented by this PickerBitmapView.
     */
    public void initializeSpecialTile(PickerBitmap bitmapDetails) {
        int labelStringId;
        Drawable image;
        Resources resources = mContext.getResources();

        if (isCameraTile()) {
            image = ApiCompatibilityUtils.getDrawable(resources, R.drawable.ic_photo_camera);
            labelStringId = R.string.photo_picker_camera;
        } else {
            image = VectorDrawableCompat.create(
                    resources, R.drawable.ic_collections_grey, mContext.getTheme());
            labelStringId = R.string.photo_picker_browse;
        }

        ApiCompatibilityUtils.setCompoundDrawablesRelativeWithIntrinsicBounds(
                mSpecialTile, null, image, null, null);
        mSpecialTile.setText(labelStringId);

        initialize(bitmapDetails, null, false);

        // Reset visibility, since #initialize() sets mSpecialTile visibility to GONE.
        mSpecialTile.setVisibility(View.VISIBLE);
    }

    /**
     * Sets a thumbnail bitmap for the current view and ensures the selection border and scrim is
     * showing, if the image has already been selected.
     * @param thumbnail The Bitmap to use for the icon ImageView.
     * @return True if no image was loaded before (e.g. not even a low-res image).
     */
    public boolean setThumbnailBitmap(Bitmap thumbnail) {
        mIconView.setImageBitmap(thumbnail);

        // If the tile has been selected before the bitmap has loaded, make sure it shows up with
        // a selection border and scrim on load.
        if (super.isChecked()) {
            mIconView.getLayoutParams().height = imageSizeWithBorders();
            mIconView.getLayoutParams().width = imageSizeWithBorders();
            addPaddingToParent(mIconView, mBorder);
            mScrim.setVisibility(View.VISIBLE);
        }

        boolean noImageWasLoaded = !mImageLoaded;
        mImageLoaded = true;
        updateSelectionState();

        return noImageWasLoaded;
    }

    /* Returns the size of the image plus the pre-determined border on each side. */
    private int imageSizeWithBorders() {
        return mCategoryView.getImageSize() - 2 * mBorder;
    }

    /**
     * Initiates fading in of the thumbnail. Note, this should not be called if a grainy version of
     * the thumbnail was loaded from cache. Otherwise a flash will appear.
     */
    public void fadeInThumbnail() {
        mIconView.setAlpha(0.0f);
        mIconView.animate().alpha(1.0f).setDuration(IMAGE_FADE_IN_DURATION).start();
    }

    @VisibleForTesting
    public boolean getImageLoadedForTesting() {
        return mImageLoaded;
    }

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

    /**
     * Adds padding to the parent of the |view|.
     * @param view The child view of the view to receive the padding.
     * @param padding The amount of padding to use (in pixels).
     */
    private static void addPaddingToParent(View view, int padding) {
        ViewGroup layout = (ViewGroup) view.getParent();
        layout.setPadding(padding, padding, padding, padding);
        layout.requestLayout();
    }

    /**
     * Updates the selection controls for this view.
     */
    private void updateSelectionState() {
        boolean special = !isPictureTile();
        boolean checked = super.isChecked();
        boolean anySelection =
                mSelectionDelegate != null && mSelectionDelegate.isSelectionEnabled();
        int bgColorId, fgColorId;
        if (!special) {
            bgColorId = R.color.photo_picker_tile_bg_color;
            fgColorId = R.color.photo_picker_special_tile_color;
        } else if (!anySelection) {
            bgColorId = R.color.photo_picker_special_tile_bg_color;
            fgColorId = R.color.photo_picker_special_tile_color;
        } else {
            bgColorId = R.color.photo_picker_special_tile_disabled_bg_color;
            fgColorId = R.color.photo_picker_special_tile_disabled_color;
        }

        Resources resources = mContext.getResources();
        setBackgroundColor(ApiCompatibilityUtils.getColor(resources, bgColorId));
        mSpecialTile.setTextColor(ApiCompatibilityUtils.getColor(resources, fgColorId));
        Drawable[] drawables = mSpecialTile.getCompoundDrawables();
        // The textview only has a top compound drawable (2nd element).
        if (drawables[1] != null) {
            int color = ApiCompatibilityUtils.getColor(resources, fgColorId);
            drawables[1].setColorFilter(color, PorterDuff.Mode.SRC_IN);
        }

        // The visibility of the unselected image is a little more complex because we don't want
        // to show it when nothing is selected and also not on a blank canvas.
        mSelectedView.setVisibility(!special && checked ? View.VISIBLE : View.GONE);
        mUnselectedView.setVisibility(
                !special && !checked && anySelection ? View.VISIBLE : View.GONE);
        mScrim.setVisibility(!special && !checked && anySelection ? View.VISIBLE : View.GONE);
    }

    private boolean isGalleryTile() {
        // TODO(finnur): Remove the null checks here and below.
        return mBitmapDetails != null && mBitmapDetails.type() == PickerBitmap.TileTypes.GALLERY;
    }

    private boolean isCameraTile() {
        return mBitmapDetails != null && mBitmapDetails.type() == PickerBitmap.TileTypes.CAMERA;
    }

    private boolean isPictureTile() {
        return mBitmapDetails != null && mBitmapDetails.type() == PickerBitmap.TileTypes.PICTURE;
    }
}
