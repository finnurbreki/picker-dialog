// Copyright 2016 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.example.finnur.finnursphotopicker;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
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
 * A container class for a view showing a photo in the photo picker.
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
    private PickerBitmap mRequest;

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
    private ImageView mUnselectedView;

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
        mUnselectedView = (ImageView) findViewById(R.id.unselected);
        mSpecialTile = (TextView) findViewById(R.id.special_tile);
    }

    @Override
    public void onClick() {
        if (mRequest.type() == PickerBitmap.TileTypes.GALLERY) {
            mCategoryView.showGallery();
            return;
        } else if (mRequest.type() == PickerBitmap.TileTypes.CAMERA) {
            mCategoryView.showCamera();
            return;
        }

        mSelectionDelegate.toggleSelectionForItem(mRequest);
        setChecked(!super.isChecked());
    }

    @Override
    public void setChecked(boolean checked) {
        if (mRequest.type() != PickerBitmap.TileTypes.PICTURE) {
            return;
        }

        super.setChecked(checked);
        updateSelectionState();
    }

    @Override
    public void onSelectionStateChange(List<PickerBitmap> selectedItems) {
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

        updateSelectionState();

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
    }

    /**
     * Pre-initializes the PickerBitmapView.
     * @param categoryView The category view showing the images.
     */
    public void preInitialize(PickerCategoryView categoryView) {
        mCategoryView = categoryView;
        mSelectionDelegate = mCategoryView.getSelectionDelegate();
        super.setSelectionDelegate(mSelectionDelegate);

        mBorder = (int) getResources().getDimension(R.dimen.file_picker_selected_padding);
    }

    /**
     * Completes the initialization of the PickerBitmapView. Must be called before the image can
     * respond to click events.
     * @param request The request represented by this PickerBitmapView.
     * @param thumbnail The Bitmap to use for the thumbnail (or null).
     * @param placeholder Whether the image given is a placeholder or the actual image.
     */
    public void initialize(PickerBitmap request, @Nullable Bitmap thumbnail, boolean placeholder) {
        resetTile();

        mRequest = request;
        setItem(request);
        setThumbnailBitmap(thumbnail);
        mImageLoaded = !placeholder;

        updateSelectionState();

        setOnClickListener(this);
    }

    /**
     * Initialization for the special tiles (camera/gallery icon).
     */
    public void initializeSpecialTile() {
        int size = mCategoryView.getImageSize();
        Bitmap tile = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        tile.eraseColor(Color.argb(0, 0, 0, 0));

        int iconBitmapId, labelStringId;
        if (mRequest.type() == PickerBitmap.TileTypes.CAMERA) {
            iconBitmapId = R.drawable.ic_photo_camera;
            labelStringId = R.string.file_picker_camera;
        } else {
            iconBitmapId = R.drawable.ic_collections_black_24dp;
            labelStringId = R.string.file_picker_browse;
        }

        Resources resources = mContext.getResources();
        mSpecialTile.setText(labelStringId);
        Bitmap icon = BitmapFactory.decodeResource(resources, iconBitmapId);
        float pixels = resources.getDimensionPixelOffset(R.dimen.file_picker_special_icon_size);
        BitmapDrawable drawable = new BitmapDrawable(
                resources, Bitmap.createScaledBitmap(icon, (int) pixels, (int) pixels, true));
        ApiCompatibilityUtils.setCompoundDrawablesRelativeWithIntrinsicBounds(
                mSpecialTile, null, drawable, null, null);

        initialize(mRequest, tile, false);

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
        boolean special = mRequest.type() != PickerBitmap.TileTypes.PICTURE;
        boolean checked = super.isChecked();
        boolean anySelection =
                mSelectionDelegate != null && mSelectionDelegate.isSelectionEnabled();
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
            int color = ContextCompat.getColor(mContext, fgColorId);
            drawables[1].setColorFilter(color, PorterDuff.Mode.SRC_IN);
        }

        // The visibility of the unselected image is a little more complex because we don't want
        // to show it when nothing is selected and also not on a blank canvas.
        mSelectedView.setVisibility(!special && checked ? View.VISIBLE : View.GONE);
        mUnselectedView.setVisibility(
                !special && !checked && anySelection ? View.VISIBLE : View.GONE);
        mScrim.setVisibility(!special && !checked && anySelection ? View.VISIBLE : View.GONE);
    }
}
