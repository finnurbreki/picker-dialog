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
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

public class DecoderServiceHost {
    /**
     * Interface for notifying clients of the service being ready.
     */
    public interface ServiceReadyCallback {
        void serviceReady();
    }

    /**
     * Class for interacting with the main interface of the service.
     */
    private class DecoderServiceConnection implements ServiceConnection {
        private ServiceReadyCallback mCallback;

        public DecoderServiceConnection(ServiceReadyCallback callback) {
            mCallback = callback;
        }

        public void onServiceConnected(ComponentName className, IBinder service) {
            mService = new Messenger(service);

            try {
                Message msg = Message.obtain(null, DecoderService.MSG_REGISTER_CLIENT);
                msg.replyTo = mMessenger;
                mService.send(msg);
            } catch (RemoteException e) {
                return;
            }

            mBound = true;

            long endTime = System.nanoTime();
            long durationInMs =
                    TimeUnit.MILLISECONDS.convert(endTime - mStartTime, TimeUnit.NANOSECONDS);
            Log.e("chromium", "Time from start of service to bound: " + durationInMs + " ms");

            mCallback.serviceReady();
        }

        public void onServiceDisconnected(ComponentName className) {
            mBound = false;
        }
    }

    // The callback the client wants us to use to report back when the service is ready.
    private ServiceReadyCallback mCallback;

    public DecoderServiceHost(ServiceReadyCallback callback) {
        mCallback = callback;
    }

    /** Messenger for communicating with the service. */
    Messenger mService = null;

    // Our service connection to the decoder service.
    private DecoderServiceConnection mConnection;

    HashMap<String, BitmapWorkerTask.ImageDecodedCallback> mCallbacks =
            new HashMap<String, BitmapWorkerTask.ImageDecodedCallback>();
    HashMap<String, BitmapWorkerTask.ImageDecodedCallback> getCallbacks() {
        return mCallbacks;
    }

    /** Flag indicating whether we have called bind on the service. */
    boolean mBound;

    // TODOf doc
    final Messenger mMessenger = new Messenger(new IncomingHandler(this));

    private long mStartTime = System.nanoTime();

    public void bind(Context context) {
        mConnection = new DecoderServiceConnection(mCallback);
        Intent intent = new Intent(context, DecoderService.class);
        boolean success = context.bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    public void unbind(Context context) {
        // Unbind from the service
        if (mBound) {
            context.unbindService(mConnection);
            mBound = false;
        }
    }

    public void decodeImage(String filePath, int width,
            BitmapWorkerTask.ImageDecodedCallback callback, long startTime) {
        // Obtain a file descriptor to send over to the sandboxed process.
        File file = new File(filePath);
        FileInputStream inputFile = null;
        ParcelFileDescriptor pfd = null;
        Bundle bundle = new Bundle();
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

        if (pfd == null) {
            return;
        }

        // Prepare and send the data over.
        Message payload = Message.obtain(null, DecoderService.MSG_DECODE_IMAGE);
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

    static class IncomingHandler extends Handler {
        private final WeakReference<DecoderServiceHost> mHost;

        IncomingHandler(DecoderServiceHost host) {
            mHost = new WeakReference<DecoderServiceHost>(host);
        }

        @Override
        public void handleMessage(Message msg) {
            DecoderServiceHost host = mHost.get();
            if (host == null) {
                super.handleMessage(msg);
                return;
            }
            HashMap<String, BitmapWorkerTask.ImageDecodedCallback> callbacks =
                    host.getCallbacks();

            switch (msg.what) {
                case DecoderService.MSG_IMAGE_DECODED_REPLY:
                    Bundle payload = msg.getData();

                    // Read the reply back from the service.
                    long startTime = payload.getLong(DecoderService.KEY_START_TIME);
                    String filePath = payload.getString(DecoderService.KEY_FILE_PATH);
                    Bitmap bitmap = payload.getParcelable(DecoderService.KEY_IMAGE_BITMAP);

                    // Direct passing of bitmaps via ashmem became available in Marshmallow. For
                    // older clients, we manage our own memory file.
                    if (bitmap == null) {
                        ParcelFileDescriptor pfd =
                                payload.getParcelable(DecoderService.KEY_IMAGE_DESCRIPTOR);
                        int width = payload.getInt(DecoderService.KEY_WIDTH);
                        int height = width;
                        int byteCount = payload.getInt(DecoderService.KEY_IMAGE_BYTE_COUNT);

                        // Grab the decoded pixels from memory and construct a bitmap object.
                        FileInputStream inFile =
                                new ParcelFileDescriptor.AutoCloseInputStream(pfd);
                        byte[] pixels = new byte[byteCount];

                        try {
                            try {
                                inFile.read(pixels, 0, byteCount);
                                ByteBuffer buffer = ByteBuffer.wrap(pixels);
                                bitmap = Bitmap.createBitmap(
                                        width, height, Bitmap.Config.ARGB_8888);
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
                    }

                    // Reply back to the original caller.
                    BitmapWorkerTask.ImageDecodedCallback callback = callbacks.get(filePath);
                    callback.imageDecodedCallback(filePath, bitmap, startTime);
                    callbacks.remove(filePath);
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }
}
