package com.ctg.coptok.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.kaopiz.kprogresshud.KProgressHUD;

import io.agora.rtc.Constants;
import io.agora.rtc.IRtcEngineEventHandler;
import io.agora.rtc.RtcEngine;
import io.agora.rtc.models.ClientRoleOptions;
import io.agora.rtc.video.VideoCanvas;
import io.agora.rtm.ErrorInfo;
import io.agora.rtm.ResultCallback;
import io.agora.rtm.RtmChannel;
import io.agora.rtm.RtmChannelListener;
import io.agora.rtm.RtmChannelMember;
import io.agora.rtm.RtmClient;
import io.agora.rtm.RtmClientListener;
import io.agora.rtm.RtmMessage;
import com.ctg.coptok.MainApplication;
import com.ctg.coptok.R;
import com.ctg.coptok.SharedConstants;
import com.ctg.coptok.agora.DefaultRtmChannelListener;
import com.ctg.coptok.agora.DefaultRtmClientListener;
import com.ctg.coptok.data.api.REST;
import com.ctg.coptok.data.models.LiveStream;
import com.ctg.coptok.data.models.LiveStreamAgora;
import com.ctg.coptok.data.models.User;
import pub.devrel.easypermissions.AfterPermissionGranted;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LiveStreamAgoraActivity extends LiveStreamBaseActivity {

    private static final String TAG = "LiveStreamAgoraActivity";

    private final IRtcEngineEventHandler mRtcEngineEventHandler = new IRtcEngineEventHandler() {

        @Override
        public void onRemoteVideoStateChanged(int uid, int state, int reason, int elapsed) {
            runOnUiThread(() -> onRtcRemoteVideoStateChanged(uid, state, reason, elapsed));
        }

        @Override
        public void onUserOffline(int uid, int reason) {
            runOnUiThread(() -> onRtcUserOffline(uid, reason));
        }

        @Override
        public void onUserJoined(int uid, int elapsed) {
            runOnUiThread(() -> onRtcUserJoined(uid, elapsed));
        }
    };
    private RtcEngine mRtcEngine;
    private RtmChannel mRtmChannel;
    private final RtmChannelListener mRtmChannelListener = new DefaultRtmChannelListener() {

        @Override
        public void onMemberCountUpdated(int count) {
            runOnUiThread(() -> onRtmMemberCountUpdated(count));
        }

        @Override
        public void onMemberJoined(RtmChannelMember member) {
            runOnUiThread(() -> onRtmMemberJoined(member));
        }

        @Override
        public void onMemberLeft(RtmChannelMember member) {
            runOnUiThread(() -> onRtmMemberLeft(member));
        }

        @Override
        public void onMessageReceived(RtmMessage message, RtmChannelMember member) {
            runOnUiThread(() -> onRtmMessageReceived(message, member));
        }
    };
    private RtmClient mRtmClient;
    private final RtmClientListener mRtmClientListener = new DefaultRtmClientListener() {

        @Override
        public void onConnectionStateChanged(int state, int reason) {
            runOnUiThread(() -> onRtmConnectionStateChanged(state, reason));
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mRtcEngine != null) {
            try {
                mRtcEngine.disableVideo();
            } catch (Exception ignore) {
            }

            mRtcEngine.leaveChannel();
            RtcEngine.destroy();
            mRtcEngine = null;
        }

        if (mRtmClient != null) {
            mRtmClient.logout(null);
            mRtmClient = null;
        }

        if (mRtmChannel != null) {
            mRtmChannel.leave(null);
            mRtmChannel = null;
        }
    }

    private void onRtcRemoteVideoStateChanged(int uid, int state, int reason, int elapsed) {
        Log.v(TAG, "Remote video state for user with ID " + uid + " is " + state + ".");
        if (state == Constants.REMOTE_VIDEO_STATE_DECODING) {
            toggleVideoVisibility(true);
        }
    }

    private void onRtcUserOffline(int uid, int reason) {
        Log.v(TAG, "User with ID " + uid + " went offline for " + reason + " reason.");
    }

    private void onRtcUserJoined(int uid, int elapsed) {
        Log.v(TAG, "Host user with ID " + uid + " has joined channel.");
        setupRemoteVideo(uid);
    }

    private void onRtmConnectionStateChanged(int state, int reason) {
        Log.v(TAG, "RTM connection state is: " + state + "; reason: " + reason);
    }

    private void onRtmMemberCountUpdated(int count) {
        Log.v(TAG, "RTM member count is now " + count + ".");
        updateViewerCount(count);
    }

    private void onRtmMemberJoined(RtmChannelMember member) {
        Log.v(TAG, "RTM member " + member.getUserId() + " went joined.");
        try {
            userJoined(Integer.parseInt(member.getUserId()));
        } catch (NumberFormatException ignore) {
        }
    }

    private void onRtmMemberLeft(RtmChannelMember member) {
        Log.v(TAG, "RTM member " + member.getUserId() + " went left.");
        try {
            userLeft(Integer.parseInt(member.getUserId()));
        } catch (NumberFormatException ignore) {
        }
    }

    private void onRtmMessageReceived(RtmMessage message, RtmChannelMember member) {
        Log.v(TAG, "RTM message received from " + member.getUserId() + ".");
        try {
            userSentMessage(Integer.parseInt(member.getUserId()), message.getText());
        } catch (NumberFormatException ignore) {
        }
    }

    @Override
    @AfterPermissionGranted(SharedConstants.REQUEST_CODE_PERMISSIONS_LIVE_STREAM)
    protected void joinLiveStream() {
        LiveStream liveStream = mModel.liveStream.getValue();
        User user = mModel.user.getValue();
        KProgressHUD progress = KProgressHUD.create(this)
                .setStyle(KProgressHUD.Style.SPIN_INDETERMINATE)
                .setLabel(getString(R.string.progress_title))
                .setCancellable(false)
                .show();
        REST rest = MainApplication.getContainer().get(REST.class);
        //noinspection ConstantConditions
        rest.liveStreamsJoinAgora(liveStream.id, 0)
                .enqueue(new Callback<LiveStreamAgora>() {

                    @Override
                    public void onResponse(
                            @Nullable Call<LiveStreamAgora> call,
                            @Nullable Response<LiveStreamAgora> response
                    ) {
                        progress.dismiss();
                        if (response != null && response.isSuccessful()) {
                            LiveStreamAgora liveStreamAgora = response.body();
                            //noinspection ConstantConditions
                            Log.v(TAG, "Received RTC token from server: " + liveStreamAgora.tokenRtc);
                            Log.v(TAG, "Received RTM token from server: " + liveStreamAgora.tokenRtm);
                            setupRtcEngine(user, liveStream, liveStreamAgora);
                            if (user != null) {
                                setupRtmClient(user, liveStream, liveStreamAgora);
                            }
                        } else {
                            Toast.makeText(LiveStreamAgoraActivity.this, R.string.error_server, Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    }

                    @Override
                    public void onFailure(
                            @Nullable Call<LiveStreamAgora> call,
                            @Nullable Throwable t
                    ) {
                        Log.e(TAG, "Failed when trying to create agora token.", t);
                        progress.dismiss();
                        Toast.makeText(LiveStreamAgoraActivity.this, R.string.error_internet, Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
    }

    @Override
    protected void sendMessage(String text) {
        if (mRtmClient != null && mRtmChannel != null) {
            KProgressHUD progress = KProgressHUD.create(this)
                    .setStyle(KProgressHUD.Style.SPIN_INDETERMINATE)
                    .setLabel(getString(R.string.progress_title))
                    .setCancellable(false)
                    .show();
            RtmMessage message = mRtmClient.createMessage();
            message.setText(text);
            mRtmChannel.sendMessage(message, new ResultCallback<Void>() {

                @Override
                public void onSuccess(Void unused) {
                    Log.d(TAG, "Successfully send message in RTM channel " + mRtmChannel.getId() + ".");
                    progress.dismiss();
                    User user = mModel.user.getValue();
                    //noinspection ConstantConditions
                    userSentMessage(user.id, message.getText());
                }

                @Override
                public void onFailure(ErrorInfo errorInfo) {
                    Log.w(TAG, "Could not send message (" + errorInfo.getErrorCode() + "): " + errorInfo.getErrorDescription());
                    progress.dismiss();
                }
            });
        }
    }

    private void setupRtcEngine(
            @Nullable User user,
            @NonNull LiveStream liveStream,
            @NonNull LiveStreamAgora liveStreamAgora
    ) {
        String appId = getString(R.string.agora_app_id);
        try {
            mRtcEngine = RtcEngine.create(getBaseContext(), appId, mRtcEngineEventHandler);
        } catch (Exception e) {
            Log.e(TAG, "Failed to create RTC engine instance.", e);
            finish();
            return;
        }

        mRtcEngine.setChannelProfile(Constants.CHANNEL_PROFILE_LIVE_BROADCASTING);
        boolean publisher = user != null && user.id == liveStream.user.id;
        if (publisher) {
            mRtcEngine.setClientRole(Constants.CLIENT_ROLE_BROADCASTER);
        } else {
            ClientRoleOptions options = new ClientRoleOptions();
            options.audienceLatencyLevel = Constants.AUDIENCE_LATENCY_LEVEL_LOW_LATENCY;
            mRtcEngine.setClientRole(Constants.CLIENT_ROLE_AUDIENCE, options);
        }

        mRtcEngine.enableVideo();
        if (publisher) {
            FrameLayout container = findViewById(R.id.video_view_container);
            SurfaceView surfaceView = RtcEngine.CreateRendererView(getBaseContext());
            container.addView(surfaceView);
            mRtcEngine.setupLocalVideo(new VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_FILL, 0));
        } else {
            toggleVideoVisibility(false);
        }

        if (user != null) {
            mRtcEngine.joinChannelWithUserAccount(liveStreamAgora.tokenRtc, liveStreamAgora.channel, "" + user.id);
        } else {
            mRtcEngine.joinChannel(liveStreamAgora.tokenRtc, liveStreamAgora.channel, "", 0);
        }
    }

    private void setupRtmClient(
            @NonNull User user,
            @NonNull LiveStream liveStream,
            @NonNull LiveStreamAgora liveStreamAgora
    ) {
        String appId = getString(R.string.agora_app_id);
        try {
            mRtmClient = RtmClient.createInstance(getBaseContext(), appId, mRtmClientListener);
        } catch (Exception e) {
            Log.e(TAG, "Failed to create RTM client instance.", e);
            finish();
            return;
        }
        mRtmClient.login(liveStreamAgora.tokenRtm, "" + user.id, new ResultCallback<Void>() {

            @Override
            public void onSuccess(Void responseInfo) {
                Log.d(TAG, "Successfully logged in to RTM client.");
                mRtmChannel = mRtmClient.createChannel(liveStreamAgora.channel, mRtmChannelListener);
                mRtmChannel.join(new ResultCallback<Void>() {

                    @Override
                    public void onSuccess(Void responseInfo) {
                        Log.d(TAG, "Successfully joined RTM channel " + liveStreamAgora.channel);
                    }

                    @Override
                    public void onFailure(ErrorInfo errorInfo) {
                        Log.w(TAG, "Could not join RTM channel (" + errorInfo.getErrorCode() + "): " + errorInfo.getErrorDescription());
                    }
                });
            }

            @Override
            public void onFailure(ErrorInfo errorInfo) {
                Log.w(TAG, "Could not login to RTM client (" + errorInfo.getErrorCode() + "): " + errorInfo.getErrorDescription());
            }
        });
    }

    private void setupRemoteVideo(int uid) {
        Log.d(TAG, "Setting up remote video from " + uid);
        FrameLayout container = findViewById(R.id.video_view_container);
        SurfaceView surfaceView = RtcEngine.CreateRendererView(getBaseContext());
        container.addView(surfaceView);
        surfaceView.setZOrderMediaOverlay(true);
        mRtcEngine.setupRemoteVideo(new VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_FILL, uid));
    }
}
