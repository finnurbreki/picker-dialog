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
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.support.annotation.DimenRes;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.util.LruCache;
import android.view.View;
import android.widget.RelativeLayout;

// Chrome-specific:
/* FLIP
import org.chromium.chrome.R;
import org.chromium.chrome.browser.download.ui.ThumbnailProviderImpl;
import org.chromium.chrome.browser.widget.selection.SelectionDelegate;
import org.chromium.ui.OnPhotoPickerListener;
*/

import java.util.List;

public class PickerCategoryView extends RelativeLayout
        implements FileEnumWorkerTask.FilesEnumeratedCallback,
                DecoderServiceHost.ServiceReadyCallback,
                RecyclerView.RecyclerListener {
    private Context mContext;
    private PickerAdapter mPickerAdapter;
    private List<PickerBitmap> mPickerBitmaps;
    private boolean mMultiSelection;
    private OnPhotoPickerListener mListener;

    private RecyclerView mRecyclerView;

    private SelectionDelegate<PickerBitmap> mSelectionDelegate;

    private DecoderServiceHost mDecoderServiceHost;

    private ThumbnailProviderImpl mThumbnailProvider;

    private LruCache<String, Bitmap> mUglyBitmaps;
    private LruCache<String, Bitmap> mPrettyBitmaps;

    // mColumns and mPadding should both be even numbers or both odd, not a mix (the column padding
    // will not be of uniform thickness if they are a mix).
    private int mColumns;

    // The padding between columns. See also comment for mColumns.
    private int mPadding;

    // Maximum number of bitmaps to show.
    private int mMaxImages;

    // The size of the bitmaps (equal length for width and height).
    private int mImageSize;

    // The control to show for when an image is selected.
    private Bitmap mBitmapSelected;

    // The control to show for when an image is not selected.
    private Bitmap mBitmapUnselected;

    // A worker task for asynchronously enumerating files off the main thread.
    private FileEnumWorkerTask mWorkerTask;

    // Default value to use as an accept attribute for testing
    private static final String DEFAULT_ACCEPT_ATTR = "image/*,video/*";
/*
    private class RecyclerViewItemDecoration extends RecyclerView.ItemDecoration {
        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent,
                                   RecyclerView.State state) {
            int position = parent.getChildAdapterPosition(view);

            // To understand the right shift, one needs to look at an example. Lets say mColumns
            // is 3 and mPadding is 21. Then RecyclerView will draw items in the following manner:
            // [Img1] 28px [Img2] 28px [Img3] 28px.
            // This needs to be converted to:
            // 21px [Img1] 21px [Img2] 21px [Img3] 21px.
            // In other words, the first image needs to be shifted right by 21px, the next by 14px
            // and the last one by 7px to achieve even distribution of padding everywhere. Put
            // another way: the 1st image shifts 3 steps, 2nd shifts 2 steps and 3rd shifts 1 step.
            int step = mPadding / mColumns;
            int shift = mPadding - (step * (position % mColumns));
            outRect.left = shift;

            outRect.top = mPadding;
            // Bottom row also gets padding below it.
            int index = parent.getAdapter().getItemCount() / mColumns;
            if (position >= index * mColumns) {
                outRect.bottom = mPadding;
            }
        }
    }
*/
    public PickerCategoryView(Context context) {
        super(context);
        init(context);
    }

    public PickerCategoryView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public PickerCategoryView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
        mContext = context;

        mDecoderServiceHost = useDecoderService() ? new DecoderServiceHost(this) : null;
        if (mDecoderServiceHost != null) {
            mDecoderServiceHost.bind(mContext);
        }

        if (((mColumns % 2) == 0) != ((mPadding % 2) == 0)) {
            throw new AssertionError("Columns and padding should both be odd or both even");
        }
        inflate(mContext, R.layout.picker_category_view, this);
    }

    public void endConnection() {
        if (mDecoderServiceHost != null) {
            mDecoderServiceHost.unbind(mContext);
        }
    }

    @Override
    public void serviceReady() {
        // TODOf instead of waiting to enumerate, perhaps let filesEnumeratedCallback wait to
        // provide the data?
        prepareBitmaps();
    }

    public int getMaxImagesShown() {
        return mMaxImages;
    }
    public int getImageSize() {
        return mImageSize;
    }
    public SelectionDelegate<PickerBitmap> getSelectionDelegate() {
        return mSelectionDelegate;
    }
    public List<PickerBitmap> getPickerBitmaps() {
        return mPickerBitmaps;
    }
    public ThumbnailProviderImpl getThumbnailProvider() {
        return mThumbnailProvider;
    }
    public DecoderServiceHost getDecoderServiceHost() {
        return mDecoderServiceHost;
    }
    public LruCache<String, Bitmap> getUglyBitmaps() {
        return mUglyBitmaps;
    }
    public LruCache<String, Bitmap> getPrettyBitmaps() {
        return mPrettyBitmaps;
    }
    public boolean isMultiSelect() {
        return mMultiSelection;
    }
    // FLIP
    public static boolean useDecoderService() {
        return true;
    }  // TODOf remove.

    public Bitmap getSelectionBitmap(boolean selected) {
        if (selected) {
            return mBitmapSelected;
        } else {
            return mBitmapUnselected;
        }
    }

    private Bitmap colorBitmap(Bitmap original, int color) {
        Bitmap mutable = original.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(mutable);
        Paint paint = new Paint();
        paint.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(mutable, 0.f, 0.f, paint);
        return mutable;
    }

    public void setInitialState(SelectionDelegate<PickerBitmap> selectionDelegate,
            OnPhotoPickerListener listener, boolean multiSelection, int width) {
        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        // mRecyclerView.addItemDecoration(new RecyclerViewItemDecoration());
        mRecyclerView.setRecyclerListener(this);
        mSelectionDelegate = selectionDelegate;
        mMultiSelection = multiSelection;
        mListener = listener;

        // FLIP
        mBitmapSelected = BitmapFactory.decodeResource(mContext.getResources(),
                  R.mipmap.ic_check_circle_black_24dp);
        //          R.drawable.ic_share_white_24dp);
        mBitmapUnselected = BitmapFactory.decodeResource(mContext.getResources(),
                  R.mipmap.ic_radio_button_unchecked_black_24dp);
        //          R.drawable.ic_arrow_back_white_24dp);

        // Apply color to the bitmaps.
        int prefAccentColor = ContextCompat.getColor(mContext, R.color.pref_accent_color);
        mBitmapSelected = colorBitmap(mBitmapSelected, prefAccentColor);
        int unselectedColor = ContextCompat.getColor(mContext, R.color.white_mode_tint);
        mBitmapUnselected = colorBitmap(mBitmapUnselected, unselectedColor);

        calculateGridMetrics(width);
        mMaxImages = 40 * mColumns;
        mThumbnailProvider = new ThumbnailProviderImpl(mImageSize);

        // The dialog width is known, the padding between images is known and the number of
        // image columns is known. From that we can calculate how much space is remaining for
        // showing images. The layout used when mColumns equals 3 is:
        // Padding [Img] Padding [Img] Padding.
        // mImageSize = (width - (mPadding * (mColumns + 1))) / mColumns;

        // The thumbnail clamps the maximum of the smaller side, we need to clamp
        // down the maximum of the larger side, so we flip the sizes.
        // mThumbnailProvider = new ThumbnailProviderImpl(mImageSize * 3 / 4);

        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        final int cacheSizeLarge = maxMemory / 2; // 1/2th of the available memory.
        final int cacheSizeSmall = maxMemory / 8; // 1/8th of the available memory.
        Log.e("chromium", "Cache sizes: " + cacheSizeLarge + " " + cacheSizeSmall);
        mUglyBitmaps = new LruCache<String, Bitmap>(cacheSizeSmall) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                return bitmap.getByteCount() / 1024;
            }
        };
        mPrettyBitmaps = new LruCache<String, Bitmap>(cacheSizeLarge) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                return bitmap.getByteCount() / 1024;
            }
        };

        mPickerAdapter = new PickerAdapter(mContext, this);
        RecyclerView.LayoutManager mLayoutManager = new GridLayoutManager(mContext, mColumns);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mRecyclerView.addItemDecoration(new GridSpacingItemDecoration(mColumns, mPadding));

        if (!useDecoderService()) {
            serviceReady();  // Call it manually because the decoder won't do it for us.
        }
    }

    private void calculateGridMetrics(int width) {
        int minSize
                = mContext.getResources().getDimensionPixelSize(R.dimen.file_picker_tile_min_size);
        mPadding = mContext.getResources().getDimensionPixelSize(R.dimen.file_picker_tile_gap);
        mColumns = Math.max(1, (width - mPadding) / (minSize+mPadding));
        mImageSize = (width - mPadding*(mColumns+1)) / (mColumns);
    }

    public void showGallery() {
        mListener.onPickerUserAction(OnPhotoPickerListener.Action.LAUNCH_GALLERY, null);
    }

    public void showCamera() {
        mListener.onPickerUserAction(OnPhotoPickerListener.Action.LAUNCH_CAMERA, null);
    }

    public void filesEnumeratedCallback(List<PickerBitmap> files) {
        mPickerBitmaps = files;
        if (files != null && files.size() > 0) {
            mRecyclerView.setAdapter(mPickerAdapter);
        } else {
            setVisibility(View.GONE);
        }
    }

    private void prepareBitmaps() {
        if (mWorkerTask != null) {
            mWorkerTask.cancel(true);
        }

        mWorkerTask = new FileEnumWorkerTask(this);
        mWorkerTask.execute();
    }

    @Override
    public void onViewRecycled(RecyclerView.ViewHolder holder) {
        if (!useDecoderService()) {
            return;
        }

        PickerBitmapViewHolder bitmapHolder = (PickerBitmapViewHolder) holder;
        String filePath = bitmapHolder.getFilePath();
        if (filePath != null) {
            getDecoderServiceHost().cancelDecodeImage(filePath);
        }
    }

    private class GridSpacingItemDecoration extends RecyclerView.ItemDecoration {
        private int spanCount;
        private int spacing;

        public GridSpacingItemDecoration(int spanCount, int spacing) {
            this.spanCount = spanCount;
            this.spacing = spacing;
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
            int left = 0, right = 0, top = 0, bottom = 0;
            int position = parent.getChildAdapterPosition(view);

            if (position >= 0) {
                int column = position % spanCount; // item column

                left = spacing - column * spacing / spanCount;
                right = (column + 1) * spacing / spanCount;

                if (position < spanCount) { // top edge
                    top = spacing;
                }
                bottom = spacing; // item bottom
            }

            outRect.set(left, top, right, bottom);
        }
    }
}
