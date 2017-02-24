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
import android.os.Environment;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private PhotoPickerDialog mDialog;

    // An ID for the system intent to show the gallery and have the user pick a photo.
    static final int PICK_IMAGE_REQUEST = 1;

    // An ID for the system intent to show the camera intent.
    static final int TAKE_PHOTO_REQUEST = 2;

    // Whether multi-select should be enabled.
    static final boolean mMultiSelect = true;

    // The path to the photo captured from the camera.
    private String mCurrentPhotoPath;

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
                            case LAUNCH_CAMERA:
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

                                    // Continue only if the File was successfully created
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
                        }
                    }

                    @Override
                    public Map<String, Long> getFilesForTesting() {
                        return null;
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
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK) {
            processGalleryIntentResults(data);
        } else if (requestCode == TAKE_PHOTO_REQUEST && resultCode == RESULT_OK) {
            processCameraIntentResults(data);
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
}
