// Copyright 2016 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.example.finnur.finnursphotopicker;

import android.view.WindowManager;
import android.view.ViewGroup.LayoutParams;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import android.util.Log;

/**
 * UI for the photo chooser that shows on the Android platform as a result of
 * &lt;input type=file accept=image &gt; form element.
 */
public class PhotoPickerDialog extends AlertDialog {

    private Context mContext;

    // The listener for the photo changed event.
    private final OnPhotoChangedListener mListener;

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

        TextView titleText = (TextView) title.findViewById(R.id.title);
        titleText.setText(R.string.photo_picker_dialog_title);

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
        categoryCamera.setInitialState("Camera", "/DCIM/Camera");
        parentLayout.addView(categoryCamera);

        PickerCategoryView categoryScreenshots = new PickerCategoryView(context);
        categoryScreenshots.setInitialState("Screenshots", "/Pictures/Screenshots");
        parentLayout.addView(categoryScreenshots);

        PickerCategoryView categoryDownloads = new PickerCategoryView(context);
        categoryDownloads.setInitialState("Downloads", "/Download");
        parentLayout.addView(categoryDownloads);

        boolean hasItems = categoryCamera.getVisibility() == View.VISIBLE
                || categoryScreenshots.getVisibility() == View.VISIBLE
                || categoryDownloads.getVisibility() == View.VISIBLE;
        if (!hasItems) {
            Log.e("FOOO", "Show empty message");
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

    /**
     * Tries to notify any listeners that the color has been set.
     */
    private void tryNotifyPhotoSet(String[] photos) {
        if (mListener != null) mListener.onPhotoChanged(photos);
    }
}
