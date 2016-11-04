// Copyright 2016 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

// FLIP
//package org.chromium.chrome.browser;
package com.example.finnur.finnursphotopicker;

import android.content.Context;
import android.content.res.Resources;
import android.os.Environment;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
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
    private TextView mTitle;

    private int mMaxImages;

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

    public void setInitialState(String path, SelectionDelegate<String> selectionDelegate) {
        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);

        int columns = 3;
        mMaxImages = 30 * columns;
        DisplayMetrics metrics = Resources.getSystem().getDisplayMetrics();
        int widthPerColumn = metrics.widthPixels / columns;

        mPickerAdapter = new PickerAdapter(mPickerBitmaps, mContext, widthPerColumn, mMaxImages, selectionDelegate);
        RecyclerView.LayoutManager mLayoutManager = new GridLayoutManager(mContext, columns);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mRecyclerView.setAdapter(mPickerAdapter);

        prepareBitmaps(path, mMaxImages);
    }

    private void prepareBitmaps(String path, int maxPhotos) {
        long startTime = System.nanoTime();
        String fullPath = Environment.getExternalStorageDirectory().toString() + path;

        //Log.e("chromium", "Path: " + fullPath);
        File directory = new File(fullPath);
        File[] files = directory.listFiles();
        //Log.e("chromium", "Size: "+ files.length);
        if (files == null || files.length == 0) {
            setVisibility(View.GONE);
        } else {
            for (int i = 0; i < files.length; i++) {
                //Log.e("chromium", "FileName:" + fullPath + "/" + files[i].getName() + " size: " + files[i].length());
                //if (files[i].length() < 10000)
                    mPickerBitmaps.add(new PickerBitmap(fullPath + "/" + files[i].getName()));
                //else
                //    Log.e("chromium", "Skipping file " + files[i].getName() + " " + files[i].length());
            }
        }

        long endTime = System.nanoTime();
        long durationInMs = TimeUnit.MILLISECONDS.convert(endTime - startTime, TimeUnit.NANOSECONDS);
        Log.e("chromium", "Enumerated " + mPickerAdapter.getItemCount() + " files: " + durationInMs + " ms");
        mPickerAdapter.notifyDataSetChanged();
    }
}
