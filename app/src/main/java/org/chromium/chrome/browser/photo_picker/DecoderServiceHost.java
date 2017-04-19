// Copyright 2017 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.chrome.browser.photo_picker;

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
import android.os.StrictMode;

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
    public interface ServiceReadyCallback {
        /**
         * A function to define to receive a notification once the service is up and running.
         */
        void serviceReady();
    }

    /**
     * An interface notifying clients when an image has finished decoding.
     */
    public interface ImageDecodedCallback {
        /**
         * A function to define to receive a notification that an image has been decoded.
         * @param filePath The file path for the newly decoded image.
         * @param bitmap The results of the decoding (or placeholder image, if failed).
         */
        void imageDecodedCallback(String filePath, Bitmap bitmap);
    }

    /**
     * Class for interacting with the main interface of the service.
     */
    private class DecoderServiceConnection implements ServiceConnection {
        // The callback to use to notify the service being ready.
        private ServiceReadyCallback mCallback;

        public DecoderServiceConnection(ServiceReadyCallback callback) {
            mCallback = callback;
        }

        // Called when a connection to the service has been established.
        public void onServiceConnected(ComponentName name, IBinder service) {
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

        // Called when a connection to the Service has been lost.
        public void onServiceDisconnected(ComponentName name) {
            mBound = false;
        }
    }

    /**
     * Class for keeping track of the data involved with each request.
     */
    private class DecoderServiceParams {
        // The path to the file containing the bitmap to decode.
        public String mFilePath;

        // The requested width of the bitmap, once decoded.
        public int mWidth;

        // The callback to use to communicate the results of the decoding.
        ImageDecodedCallback mCallback;

        public DecoderServiceParams(String filePath, int width, ImageDecodedCallback callback) {
            mFilePath = filePath;
            mWidth = width;
            mCallback = callback;
        }
    }

    // Map of file paths to decoder parameters in order of request.
    private LinkedHashMap<String, DecoderServiceParams> mRequests = new LinkedHashMap<>();
    LinkedHashMap<String, DecoderServiceParams> getRequests() {
        return mRequests;
    }

    // The callback used to notify the client when the service is ready.
    private ServiceReadyCallback mCallback;

    // Messenger for communicating with the remote service.
    Messenger mService = null;

    // Our service connection to the decoder service.
    private DecoderServiceConnection mConnection;

    // Flag indicating whether we are bound to the service.
    boolean mBound;

    // The inbound messenger used by the remote service to communicate with us.
    final Messenger mMessenger = new Messenger(new IncomingHandler(this));

    /**
     * The DecoderServiceHost constructor.
     * @param callback The callback to use when communicating back to the client.
     */
    public DecoderServiceHost(ServiceReadyCallback callback) {
        mCallback = callback;
    }

    /**
     * Initiate binding with the decoder service.
     * @param context The context to use.
     */
    public void bind(Context context) {
        mConnection = new DecoderServiceConnection(mCallback);
        Intent intent = new Intent(context, DecoderService.class);
        context.bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    /**
     * Unbind from the decoder service.
     * @param context The context to use.
     */
    public void unbind(Context context) {
        if (mBound) {
            context.unbindService(mConnection);
            mBound = false;
        }
    }

    /**
     * Accepts a request to decode a single image. Queues up the request and reports back
     * asynchronously on |callback|.
     * @param filePath The path to the file to decode.
     * @param width The requested width of the resulting bitmap.
     * @param callback The callback to use to communicate the decoding results.
     */
    public void decodeImage(String filePath, int width, ImageDecodedCallback callback) {
        DecoderServiceParams params = new DecoderServiceParams(filePath, width, callback);
        mRequests.put(filePath, params);
        if (mRequests.size() == 1) {
            dispatchNextDecodeImageRequest();
        }
    }

    /**
     * Dispatches the next image for decoding (from the queue).
     */
    private void dispatchNextDecodeImageRequest() {
        for (DecoderServiceParams params : mRequests.values()) {
            dispatchDecodeImageRequest(params.mFilePath, params.mWidth);
            break;
        }
    }

    /**
     * Communicates with the server to decode a single bitmap.
     * @param filePath The path to the image on disk.
     * @param size The requested width and height of the resulting bitmap.
     */
    private void dispatchDecodeImageRequest(String filePath, int size) {
        // Obtain a file descriptor to send over to the sandboxed process.
        File file = new File(filePath);
        FileInputStream inputFile = null;
        ParcelFileDescriptor pfd = null;
        Bundle bundle = new Bundle();

        // The restricted utility process can't open the file to read the
        // contents, so we need to obtain a file descriptor to pass over.
        StrictMode.ThreadPolicy oldPolicy = StrictMode.allowThreadDiskReads();
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
            StrictMode.setThreadPolicy(oldPolicy);
        }

        if (pfd == null) {
            return;
        }

        // Prepare and send the data over.
        Message payload = Message.obtain(null, DecoderService.MSG_DECODE_IMAGE);
        bundle.putString(DecoderService.KEY_FILE_PATH, filePath);
        bundle.putInt(DecoderService.KEY_SIZE, size);
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

    /**
     * Cancels a request to decode an image (if it hasn't already been dispatched).
     * @param filePath The path to the image to cancel decoding.
     */
    public void cancelDecodeImage(String filePath) {
        mRequests.remove(filePath);
    }

    /**
     * A class for handling communications from the server to us.
     */
    static class IncomingHandler extends Handler {
        // The DecoderServiceHost object to communicate with.
        private final WeakReference<DecoderServiceHost> mHost;

        /**
         * Constructor for IncomingHandler.
         * @param host The DecoderServiceHost object to communicate with.
         */
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
                    String filePath = payload.getString(DecoderService.KEY_FILE_PATH);
                    Boolean success = payload.getBoolean(DecoderService.KEY_SUCCESS);
                    Bitmap bitmap = payload.getParcelable(DecoderService.KEY_IMAGE_BITMAP);
                    int size = payload.getInt(DecoderService.KEY_SIZE);

                    if (!success) {
                        closeRequest(host, filePath, createPlaceholderBitmap(size, size));
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
                                bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
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

        /**
         * Creates a placeholder bitmap, used when the server failed to decode the image.
         * @param width The requested width of the resulting bitmap.
         * @param height The requested height of the resulting bitmap.
         * @return Placeholder bitmap.
         */
        private Bitmap createPlaceholderBitmap(int width, int height) {
            Bitmap placeholder = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(placeholder);
            Paint paint = new Paint();
            paint.setColor(Color.GRAY);
            canvas.drawRect(0, 0, (float) width, (float) height, paint);
            return placeholder;
        }

        /**
         * Ties up all the lose ends from the decoding request (communicates the results of the
         * decoding process back to the client, and takes care of house-keeping chores regarding
         * the request queue).
         * @param host The DecoderServiceHost object to communicate with.
         * @param filePath The path to the image that was just decoded.
         * @param bitmap The resulting decoded bitmap.
         */
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
