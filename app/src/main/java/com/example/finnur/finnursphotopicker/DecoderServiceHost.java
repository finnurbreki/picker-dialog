// Copyright 2016 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.example.finnur.finnursphotopicker;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.util.LinkedHashMap;

/**
 * A class to communicate with the decoder service.
 */
public class DecoderServiceHost {
    /**
     * Interface for notifying clients of the service being ready.
     */
    public interface ServiceReadyCallback { void serviceReady(); }

    /**
     * An interface notifying clients when an image has finished decoding.
     */
    public interface ImageDecodedCallback {
        void imageDecodedCallback(String filePath, Bitmap bitmap);
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

            mCallback.serviceReady();
        }

        public void onServiceDisconnected(ComponentName className) {
            mBound = false;
        }
    }

    /**
     * Class for keeping track of the data involved with each request.
     */
    private class DecoderServiceParams {
        public String mFilePath;
        public int mWidth;
        ImageDecodedCallback mCallback;
        public long mStartTime;

        public DecoderServiceParams(
                String filePath, int width, ImageDecodedCallback callback, long startTime) {
            mFilePath = filePath;
            mWidth = width;
            mCallback = callback;
            mStartTime = startTime;
        }
    }

    // Map of file paths to decoder parameters in order of request.
    private LinkedHashMap<String, DecoderServiceParams> mRequests = new LinkedHashMap<>();
    LinkedHashMap<String, DecoderServiceParams> getRequests() {
        return mRequests;
    }

    // The callback the client wants us to use to report back when the service is ready.
    private ServiceReadyCallback mCallback;

    public DecoderServiceHost(ServiceReadyCallback callback) {
        mCallback = callback;
    }

    // Messenger for communicating with the remote service.
    Messenger mService = null;

    // Our service connection to the decoder service.
    private DecoderServiceConnection mConnection;

    // Flag indicating whether we have called bind on the service.
    boolean mBound;

    // The inbound messenger used by the remote service to communicate with us.
    final Messenger mMessenger = new Messenger(new IncomingHandler(this));

    private long mStartTime = System.nanoTime();

    public void bind(Context context) {
        mConnection = new DecoderServiceConnection(mCallback);
        Intent intent = new Intent(context, DecoderService.class);
        context.bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    public void unbind(Context context) {
        if (mBound) {
            context.unbindService(mConnection);
            mBound = false;
        }
    }

    public void decodeImage(
            String filePath, int width, ImageDecodedCallback callback, long startTime) {
        DecoderServiceParams params =
                new DecoderServiceParams(filePath, width, callback, startTime);
        mRequests.put(filePath, params);
        if (mRequests.size() == 1) {
            dispatchNextDecodeImageRequest();
        }
    }

    void dispatchNextDecodeImageRequest() {
        for (DecoderServiceParams params : mRequests.values()) {
            dispatchDecodeImageRequest(params.mFilePath, params.mWidth, params.mStartTime);
            break;
        }
    }

    private void dispatchDecodeImageRequest(String filePath, int width, long startTime) {
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
            pfd.close();
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void cancelDecodeImage(String filePath) {
        mRequests.remove(filePath);
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

            switch (msg.what) {
                case DecoderService.MSG_IMAGE_DECODED_REPLY:
                    Bundle payload = msg.getData();

                    // Read the reply back from the service.
                    long startTime = payload.getLong(DecoderService.KEY_START_TIME);
                    String filePath = payload.getString(DecoderService.KEY_FILE_PATH);
                    Boolean success = payload.getBoolean(DecoderService.KEY_SUCCESS);
                    Bitmap bitmap = payload.getParcelable(DecoderService.KEY_IMAGE_BITMAP);
                    int width = payload.getInt(DecoderService.KEY_WIDTH);
                    int height = width;

                    if (!success) {
                        closeRequest(host, filePath, createPlaceholderBitmap(width, height));
                        return;
                    }

                    // Direct passing of bitmaps via ashmem became available in Marshmallow. For
                    // older clients, we manage our own memory file.
                    if (bitmap == null) {
                        ParcelFileDescriptor pfd =
                                payload.getParcelable(DecoderService.KEY_IMAGE_DESCRIPTOR);
                        int byteCount = payload.getInt(DecoderService.KEY_IMAGE_BYTE_COUNT);

                        // Grab the decoded pixels from memory and construct a bitmap object.
                        FileInputStream inFile = new ParcelFileDescriptor.AutoCloseInputStream(pfd);
                        byte[] pixels = new byte[byteCount];

                        try {
                            try {
                                inFile.read(pixels, 0, byteCount);
                                ByteBuffer buffer = ByteBuffer.wrap(pixels);
                                bitmap =
                                        Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
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
                    closeRequest(host, filePath, bitmap);
                    break;
                default:
                    super.handleMessage(msg);
            }
        }

        private Bitmap createPlaceholderBitmap(int width, int height) {
            Bitmap placeholder = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(placeholder);
            Paint paint = new Paint();
            paint.setColor(Color.GRAY);
            canvas.drawRect(0, 0, (float) width, (float) height, paint);
            return placeholder;
        }

        private void closeRequest(DecoderServiceHost host, String filePath, Bitmap bitmap) {
            DecoderServiceParams params = host.getRequests().get(filePath);
            if (params != null) {
                params.mCallback.imageDecodedCallback(filePath, bitmap);
                host.getRequests().remove(filePath);
            }
            host.dispatchNextDecodeImageRequest();
        }
    }
}
