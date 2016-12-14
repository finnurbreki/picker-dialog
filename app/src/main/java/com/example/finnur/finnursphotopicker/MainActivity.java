// Copyright 2016 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.example.finnur.finnursphotopicker;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Debug;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.util.Log;

public class MainActivity extends AppCompatActivity {

    private PhotoPickerDialog mDialog;

    private DecoderServiceHost mDecoderServiceHost =
            PickerCategoryView.useDecoderService() ? new DecoderServiceHost() : null;

    public DecoderServiceHost getDecoderServiceHost() { return mDecoderServiceHost; }

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
                /*
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                        */
                OnPhotoChangedListener listener = new OnPhotoChangedListener() {
                    @Override
                    public void onPhotoChanged(String[] photos) {
                        mDialog.dismiss();
                        //Debug.stopMethodTracing();
                        if (photos != null) {
                            for (String path : photos) {
                                Log.e("***** ", "**** Photo selected: " + path);
                            }
                        }
                    }
                };

                //Debug.startMethodTracing("showDlg");
                mDialog = new PhotoPickerDialog(getWindow().getContext(), listener, false);

                // This removes the padding around the dialog.
                mDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.WHITE));
                mDialog.show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mDecoderServiceHost != null) mDecoderServiceHost.onResume(this);
    }

    /*
    @Override
    protected void onPause() {
        super.onPause();
        Log.e("chromium", "Bind paused");
        unbindService(mConnection);
    }
    */

    @Override
    protected void onStop() {
        super.onStop();
        if (mDecoderServiceHost != null) mDecoderServiceHost.onStop(this);
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
