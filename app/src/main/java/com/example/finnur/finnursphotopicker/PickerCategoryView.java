// Copyright 2016 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.example.finnur.finnursphotopicker;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.LruCache;
import android.view.View;
import android.widget.RelativeLayout;

// Chrome-specific imports:
/*
import org.chromium.base.VisibleForTesting;
import org.chromium.chrome.R;
import org.chromium.chrome.browser.widget.selection.SelectionDelegate;
import org.chromium.ui.OnPhotoPickerListener;
*/

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * A class for keeping track of common data associated with showing photos in
 * the photo picker, for example the RecyclerView and the bitmap caches.
 */
public class PickerCategoryView extends RelativeLayout
        implements FileEnumWorkerTask.FilesEnumeratedCallback,
                   DecoderServiceHost.ServiceReadyCallback, RecyclerView.RecyclerListener {
    // Our context.
    private Context mContext;

    // The list of images on disk, sorted by last-modified first.
    private List<PickerBitmap> mPickerBitmaps;

    // True if multi-selection is allowed in the picker.
    private boolean mMultiSelection;

    // The callback to notify the listener of decisions reached in the picker.
    private OnPhotoPickerListener mListener;

    // The host class for the decoding service.
    private DecoderServiceHost mDecoderServiceHost;

    // The recycler view showing the images.
    private RecyclerView mRecyclerView;

    // The picker adapter for the RecyclerView.
    private PickerAdapter mPickerAdapter;

    // The selection delegate keeping track of which images are selected.
    private SelectionDelegate<PickerBitmap> mSelectionDelegate;

    // A low-resolution cache for images. Helpful for cache misses from the high-resolution cache
    // to avoid showing gray squares (show pixelated versions instead until image can be loaded off
    // disk).
    private LruCache<String, Bitmap> mLowResBitmaps;

    // A high-resolution cache for images.
    private LruCache<String, Bitmap> mHighResBitmaps;

    // The number of columns to show. Note: mColumns and mPadding (see below) should both be even
    // numbers or both odd, not a mix (the column padding will not be of uniform thickness if they
    // are a mix).
    private int mColumns;

    // The padding between columns. See also comment for mColumns.
    private int mPadding;

    // The size of the bitmaps (equal length for width and height).
    private int mImageSize;

    // The control to show for when an image is selected.
    private Bitmap mBitmapSelected;

    // The control to show for when an image is not selected.
    private Bitmap mBitmapUnselected;

    // A worker task for asynchronously enumerating files off the main thread.
    private FileEnumWorkerTask mWorkerTask;

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

    /**
     * A helper function for initializing the PickerCategoryView.
     * @param context The context to use.
     */
    private void init(Context context) {
        mContext = context;

        mDecoderServiceHost = new DecoderServiceHost(this);
        mDecoderServiceHost.bind(mContext);

        if (((mColumns % 2) == 0) != ((mPadding % 2) == 0)) {
            throw new AssertionError("Columns and padding should both be odd or both even");
        }
        inflate(mContext, R.layout.picker_category_view, this);
    }

    /**
     * Severs the connection to the decoding utility process.
     */
    public void endConnection() {
        if (mDecoderServiceHost != null) {
            mDecoderServiceHost.unbind(mContext);
        }
    }

    /**
     * Sets the starting state for the PickerCategoryView object.
     * @param selectionDelegate The selection delegate to use.
     * @param listener The listener who should be notified of actions.
     * @param multiSelection Whether to allow the user to select more than one image.
     * @param width The width of the dialog showing the photos.
     */
    public void setStartingState(SelectionDelegate<PickerBitmap> selectionDelegate,
            OnPhotoPickerListener listener, boolean multiSelection, int width) {
        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        mRecyclerView.setRecyclerListener(this);
        mSelectionDelegate = selectionDelegate;
        mMultiSelection = multiSelection;
        mListener = listener;

        mBitmapSelected = BitmapFactory.decodeResource(
                mContext.getResources(), R.drawable.ic_check_circle_black_24dp);
        mBitmapUnselected = BitmapFactory.decodeResource(
                mContext.getResources(), R.drawable.ic_radio_button_unchecked_black_24dp);

        // Apply color to the bitmaps.
        int prefAccentColor = ContextCompat.getColor(mContext, R.color.pref_accent_color);
        mBitmapSelected = colorBitmap(mBitmapSelected, prefAccentColor);
        int unselectedColor = ContextCompat.getColor(mContext, R.color.white_mode_tint);
        mBitmapUnselected = colorBitmap(mBitmapUnselected, unselectedColor);

        calculateGridMetrics(width);

        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        final int cacheSizeLarge = maxMemory / 2; // 1/2 of the available memory.
        final int cacheSizeSmall = maxMemory / 8; // 1/8th of the available memory.
        mLowResBitmaps = new LruCache<String, Bitmap>(cacheSizeSmall) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                return bitmap.getByteCount() / 1024;
            }
        };
        mHighResBitmaps = new LruCache<String, Bitmap>(cacheSizeLarge) {
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
    }

    // DecoderServiceHost.ServiceReadyCallback:

    @Override
    public void serviceReady() {
        prepareBitmaps();
    }

    // FileEnumWorkerTask.FilesEnumeratedCallback:

    @Override
    public void filesEnumeratedCallback(List<PickerBitmap> files) {
        mPickerBitmaps = files;
        if (files != null && files.size() > 0) {
            mRecyclerView.setAdapter(mPickerAdapter);
        } else {
            setVisibility(View.GONE);
        }
    }

    // RecyclerView.RecyclerListener:

    @Override
    public void onViewRecycled(RecyclerView.ViewHolder holder) {
        PickerBitmapViewHolder bitmapHolder = (PickerBitmapViewHolder) holder;
        String filePath = bitmapHolder.getFilePath();
        if (filePath != null) {
            getDecoderServiceHost().cancelDecodeImage(filePath);
        }
    }

    // Simple accessors:

    public int getImageSize() {
        return mImageSize;
    }
    public SelectionDelegate<PickerBitmap> getSelectionDelegate() {
        return mSelectionDelegate;
    }
    public List<PickerBitmap> getPickerBitmaps() {
        return mPickerBitmaps;
    }
    public DecoderServiceHost getDecoderServiceHost() {
        return mDecoderServiceHost;
    }
    public LruCache<String, Bitmap> getLowResBitmaps() {
        return mLowResBitmaps;
    }
    public LruCache<String, Bitmap> getHighResBitmaps() {
        return mHighResBitmaps;
    }
    public boolean isMultiSelect() {
        return mMultiSelection;
    }

    /**
     * Notifies the caller that the user selected to launch the gallery.
     */
    public void showGallery() {
        mListener.onPickerUserAction(OnPhotoPickerListener.Action.LAUNCH_GALLERY, null);
    }

    /**
     * Notifies the caller that the user selected to launch the camera intent.
     */
    public void showCamera() {
        mListener.onPickerUserAction(OnPhotoPickerListener.Action.LAUNCH_CAMERA, null);
    }

    /**
     * Returns the selection bitmaps (control indicating whether the image is selected or not).
     * @param selected See return value.
     * @return If |selected| is true, the selection bitmap is returned. Otherwise the unselection
     *         bitmap is returned.
     */
    public Bitmap getSelectionBitmap(boolean selected) {
        if (selected) {
            return mBitmapSelected;
        } else {
            return mBitmapUnselected;
        }
    }

    /**
     * Applies a color filter to a bitmap.
     * @param original The bitmap to color.
     * @param color The color to apply.
     * @return A colored bitmap.
     */
    private Bitmap colorBitmap(Bitmap original, int color) {
        Bitmap mutable = original.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(mutable);
        Paint paint = new Paint();
        paint.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(mutable, 0.f, 0.f, paint);
        return mutable;
    }

    /**
     * Calculates image size and how many columns can fit on-screen.
     * @param width The total width of the boundary to show the images in.
     */
    private void calculateGridMetrics(int width) {
        int minSize =
                mContext.getResources().getDimensionPixelSize(R.dimen.file_picker_tile_min_size);
        mPadding = mContext.getResources().getDimensionPixelSize(R.dimen.file_picker_tile_gap);
        mColumns = Math.max(1, (width - mPadding) / (minSize + mPadding));
        mImageSize = (width - mPadding * (mColumns + 1)) / (mColumns);
    }

    @VisibleForTesting
    public RecyclerView getRecyclerViewForTesting() {
        return mRecyclerView;
    }

    @VisibleForTesting
    private boolean loadTestFiles() {
        Map<String, Long> testFiles = mListener.getFilesForTesting();
        if (testFiles == null) {
            return false;
        }

        List<PickerBitmap> files = new ArrayList<>();
        for (Map.Entry<String, Long> entry : testFiles.entrySet()) {
            String key = entry.getKey();
            Long value = entry.getValue();
            files.add(new PickerBitmap(key, value, PickerBitmap.TileTypes.PICTURE));
        }
        Collections.sort(files);
        filesEnumeratedCallback(files);
        return true;
    }

    /**
     * Prepares bitmaps for loading.
     */
    private void prepareBitmaps() {
        if (loadTestFiles()) return;

        if (mWorkerTask != null) {
            mWorkerTask.cancel(true);
        }

        mWorkerTask = new FileEnumWorkerTask(this, AcceptFileFilter.forAttr("image/*,video/*"));
        mWorkerTask.execute();
    }

    /**
     * A class for implementing grid spacing between items.
     */
    private class GridSpacingItemDecoration extends RecyclerView.ItemDecoration {
        // The number of spans to account for.
        private int mSpanCount;

        // The amount of spacing to use.
        private int mSpacing;

        public GridSpacingItemDecoration(int spanCount, int spacing) {
            mSpanCount = spanCount;
            mSpacing = spacing;
        }

        @Override
        public void getItemOffsets(
                Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
            int left = 0, right = 0, top = 0, bottom = 0;
            int position = parent.getChildAdapterPosition(view);

            if (position >= 0) {
                int column = position % mSpanCount;

                left = mSpacing - column * mSpacing / mSpanCount;
                right = (column + 1) * mSpacing / mSpanCount;

                if (position < mSpanCount) {
                    top = mSpacing;
                }
                bottom = mSpacing;
            }

            outRect.set(left, top, right, bottom);
        }
    }
}
