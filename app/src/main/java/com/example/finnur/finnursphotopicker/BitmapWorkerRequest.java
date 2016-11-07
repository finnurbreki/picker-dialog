package com.example.finnur.finnursphotopicker;

import android.content.Context;
import android.graphics.Bitmap;

public class BitmapWorkerRequest {
    public interface ImageDecodedCallback {
        void imageDecodedCallback(String filePath, Bitmap bitmap);
    }

    private Context mContext;
    private String mFilePath;
    private int mWidthPerColumn;
    private ImageDecodedCallback mCallback;

    public BitmapWorkerRequest(Context context, String filePath, int widthPerColumn, ImageDecodedCallback callback) {
        mContext = context;
        mFilePath = filePath;
        mWidthPerColumn = widthPerColumn;
        mCallback = callback;
    }

    public Context getContext() { return mContext; }
    public String getFilePath() { return mFilePath; }
    public int getWidthPerColumn() { return mWidthPerColumn; }
    public ImageDecodedCallback getCallback() { return mCallback; }
}
