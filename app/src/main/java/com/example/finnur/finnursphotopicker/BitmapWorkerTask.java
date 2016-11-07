package com.example.finnur.finnursphotopicker;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;

class BitmapWorkerTask extends AsyncTask<String, Void, Bitmap> {
    private Context mContext;
    private String mFilePath;
    private int mWidth;
    private BitmapWorkerRequest.ImageDecodedCallback mCallback;

    public BitmapWorkerTask(BitmapWorkerRequest request) {
        mContext = request.getContext();
        mCallback = request.getCallback();
        mWidth = request.getWidthPerColumn();
    }

    // Decode image in background.
    @Override
    protected Bitmap doInBackground(String... params) {
        if (isCancelled()) {
            return null;
        }

        mFilePath = params[0];
        return BitmapUtils.retrieveBitmap(mContext, mFilePath, mWidth);
    }

    // Once complete, see if ImageView is still around and set bitmap.
    @Override
    protected void onPostExecute(Bitmap bitmap) {
        if (isCancelled()) {
            return;
        }

        mCallback.imageDecodedCallback(mFilePath, bitmap);
    }
}
