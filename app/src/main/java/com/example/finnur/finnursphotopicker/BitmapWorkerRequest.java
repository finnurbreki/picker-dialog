package com.example.finnur.finnursphotopicker;

import android.content.Context;
import android.widget.ImageView;

public class BitmapWorkerRequest {
    private Context mContext;
    private String mFilePath;
    private ImageView mImageView;
    private int mWidthPerColumn;

    public BitmapWorkerRequest(Context context, String filePath, ImageView imageView, int widthPerColumn) {
        mContext = context;
        mFilePath = filePath;
        mImageView = imageView;
        mWidthPerColumn = widthPerColumn;
    }

    public Context getContext() { return mContext; }
    public String getFilePath() { return mFilePath; }
    public ImageView getImageView() { return mImageView; }
    public int getWidthPerColumn() { return mWidthPerColumn; }

}
