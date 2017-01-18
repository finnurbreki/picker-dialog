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
                DecoderServiceHost.ServiceReadyCallback {
    private Context mContext;
    private PickerAdapter mPickerAdapter;
    private List<PickerBitmap> mPickerBitmaps;
    private boolean mMultiSelection;
    private String mPath;
    private OnPhotoPickerListener mListener;

    private RecyclerView mRecyclerView;

    private SelectionDelegate<PickerBitmap> mSelectionDelegate;

    private DecoderServiceHost mDecoderServiceHost;

    private ThumbnailProviderImpl mThumbnailProvider;

    private LruCache<String, Bitmap> mUglyBitmaps;
    private LruCache<String, Bitmap> mPrettyBitmaps;

    // mColumns and mPadding should both be even numbers or both odd, not a mix (the column padding
    // will not be of uniform thickness if they are a mix).
    private int mColumns = 3;

    // The padding between columns. See also comment for mColumns.
    private int mPadding = 21;

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
        prepareBitmaps(mPath);
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

    public void setInitialState(String path, SelectionDelegate<PickerBitmap> selectionDelegate,
            OnPhotoPickerListener listener, boolean multiSelection, int width) {
        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        mRecyclerView.addItemDecoration(new RecyclerViewItemDecoration());
        mSelectionDelegate = selectionDelegate;
        mMultiSelection = multiSelection;
        mPath = path;
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
        int grayColor = ContextCompat.getColor(mContext, R.color.google_grey_600);
        mBitmapUnselected = colorBitmap(mBitmapUnselected, grayColor);

        mMaxImages = 40 * mColumns;

        // The dialog width is known, the padding between images is known and the number of
        // image columns is known. From that we can calculate how much space is remaining for
        // showing images. The layout used when mColumns equals 3 is:
        // Padding [Img] Padding [Img] Padding.
        mImageSize = (width - (mPadding * (mColumns + 1))) / mColumns;

        // The thumbnail clamps the maximum of the smaller side, we need to clamp
        // down the maximum of the larger side, so we flip the sizes.
        mThumbnailProvider = new ThumbnailProviderImpl(mImageSize * 3 / 4);

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

        if (!useDecoderService()) {
            serviceReady();  // Call it manually because the decoder won't do it for us.
        }
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
            //mPickerAdapter.notifyDataSetChanged();
        } else {
            setVisibility(View.GONE);
        }
    }

    private void prepareBitmaps(String path) {
        if (mWorkerTask != null) {
            mWorkerTask.cancel(true);
        }

        mWorkerTask = new FileEnumWorkerTask(this);
        mWorkerTask.execute(path);
    }
}
