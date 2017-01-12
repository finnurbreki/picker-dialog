// Copyright 2016 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.example.finnur.finnursphotopicker;

import android.content.ClipData;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.util.Log;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private PhotoPickerDialog mDialog;

    // An ID for the system intent to show the gallery and have the user pick a photo.
    static final int PICK_IMAGE_REQUEST = 1;

    // Whether multi-select should be enabled.
    static final boolean mMultiSelect = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                OnPhotoPickerListener listener = new OnPhotoPickerListener() {
                    @Override
                    public void onPickerUserAction(
                            OnPhotoPickerListener.Action action, String[] photos) {
                        switch (action) {
                            case PHOTOS_SELECTED:
                                if (photos != null) {
                                    for (String path : photos) {
                                        Log.e("***** ", "**** Photo selected: " + path);
                                    }
                                }
                                break;
                            case LAUNCH_GALLERY:
                                Intent intent = new Intent();
                                intent.setType("image/*");
                                if (mMultiSelect) {
                                    intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                                }
                                intent.setAction(Intent.ACTION_GET_CONTENT);
                                startActivityForResult(
                                        Intent.createChooser(intent, "Select Picture"),
                                        PICK_IMAGE_REQUEST);

                                mDialog.dismiss();
                                break;
                        }
                    }
                };

                mDialog = new PhotoPickerDialog(getWindow().getContext(), listener, mMultiSelect);
                // This removes the padding around the dialog.
                mDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.WHITE));
                mDialog.show();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PICK_IMAGE_REQUEST) {
            if (resultCode == RESULT_OK) {
                ArrayList<Uri> images = new ArrayList<Uri>();
                ClipData clipData = data.getClipData();
                if (clipData == null) {
                    images.add(data.getData());
                } else {
                    int count = data.getClipData().getItemCount();
                    for (int i = 0; i < count; ++i) {
                        images.add(data.getClipData().getItemAt(i).getUri());
                    }
                }

                // |images| now holds the selected images.
                Log.e("***** ", "**** Photos selected: " + images.size());
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
