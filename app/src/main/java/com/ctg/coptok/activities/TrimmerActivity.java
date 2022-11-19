package com.ctg.coptok.activities;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.analytics.AnalyticsListener;
import com.google.android.exoplayer2.source.ClippingMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.kaopiz.kprogresshud.KProgressHUD;

import java.io.File;
import java.util.concurrent.TimeUnit;

import com.ctg.coptok.R;
import com.ctg.coptok.SharedConstants;
import com.ctg.coptok.trimmer.TrimTimeBar;
import com.ctg.coptok.utils.LocaleUtil;
import com.ctg.coptok.utils.TempUtil;
import com.ctg.coptok.utils.TextFormatUtil;
import com.ctg.coptok.utils.VideoUtil;
import com.ctg.coptok.workers.MergeAudioVideoWorker2;
import com.ctg.coptok.workers.VideoTrimmerWorker4;

public class TrimmerActivity extends AppCompatActivity implements AnalyticsListener, TrimTimeBar.Listener {

    public static final String EXTRA_AUDIO = "audio";
    public static final String EXTRA_SONG_ID = "song_id";
    public static final String EXTRA_VIDEO = "video";
    private static final String TAG = "TrimmerActivity";

    private String mAudio;
    private int mDuration = 0;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private SimpleExoPlayer mPlayer;
    private final Runnable mProgress = new Runnable() {

        @Override
        public void run() {
            if (mPlayer != null && mPlayer.isPlaying()) {
                long position = mPlayer.getCurrentPosition();
                mTimeBar.setTime(
                        mTrimStartTime + (int) position,
                        mDuration,
                        mTrimStartTime,
                        mTrimEndTime
                );
            }

            mHandler.postDelayed(mProgress, 100);
        }
    };
    private TrimTimeBar mTimeBar;
    private int mTrimEndTime = 0;
    private int mTrimStartTime = 0;
    private int mSongId;
    private String mVideo;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleUtil.wrap(base));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trimmer);
        ImageButton close = findViewById(R.id.header_back);
        close.setImageResource(R.drawable.ic_baseline_close_24);
        close.setOnClickListener(view -> finish());
        TextView title = findViewById(R.id.header_title);
        title.setText(R.string.trimmer_label);
        ImageButton done = findViewById(R.id.header_more);
        done.setImageResource(R.drawable.ic_baseline_check_24);
        done.setOnClickListener(view -> commitSelection());
        mAudio = getIntent().getStringExtra(EXTRA_AUDIO);
        mSongId = getIntent().getIntExtra(EXTRA_SONG_ID, 0);
        mVideo = getIntent().getStringExtra(EXTRA_VIDEO);
        mPlayer = new SimpleExoPlayer.Builder(this).build();
        mPlayer.addAnalyticsListener(this);
        mPlayer.setRepeatMode(ExoPlayer.REPEAT_MODE_ALL);
        PlayerView player = findViewById(R.id.player);
        player.setPlayer(mPlayer);
        mDuration = (int) VideoUtil.getDuration(this, Uri.parse(mVideo));
        mTrimStartTime = 0;
        mTrimEndTime = Math.min(mDuration, (int) SharedConstants.MAX_DURATION);
        Log.v(TAG, "Duration of video is " + mDuration + "ms.");
        mTimeBar = new TrimTimeBar(this, this);
        mTimeBar.setTime(0, mDuration, 0, mTrimEndTime);
        LinearLayout wrapper = findViewById(R.id.trimmer);
        wrapper.addView(mTimeBar);
        startPlayer();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mHandler.removeCallbacks(mProgress);
        mPlayer.setPlayWhenReady(false);
        if (mPlayer.isPlaying()) {
            mPlayer.stop(true);
        }

        mPlayer.release();
        mPlayer = null;
        File video = new File(mVideo);
        if (!video.delete()) {
            Log.w(TAG, "Could not delete input video: " + video);
        }
    }

    @Override
    public void onScrubbingStart() {
        mPlayer.setPlayWhenReady(false);
    }

    @Override
    public void onScrubbingMove(int time) {
    }

    @Override
    public void onScrubbingEnd(int time, int start, int end) {
        Log.v(TAG, "Scrub position is " + start + "ms to " + end + "ms.");
        mTrimStartTime = start;
        mTrimEndTime = end;
        startPlayer();
    }

    private void commitSelection() {
        int duration = mTrimEndTime - mTrimStartTime;
        if (duration > SharedConstants.MAX_DURATION) {
            String message = getString(
                    R.string.message_trim_too_long,
                    TimeUnit.MILLISECONDS.toSeconds(SharedConstants.MAX_DURATION)
            );
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        } else if (duration < SharedConstants.MIN_DURATION) {
            String message = getString(
                    R.string.message_trim_too_short,
                    TimeUnit.MILLISECONDS.toSeconds(SharedConstants.MIN_DURATION)
            );
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        } else {
            submitForTrim();
        }
    }

    private void proceedToAdjustment(File file) {
        Intent intent = new Intent(this, AdjustAudioActivity.class);
        intent.putExtra(AdjustAudioActivity.EXTRA_VIDEO, file.getAbsolutePath());
        intent.putExtra(AdjustAudioActivity.EXTRA_SONG, mAudio);
        intent.putExtra(AdjustAudioActivity.EXTRA_SONG_ID, mSongId);
        startActivity(intent);
        finish();
    }

    private void proceedToFilter(File file) {
        Intent intent;
        if (getResources().getBoolean(R.bool.filters_enabled)) {
            intent = new Intent(this, FilterActivity.class);
            intent.putExtra(FilterActivity.EXTRA_VIDEO, file.getAbsolutePath());
            intent.putExtra(FilterActivity.EXTRA_SONG_ID, mSongId);
        } else {
            intent = new Intent(this, UploadActivity.class);
            intent.putExtra(UploadActivity.EXTRA_VIDEO, file.getAbsolutePath());
            intent.putExtra(UploadActivity.EXTRA_SONG_ID, mSongId);
        }

        startActivity(intent);
        finish();
    }

    private void startPlayer() {
        DefaultDataSourceFactory factory =
                new DefaultDataSourceFactory(this, getString(R.string.app_name));
        MediaSource source = new ProgressiveMediaSource.Factory(factory)
                .createMediaSource(Uri.fromFile(new File(mVideo)));
        source = new ClippingMediaSource(
                source,
                TimeUnit.MILLISECONDS.toMicros(mTrimStartTime),
                TimeUnit.MILLISECONDS.toMicros(mTrimEndTime)
        );
        mPlayer.setPlayWhenReady(true);
        mPlayer.prepare(source);
        mHandler.removeCallbacks(mProgress);
        mHandler.postDelayed(mProgress, 100);
        TextView selection = findViewById(R.id.selection);
        selection.setText(TextFormatUtil.toMMSS(mTrimEndTime - mTrimStartTime));
    }

    private void submitForMerge(File file) {
        KProgressHUD progress = KProgressHUD.create(this)
                .setStyle(KProgressHUD.Style.SPIN_INDETERMINATE)
                .setLabel(getString(R.string.progress_title))
                .setCancellable(false)
                .show();
        File merged = TempUtil.createNewFile(this, ".mp4");
        Data data = new Data.Builder()
                .putString(MergeAudioVideoWorker2.KEY_AUDIO, mAudio)
                .putString(MergeAudioVideoWorker2.KEY_VIDEO, file.getAbsolutePath())
                .putString(MergeAudioVideoWorker2.KEY_OUTPUT, merged.getAbsolutePath())
                .build();
        OneTimeWorkRequest request =
                new OneTimeWorkRequest.Builder(MergeAudioVideoWorker2.class)
                        .setInputData(data)
                        .build();
        WorkManager wm = WorkManager.getInstance(this);
        wm.enqueue(request);
        wm.getWorkInfoByIdLiveData(request.getId())
                .observe(this, info -> {
                    boolean ended = info.getState() == WorkInfo.State.CANCELLED
                            || info.getState() == WorkInfo.State.FAILED;
                    if (ended) {
                        progress.dismiss();
                    }

                    if (info.getState() == WorkInfo.State.SUCCEEDED) {
                        proceedToFilter(merged);
                    }
                });
    }

    private void submitForTrim() {
        mPlayer.setPlayWhenReady(false);
        KProgressHUD progress = KProgressHUD.create(this)
                .setStyle(KProgressHUD.Style.SPIN_INDETERMINATE)
                .setLabel(getString(R.string.progress_title))
                .setCancellable(false)
                .show();
        WorkManager wm = WorkManager.getInstance(this);
        File trimmed = TempUtil.createNewFile(this, ".mp4");
        Data data = new Data.Builder()
                .putString(VideoTrimmerWorker4.KEY_INPUT, mVideo)
                .putString(VideoTrimmerWorker4.KEY_OUTPUT, trimmed.getAbsolutePath())
                .putLong(VideoTrimmerWorker4.KEY_START, mTrimStartTime)
                .putLong(VideoTrimmerWorker4.KEY_END, mTrimEndTime)
                .build();
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(VideoTrimmerWorker4.class)
                .setInputData(data)
                .build();
        wm.enqueue(request);
        wm.getWorkInfoByIdLiveData(request.getId())
                .observe(this, info -> {
                    boolean ended = info.getState() == WorkInfo.State.CANCELLED
                            || info.getState() == WorkInfo.State.FAILED
                            || info.getState() == WorkInfo.State.SUCCEEDED;
                    if (ended) {
                        progress.dismiss();
                    }

                    if (info.getState() == WorkInfo.State.SUCCEEDED) {
                        if (getResources().getBoolean(R.bool.skip_adjustment_screen)) {
                            if (TextUtils.isEmpty(mAudio)) {
                                proceedToFilter(trimmed);
                            } else {
                                submitForMerge(trimmed);
                            }
                        } else {
                            proceedToAdjustment(trimmed);
                        }
                    }
                });
    }
}
