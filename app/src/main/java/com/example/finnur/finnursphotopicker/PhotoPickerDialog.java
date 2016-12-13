// Copyright 2016 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

// FLIP
//package org.chromium.chrome.browser;
package com.example.finnur.finnursphotopicker;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.widget.Toolbar.OnMenuItemClickListener;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.LinearLayout;

// Chrome-specific:
/*
FLIP
import org.chromium.chrome.R;
import org.chromium.chrome.browser.widget.selection.SelectionDelegate;
import org.chromium.ui.OnPhotoChangedListener;
*/

import java.util.List;

/**
 * UI for the photo chooser that shows on the Android platform as a result of
 * &lt;input type=file accept=image &gt; form element.
 */
public class PhotoPickerDialog extends AlertDialog implements OnMenuItemClickListener {

    private final Context mContext;

    // The listener for the photo changed event.
    private final OnPhotoChangedListener mListener;

    static int sFolder = 0;

    // FLIP
    // The toolbar at the top of the dialog.
    //private PhotoPickerToolbar mToolbar;  // TODOf make final once inside in ctor?

    private SelectionDelegate<PickerBitmap> mSelectionDelegate;

    /**
     * @param context The context the dialog is to run in.
     * @param listener The object to notify when the color is set.
     */
    public PhotoPickerDialog(Context context,
                             OnPhotoChangedListener listener,
                             boolean multiSelection) {
        super(context, 0);

        mContext = context;
        mListener = listener;

        // Initialize title
        LayoutInflater inflater = (LayoutInflater) mContext
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View title = inflater.inflate(R.layout.photo_picker_dialog_title, null);
        title.setPadding(0, 0, 0, 0);
        setCustomTitle(title);

        mSelectionDelegate = multiSelection
                ? new SelectionDelegateMulti<PickerBitmap>()
                : new SelectionDelegateSingle<PickerBitmap>();

        initializeChromeSpecificStuff(title);
        initializeNonChromeSpecificStuff();

        // Initialize main content view
        View content = inflater.inflate(R.layout.photo_picker_dialog_content, null);
        setView(content);

        LinearLayout parentLayout = (LinearLayout) content.findViewById(R.id.layout);
        LayoutInflater layoutInflater = getLayoutInflater();

        PickerCategoryView categoryCamera = new PickerCategoryView(context);
        PickerCategoryView categoryScreenshots = new PickerCategoryView(context);
        PickerCategoryView categoryDownloads = new PickerCategoryView(context);

        if (++sFolder == 1) {
            categoryCamera.setInitialState("/DCIM/Camera", mSelectionDelegate, multiSelection);
            parentLayout.addView(categoryCamera);
        } else if (sFolder == 2) {
            categoryScreenshots.setInitialState("/Pictures/Screenshots", mSelectionDelegate, multiSelection);
            parentLayout.addView(categoryScreenshots);
        } else {
            categoryDownloads.setInitialState("/Download", mSelectionDelegate, multiSelection);
            parentLayout.addView(categoryDownloads);
            sFolder = 0;
        }

        boolean hasItems = categoryCamera.getVisibility() == View.VISIBLE
                || categoryScreenshots.getVisibility() == View.VISIBLE
                || categoryDownloads.getVisibility() == View.VISIBLE;
        if (!hasItems) {
            Log.e("chromium", "Show empty message");
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setLayout(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        //getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        // FLIP
        /*
        if (item.getItemId() == R.id.close_menu_id
                || item.getItemId() == R.id.selection_mode_done_menu_id) {
            tryNotifyPhotoSet();
            dismiss();
            return true;
        }
        Log.e("chromium ", "onMenuItemClick");
        */
        return false;
    }

    /**
     * Tries to notify any listeners that one or more photos have been selected.
     */
    private void tryNotifyPhotoSet() {
        if (mListener == null) {
            return;
        }

        List<PickerBitmap> selectedFiles = mSelectionDelegate.getSelectedItems();
        String[] photos = new String[selectedFiles.size()];
        int i = 0;
        for (PickerBitmap bitmap : selectedFiles) {
            photos[i++] = bitmap.getFilePath();
        }

        mListener.onPhotoChanged(photos);
    }

    private void initializeChromeSpecificStuff(View title) {
        // FLIP
        /*
        mToolbar = (PhotoPickerToolbar) title.findViewById(R.id.action_bar);
        mToolbar.setOnMenuItemClickListener(this);
        DrawerLayout drawerLayout = null;

        //if (!DeviceFormFactor.isLargeTablet(mContext)) {
        //    drawerLayout = (DrawerLayout) mMainView;
        //    addDrawerListener(drawerLayout);
        //}
        mToolbar.initialize(mSelectionDelegate, 0, drawerLayout,
                R.id.file_picker_normal_menu_group,
                R.id.file_picker_selection_mode_menu_group);
        mToolbar.setTitle(R.string.file_picker_select_files);
        //addObserver(mToolbar);  // REMOVE?
    */
    }

    private void initializeNonChromeSpecificStuff() {
        // FLIP
        /*
        */
        String negativeButtonText = mContext.getString(R.string.color_picker_button_cancel);
        setButton(BUTTON_NEGATIVE, negativeButtonText,
                new Dialog.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        tryNotifyPhotoSet();
                    }
                });

        setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface arg0) {
                tryNotifyPhotoSet();
            }
        });
    }

}
