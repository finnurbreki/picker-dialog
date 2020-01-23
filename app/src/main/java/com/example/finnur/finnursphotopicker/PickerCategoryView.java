// Copyright 2017 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.example.finnur.finnursphotopicker;

import android.animation.Animator;
import android.app.Activity;  // Android Studio only.
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.SystemClock;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.transition.ChangeBounds;
import android.transition.Transition;
import android.transition.TransitionManager;
import android.util.DisplayMetrics;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.VideoView;

import androidx.annotation.VisibleForTesting;

//import org.chromium.base.DiscardableReferencePool.DiscardableReference;
import org.chromium.base.ThreadUtils;  // Android Studio Project only.
import org.chromium.base.metrics.RecordHistogram;
import org.chromium.base.task.AsyncTask;
import org.chromium.base.task.PostTask;
//import org.chromium.chrome.R;
//import org.chromium.chrome.browser.ChromeActivity;
//import org.chromium.chrome.browser.flags.ChromeFeatureList;
import org.chromium.chrome.browser.util.ConversionUtils;
import org.chromium.chrome.browser.widget.selection.SelectableListLayout;
import org.chromium.chrome.browser.widget.selection.SelectionDelegate;
import org.chromium.content_public.browser.UiThreadTaskTraits;
import org.chromium.net.MimeTypeFilter;
import org.chromium.ui.PhotoPickerListener;
import org.chromium.ui.UiUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Timer;  // Android Studio Project only.
import java.util.TimerTask;  // Android Studio Project only.

/**
 * A class for keeping track of common data associated with showing photos in
 * the photo picker, for example the RecyclerView and the bitmap caches.
 */
public class PickerCategoryView extends RelativeLayout
        implements FileEnumWorkerTask.FilesEnumeratedCallback, RecyclerView.RecyclerListener,
                   DecoderServiceHost.ServiceReadyCallback, View.OnClickListener,
                   SeekBar.OnSeekBarChangeListener,
                   SelectionDelegate.SelectionObserver<PickerBitmap> {
    // These values are written to logs.  New enum values can be added, but existing
    // enums must never be renumbered or deleted and reused.
    private static final int ACTION_CANCEL = 0;
    private static final int ACTION_PHOTO_PICKED = 1;
    private static final int ACTION_NEW_PHOTO = 2;
    private static final int ACTION_BROWSE = 3;
    private static final int ACTION_BOUNDARY = 4;

    /**
     * A container class for keeping track of the data we need to show a photo/video tile in the
     * photo picker (the data we store in the cache).
     */
    public static class Thumbnail {
        public List<Bitmap> bitmaps;
        public Boolean fullWidth;
        public String videoDuration;

        // The calculated ratio of the originals for the bitmaps above, were they to be shown
        // un-cropped. NOTE: The |bitmaps| above may already have been cropped and as such might
        // have a different ratio.
        public float ratioOriginal;

        Thumbnail(List<Bitmap> bitmaps, String videoDuration, Boolean fullWidth, float ratio) {
            this.bitmaps = bitmaps;
            this.videoDuration = videoDuration;
            this.fullWidth = fullWidth;
            this.ratioOriginal = ratio;
        }
    }

    /**
     * A callback interface for notifying about video playback status.
     */
    public interface VideoPlaybackStatusCallback {
        // Called when the video starts playing.
        void onVideoPlaying();

        // Called when the video stops playing.
        void onVideoEnded();
    }

    // The dialog that owns us.
    private PhotoPickerDialog mDialog;

    // The view containing the RecyclerView and the toolbar, etc.
    private SelectableListLayout<PickerBitmap> mSelectableListLayout;

    // Our activity.
    private /*ChromeActivity*/ Activity mActivity;

    // The list of images on disk, sorted by last-modified first.
    private List<PickerBitmap> mPickerBitmaps;

    // True if multi-selection is allowed in the picker.
    private boolean mMultiSelectionAllowed;

    // The callback to notify the listener of decisions reached in the picker.
    private PhotoPickerListener mListener;

    // The host class for the decoding service.
    private DecoderServiceHost mDecoderServiceHost;

    // The RecyclerView showing the images.
    private RecyclerView mRecyclerView;

    // The {@link PickerAdapter} for the RecyclerView.
    private PickerAdapter mPickerAdapter;

    // The layout manager for the RecyclerView.
    private GridLayoutManager mLayoutManager;

    // The decoration to use for the RecyclerView.
    private GridSpacingItemDecoration mSpacingDecoration;

    // The {@link SelectionDelegate} keeping track of which images are selected.
    private SelectionDelegate<PickerBitmap> mSelectionDelegate;

    // A low-resolution cache for thumbnails, lazily created. Helpful for cache misses from the
    // high-resolution cache to avoid showing gray squares (we show pixelated versions instead until
    // image can be loaded off disk, which is much less jarring).
    private /*DiscardableReference<*/ LruCache<String, Thumbnail> /*>*/ mLowResThumbnails;

    // A high-resolution cache for thumbnails, lazily created.
    private /*DiscardableReference<*/ LruCache<String, Thumbnail> /*>*/ mHighResThumbnails;

    // A cache for full-screen versions of images, lazily created.
    private /*DiscardableReference<*/ LruCache<String, Thumbnail> /*>*/ mFullScreenBitmaps;

    // The size of the low-res cache.
    private int mCacheSizeLarge;

    // The size of the high-res cache.
    private int mCacheSizeSmall;

    // The size of the full-screen cache.
    private int mCacheSizeFullScreen;

    // Whether we are in magnifying mode (one image per column).
    private boolean mMagnifyingMode;

    // Whether we are in the middle of animating between magnifying modes.
    private boolean mZoomSwitchingInEffect;

    /**
     * The number of columns to show. Note: mColumns and mPadding (see below) should both be even
     * numbers or both odd, not a mix (the column padding will not be of uniform thickness if they
     * are a mix).
     */
    private int mColumns;

    // The padding between columns. See also comment for mColumns.
    private int mPadding;

    // The width of the bitmaps.
    private int mImageWidth;

    // The height of the bitmaps.
    private int mImageHeight;

    // The height of the special tiles.
    private int mSpecialTileHeight;

    // A worker task for asynchronously enumerating files off the main thread.
    private FileEnumWorkerTask mWorkerTask;

    // The timestamp for the start of the enumeration of files on disk.
    private long mEnumStartTime;

    // Whether the connection to the service has been established.
    private boolean mServiceReady;

    // The MIME types requested.
    private List<String> mMimeTypes;

    // A list of files to use for testing (instead of reading files on disk).
    private static List<PickerBitmap> sTestFiles;

    // The callback to use for reporting playback progress in tests.
    private static VideoPlaybackStatusCallback sProgressCallback;

    // The video preview view.
    private final VideoView mVideoView;

    // The MediaPlayer object used to control the VideoView.
    private MediaPlayer mMediaPlayer;

    // The container view for all the UI elements overlaid on top of the video.
    private final View mVideoOverlayContainer;

    // The container view for the UI video controls within the overlaid window.
    private final View mVideoControls;

    // The large Play button overlaid on top of the video.
    private ImageView mLargePlayButton;

    // The Mute button for the video.
    private ImageView mMuteButton;

    // Keeps track of whether audio track is enabled or not.
    private boolean mAudioOn = true;

    // The Fullscreen button.
    private ImageView mFullscreenButton;

    // Keeps track of whether full screen is enabled or not.
    private boolean mFullScreen;

    // The SeekBar showing the video playback progress (allows user seeking).
    private SeekBar mSeekBar;

    // Android Studio project only.
    // A timer for periodically updating the progress of video playback to the user.
    private Timer mPlaybackUpdateTimer;

    // A flag to control when the playback monitor schedules new tasks.
    private boolean mRunPlaybackMonitoringTask;

    // The Zoom (floating action) button.
    private ImageView mZoom;

    /**
     * @param context The context to use.
     * @param multiSelectionAllowed Whether to allow the user to select more than one image.
     */
    @SuppressWarnings("unchecked") // mSelectableListLayout
    public PickerCategoryView(Context context, boolean multiSelectionAllowed,
            PhotoPickerToolbar.PhotoPickerToolbarDelegate delegate) {
        super(context);
        mActivity = /*(ChromeActivity)*/ (Activity) context;
        mMultiSelectionAllowed = multiSelectionAllowed;

        mDecoderServiceHost = new DecoderServiceHost(this, context);
        mDecoderServiceHost.bind(context);

        mSelectionDelegate = new SelectionDelegate<PickerBitmap>();
        if (true /*ChromeFeatureList.isEnabled(ChromeFeatureList.PHOTO_PICKER_ZOOM)*/) {
            mSelectionDelegate.addObserver(this);
        }
        if (!multiSelectionAllowed) mSelectionDelegate.setSingleSelectionMode();

        View root = LayoutInflater.from(context).inflate(R.layout.photo_picker_dialog, this);
        mSelectableListLayout =
                (SelectableListLayout<PickerBitmap>) root.findViewById(R.id.selectable_list);

        mPickerAdapter = new PickerAdapter(this);
        mRecyclerView = mSelectableListLayout.initializeRecyclerView(mPickerAdapter);
        int titleId = multiSelectionAllowed ? R.string.photo_picker_select_images
                                            : R.string.photo_picker_select_image;
        PhotoPickerToolbar toolbar = (PhotoPickerToolbar) mSelectableListLayout.initializeToolbar(
                R.layout.photo_picker_toolbar, mSelectionDelegate, titleId, 0, 0, null, false,
                false);
        toolbar.setNavigationOnClickListener(this);
        toolbar.setDelegate(delegate);
        Button doneButton = (Button) toolbar.findViewById(R.id.done);
        doneButton.setOnClickListener(this);
        mVideoView = findViewById(R.id.video_player);
        mVideoOverlayContainer = findViewById(R.id.video_overlay_container);
        mVideoOverlayContainer.setOnClickListener(this);
        mVideoControls = findViewById(R.id.video_controls);
        mLargePlayButton = findViewById(R.id.video_player_play_button);
        mLargePlayButton.setOnClickListener(this);
        mMuteButton = findViewById(R.id.mute);
        mMuteButton.setImageResource(R.drawable.ic_volume_on_white_24dp);
        mMuteButton.setOnClickListener(this);
        mFullscreenButton = findViewById(R.id.fullscreen);
        mFullscreenButton.setOnClickListener(this);
        mSeekBar = findViewById(R.id.seek_bar);
        mSeekBar.setOnSeekBarChangeListener(this);
        mZoom = findViewById(R.id.zoom);

        calculateGridMetrics();

        mLayoutManager = new GridLayoutManager(mActivity, mColumns);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mSpacingDecoration = new GridSpacingItemDecoration(mColumns, mPadding);
        mRecyclerView.addItemDecoration(mSpacingDecoration);
        mRecyclerView.setRecyclerListener(this);

        final long maxMemory = ConversionUtils.bytesToKilobytes(Runtime.getRuntime().maxMemory());
        if (true /* ChromeFeatureList.isEnabled(ChromeFeatureList.PHOTO_PICKER_ZOOM) */) {
            mCacheSizeFullScreen = (int) (maxMemory / 4); // 1/4 of the available memory.
            mCacheSizeLarge = (int) (maxMemory / 4); // 1/4 of the available memory.
        } else {
            mCacheSizeFullScreen = 0;
            mCacheSizeLarge = (int) (maxMemory / 2); // 1/2 of the available memory.
        }
        mCacheSizeSmall = (int) (maxMemory / 8); // 1/8th of the available memory.

        // Android Studio project only:
        mLowResThumbnails = new LruCache<String, Thumbnail>(mCacheSizeSmall) {
            @Override
            protected int sizeOf(String key, Thumbnail thumbnail) {
                return (int) ConversionUtils.bytesToKilobytes(thumbnail.bitmaps.get(0).getByteCount());
            }
        };
        mHighResThumbnails = new LruCache<String, Thumbnail>(mCacheSizeLarge) {
            @Override
            protected int sizeOf(String key, Thumbnail thumbnail) {
                 return (int) ConversionUtils.bytesToKilobytes(thumbnail.bitmaps.get(0).getByteCount());
            }
        };
        mFullScreenBitmaps = new LruCache<String, Thumbnail>(mCacheSizeFullScreen) {
            @Override
            protected int sizeOf(String key, Thumbnail thumbnail) {
                return (int) ConversionUtils.bytesToKilobytes(thumbnail.bitmaps.get(0).getByteCount());
            }
        };
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        calculateGridMetrics();
        mLayoutManager.setSpanCount(mColumns);
        mRecyclerView.removeItemDecoration(mSpacingDecoration);
        mSpacingDecoration = new GridSpacingItemDecoration(mColumns, mPadding);
        mRecyclerView.addItemDecoration(mSpacingDecoration);

        // Configuration change can happen at any time, even before the photos have been
        // enumerated (when mPickerBitmaps is null, causing: https://crbug.com/947657). There's no
        // need to call notifyDataSetChanged in that case because it will be called once the photo
        // list becomes ready.
        if (mPickerBitmaps != null) {
            mPickerAdapter.notifyDataSetChanged();
            mRecyclerView.requestLayout();
        }

        if (mVideoControls.getVisibility() != View.GONE) {
            // When configuration changes, the video overlay controls need to be synced to the new
            // video size. Post a task, so that size adjustments happen after layout of the video
            // controls has completed.
            ThreadUtils.postOnUiThread(() -> { syncOverlayControlsSize(); });
        }
    }

    /**
     * Severs the connection to the decoding utility process and cancels any outstanding requests.
     */
    public void onDialogDismissed() {
        if (mWorkerTask != null) {
            mWorkerTask.cancel(true);
            mWorkerTask = null;
        }

        if (mDecoderServiceHost != null) {
            mDecoderServiceHost.unbind(mActivity);
            mDecoderServiceHost = null;
        }
    }

    /**
     * Start playback of a video in an overlay above the photo picker.
     * @param uri The uri of the video to start playing.
     */
    public void startVideoPlaybackAsync(Uri uri) {
        View playbackContainer = findViewById(R.id.playback_container);
        playbackContainer.setVisibility(View.VISIBLE);

        mVideoView.setVisibility(View.VISIBLE);
        mVideoView.setVideoURI(uri);

        mVideoView.setOnPreparedListener((MediaPlayer mediaPlayer) -> {
            mMediaPlayer = mediaPlayer;
            startVideoPlayback();

            mMediaPlayer.setOnVideoSizeChangedListener(
                    (MediaPlayer player, int width, int height) -> { syncOverlayControlsSize(); });

            if (sProgressCallback != null) {
                mMediaPlayer.setOnInfoListener((MediaPlayer player, int what, int extra) -> {
                    if (what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) {
                        sProgressCallback.onVideoPlaying();
                        return true;
                    }
                    return false;
                });
            }
        });

        mVideoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                // Once we reach the completion point, show the overlay controls (without fading
                // away) to indicate that playback has reached the end of the video (and didn't
                // break before reaching the end). This also allows the user to restart playback
                // from the start, by pressing Play.
                mLargePlayButton.setImageResource(R.drawable.ic_play_circle_filled_white_24dp);
                updateProgress();
                showOverlayControls(/*animateAway=*/false);
                if (sProgressCallback != null) {
                    sProgressCallback.onVideoEnded();
                }
            }
        });
    }

    /**
     * Ends video playback (if a video is playing) and closes the video player. Aborts if the video
     * playback container is not showing.
     * @return true if a video container was showing, false otherwise.
     */
    public boolean closeVideoPlayer() {
        View playbackContainer = findViewById(R.id.playback_container);
        if (playbackContainer.getVisibility() != View.VISIBLE) {
            return false;
        }

        playbackContainer.setVisibility(View.GONE);
        stopVideoPlayback();
        mVideoView.setMediaController(null);
        mMuteButton.setImageResource(R.drawable.ic_volume_on_white_24dp);
        return true;
    }

    /**
     * Initializes the PickerCategoryView object.
     * @param dialog The dialog showing us.
     * @param listener The listener who should be notified of actions.
     * @param mimeTypes A list of mime types to show in the dialog.
     */
    public void initialize(
            PhotoPickerDialog dialog, PhotoPickerListener listener, List<String> mimeTypes) {
        mDialog = dialog;
        mListener = listener;
        mMimeTypes = new ArrayList<>(mimeTypes);

        enumerateBitmaps();

        mDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                executeAction(PhotoPickerListener.PhotoPickerAction.CANCEL, null, ACTION_CANCEL);
            }
        });
    }

    // FileEnumWorkerTask.FilesEnumeratedCallback:

    @Override
    public void filesEnumeratedCallback(List<PickerBitmap> files) {
        // Calculate the rate of files enumerated per tenth of a second.
        long elapsedTimeMs = SystemClock.elapsedRealtime() - mEnumStartTime;
        int rate = (int) (100 * files.size() / elapsedTimeMs);
        RecordHistogram.recordTimesHistogram("Android.PhotoPicker.EnumerationTime", elapsedTimeMs);
        RecordHistogram.recordCustomCountHistogram(
                "Android.PhotoPicker.EnumeratedFiles", files.size(), 1, 10000, 50);
        RecordHistogram.recordCount1000Histogram("Android.PhotoPicker.EnumeratedRate", rate);

        mPickerBitmaps = files;
        processBitmaps();
    }

    // DecoderServiceHost.ServiceReadyCallback:

    @Override
    public void serviceReady() {
        mServiceReady = true;
        processBitmaps();
    }

    // RecyclerView.RecyclerListener:

    @Override
    public void onViewRecycled(RecyclerView.ViewHolder holder) {
        PickerBitmapViewHolder bitmapHolder = (PickerBitmapViewHolder) holder;
        String filePath = bitmapHolder.getFilePath();
        if (filePath != null) {
            getDecoderServiceHost().cancelDecodeImage(filePath);
        }
    }

    // SelectionDelegate.SelectionObserver:

    @Override
    public void onSelectionStateChange(List<PickerBitmap> selectedItems) {
        if (false /*!ChromeFeatureList.isEnabled(ChromeFeatureList.PHOTO_PICKER_ZOOM)*/) {
            return;
        }

        if (mZoom.getVisibility() != View.VISIBLE) {
            mZoom.setVisibility(View.VISIBLE);
            mZoom.setOnClickListener(this);
        }
    }

    // OnClickListener:

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.done) {
            notifyPhotosSelected();
        } else if (id == R.id.zoom) {
            if (!mZoomSwitchingInEffect) {
                flipZoomMode();
            }
        } else if (id == R.id.video_overlay_container) {
            showOverlayControls(/*animateAway=*/true);
        } else if (id == R.id.video_player_play_button) {
            toggleVideoPlayback();
        } else if (id == R.id.mute) {
            toggleMute();
        } else if (id == R.id.fullscreen) {
            toggleFullscreen();
        } else {
            executeAction(PhotoPickerListener.PhotoPickerAction.CANCEL, null, ACTION_CANCEL);
        }
    }

    // SeekBar.OnSeekBarChangeListener:

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser) {
            final boolean seekDuringPlay = mVideoView.isPlaying();
            mMediaPlayer.setOnSeekCompleteListener(mp -> {
                mMediaPlayer.setOnSeekCompleteListener(null);
                if (seekDuringPlay) {
                    startVideoPlayback();
                }
            });

            float percentage = progress / 100f;
            int seekTo = Math.round(percentage * mVideoView.getDuration());
            if (Build.VERSION.SDK_INT >= 26) {
                mMediaPlayer.seekTo(seekTo, MediaPlayer.SEEK_CLOSEST);
            } else {
                // On older versions, sync to nearest previous key frame.
                mVideoView.seekTo(seekTo);
            }
            updateProgress();
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        cancelFadeAwayAnimation();
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        fadeAwayVideoControls();
    }

    /**
     * Start loading of bitmaps, once files have been enumerated and service is
     * ready to decode.
     */
    private void processBitmaps() {
        if (mServiceReady && mPickerBitmaps != null) {
            mPickerAdapter.notifyDataSetChanged();
        }
    }

    private void flipZoomMode() {
        // Bitmap scaling is cumulative, so if an image is selected when we switch modes, it will
        // become skewed when switching between full size and square modes because dimensions of the
        // picture also change (from square to full width). We therefore un-select all items before
        // starting the animation and then reselect them once animation has ended.
        final HashSet<PickerBitmap> selectedItems =
                new HashSet<>(mSelectionDelegate.getSelectedItems());
        mSelectionDelegate.clearSelection();

        mMagnifyingMode = !mMagnifyingMode;

        if (mMagnifyingMode) {
            mZoom.setImageResource(R.drawable.zoom_out);
        } else {
            mZoom.setImageResource(R.drawable.zoom_in);
        }

        calculateGridMetrics();

        if (!mMagnifyingMode) {
            getFullScreenBitmaps().evictAll();
        }

        mZoomSwitchingInEffect = true;

        ChangeBounds transition = new ChangeBounds();
        transition.addListener(new Transition.TransitionListener() {
            @Override
            public void onTransitionStart(Transition transition) {}

            @Override
            public void onTransitionEnd(Transition transition) {
                mZoomSwitchingInEffect = false;

                // Redo selection when switching between modes to make it obvious what got selected.
                mSelectionDelegate.setSelectedItems(selectedItems);
            }

            @Override
            public void onTransitionCancel(Transition transition) {}

            @Override
            public void onTransitionPause(Transition transition) {}

            @Override
            public void onTransitionResume(Transition transition) {}
        });

        TransitionManager.beginDelayedTransition(mRecyclerView, transition);

        mLayoutManager.setSpanCount(mColumns);
        mPickerAdapter.notifyDataSetChanged();
        mRecyclerView.requestLayout();
    }

    // Simple accessors:

    public int getImageWidth() {
        return mImageWidth;
    }

    public int getSpecialTileHeight() {
        return mSpecialTileHeight;
    }

    public boolean isInMagnifyingMode() {
        return mMagnifyingMode;
    }

    public boolean isZoomSwitchingInEffect() {
        return mZoomSwitchingInEffect;
    }

    public SelectionDelegate<PickerBitmap> getSelectionDelegate() {
        return mSelectionDelegate;
    }

    public List<PickerBitmap> getPickerBitmaps() {
        return mPickerBitmaps;
    }

    public DecoderServiceHost getDecoderServiceHost() {
        return mDecoderServiceHost;
    }

    public LruCache<String, Thumbnail> getLowResThumbnails() {
        /* Not used for the Android project, but used in Chrome.
        if (mLowResThumbnails == null || mLowResThumbnails.get() == null) {
            mLowResThumbnails = mActivity.getReferencePool().put(
                    new LruCache<String, Thumbnail>(mCacheSizeSmall));
        }
        return mLowResThumbnails.get();
        */
        return mLowResThumbnails;
    }

    public LruCache<String, Thumbnail> getHighResThumbnails() {
        /* Not used for the Android project, but used in Chrome.
        if (mHighResThumbnails == null || mHighResThumbnails.get() == null) {
            mHighResThumbnails = mActivity.getReferencePool().put(
                    new LruCache<String, Thumbnail>(mCacheSizeLarge));
        }
        return mHighResThumbnails.get();
        */
        return mHighResThumbnails;
    }

    public LruCache<String, Thumbnail> getFullScreenBitmaps() {
        /* Not used for the Android project, but used in Chrome.
        if (mFullScreenBitmaps == null || mFullScreenBitmaps.get() == null) {
            mFullScreenBitmaps = mActivity.getReferencePool().put(
                    new LruCache<String, Thumbnail>(mCacheSizeFullScreen));
        }
        return mFullScreenBitmaps.get();
        */
        return mFullScreenBitmaps;
    }

    public boolean isMultiSelectAllowed() {
        return mMultiSelectionAllowed;
    }

    /**
     * Notifies the listener that the user selected to launch the gallery.
     */
    public void showGallery() {
        executeAction(PhotoPickerListener.PhotoPickerAction.LAUNCH_GALLERY, null, ACTION_BROWSE);
    }

    /**
     * Notifies the listener that the user selected to launch the camera intent.
     */
    public void showCamera() {
        executeAction(PhotoPickerListener.PhotoPickerAction.LAUNCH_CAMERA, null, ACTION_NEW_PHOTO);
    }

    /**
     * Calculates image size and how many columns can fit on-screen.
     */
    private void calculateGridMetrics() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        mActivity.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

        int width = displayMetrics.widthPixels;
        int minSize =
                mActivity.getResources().getDimensionPixelSize(R.dimen.photo_picker_tile_min_size);
        mPadding = mMagnifyingMode
                ? 0
                : mActivity.getResources().getDimensionPixelSize(R.dimen.photo_picker_tile_gap);
        mColumns = mMagnifyingMode ? 1 : Math.max(1, (width - mPadding) / (minSize + mPadding));
        mImageWidth = (width - mPadding * (mColumns + 1)) / (mColumns);
        mImageHeight = mMagnifyingMode
                ? displayMetrics.heightPixels - findViewById(R.id.action_bar_bg).getHeight()
                : mImageWidth;
        if (!mMagnifyingMode) mSpecialTileHeight = mImageWidth;

        // Make sure columns and padding are either both even or both odd.
        if (!mMagnifyingMode && ((mColumns % 2) == 0) != ((mPadding % 2) == 0)) {
            mPadding++;
        }
    }

    /**
     * Asynchronously enumerates bitmaps on disk.
     */
    private void enumerateBitmaps() {
        if (sTestFiles != null) {
            filesEnumeratedCallback(sTestFiles);
            return;
        }

        if (mWorkerTask != null) {
            mWorkerTask.cancel(true);
        }

        mEnumStartTime = SystemClock.elapsedRealtime();
        // Android Studio project does not use WindowAndroid parameter.
        mWorkerTask = new FileEnumWorkerTask(this,
                new MimeTypeFilter(mMimeTypes, true), mMimeTypes, mActivity.getContentResolver());
        mWorkerTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    /**
     * Notifies any listeners that one or more photos have been selected.
     */
    private void notifyPhotosSelected() {
        List<PickerBitmap> selectedFiles = mSelectionDelegate.getSelectedItemsAsList();
        Collections.sort(selectedFiles);
        Uri[] photos = new Uri[selectedFiles.size()];
        int i = 0;
        for (PickerBitmap bitmap : selectedFiles) {
            photos[i++] = bitmap.getUri();
        }

        executeAction(
                PhotoPickerListener.PhotoPickerAction.PHOTOS_SELECTED, photos, ACTION_PHOTO_PICKED);
    }

    /**
     * A class for implementing grid spacing between items.
     */
    private class GridSpacingItemDecoration extends RecyclerView.ItemDecoration {
        // The number of spans to account for.
        private int mSpanCount;

        // The amount of spacing to use.
        private int mSpacing;

        public GridSpacingItemDecoration(int spanCount, int spacing) {
            mSpanCount = spanCount;
            mSpacing = spacing;
        }

        @Override
        public void getItemOffsets(
                Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
            if (mMagnifyingMode) {
                outRect.set(0, 0, 0, mSpacing);
                return;
            }

            int left = 0;
            int right = 0;
            int top = 0;
            int bottom = 0;
            int position = parent.getChildAdapterPosition(view);

            if (position >= 0) {
                int column = position % mSpanCount;

                left = mSpacing - ((column * mSpacing) / mSpanCount);
                right = (column + 1) * mSpacing / mSpanCount;

                if (position < mSpanCount) {
                    top = mSpacing;
                }
                bottom = mSpacing;
            }

            outRect.set(left, top, right, bottom);
        }
    }

    /**
     * Report back what the user selected in the dialog, report UMA and clean up.
     * @param action The action taken.
     * @param photos The photos that were selected (if any).
     * @param umaId The UMA value to record with the action.
     */
    private void executeAction(
            @PhotoPickerListener.PhotoPickerAction int action, Uri[] photos, int umaId) {
        mListener.onPhotoPickerUserAction(action, photos);
        mDialog.dismiss();
        UiUtils.onPhotoPickerDismissed();
        recordFinalUmaStats(umaId);
    }

    /**
     * Record UMA statistics (what action was taken in the dialog and other performance stats).
     * @param action The action the user took in the dialog.
     */
    private void recordFinalUmaStats(int action) {
        RecordHistogram.recordEnumeratedHistogram(
                "Android.PhotoPicker.DialogAction", action, ACTION_BOUNDARY);
        RecordHistogram.recordCountHistogram(
                "Android.PhotoPicker.DecodeRequests", mPickerAdapter.getDecodeRequestCount());
        RecordHistogram.recordCountHistogram(
                "Android.PhotoPicker.CacheHits", mPickerAdapter.getCacheHitCount());
    }

    private void showOverlayControls(boolean animateAway) {
        cancelFadeAwayAnimation();

        if (animateAway && mVideoView.isPlaying()) {
            fadeAwayVideoControls();
            startPlaybackMonitor();
        }
    }

    private void fadeAwayVideoControls() {
        mVideoOverlayContainer.animate()
                .alpha(0.0f)
                .setStartDelay(3000)
                .setDuration(1000)
                .setListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {}

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        enableClickableButtons(false);
                        stopPlaybackMonitor();
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {}

                    @Override
                    public void onAnimationRepeat(Animator animation) {}
                });
    }

    private void cancelFadeAwayAnimation() {
        // Canceling the animation will leave the alpha in the state it had reached while animating,
        // so we need to explicitly set the alpha to 1.0 to reset it.
        mVideoOverlayContainer.animate().cancel();
        mVideoOverlayContainer.setAlpha(1.0f);
        enableClickableButtons(true);
    }

    private void enableClickableButtons(boolean enable) {
        mLargePlayButton.setClickable(enable);
        mMuteButton.setClickable(enable);
    }

    private void updateProgress() {
        String current;
        String total;
        try {
            current = DecodeVideoTask.formatDuration(Long.valueOf(mVideoView.getCurrentPosition()));
            total = DecodeVideoTask.formatDuration(Long.valueOf(mVideoView.getDuration()));
        } catch (IllegalStateException exception) {
            // VideoView#getCurrentPosition throws this error if the dialog has been dismissed while
            // waiting to update the status.
            return;
        }

        SeekBar seekBar = findViewById(R.id.seek_bar);
        if (seekBar == null) {
            return;
        }

        ThreadUtils.postOnUiThread(() -> {
            TextView remainingTime = findViewById(R.id.remaining_time);
            String formattedProgress = current + " / " + total;
            remainingTime.setText(formattedProgress);
            int percentage = mVideoView.getDuration() == 0
                    ? 0
                    : mVideoView.getCurrentPosition() * 100 / mVideoView.getDuration();
            seekBar.setProgress(percentage);
        });

        /* Not needed for Android Studio project
        if (mVideoView.isPlaying() && !mInterruptPlaybackMonitor) {
            startPlaybackMonitor();
        }
        */
    }

    private void startVideoPlayback() {
        mMediaPlayer.start();
        mLargePlayButton.setImageResource(R.drawable.ic_pause_circle_outline_white_24dp);
        showOverlayControls(/*animateAway=*/true);
    }

    private void stopVideoPlayback() {
        stopPlaybackMonitor();

        mMediaPlayer.pause();
        mLargePlayButton.setImageResource(R.drawable.ic_play_circle_filled_white_24dp);
        showOverlayControls(/*animateAway=*/false);
    }

    private void toggleVideoPlayback() {
        if (mVideoView.isPlaying()) {
            stopVideoPlayback();
        } else {
            startVideoPlayback();
        }
    }

    private void syncOverlayControlsSize() {
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                mVideoView.getMeasuredWidth(), mVideoView.getMeasuredHeight());
        mVideoControls.setLayoutParams(params);
    }

    private void toggleMute() {
        mAudioOn = !mAudioOn;
        if (mAudioOn) {
            mMediaPlayer.setVolume(1f, 1f);
            mMuteButton.setImageResource(R.drawable.ic_volume_on_white_24dp);
        } else {
            mMediaPlayer.setVolume(0f, 0f);
            mMuteButton.setImageResource(R.drawable.ic_volume_off_white_24dp);
        }
    }

    private void toggleFullscreen() {
        mFullScreen = !mFullScreen;
        if (mFullScreen) {
            mFullscreenButton.setImageResource(R.drawable.ic_full_screen_exit_white_24dp);
        } else {
            mFullscreenButton.setImageResource(R.drawable.ic_full_screen_white_24dp);
        }

        showOverlayControls(true);
    }

    private void startPlaybackMonitor() {
        mRunPlaybackMonitoringTask = true;
        startPlaybackMonitorTask();
    }

    private void startPlaybackMonitorTask() {
        //PostTask.postDelayedTask(UiThreadTaskTraits.DEFAULT, () -> updateProgress(), 250);

        // Android-Studio uses a Timer instead of PostTask.
        if (mPlaybackUpdateTimer == null) {
            mPlaybackUpdateTimer = new Timer();
            final TimerTask tickTask = new TimerTask() {
                @Override
                public void run() {
                    updateProgress();
                }
            };

            mPlaybackUpdateTimer.schedule(tickTask, 0, 250);
        }
    }

    private void stopPlaybackMonitor() {
        mRunPlaybackMonitoringTask = false;

        // Android-Studio uses a Timer instead of PostTask.
        if (mPlaybackUpdateTimer != null) {
            mPlaybackUpdateTimer.cancel();
            mPlaybackUpdateTimer = null;
        }
    }

    /** Sets a list of files to use as data for the dialog. For testing use only. */
    @VisibleForTesting
    public static void setTestFiles(List<PickerBitmap> testFiles) {
        sTestFiles = new ArrayList<>(testFiles);
    }

    /** Sets the video playback progress callback. For testing use only. */
    @VisibleForTesting
    public static void setProgressCallback(VideoPlaybackStatusCallback callback) {
        sProgressCallback = callback;
    }

    @VisibleForTesting
    public SelectionDelegate<PickerBitmap> getSelectionDelegateForTesting() {
        return mSelectionDelegate;
    }
}
