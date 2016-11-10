package com.example.finnur.finnursphotopicker;

import android.content.Context;
import android.graphics.Bitmap;

public class BitmapWorkerRequest {
    public interface ImageDecodedCallback {
        void imageDecodedCallback(String filePath, Bitmap bitmap);
    }

    private Context mContext;
    private int mImageSize;
    private ImageDecodedCallback mCallback;

    public BitmapWorkerRequest(Context context, int imageSize, ImageDecodedCallback callback) {
        mContext = context;
        mImageSize = imageSize;
        mCallback = callback;
    }

    public Context getContext() { return mContext; }
    public int getImageSize() { return mImageSize; }
    public ImageDecodedCallback getCallback() { return mCallback; }
}
