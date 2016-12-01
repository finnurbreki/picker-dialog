// Copyright 2016 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

// FLIP
//package org.chromium.chrome.browser;
package com.example.finnur.finnursphotopicker;

import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.MemoryFile;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.ByteBuffer;

public class DecoderService extends Service {

    static final int MSG_REGISTER_CLIENT = 1;
    static final int MSG_UNREGISTER_CLIENT = 2;
    static final int MSG_DECODE_IMAGE = 3;
    static final int MSG_IMAGE_REPLY = 4;

    static final String KEY_FILE = "file_contents";
    static final String KEY_FILE_PATH = "file_path";
    static final String KEY_IMAGE = "image";
    static final String KEY_WIDTH = "width";
    static final String KEY_START_TIME = "start_time";

    private Messenger mClient;

    /**
     * Handler of incoming messages from clients.
     */
    class IncomingHandler extends Handler {

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
                    byte[] fileContents = payload.getByteArray(KEY_IMAGE);
                    String filePath = payload.getString(KEY_FILE_PATH);
                    int width = payload.getInt(KEY_WIDTH);
                    int height = width;
                    Log.e("chromium", "GOT MSG_SAY_HELLO " + fileContents.length);

                    Bitmap bitmap = BitmapUtils.decodeBitmapFromMemory(fileContents, fileContents.length, width);
                    Log.e("chromium", "Config: " + bitmap.getConfig());
                    int byteCount = bitmap.getByteCount();

                    /*
                    MemoryFile memFile;
                    try {
                        memFile = new MemoryFile("MemoryFileTest", byteCount);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    memFile.writeBytes(bitmap.byt);

                    OutputStream out = memFile.getOutputStream();
                    out.writeBytes(bytes);
                    out.close();

                    InputStream in = memFile.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                    */


                    ByteBuffer buffer = ByteBuffer.allocate(byteCount);
                    bitmap.copyPixelsToBuffer(buffer);
                    buffer.rewind();
                    Log.e("chromium", "Bitmap decoded " + byteCount + " buffer: " + (buffer.array() != null));
                    Log.e("chromium", "Byte sequence: " + buffer.array()[0] + ":" + buffer.array()[1] + ":" + buffer.array()[2] + ":" + buffer.array()[3]);

                    Bitmap newImg = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                    newImg.copyPixelsFromBuffer(buffer);

                    try {
                        Bundle bundle = new Bundle();
                        bundle.putByteArray(KEY_IMAGE, buffer.array());
                        bundle.putString(KEY_FILE_PATH, filePath);
                        bundle.putInt(DecoderService.KEY_WIDTH, width);
                        Message reply = Message.obtain(null, MSG_IMAGE_REPLY);
                        reply.setData(bundle);
                        mClient.send(reply);
                    } catch (RemoteException e) {
                        Log.e("chromium", "DEAD CLIENT");
                        mClient = null;  // He's dead, Jim.
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