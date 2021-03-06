// Copyright 2016 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.example.finnur.finnursphotopicker;

import android.Manifest;
import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.MediaStore;
//import android.support.design.widget.FloatingActionButton;
//import android.support.v4.content.FileProvider;
//import android.support.v7.app.AppCompatActivity;
//import android.support.v7.app.AppCompatDelegate;
//import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.util.Log;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.chromium.base.ContextUtils;
import org.chromium.ui.base.PhotoPickerListener;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private PhotoPickerDialog mDialog;

    // An ID for the system intent to show the gallery and have the user pick a photo.
    static final int PICK_IMAGE_REQUEST = 1;

    // An ID for the system intent to show the camera intent.
    static final int TAKE_PHOTO_REQUEST = 2;

    // The id of the permission request for permission Storage.
    static final int PERMISSIONS_REQUEST_STORAGE = 1;

    // Whether multi-select should be enabled.
    static final boolean mMultiSelect = true;

    // The path to the photo captured from the camera.
    private String mCurrentPhotoPath;

    static public int mStartingFrame = -1;
    static public int mInterval = -1;
    static public int mTotalFrames = -1;
    static public boolean mAlignOnKeyFrames = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        /* Enable for tracking down StrictMode violations.
        StrictMode.setThreadPolicy(
                new StrictMode.ThreadPolicy.Builder().detectAll().penaltyDeath().build());
        StrictMode.setVmPolicy(
                new StrictMode.VmPolicy.Builder().detectAll().build());
        */
        super.onCreate(savedInstanceState);

        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);

        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DecoderServiceHost.setIntentSupplier(() -> {
            return new Intent(getApplicationContext(), DecoderService.class);
        });

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        final Activity self = this;
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ContextCompat.checkSelfPermission(self, Manifest.permission.READ_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(self,
                            new String[]{ Manifest.permission.READ_EXTERNAL_STORAGE },
                            PERMISSIONS_REQUEST_STORAGE);
                    return;
                }
                showDialog();

                EditText startingFrame = findViewById(R.id.starting_frame);
                EditText interval = findViewById(R.id.interval);
                EditText numFrames = findViewById(R.id.total_frames);
                CheckBox alignOnKeyFrames = findViewById(R.id.align);
                if (startingFrame.getText().length() > 0) {
                    mStartingFrame = Integer.parseInt(startingFrame.getText().toString());
                }
                if (interval.getText().length() > 0) {
                    mInterval = Integer.parseInt(interval.getText().toString());
                }
                if (numFrames.getText().length() > 0) {
                    mTotalFrames = Integer.parseInt(numFrames.getText().toString());
                }
                mAlignOnKeyFrames = alignOnKeyFrames.isChecked();
            }
        });

        // fab.callOnClick();
    }

    private void showDialog() {
        PhotoPickerListener listener = new PhotoPickerListener() {
            @Override
            public void onPhotoPickerUserAction(
                    @PhotoPickerAction int action, Uri[] photos) {
                switch (action) {
                    case PhotoPickerAction.PHOTOS_SELECTED:
                        if (photos != null) {
                            for (Uri mediaUri : photos) {
                                Log.e("***** ", "**** Media selected: " + mediaUri.getPath());
                            }
                        }
                        break;
                    case PhotoPickerAction.LAUNCH_GALLERY:
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
                    case PhotoPickerAction.LAUNCH_CAMERA:
                        Intent takePictureIntent = new Intent(
                                MediaStore.ACTION_IMAGE_CAPTURE);
                        if (takePictureIntent.resolveActivity(
                                getPackageManager()) != null) {
                            // Create the File where the photo should go
                            File photoFile = null;
                            try {
                                photoFile = createImageFile();
                                mCurrentPhotoPath = photoFile.getAbsolutePath();
                            } catch (IOException ex) {
                                // Error occurred while creating the File.
                                Toast.makeText(getWindow().getContext(),
                                        R.string.failed_to_launch_photo_activity,
                                        Toast.LENGTH_SHORT).show();
                            }

                            if (photoFile != null) {
                                Uri photoURI = FileProvider.getUriForFile(
                                        getWindow().getContext(),
                                        "com.example.finnur.finnursphotopicker",
                                        photoFile);
                                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                                startActivityForResult(
                                        takePictureIntent, TAKE_PHOTO_REQUEST);
                            }
                        }
                        mDialog.dismiss();
                        break;
                    case PhotoPickerAction.CANCEL:
                        Log.e("***** ", "**** Cancelled");
                        break;
                }
            }

            @Override
            public void onPhotoPickerDismissed() {
                // TODOf figure out if needed in Android Studio.
            }
        };

        List<String> mimeTypes = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            mimeTypes = Arrays.asList("video/*", "image/*");
        } else {
            mimeTypes = Arrays.asList("image/*");
        }
        Context context = getWindow().getContext();
        mDialog = new PhotoPickerDialog(context, context.getContentResolver(), listener, mMultiSelect, mimeTypes);
        mDialog.getWindow().getAttributes().windowAnimations = R.style.PhotoPickerDialogAnimation;
        // This removes the padding around the dialog.
        mDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.WHITE));
        mDialog.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK) {
            processGalleryIntentResults(data);
        } else if (requestCode == TAKE_PHOTO_REQUEST && resultCode == RESULT_OK) {
            processCameraIntentResults(data);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_STORAGE: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    showDialog();
                }
            }
        }
    }

    private void processGalleryIntentResults(Intent data) {
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

    private void processCameraIntentResults(Intent data) {
        Log.e("***** ", "**** Photos captured: " + mCurrentPhotoPath);
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
        image.deleteOnExit();
        return image;
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

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }
}
