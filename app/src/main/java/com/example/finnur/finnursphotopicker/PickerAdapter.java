// Copyright 2016 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.example.finnur.finnursphotopicker;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

// Chrome-specific imports:
// import org.chromium.chrome.R;

import java.util.List;

/**
 * A data adapter for the Photo Picker.
 */
public class PickerAdapter extends RecyclerView.Adapter<PickerBitmapViewHolder> {
    // Our context.
    private Context mContext;

    // The category view to use to show the images.
    private PickerCategoryView mCategoryView;

    /**
     * The PickerAdapter constructor.
     * @param context The context to use.
     * @param categoryView The category view to use to show the images.
     */
    public PickerAdapter(Context context, PickerCategoryView categoryView) {
        mContext = context;
        mCategoryView = categoryView;
    }

    // RecyclerView.Adapter:

    @Override
    public PickerBitmapViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                                .inflate(R.layout.picker_bitmap_view, parent, false);
        PickerBitmapView bitmapView = (PickerBitmapView) itemView;
        bitmapView.preInitialize(mCategoryView);
        return new PickerBitmapViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(PickerBitmapViewHolder holder, int position) {
        onBindViewHolder(holder, position, null);
    }

    @Override
    public void onBindViewHolder(PickerBitmapViewHolder holder, int position, List payloads) {
        holder.displayItem(mCategoryView, position);
    }

    @Override
    public int getItemCount() {
        return mCategoryView.getPickerBitmaps().size();
    }
}
