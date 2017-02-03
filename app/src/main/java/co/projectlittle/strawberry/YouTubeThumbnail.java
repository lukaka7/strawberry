package co.projectlittle.strawberry;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubeStandalonePlayer;
import com.google.android.youtube.player.YouTubeThumbnailLoader;
import com.google.android.youtube.player.YouTubeThumbnailView;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class YouTubeThumbnail extends FrameLayout {
    private String mVideoId;

    private YouTubeThumbnailView mThumbnailView;
    private ImageView mImageView;
    private boolean mInitialized;
    private YouTubeThumbnailLoader mLoader;

    private Drawable mLoadingDrawable;
    private Drawable mErrorDrawable;
    private Drawable mPlayDrawable;

    private boolean mIsBeingInitialized;

    public YouTubeThumbnail(Context context) {
        super(context);
        init();
    }

    public YouTubeThumbnail(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public YouTubeThumbnail(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        inflate(getContext(), R.layout.youtube_thumbnail, this);
        mThumbnailView = (YouTubeThumbnailView) findViewById(R.id.thumbnail);
        mImageView = (ImageView) findViewById(R.id.thumbnail_image);
        mLoadingDrawable = getContext().getResources().getDrawable(R.drawable.thumbnail_loading);
        mErrorDrawable = getContext().getResources().getDrawable(R.drawable.thumbnail_error);
        mPlayDrawable = getContext().getResources().getDrawable(R.drawable.thumbnail_play);
        mInitialized = false;
        mIsBeingInitialized = false;
        this.setVisibility(View.GONE);
        mThumbnailView.setOnClickListener(new OnThumbnailClickListener());
    }

    public void loadVideo(String url) {

        if (TextUtils.isEmpty(url)) {
            this.setVisibility(View.GONE);
            releaseResource();
        } else {
            this.setVisibility(View.VISIBLE);
            mThumbnailView.setVisibility(View.GONE);
            mVideoId = getIdFromUrl(url);
            if (!TextUtils.isEmpty(mVideoId)) {
                mImageView.setImageDrawable(mLoadingDrawable);
                mImageView.setVisibility(View.VISIBLE);
                if (!mInitialized) {
                    if (!mIsBeingInitialized) {
                        mIsBeingInitialized = true;
                        mThumbnailView.initialize(getContext().getString(R.string.gcm_geo_key),
                                new ThumbnailListener());
                    }
                } else {
                    mLoader.setVideo(mVideoId);
                }
            } else {
                mImageView.setImageDrawable(mErrorDrawable);
                mImageView.setVisibility(View.VISIBLE);
                releaseResource();
            }
        }
    }

    private class OnThumbnailClickListener implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            releaseResource();
            Activity activity = (Activity) getContext();
            Intent intent = YouTubeStandalonePlayer
                    .createVideoIntent(activity, getContext().getString(R.string.gcm_geo_key),
                            mVideoId, 0, true, true);
            activity.startActivity(intent);
        }
    }

    private class ThumbnailListener implements
            YouTubeThumbnailView.OnInitializedListener,
            YouTubeThumbnailLoader.OnThumbnailLoadedListener {

        @Override
        public void onInitializationSuccess(YouTubeThumbnailView view,
                YouTubeThumbnailLoader loader) {
            loader.setOnThumbnailLoadedListener(this);
            loader.setVideo(mVideoId);
            mLoader = loader;
            mIsBeingInitialized = false;
            mInitialized = true;
            if (!TextUtils.isEmpty(mVideoId)) {
                mLoader.setVideo(mVideoId);
            }
        }

        @Override
        public void onInitializationFailure(YouTubeThumbnailView view,
                YouTubeInitializationResult loader) {
            mThumbnailView.setVisibility(View.GONE);
            mImageView.setImageDrawable(mErrorDrawable);
            mImageView.setVisibility(View.VISIBLE);
            mIsBeingInitialized = false;
            mInitialized = false;
        }

        @Override
        public void onThumbnailLoaded(YouTubeThumbnailView view, String videoId) {
            mThumbnailView.setVisibility(View.VISIBLE);
            mImageView.setImageDrawable(mPlayDrawable);
            mImageView.setVisibility(View.VISIBLE);
        }

        @Override
        public void onThumbnailError(YouTubeThumbnailView view,
                YouTubeThumbnailLoader.ErrorReason errorReason) {
            mThumbnailView.setVisibility(View.GONE);
            mImageView.setImageDrawable(mErrorDrawable);
            mImageView.setVisibility(View.VISIBLE);
        }
    }

    public static String getIdFromUrl(String url) {
        String videoId = null;
        String patternStr =
                "https?:\\/\\/(?:[0-9A-Z-]+\\.)?(?:youtu\\.be\\/|youtube\\.com\\S*[^\\w\\-\\s])([\\w\\-]{11})(?=[^\\w\\-]|$)(?![?=&+%\\w]*(?:['\"][^<>]*>|<\\/a>))[?=&+%\\w]*";

        Pattern pattern = Pattern.compile(
                patternStr,
                Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(url);
        if (matcher.matches()) {
            String match = matcher.group(1);
            if (match.length() == 11) {
                videoId = match;
            }
        }
        return videoId;
    }

    public void releaseResource() {
        if (mLoader != null) {
            mLoader.release();
            mLoader = null;
            mInitialized = false;
            mIsBeingInitialized = false;
        }
    }

    @Override
    protected void finalize() {
        releaseResource();
    }
}
