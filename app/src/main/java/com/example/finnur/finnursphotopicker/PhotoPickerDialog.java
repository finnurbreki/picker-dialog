// Copyright 2016 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.example.finnur.finnursphotopicker;

import android.app.Dialog;  // Android Studio project specific (not required for Chromium).
import android.content.Context;
import android.content.DialogInterface;  // Android Studio project specific (not required for Chromium).
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar.OnMenuItemClickListener;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.LinearLayout;

// Chrome-specific imports:
/*
import org.chromium.base.VisibleForTesting;
import org.chromium.chrome.R;
import org.chromium.chrome.browser.widget.selection.SelectionDelegate;
import org.chromium.ui.OnPhotoPickerListener;
*/

import java.util.List;

/**
 * UI for the photo chooser that shows on the Android platform as a result of
 * &lt;input type=file accept=image &gt; form element.
 */
public class PhotoPickerDialog extends AlertDialog implements OnMenuItemClickListener {
    // The context to use.
    private final Context mContext;

    // The listener for the photo changed event.
    private final OnPhotoPickerListener mListener;

    // The category we're showing photos for.
    private PickerCategoryView mCategoryView;

    // True after this widget has been initialized.
    private boolean mInitialized;

    // True if the file picker should allow multi-selection.
    private boolean mMultiSelection;

    // The toolbar at the top of the dialog.
    //private PhotoPickerToolbar mToolbar;

    // The selection delegate to keep track of the selected items.
    private SelectionDelegate<PickerBitmap> mSelectionDelegate;

    /**
     * The PhotoPickerDialog constructor.
     * @param context The context to use.
     * @param listener The listener object that gets notified when an action is taken.
     * @param multiSelection True if the photo picker should allow multiple items to be selected.
     */
    public PhotoPickerDialog(
            Context context, OnPhotoPickerListener listener, boolean multiSelection) {
        super(context, R.style.FullscreenWhite);

        mContext = context;
        mListener = listener;
        mMultiSelection = multiSelection;

        mSelectionDelegate = new SelectionDelegate<PickerBitmap>();
        if (!multiSelection) mSelectionDelegate.setSingleSelectionMode();

        // Initialize the title.
        LayoutInflater inflater =
                (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View title = inflater.inflate(R.layout.photo_picker_dialog_title, null);
        title.setPadding(0, 0, 0, 0);
        setCustomTitle(title);
        initialize(title);

        // Initialize the main content view.
        View content = inflater.inflate(R.layout.photo_picker_dialog_content, null);
        setView(content);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        if (hasFocus && !mInitialized) initializeContent();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setLayout(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    @Override
    public void dismiss() {
        super.dismiss();
        mCategoryView.endConnection();
    }

    @VisibleForTesting
    public PickerCategoryView getPickerCategoryViewForTesting() {
        return mCategoryView;
    }

    @VisibleForTesting
    public SelectionDelegate<PickerBitmap> getSelectionDelegateForTesting() {
        return mSelectionDelegate;
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        // This is a Chrome-specific function, not needed otherwise.
        return false;
    }

    /**
     * Initializes the PickerCategoryView (it is dynamically adjusted based on the size afforded to
     * it).
     */
    private void initializeContent() {
        LinearLayout parentLayout = (LinearLayout) findViewById(R.id.layout);

        mCategoryView = new PickerCategoryView(mContext);
        View view = getWindow().getDecorView();
        int width = view.getWidth() - view.getPaddingLeft() - view.getPaddingRight();
        mCategoryView.setStartingState(mSelectionDelegate, mListener, mMultiSelection, width);
        parentLayout.addView(mCategoryView);

        mInitialized = true;
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

        mListener.onPickerUserAction(OnPhotoPickerListener.Action.PHOTOS_SELECTED, photos);
    }

    /**
     * Initializes the view.
     * Note: This function is implemented differently in Chrome.
     * @param title The title view.
     */
    private void initialize(View title) {
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
