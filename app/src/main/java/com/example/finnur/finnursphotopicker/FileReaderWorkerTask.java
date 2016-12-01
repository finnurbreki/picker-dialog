// Copyright 2016 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

// FLIP
//package org.chromium.chrome.browser;
package com.example.finnur.finnursphotopicker;

import android.os.AsyncTask;
import android.os.MemoryFile;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

class FileReaderWorkerTask extends AsyncTask<String, Void, byte[]> {
    public interface FileReadCallback {
        void fileReadCallback(String filePath, byte[] fileContents, int width, long requestStartTime);
    }

    private FileReaderWorkerRequest mRequest;
    private String mFilePath;
    private FileReadCallback mCallback;

    public FileReaderWorkerTask(FileReaderWorkerRequest request) {
        mRequest = request;
        mCallback = request.getCallback();
    }

    // Read the file in the background.
    @Override
    protected byte[] doInBackground(String... params) {
        if (isCancelled()) {
            return null;
        }

        mFilePath = params[0];

        long cp1 = System.nanoTime();

        File file = new File(mFilePath);
        int size = (int) file.length();
        byte[] bytes = new byte[size];
        try {
            BufferedInputStream buffer = new BufferedInputStream(new FileInputStream(file));
            buffer.read(bytes, 0, bytes.length);

            long cp2 = System.nanoTime();
            long durationInMs = TimeUnit.MILLISECONDS.convert(cp2 - cp1, TimeUnit.NANOSECONDS);
            Log.e("chromium", "Buffer read: " + durationInMs + " ms");

            buffer.close();
            return bytes;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    protected void onPostExecute(byte[] contents) {
        if (isCancelled()) {
            return;
        }

        mCallback.fileReadCallback(
                mFilePath, contents, mRequest.getImageSize(), mRequest.getRequestStartTime());
    }
}
