package com.example.finnur.finnursphotopicker;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ImageView;

import java.lang.ref.WeakReference;
import java.util.concurrent.TimeUnit;

class BitmapWorkerTask extends AsyncTask<String, Void, Bitmap> {
    private final WeakReference<ImageView> imageViewReference;
    private Context mContext;

    public BitmapWorkerTask(BitmapWorkerRequest request) {
        // Use a WeakReference to ensure the ImageView can be garbage collected
        imageViewReference = new WeakReference<ImageView>(request.getImageView());
        mContext = request.getContext();
    }

    // Decode image in background.
    @Override
    protected Bitmap doInBackground(String... params) {
        String currentFilePath = params[0];
        return BitmapUtils.retrieveBitmap(mContext, currentFilePath);
    }

    // Once complete, see if ImageView is still around and set bitmap.
    @Override
    protected void onPostExecute(Bitmap bitmap) {
        if (imageViewReference != null && bitmap != null) {
            final ImageView imageView = imageViewReference.get();
            if (imageView != null) {
                imageView.setImageBitmap(bitmap);
            } else {
                Log.e("chromium", "dead reference");
            }
        } else {
            Log.e("chromium", "dead reference or no bitmap");
        }
    }
}
