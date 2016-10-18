// Copyright 2016 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.example.finnur.finnursphotopicker;

import android.content.Context;
import android.content.res.Resources;
import android.os.Environment;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class PickerCategoryView extends RelativeLayout {
    private Context mContext;
    private PickerAdapter mPickerAdapter;
    private List<PickerBitmap> pickerBitmaps = new ArrayList<>();

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

    public void setInitialState(String header, String path) {
        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        mTitle = (TextView) findViewById(R.id.title);

        mTitle.setText(header);

        int columns = 4;
        mMaxImages = 2 * columns;  // Show two rows of images.
        DisplayMetrics metrics = Resources.getSystem().getDisplayMetrics();
        int widthPerColumn = metrics.widthPixels / columns;

        mPickerAdapter = new PickerAdapter(pickerBitmaps, mContext.getResources(), widthPerColumn, mMaxImages);
        RecyclerView.LayoutManager mLayoutManager = new GridLayoutManager(mContext, columns);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mRecyclerView.setAdapter(mPickerAdapter);

        prepareBitmaps(path, mMaxImages);
    }

    private void prepareBitmaps(String path, int maxPhotos) {
        String fullPath = Environment.getExternalStorageDirectory().toString() + path;
        //Log.d("Files", "Path: " + fullPath);
        File directory = new File(fullPath);
        File[] files = directory.listFiles();
        //Log.d("Files", "Size: "+ files.length);
        if (files.length == 0) {
            setVisibility(View.GONE);
        } else {
            for (int i = 0; i < files.length; i++) {
                //Log.d("Files", "FileName2:" + fullPath + "/" + files[i].getName());
                pickerBitmaps.add(new PickerBitmap(fullPath + "/" + files[i].getName()));
            }
        }

        mPickerAdapter.notifyDataSetChanged();
    }
}
