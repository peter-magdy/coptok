package com.ctg.coptok.activities;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.View;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.analytics.AnalyticsListener;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.kaopiz.kprogresshud.KProgressHUD;

import java.io.File;
import java.util.List;

import com.ctg.coptok.R;
import com.ctg.coptok.utils.LocaleUtil;
import com.ctg.coptok.utils.SizeUtil;
import com.ctg.coptok.utils.TempUtil;
import com.ctg.coptok.utils.VideoUtil;
import com.ctg.coptok.workers.MergeDuetVideosWorker;

public class DuetRecorderActivity extends RecorderActivity implements AnalyticsListener {

    public static final String EXTRA_VIDEO = "video";
    private static final String TAG = "DuetRecorderActivity";
    private boolean mDeleteOnExit = true;
    private boolean mFinished;
    private SimpleExoPlayer mPlayer;
    private Uri mVideo;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleUtil.wrap(base));
    }

    @Override
    protected int getContentView() {
        return R.layout.activity_recorder_duet;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        findViewById(R.id.sound).setVisibility(View.GONE);
        findViewById(R.id.speed).setVisibility(View.GONE);
        findViewById(R.id.filter).setVisibility(View.GONE);
        mVideo = getIntent().getParcelableExtra(EXTRA_VIDEO);
        Size size = VideoUtil.getDimensions(mVideo.getPath());
        Size resized = VideoUtil.getBestFit(size, new Size(
                SizeUtil.toPx(getResources(), 150),
                SizeUtil.toPx(getResources(), 150)
        ));
        RelativeLayout.LayoutParams params =
                new RelativeLayout.LayoutParams(resized.getWidth(), resized.getHeight());
        params.addRule(RelativeLayout.ALIGN_PARENT_END);
        params.setMargins(
                SizeUtil.toPx(getResources(), 10),
                SizeUtil.toPx(getResources(), 10),
                SizeUtil.toPx(getResources(), 10),
                SizeUtil.toPx(getResources(), 10)
        );
        PlayerView player = findViewById(R.id.player);
        player.setLayoutParams(params);
        mPlayer = new SimpleExoPlayer.Builder(this).build();
        DefaultDataSourceFactory factory =
                new DefaultDataSourceFactory(this, getString(R.string.app_name));
        ProgressiveMediaSource source = new ProgressiveMediaSource.Factory(factory)
                .createMediaSource(mVideo);
        mPlayer.addAnalyticsListener(this);
        mPlayer.setPlayWhenReady(false);
        mPlayer.prepare(source, false, false);
        player.setPlayer(mPlayer);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mPlayer.setPlayWhenReady(false);
        mPlayer.stop();
        mPlayer.release();
        File video = new File(mVideo.getPath());
        if (mDeleteOnExit && !video.delete()) {
            Log.w(TAG, "Could not delete original video: " + video);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mPlayer.setPlayWhenReady(false);
    }

    @Override
    public void onPlayerStateChanged(@Nullable EventTime time, boolean play, @Player.State int state) {
        if (state == Player.STATE_ENDED) {
            mFinished = true;
            stopRecording();
        }
    }

    @Override
    protected void onRecordingStarted() {
        Log.v(TAG, "Recording has started.");
        if (!mFinished) {
            mPlayer.setPlayWhenReady(true);
        }
    }

    @Override
    protected void onRecordingStopped() {
        Log.v(TAG, "Recording has stopped.");
        mPlayer.setPlayWhenReady(false);
    }

    @Override
    protected void submitForConcat(@NonNull List<RecordSegment> segments, @Nullable Uri audio) {
        KProgressHUD progress = KProgressHUD.create(this)
                .setStyle(KProgressHUD.Style.SPIN_INDETERMINATE)
                .setLabel(getString(R.string.progress_title))
                .setCancellable(false)
                .show();
        File merged1 = TempUtil.createNewFile(this, ".mp4");
        OneTimeWorkRequest request1 = createConcatTask(segments, merged1);
        File merged2 = TempUtil.createNewFile(this, ".mp4");
        boolean adjustment = getResources().getBoolean(R.bool.skip_adjustment_screen);
        Data data2 = new Data.Builder()
                .putString(MergeDuetVideosWorker.KEY_ORIGINAL, mVideo.getPath())
                .putString(MergeDuetVideosWorker.KEY_RECORDED, merged1.getAbsolutePath())
                .putString(MergeDuetVideosWorker.KEY_OUTPUT, merged2.getAbsolutePath())
                .putString(MergeDuetVideosWorker.KEY_AUDIO, adjustment ? "R" : "L")
                .build();
        OneTimeWorkRequest request2 =
                new OneTimeWorkRequest.Builder(MergeDuetVideosWorker.class)
                        .setInputData(data2)
                        .build();
        WorkManager wm = WorkManager.getInstance(this);
        wm.beginWith(request1).then(request2).enqueue();
        wm.getWorkInfoByIdLiveData(request2.getId())
                .observe(this, info -> {
                    boolean ended = info.getState() == WorkInfo.State.CANCELLED
                            || info.getState() == WorkInfo.State.FAILED
                            || info.getState() == WorkInfo.State.SUCCEEDED;
                    if (ended) {
                        progress.dismiss();
                    }

                    if (info.getState() == WorkInfo.State.SUCCEEDED) {
                        mDeleteOnExit = false;
                        if (adjustment) {
                            proceedToFilter(merged2.getAbsolutePath());
                        } else {
                            proceedToAdjustment(merged2.getAbsolutePath(), mVideo.getPath());
                        }
                    }
                });
    }
}
