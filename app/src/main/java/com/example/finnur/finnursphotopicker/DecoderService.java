// Copyright 2016 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

// FLIP
//package org.chromium.chrome.browser;
package com.example.finnur.finnursphotopicker;

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
import android.util.Log;

import java.io.FileDescriptor;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;

public class DecoderService extends Service {
    static final int MSG_REGISTER_CLIENT = 1;
    static final int MSG_UNREGISTER_CLIENT = 2;
    static final int MSG_DECODE_IMAGE = 3;
    static final int MSG_IMAGE_DECODED_REPLY = 4;

    static final String KEY_FILE_DESCRIPTOR = "file_descriptor";
    static final String KEY_FILE_PATH = "file_path";
    static final String KEY_IMAGE_BITMAP = "image_bitmap";
    static final String KEY_IMAGE_BYTE_COUNT = "image_byte_count";
    static final String KEY_IMAGE_DESCRIPTOR = "image_descriptor";
    static final String KEY_START_TIME = "start_time";
    static final String KEY_WIDTH = "width";

    private Messenger mClient;

    private static final Method sMethodGetFileDescriptor;
    static {
        sMethodGetFileDescriptor = getMethod("getFileDescriptor");
    }
    private static final Method sMethodCreateAshmemBitmap;
    static {
        sMethodCreateAshmemBitmap = getMethod("createAshmemBitmap");
    }

    private static Method getMethod(String name) {
        try {
            if (name.equals("getFileDescriptor"))
                return MemoryFile.class.getDeclaredMethod(name);
            if (name.equals("createAshmemBitmap"))
                return Bitmap.class.getDeclaredMethod(name);
            return null;
        } catch (NoSuchMethodException e) {
            if (name.equals("createAshmemBitmap"))
                return null;  // Expected error on pre-M devices.

            throw new RuntimeException(e);
        }
    }

    public static FileDescriptor getFileDescriptor(MemoryFile file) {
        try {
            return (FileDescriptor) sMethodGetFileDescriptor.invoke(file);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

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
     * Handler of incoming messages from clients.
     */
    static class IncomingHandler extends Handler {
        private Messenger mClient;

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_REGISTER_CLIENT:
                    Log.e("chromium", "GOT MSG_REGISTER_CLIENT");
                    mClient = msg.replyTo;
                    break;
                case MSG_UNREGISTER_CLIENT:
                    Log.e("chromium", "GOT MSG_UNREGISTER_CLIENT");
                    // TODOf implement?
                    mClient = null;
                    break;
                case MSG_DECODE_IMAGE:
                    Bundle payload = msg.getData();
                    String filePath = payload.getString(KEY_FILE_PATH);
                    Log.e("chromium", "GOT MSG_DECODE_IMAGE " + filePath);

                    ParcelFileDescriptor pfd = payload.getParcelable(KEY_FILE_DESCRIPTOR);
                    FileDescriptor fd = pfd.getFileDescriptor();
                    int width = payload.getInt(KEY_WIDTH);
                    long startTime = payload.getLong(KEY_START_TIME);

                    Bitmap bitmap = BitmapUtils.decodeBitmapFromFileDescriptor(fd, width);
                    try {
                        pfd.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    MemoryFile imageFile = null;
                    ParcelFileDescriptor imagePfd = null;

                    Bundle bundle = new Bundle();
                    // createAshmemBitmap was not available until Marshmallow.
                    if (sMethodCreateAshmemBitmap != null) {
                        Bitmap ashmemBitmap = createAshmemBitmap(bitmap);
                        bitmap.recycle();
                        bundle.putParcelable(KEY_IMAGE_BITMAP, ashmemBitmap);
                    } else {
                        int byteCount = bitmap.getByteCount();

                        // TODO(finnur): Copy pixels by hand using a smaller buffer to conserve memory?
                        ByteBuffer buffer = ByteBuffer.allocate(byteCount);
                        bitmap.copyPixelsToBuffer(buffer);
                        bitmap.recycle();
                        buffer.rewind();

                        try {
                            imageFile = new MemoryFile(filePath, byteCount);
                            imageFile.writeBytes(buffer.array(), 0, 0, byteCount);

                            fd = getFileDescriptor(imageFile);
                            imagePfd = ParcelFileDescriptor.dup(fd);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        bundle.putInt(KEY_WIDTH, width);
                        bundle.putParcelable(KEY_IMAGE_DESCRIPTOR, imagePfd);
                        bundle.putInt(KEY_IMAGE_BYTE_COUNT, byteCount);
                    }

                    try {
                        bundle.putString(KEY_FILE_PATH, filePath);
                        bundle.putLong(KEY_START_TIME, startTime);

                        Message reply = Message.obtain(null, MSG_IMAGE_DECODED_REPLY);
                        reply.setData(bundle);
                        mClient.send(reply);
                    } catch (RemoteException e) {
                        Log.e("chromium", "DEAD CLIENT");
                        mClient = null;  // He's dead, Jim.
                    }

                    try {
                        if (imageFile != null)
                            imageFile.close();
                        if (imagePfd != null)
                            imagePfd.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    /**
     * Target we publish for clients to send messages to IncomingHandler.
     */
    final Messenger mMessenger = new Messenger(new IncomingHandler());

    /**
     * When binding to the service, we return an interface to our messenger
     * for sending messages to the service.
     */
    @Override
    public IBinder onBind(Intent intent) {
        Log.e("chromium", "ON BIND");
        return mMessenger.getBinder();
    }
}
