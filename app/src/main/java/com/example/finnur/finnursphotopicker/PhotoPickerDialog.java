// Copyright 2016 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

// FLIP
//package org.chromium.chrome.browser;
package com.example.finnur.finnursphotopicker;

import android.content.res.Resources;
import android.view.WindowManager;
import android.view.ViewGroup.LayoutParams;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.Toolbar.OnMenuItemClickListener;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

// Android Studio-specific:

// Chrome-specific:
/*
FLIP
import org.chromium.base.ContextUtils;
import org.chromium.chrome.R;
import org.chromium.chrome.browser.download.DownloadManagerService;
import org.chromium.chrome.browser.download.DownloadInfo;
import org.chromium.chrome.browser.download.DownloadItem;
import org.chromium.chrome.browser.download.ui.BackendProvider;
import org.chromium.chrome.browser.download.ui.DownloadManagerToolbar;
import org.chromium.chrome.browser.download.ui.DownloadHistoryItemWrapper;
import org.chromium.chrome.browser.download.ui.DownloadHistoryItemWrapper.DownloadItemWrapper;
import org.chromium.chrome.browser.download.ui.ThumbnailProvider;
import org.chromium.chrome.browser.download.ui.ThumbnailProviderImpl;
import org.chromium.chrome.browser.offlinepages.downloads.OfflinePageDownloadBridge;
import org.chromium.chrome.browser.profiles.Profile;
import org.chromium.chrome.browser.widget.selection.SelectionDelegate;
import org.chromium.ui.base.DeviceFormFactor;
*/

/**
 * UI for the photo chooser that shows on the Android platform as a result of
 * &lt;input type=file accept=image &gt; form element.
 */
public class PhotoPickerDialog extends AlertDialog implements OnMenuItemClickListener {

    private Context mContext;

    // The listener for the photo changed event.
    private final OnPhotoChangedListener mListener;

    static int s_folder = 1;

    // FLIP
    // The toolbar at the top of the dialog.
    //private PhotoPickerToolbar mToolbar;  // TODOf make final once inside in ctor?

    private SelectionDelegate<String> mSelectionDelegate;

    /**
     * @param context The context the dialog is to run in.
     * @param listener The object to notify when the color is set.
     */
    public PhotoPickerDialog(Context context,
                             OnPhotoChangedListener listener) {
        super(context, 0);

        mContext = context;
        mListener = listener;

        // Initialize title
        LayoutInflater inflater = (LayoutInflater) mContext
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View title = inflater.inflate(R.layout.photo_picker_dialog_title, null);
        setCustomTitle(title);

        mSelectionDelegate = new SelectionDelegate<String>();

        initializeChromeSpecificStuff(title);

        // Note that with the color picker there's not really any such thing as
        // "cancelled".
        // The color picker flow only finishes when we return a color, so we
        // have to always
        // return something. The concept of "cancelled" in this case just means
        // returning
        // the color that we were initialized with.
        String negativeButtonText = mContext.getString(R.string.color_picker_button_cancel);
        setButton(BUTTON_NEGATIVE, negativeButtonText,
                new Dialog.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        tryNotifyPhotoSet(null);
                    }
                });

        setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface arg0) {
                tryNotifyPhotoSet(null);
            }
        });

        // Initialize main content view
        View content = inflater.inflate(R.layout.photo_picker_dialog_content, null);
        setView(content);

        LinearLayout parentLayout = (LinearLayout) content.findViewById(R.id.layout);
        LayoutInflater layoutInflater = getLayoutInflater();

        PickerCategoryView categoryCamera = new PickerCategoryView(context);
        PickerCategoryView categoryScreenshots = new PickerCategoryView(context);
        PickerCategoryView categoryDownloads = new PickerCategoryView(context);

        if (++s_folder == 1) {
            categoryCamera.setInitialState("/DCIM/Camera", mSelectionDelegate);
            parentLayout.addView(categoryCamera);
        } else if (s_folder == 2) {
            categoryScreenshots.setInitialState("/Pictures/Screenshots", mSelectionDelegate);
            parentLayout.addView(categoryScreenshots);
        } else {
            categoryDownloads.setInitialState("/Download", mSelectionDelegate);
            parentLayout.addView(categoryDownloads);
            s_folder = 0;
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
        if (item.getItemId() == R.id.close_menu_id) {
            tryNotifyPhotoSet(null);
            return true;
        }
        Log.e("chromium ", "onMenuItemClick");
        */
        return false;
    }

    /**
     * Tries to notify any listeners that the color has been set.
     */
    private void tryNotifyPhotoSet(String[] photos) {
        if (mListener != null) mListener.onPhotoChanged(photos);
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
                R.id.normal_menu_group, R.id.selection_mode_menu_group);
        mToolbar.setTitle(R.string.menu_downloads);
        //addObserver(mToolbar);  // REMOVE?
    */
    }
}
