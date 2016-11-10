// Copyright 2016 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

// FLIP
//package org.chromium.chrome.browser;
package com.example.finnur.finnursphotopicker;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.os.Environment;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

// Chrome-specific:
/*
FLIP
import org.chromium.chrome.R;
import org.chromium.chrome.browser.widget.selection.SelectionDelegate;
*/

public class PickerCategoryView extends RelativeLayout {
    private Context mContext;
    private PickerAdapter mPickerAdapter;
    private List<PickerBitmap> mPickerBitmaps = new ArrayList<>();

    private RecyclerView mRecyclerView;

    private SelectionDelegate<PickerBitmap> mSelectionDelegate;

    private int mColumns = 3;

    // Maximum number of bitmaps to show.
    private int mMaxImages = 1;

    // The size of the bitmaps (equal length for width and height).
    private int mImageSize = 10;

    private Bitmap mBitmapSelected;
    private Bitmap mBitmapUnselected;


    private class RecyclerViewItemDecoration extends RecyclerView.ItemDecoration {
        private static final int PADDING = 18;

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent,
                                   RecyclerView.State state) {
            int position = parent.getChildAdapterPosition(view);

            if (position % mColumns != 0) {
                outRect.left = PADDING;
            }

            if (position < parent.getAdapter().getItemCount() - mColumns) {
                outRect.bottom = PADDING;
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
        inflate(mContext, R.layout.picker_category_view, this);
    }

    public int getMaxImagesShown() { return mMaxImages; }
    public int getImageSize() { return mImageSize; }
    public SelectionDelegate<PickerBitmap> getSelectionDelegate() { return mSelectionDelegate; }
    public List<PickerBitmap> getPickerBitmaps() { return mPickerBitmaps; }

    public Bitmap getSelectionBitmap(boolean selected) {
        if (selected)
            return mBitmapSelected;
        else
            return mBitmapUnselected;
    }

    public void setInitialState(String path, SelectionDelegate<PickerBitmap> selectionDelegate) {
        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        mRecyclerView.addItemDecoration(new RecyclerViewItemDecoration());
        mSelectionDelegate = selectionDelegate;

        // FLIP
        mBitmapSelected = BitmapFactory.decodeResource(mContext.getResources(), R.mipmap.ic_check_circle_black_24dp);
        mBitmapUnselected = BitmapFactory.decodeResource(mContext.getResources(), R.mipmap.ic_donut_large_black_24dp);
        //mBitmapSelected = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.ic_share_white_24dp);
        //mBitmapUnselected = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.ic_arrow_back_white_24dp);

        mMaxImages = 5 * mColumns;
        DisplayMetrics metrics = Resources.getSystem().getDisplayMetrics();
        mImageSize = metrics.widthPixels / mColumns;

        mPickerAdapter = new PickerAdapter(mContext, this);
        RecyclerView.LayoutManager mLayoutManager = new GridLayoutManager(mContext, mColumns);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mRecyclerView.setAdapter(mPickerAdapter);

        prepareBitmaps(path);
    }

    private void prepareBitmaps(String path) {
        long startTime = System.nanoTime();
        String fullPath = Environment.getExternalStorageDirectory().toString() + path;

        File directory = new File(fullPath);
        File[] files = directory.listFiles();
        if (files == null || files.length == 0) {
            setVisibility(View.GONE);
        } else {
            for (int i = 0; i < files.length; i++) {
                //Log.e("chromium", "FileName:" + fullPath + "/" + files[i].getName() + " size: " + files[i].length());
                mPickerBitmaps.add(new PickerBitmap(fullPath + "/" + files[i].getName()));
            }
        }

        long endTime = System.nanoTime();
        long durationInMs = TimeUnit.MILLISECONDS.convert(endTime - startTime, TimeUnit.NANOSECONDS);
        Log.e("chromium", "Enumerated " + mPickerAdapter.getItemCount() + " files: " + durationInMs + " ms");
        mPickerAdapter.notifyDataSetChanged();
    }
}
