// Copyright 2016 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

// FLIP
//package org.chromium.chrome.browser;
package com.example.finnur.finnursphotopicker;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.util.Log;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

public class DecoderServiceHost {
    /** Messenger for communicating with the service. */
    Messenger mService = null;

    HashMap<String, BitmapWorkerTask.ImageDecodedCallback> mCallbacks =
            new HashMap<String, BitmapWorkerTask.ImageDecodedCallback>();

    /** Flag indicating whether we have called bind on the service. */
    boolean mBound;

    // TODOf doc
    final Messenger mMessenger = new Messenger(new IncomingHandler());

    private long mStartTime = System.nanoTime();

    public void DecoderServiceHost() {
    }

    public void onResume(Context context) {
        Log.e("chromium", "Binding to service " + DecoderService.class);
        Intent intent= new Intent(context, DecoderService.class);
        boolean success = context.bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        Log.e("chromium", "Service bound: " + success);
    }

    public void onStop(Context context) {
        // Unbind from the service
        if (mBound) {
            context.unbindService(mConnection);
            mBound = false;
        }
    }

    public void decodeImage(String filePath, int width, BitmapWorkerTask.ImageDecodedCallback callback, long startTime) {
        Message payload = Message.obtain(null, DecoderService.MSG_DECODE_IMAGE);
        Bundle bundle = new Bundle();

        // Obtain a file descriptor to send over to the sandboxed process.
        File file = new File(filePath);
        FileInputStream inputFile = null;
        ParcelFileDescriptor pfd = null;
        try {
            try {
                inputFile = new FileInputStream(file);
                FileDescriptor fd = inputFile.getFD();
                pfd = ParcelFileDescriptor.dup(fd);
                bundle.putParcelable(DecoderService.KEY_FILE_DESCRIPTOR, pfd);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } finally {
            try {
                inputFile.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (pfd == null)
            return;

        // Prepare and send the data over.
        bundle.putString(DecoderService.KEY_FILE_PATH, filePath);
        bundle.putInt(DecoderService.KEY_WIDTH, width);
        bundle.putLong(DecoderService.KEY_START_TIME, startTime);
        payload.setData(bundle);
        try {
            mService.send(payload);
            mCallbacks.put(filePath, callback);
            pfd.close();
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Class for interacting with the main interface of the service.
     */
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the object we can use to
            // interact with the service.  We are communicating with the
            // service using a Messenger, so here we get a client-side
            // representation of that from the raw IBinder object.
            mService = new Messenger(service);

            try {
                Message msg = Message.obtain(null, DecoderService.MSG_REGISTER_CLIENT);
                msg.replyTo = mMessenger;
                mService.send(msg);
            }
            catch (RemoteException e) {
                // In this case the service has crashed before we could even do anything with it
            }

            mBound = true;

            long endTime = System.nanoTime();
            long durationInMs = TimeUnit.MILLISECONDS.convert(endTime - mStartTime, TimeUnit.NANOSECONDS);
            Log.e("chromium", "Time from start of service to bound: " + durationInMs + " ms");
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            mService = null;
            mBound = false;
        }
    };

    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case DecoderService.MSG_IMAGE_DECODED_REPLY:
                    Bundle payload = msg.getData();

                    // Read the reply back from the service.
                    ParcelFileDescriptor pfd =
                            payload.getParcelable(DecoderService.KEY_IMAGE_DESCRIPTOR);
                    String filePath = payload.getString(DecoderService.KEY_FILE_PATH);
                    int width = payload.getInt(DecoderService.KEY_WIDTH);
                    int height = width;
                    long startTime = payload.getLong(DecoderService.KEY_START_TIME);
                    int byteCount = payload.getInt(DecoderService.KEY_IMAGE_BYTE_COUNT);

                    // Grab the decoded pixels from memory and construct a bitmap object.
                    FileInputStream inFile =
                            new ParcelFileDescriptor.AutoCloseInputStream(pfd);
                    byte[] pixels = new byte[byteCount];
                    Bitmap bitmap = null;
                    try {
                        try {
                            inFile.read(pixels, 0, byteCount);
                            ByteBuffer buffer = ByteBuffer.wrap(pixels);
                            bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                            bitmap.copyPixelsFromBuffer(buffer);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } finally {
                        try {
                            inFile.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    // Reply back to the original caller.
                    BitmapWorkerTask.ImageDecodedCallback callback = mCallbacks.get(filePath);
                    callback.imageDecodedCallback(filePath, bitmap, startTime);
                    mCallbacks.remove(filePath);
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }
}
