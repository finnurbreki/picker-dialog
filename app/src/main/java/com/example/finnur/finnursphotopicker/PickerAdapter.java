// Copyright 2016 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.example.finnur.finnursphotopicker;

import android.support.v7.widget.RecyclerView.Adapter;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

// Chrome-specific imports:
// import org.chromium.chrome.R;

import java.util.List;

/**
 * A data adapter for the Photo Picker.
 */
public class PickerAdapter extends Adapter<ViewHolder> {
    // The category view to use to show the images.
    private PickerCategoryView mCategoryView;

    /**
     * The PickerAdapter constructor.
     * @param categoryView The category view to use to show the images.
     */
    public PickerAdapter(PickerCategoryView categoryView) {
        mCategoryView = categoryView;
    }

    // RecyclerView.Adapter:

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                                .inflate(R.layout.picker_bitmap_view, parent, false);
        PickerBitmapView bitmapView = (PickerBitmapView) itemView;
        bitmapView.preInitialize(mCategoryView);
        return new PickerBitmapViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        onBindViewHolder(holder, position, null);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position, List payloads) {
        if (holder instanceof PickerBitmapViewHolder) {
            PickerBitmapViewHolder myHolder = (PickerBitmapViewHolder) holder;
            myHolder.displayItem(mCategoryView, position);
        }
    }

    @Override
    public int getItemCount() {
        return mCategoryView.getPickerBitmaps().size();
    }
}
