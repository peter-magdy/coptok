package com.ctg.coptok.activities;

import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkContinuation;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.analytics.AnalyticsListener;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.kaopiz.kprogresshud.KProgressHUD;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.io.IOException;

import com.ctg.coptok.R;
import com.ctg.coptok.events.AudioTargetUpdateEvent;
import com.ctg.coptok.fragments.AdjustAudioFragment;
import com.ctg.coptok.utils.LocaleUtil;
import com.ctg.coptok.utils.TempUtil;
import com.ctg.coptok.workers.DelayAudioWorker;
import com.ctg.coptok.workers.MergeAudioVideoWorker2;
import com.ctg.coptok.workers.MergeAudiosWorker;
import com.ctg.coptok.workers.SplitAudioWorker;

public class AdjustAudioActivity extends AppCompatActivity implements AnalyticsListener {

    public static final String EXTRA_VIDEO = "video";
    public static final String EXTRA_SONG = "song";
    public static final String EXTRA_SONG_ID = "song_id";

    private static final String TAG = "AdjustAudioActivity";

    private AdjustAudioActivityViewModel mModel;

    private String mVideo, mVideoAudio, mSong;
    private int mSongId;

    private MediaPlayer mAudioPlayerV, mAudioPlayerS;
    private SimpleExoPlayer mVideoPlayer;

    private final Handler mHandler = new Handler();
    private final Runnable mVideoStartRunnable = this::startVideoPlayer;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleUtil.wrap(base));
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAudioTargetUpdateEvent(AudioTargetUpdateEvent event) {
        if (event.getTarget() == AdjustAudioFragment.TARGET_VIDEO) {
            mModel.delayV.postValue(event.getDelay());
            mModel.volumeV.postValue(event.getVolume());
        } else {
            mModel.delayS.postValue(event.getDelay());
            mModel.volumeS.postValue(event.getVolume());
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_adjust_audio);
        ImageButton close = findViewById(R.id.header_back);
        close.setImageResource(R.drawable.ic_baseline_close_24);
        close.setOnClickListener(view -> finish());
        TextView title = findViewById(R.id.header_title);
        title.setText(R.string.adjust_audio_label);
        ImageButton done = findViewById(R.id.header_more);
        done.setImageResource(R.drawable.ic_baseline_check_24);
        done.setOnClickListener(v -> submitForAdjustment());
        mModel = new ViewModelProvider(this).get(AdjustAudioActivityViewModel.class);
        mVideo = getIntent().getStringExtra(EXTRA_VIDEO);
        mSong = getIntent().getStringExtra(EXTRA_SONG);
        mSongId = getIntent().getIntExtra(EXTRA_SONG_ID, 0);
        mVideoPlayer = new SimpleExoPlayer.Builder(this).build();
        mVideoPlayer.addAnalyticsListener(this);
        mVideoPlayer.setVolume(0);
        PlayerView player = findViewById(R.id.player);
        player.setPlayer(mVideoPlayer);
        View overlay = findViewById(R.id.overlay);
        overlay.setOnClickListener(v -> {
            if (mVideoPlayer.getPlayWhenReady()) {
                mVideoPlayer.setPlayWhenReady(false);
            } else {
                startVideoPlayer();
            }
        });
        submitForSplit();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopVideoPlayer();
        mVideoPlayer.release();
        File video = new File(mVideo);
        if (!video.delete()) {
            Log.w(TAG, "Could not delete input video: " + video);
        }

        File audioV = new File(mVideoAudio);
        if (!audioV.delete()) {
            Log.w(TAG, "Could not delete video audio: " + audioV);
        }

        if (!TextUtils.isEmpty(mSong)) {
            File audioS = new File(mSong);
            if (!audioS.delete()) {
                Log.w(TAG, "Could not delete song audio: " + audioS);
            }
        }
    }

    @Override
    public void onIsPlayingChanged(@Nullable EventTime time, boolean playing) {
        if (playing) {
            startAudioPlayerV();
            startAudioPlayerS();
        } else {
            stopAudioPlayerV();
            stopAudioPlayerS();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopVideoPlayer();
    }

    @Override
    public void onPlayerStateChanged(@Nullable EventTime time, boolean ready, int state) {
        if (state == Player.STATE_ENDED) {
            mVideoPlayer.setPlayWhenReady(false);
            mVideoPlayer.seekTo(0);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mModel.ready && mVideoPlayer.getPlaybackState() == Player.STATE_IDLE) {
            startVideoPlayer();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
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

    private void setupViews() {
        mAudioPlayerV = new MediaPlayer();
        try {
            mAudioPlayerV.setDataSource(mVideoAudio);
            mAudioPlayerV.prepare();
        } catch (IOException e) {
            Log.e(TAG, "Media player failed to initialize :?", e);
        }

        if (!TextUtils.isEmpty(mSong)) {
            mAudioPlayerS = new MediaPlayer();
            try {
                mAudioPlayerS.setDataSource(mSong);
                mAudioPlayerS.prepare();
            } catch (IOException e) {
                Log.e(TAG, "Media player failed to initialize :?", e);
            }
        }

        TabLayout tabs = findViewById(R.id.tabs);
        ViewPager2 pager = findViewById(R.id.pager);
        pager.setAdapter(new AdjustAudioPagerAdapter(this));
        new TabLayoutMediator(tabs, pager, (tab, position) -> {
            tab.setText(position == 0 ? R.string.video_label : R.string.song_label);
        }).attach();
        mModel.volumeV.observe(this, volume -> {
            mAudioPlayerV.setVolume(volume / 100, volume / 100);
        });
        mModel.volumeS.observe(this, volume -> {
            if (mAudioPlayerS != null) {
                mAudioPlayerS.setVolume(volume / 100, volume / 100);
            }
        });
        mModel.delayV.observe(this, v -> {
            stopVideoPlayer();
            if (mModel.ready) {
                mHandler.removeCallbacks(mVideoStartRunnable);
                mHandler.postDelayed(mVideoStartRunnable, 250);
            }
        });
        mModel.delayS.observe(this, v -> {
            stopVideoPlayer();
            if (mModel.ready) {
                mHandler.removeCallbacks(mVideoStartRunnable);
                mHandler.postDelayed(mVideoStartRunnable, 250);
            }
        });
    }

    private void startAudioPlayerV() {
        if (mAudioPlayerV == null || mAudioPlayerV.isPlaying()) {
            return;
        }

        //noinspection ConstantConditions
        long delay = Math.round(mModel.delayV.getValue()) - 2500;
        if (delay < 0) {
            mAudioPlayerV.seekTo((int)Math.abs(delay));
            mAudioPlayerV.start();
        } else if (delay > 0) {
            mAudioPlayerV.seekTo(0);
            mHandler.postDelayed(() -> mAudioPlayerV.start(), Math.abs(delay));
        } else {
            mAudioPlayerV.seekTo(0);
            mAudioPlayerV.start();
        }
    }

    private void startAudioPlayerS() {
        if (mAudioPlayerS == null || mAudioPlayerS.isPlaying()) {
            return;
        }

        //noinspection ConstantConditions
        long delay = Math.round(mModel.delayS.getValue()) - 2500;
        if (delay < 0) {
            mAudioPlayerS.seekTo((int)Math.abs(delay));
            mAudioPlayerS.start();
        } else if (delay > 0) {
            mAudioPlayerS.seekTo(0);
            mHandler.postDelayed(() -> mAudioPlayerS.start(), Math.abs(delay));
        } else {
            mAudioPlayerS.seekTo(0);
            mAudioPlayerS.start();
        }
    }

    private void startVideoPlayer() {
        DefaultDataSourceFactory factory =
                new DefaultDataSourceFactory(this, getString(R.string.app_name));
        ProgressiveMediaSource source = new ProgressiveMediaSource.Factory(factory)
                .createMediaSource(Uri.fromFile(new File(mVideo)));
        mVideoPlayer.setPlayWhenReady(true);
        mVideoPlayer.prepare(source, true, true);
    }

    private void stopAudioPlayerV() {
        if (mAudioPlayerV != null && mAudioPlayerV.isPlaying()) {
            mAudioPlayerV.pause();
        }
    }

    private void stopAudioPlayerS() {
        if (mAudioPlayerS != null && mAudioPlayerS.isPlaying()) {
            mAudioPlayerS.pause();
        }
    }

    private void stopVideoPlayer() {
        mModel.window = mVideoPlayer.getCurrentWindowIndex();
        mVideoPlayer.setPlayWhenReady(false);
        mVideoPlayer.stop();
    }

    private void submitForAdjustment() {
        //noinspection ConstantConditions
        long delayV = Math.round(mModel.delayV.getValue());
        File outputV = TempUtil.createNewFile(this, ".mp4");
        Data dataV = new Data.Builder()
                .putString(DelayAudioWorker.KEY_AUDIO, mVideoAudio)
                .putLong(DelayAudioWorker.KEY_DELAY, delayV - 2500)
                .putString(DelayAudioWorker.KEY_OUTPUT, outputV.getAbsolutePath())
                .build();
        OneTimeWorkRequest requestV = new OneTimeWorkRequest.Builder(DelayAudioWorker.class)
                .setInputData(dataV)
                .build();
        //noinspection ConstantConditions
        float volumeV = mModel.volumeV.getValue() / 100;
        //noinspection ConstantConditions
        float volumeS = mModel.volumeS.getValue() / 100;
        File outputA = TempUtil.createNewFile(this, ".aac");
        OneTimeWorkRequest requestS = null, requestA;
        if (TextUtils.isEmpty(mSong)) {
            Data dataA = new Data.Builder()
                    .putString(MergeAudiosWorker.KEY_INPUT_1, outputV.getAbsolutePath())
                    .putFloat(MergeAudiosWorker.KEY_INPUT_1_VOLUME, volumeV)
                    .putString(MergeAudiosWorker.KEY_OUTPUT, outputA.getAbsolutePath())
                    .build();
            requestA = new OneTimeWorkRequest.Builder(MergeAudiosWorker.class)
                    .setInputData(dataA)
                    .build();
        } else {
            //noinspection ConstantConditions
            long delayS = Math.round(mModel.delayS.getValue());
            File outputS = TempUtil.createNewFile(this, ".mp4");
            Data dataS = new Data.Builder()
                    .putString(DelayAudioWorker.KEY_AUDIO, mSong)
                    .putLong(DelayAudioWorker.KEY_DELAY, delayS - 2500)
                    .putString(DelayAudioWorker.KEY_OUTPUT, outputS.getAbsolutePath())
                    .build();
            requestS = new OneTimeWorkRequest.Builder(DelayAudioWorker.class)
                    .setInputData(dataS)
                    .build();
            Data dataA = new Data.Builder()
                    .putString(MergeAudiosWorker.KEY_INPUT_1, outputV.getAbsolutePath())
                    .putFloat(MergeAudiosWorker.KEY_INPUT_1_VOLUME, volumeV)
                    .putString(MergeAudiosWorker.KEY_INPUT_2, outputS.getAbsolutePath())
                    .putFloat(MergeAudiosWorker.KEY_INPUT_2_VOLUME, volumeS)
                    .putString(MergeAudiosWorker.KEY_OUTPUT, outputA.getAbsolutePath())
                    .build();
            requestA = new OneTimeWorkRequest.Builder(MergeAudiosWorker.class)
                    .setInputData(dataA)
                    .build();
        }

        File outputAV = TempUtil.createNewFile(this, ".mp4");
        Data dataAV = new Data.Builder()
                .putString(MergeAudioVideoWorker2.KEY_VIDEO, mVideo)
                .putString(MergeAudioVideoWorker2.KEY_AUDIO, outputA.getAbsolutePath())
                .putString(MergeAudioVideoWorker2.KEY_OUTPUT, outputAV.getAbsolutePath())
                .build();
        OneTimeWorkRequest requestAV = new OneTimeWorkRequest.Builder(MergeAudioVideoWorker2.class)
                .setInputData(dataAV)
                .build();
        WorkManager wm = WorkManager.getInstance(this);
        WorkContinuation work = wm.beginWith(requestV);
        if (requestS != null) {
            work = work.then(requestS);
        }

        work.then(requestA).then(requestAV).enqueue();
        KProgressHUD progress = KProgressHUD.create(this)
                .setStyle(KProgressHUD.Style.SPIN_INDETERMINATE)
                .setLabel(getString(R.string.progress_title))
                .setCancellable(false)
                .show();
        wm.getWorkInfoByIdLiveData(requestAV.getId())
                .observe(this, info -> {
                    boolean ended = info.getState() == WorkInfo.State.CANCELLED
                            || info.getState() == WorkInfo.State.FAILED
                            || info.getState() == WorkInfo.State.SUCCEEDED;
                    if (ended) {
                        progress.dismiss();
                    }

                    if (info.getState() == WorkInfo.State.SUCCEEDED) {
                        setResult(RESULT_OK);
                        proceedToFilter(outputAV);
                    }
                });
    }

    private void submitForSplit() {
        mVideoAudio = TempUtil.createNewFile(this, ".mp4").getAbsolutePath();
        Data data = new Data.Builder()
                .putString(SplitAudioWorker.KEY_INPUT, mVideo)
                .putString(SplitAudioWorker.KEY_OUTPUT, mVideoAudio)
                .build();
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(SplitAudioWorker.class)
                .setInputData(data)
                .build();
        WorkManager wm = WorkManager.getInstance(this);
        wm.enqueue(request);
        KProgressHUD progress = KProgressHUD.create(this)
                .setStyle(KProgressHUD.Style.SPIN_INDETERMINATE)
                .setLabel(getString(R.string.progress_title))
                .setCancellable(false)
                .show();
        wm.getWorkInfoByIdLiveData(request.getId())
                .observe(this, info -> {
                    boolean ended = info.getState() == WorkInfo.State.CANCELLED
                            || info.getState() == WorkInfo.State.FAILED
                            || info.getState() == WorkInfo.State.SUCCEEDED;
                    if (ended) {
                        progress.dismiss();
                    }

                    if (info.getState() == WorkInfo.State.SUCCEEDED) {
                        mModel.ready = true;
                        setupViews();
                    }
                });
    }

    private class AdjustAudioPagerAdapter extends FragmentStateAdapter {

        public AdjustAudioPagerAdapter(@NonNull FragmentActivity fa) {
            super(fa);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            return AdjustAudioFragment.newInstance(
                    position == 0
                            ? AdjustAudioFragment.TARGET_VIDEO
                            : AdjustAudioFragment.TARGET_SONG);
        }

        @Override
        public int getItemCount() {
            return TextUtils.isEmpty(mSong) ? 1 : 2;
        }
    }

    public static class AdjustAudioActivityViewModel extends ViewModel {

        public MutableLiveData<Float> delayV = new MutableLiveData<>(2500f);
        public MutableLiveData<Float> delayS = new MutableLiveData<>(2500f);

        public MutableLiveData<Float> volumeV = new MutableLiveData<>(100f);
        public MutableLiveData<Float> volumeS = new MutableLiveData<>(100f);

        public boolean ready;
        public int window;
    }
}
