// Copyright 2017 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.chrome.browser.photo_picker;

import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.MemoryFile;
import android.os.Message;
import android.os.Messenger;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;

import java.io.FileDescriptor;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;

/**
 * A service to accept requests to take image file contents and decode them.
 */
public class DecoderService extends Service {
    // Message ids for communicating with the client.

    // A message sent by the client to register as a consumer of this service.
    static final int MSG_REGISTER_CLIENT = 1;
    // A message sent by the client to decode an image.
    static final int MSG_DECODE_IMAGE = 2;
    // A message sent by the server to notify the client of the results of the decoding.
    static final int MSG_IMAGE_DECODED_REPLY = 3;

    // The keys for the bundle when passing data to and from this service.
    static final String KEY_FILE_DESCRIPTOR = "file_descriptor";
    static final String KEY_FILE_PATH = "file_path";
    static final String KEY_IMAGE_BITMAP = "image_bitmap";
    static final String KEY_IMAGE_BYTE_COUNT = "image_byte_count";
    static final String KEY_IMAGE_DESCRIPTOR = "image_descriptor";
    static final String KEY_SIZE = "size";
    static final String KEY_SUCCESS = "success";

    // A method for getFileDescriptor, obtained via Reflection. Can be null if not supported by
    // the Android OS.
    private static final Method sMethodGetFileDescriptor;
    static {
        sMethodGetFileDescriptor = getMethod("getFileDescriptor");
    }

    // A method for createAshmemBitmap, obtained via Reflection. Can be null if not supported by
    // the Android OS.
    private static final Method sMethodCreateAshmemBitmap;
    static {
        sMethodCreateAshmemBitmap = getMethod("createAshmemBitmap");
    }

    /**
     * A helper function to look up methods by name.
     * @param name The name of the method to look up.
     * @return The method, if found. Otherwise null.
     */
    private static Method getMethod(String name) {
        try {
            if (name.equals("getFileDescriptor")) {
                return MemoryFile.class.getDeclaredMethod(name);
            }
            if (name.equals("createAshmemBitmap")) {
                return Bitmap.class.getDeclaredMethod(name);
            }
            return null;
        } catch (NoSuchMethodException e) {
            if (name.equals("createAshmemBitmap")) {
                return null; // Expected error on pre-M devices.
            }

            throw new RuntimeException(e);
        }
    }

    /**
     * A helper function to obtain a FileDescriptor from a MemoryFile.
     * @param file The MemoryFile to get a FileDescriptor for.
     * @return The resulting FileDescriptor.
     */
    public static FileDescriptor getFileDescriptor(MemoryFile file) {
        try {
            return (FileDescriptor) sMethodGetFileDescriptor.invoke(file);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * A helper function to obtain an ashmemBitmap version of a regular |bitmap|.
     * @param bitmap The bitmap to use.
     * @return an ashmemBitmap.
     */
    public static Bitmap createAshmemBitmap(Bitmap bitmap) {
        try {
            return (Bitmap) sMethodCreateAshmemBitmap.invoke(bitmap);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Handler for incoming messages from clients.
     */
    static class IncomingHandler extends Handler {
        // The client we are communicating with.
        private Messenger mClient;

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_REGISTER_CLIENT:
                    mClient = msg.replyTo;
                    break;
                case MSG_DECODE_IMAGE:
                    Bundle bundle = new Bundle();
                    try {
                        Bundle payload = msg.getData();

                        String filePath = payload.getString(KEY_FILE_PATH);
                        ParcelFileDescriptor pfd = payload.getParcelable(KEY_FILE_DESCRIPTOR);
                        int size = payload.getInt(KEY_SIZE);

                        // Setup a minimum viable response to parent process. Will be fleshed out
                        // further below.
                        bundle.putString(KEY_FILE_PATH, filePath);
                        bundle.putInt(KEY_SIZE, size);
                        bundle.putBoolean(KEY_SUCCESS, false);

                        FileDescriptor fd = pfd.getFileDescriptor();
                        Bitmap bitmap = BitmapUtils.decodeBitmapFromFileDescriptor(fd, size);
                        try {
                            pfd.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        MemoryFile imageFile = null;
                        ParcelFileDescriptor imagePfd = null;

                        // createAshmemBitmap was not available until Marshmallow.
                        if (sMethodCreateAshmemBitmap != null) {
                            Bitmap ashmemBitmap = createAshmemBitmap(bitmap);
                            bitmap.recycle();
                            bundle.putParcelable(KEY_IMAGE_BITMAP, ashmemBitmap);
                            bundle.putBoolean(KEY_SUCCESS, true);
                        } else {
                            int byteCount = bitmap.getByteCount();

                            ByteBuffer buffer = ByteBuffer.allocate(byteCount);
                            bitmap.copyPixelsToBuffer(buffer);
                            bitmap.recycle();
                            buffer.rewind();

                            try {
                                imageFile = new MemoryFile(filePath, byteCount);
                                imageFile.writeBytes(buffer.array(), 0, 0, byteCount);

                                fd = getFileDescriptor(imageFile);
                                imagePfd = ParcelFileDescriptor.dup(fd);

                                bundle.putParcelable(KEY_IMAGE_DESCRIPTOR, imagePfd);
                                bundle.putInt(KEY_IMAGE_BYTE_COUNT, byteCount);
                                bundle.putBoolean(KEY_SUCCESS, true);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }

                        try {
                            Message reply = Message.obtain(null, MSG_IMAGE_DECODED_REPLY);
                            reply.setData(bundle);
                            mClient.send(reply);
                            bundle = null;
                        } catch (RemoteException e) {
                            mClient = null; // He's dead, Jim.
                        }

                        if (imageFile != null) {
                            imageFile.close();
                        }
                        try {
                            if (imagePfd != null) {
                                imagePfd.close();
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } catch (Exception e) {
                        // This service has no UI and maintains no state so if it crashes on
                        // decoding a photo, it is better UX to eat the exception instead of showing
                        // a crash dialog and discarding other requests that have already been sent.
                        e.printStackTrace();

                        if (bundle != null && mClient != null) {
                            Message reply = Message.obtain(null, MSG_IMAGE_DECODED_REPLY);
                            reply.setData(bundle);
                            try {
                                mClient.send(reply);
                            } catch (RemoteException remoteException) {
                                mClient = null; // He's dead, Jim.
                            }
                        }
                    }
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    /**
     * The target we publish for clients to send messages to IncomingHandler.
     */
    final Messenger mMessenger = new Messenger(new IncomingHandler());

    /**
     * When binding to the service, we return an interface to our messenger
     * for sending messages to the service.
     */
    @Override
    public IBinder onBind(Intent intent) {
        return mMessenger.getBinder();
    }
}
