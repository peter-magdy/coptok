package com.ctg.coptok.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.facebook.drawee.view.SimpleDraweeView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.kaopiz.kprogresshud.KProgressHUD;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import jp.wasabeef.recyclerview.adapters.SlideInLeftAnimationAdapter;
import com.ctg.coptok.MainApplication;
import com.ctg.coptok.R;
import com.ctg.coptok.SharedConstants;
import com.ctg.coptok.data.api.REST;
import com.ctg.coptok.data.models.LiveStream;
import com.ctg.coptok.data.models.User;
import com.ctg.coptok.data.models.Wrappers;
import com.ctg.coptok.events.LiveStreamStartStopEvent;
import com.ctg.coptok.utils.LocaleUtil;
import com.ctg.coptok.utils.SocialSpanUtil;
import com.ctg.coptok.utils.TextFormatUtil;
import me.everything.android.ui.overscroll.OverScrollDecoratorHelper;
import okhttp3.ResponseBody;
import pub.devrel.easypermissions.EasyPermissions;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

abstract public class LiveStreamBaseActivity extends AppCompatActivity {

    public static final String EXTRA_LIVE_STREAM = "live_stream";
    public static final String EXTRA_USER = "user";

    private static final String TAG = "LiveStreamBaseActivity";

    private Call<Wrappers.Single<LiveStream>> mCall;
    private CommentsAdapter mCommentsAdapter;
    protected LiveStreamBaseActivityViewModel mModel;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleUtil.wrap(base));
    }

    @Override
    public void onBackPressed() {
        LiveStream liveStream = mModel.liveStream.getValue();
        User user = mModel.user.getValue();
        if (liveStream != null && user != null && liveStream.user.id == user.id) {
            confirmClose();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_live_stream);
        mModel = new ViewModelProvider(this).get(LiveStreamBaseActivityViewModel.class);
        LiveStream liveStream = getIntent().getParcelableExtra(EXTRA_LIVE_STREAM);
        if (liveStream != null) {
            mModel.liveStream.setValue(liveStream);
        }
        User user = getIntent().getParcelableExtra(EXTRA_USER);
        if (user != null) {
            mModel.user.setValue(user);
        }
        liveStream = mModel.liveStream.getValue();
        if (liveStream == null) {
            finish();
            return;
        }
        user = mModel.user.getValue();
        setupViews(liveStream, user);
        if (user != null && liveStream.user.id == user.id) {
            String[] permissions = new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO,
            };
            if (EasyPermissions.hasPermissions(this, permissions)) {
                joinLiveStream();
            } else {
                EasyPermissions.requestPermissions(
                        this,
                        getString(R.string.permission_rationale_live_stream),
                        SharedConstants.REQUEST_CODE_PERMISSIONS_LIVE_STREAM,
                        permissions);
            }
        } else {
            joinLiveStream();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onLiveStreamStartStopEvent(LiveStreamStartStopEvent event) {
        LiveStream liveStream = mModel.liveStream.getValue();
        if (liveStream != null && liveStream.id == event.id) {
            Toast.makeText(this, R.string.live_stream_ended_message, Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        LiveStream liveStream = mModel.liveStream.getValue();
        if (liveStream != null && !mModel.viewed) {
            recordView(liveStream);
            mModel.viewed = true;
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    private void confirmClose() {
        new MaterialAlertDialogBuilder(this)
                .setMessage(R.string.live_stream_close_confirmation)
                .setNegativeButton(R.string.cancel_button, (dialog, i) -> dialog.cancel())
                .setPositiveButton(R.string.close_button, (dialog, i) -> {
                    dialog.dismiss();
                    deleteLiveStream();
                })
                .show();
    }

    private void deleteLiveStream() {
        KProgressHUD progress = KProgressHUD.create(this)
                .setStyle(KProgressHUD.Style.SPIN_INDETERMINATE)
                .setLabel(getString(R.string.progress_title))
                .setCancellable(false)
                .show();
        REST rest = MainApplication.getContainer().get(REST.class);
        LiveStream liveStream = mModel.liveStream.getValue();
        //noinspection ConstantConditions
        rest.liveStreamsDelete(liveStream.id)
                .enqueue(new Callback<ResponseBody>() {

                    @Override
                    public void onResponse(
                            @Nullable Call<ResponseBody> call,
                            @Nullable Response<ResponseBody> response
                    ) {
                        progress.dismiss();
                        finish();
                    }

                    @Override
                    public void onFailure(
                            @Nullable Call<ResponseBody> call,
                            @Nullable Throwable t
                    ) {
                        Log.e(TAG, "Failed when trying to delete live stream.", t);
                        progress.dismiss();
                    }
                });
    }

    protected void fetchUserAndRun(int id, WithUser callback) {
        REST rest = MainApplication.getContainer().get(REST.class);
        rest.usersShow(id)
                .enqueue(new Callback<Wrappers.Single<User>>() {
                    @Override
                    public void onResponse(
                            @Nullable Call<Wrappers.Single<User>> call,
                            @Nullable Response<Wrappers.Single<User>> response
                    ) {
                        int code = response != null ? response.code() : -1;
                        Log.d(TAG, "Fetching user details returned " + code + " code.");
                        if (code == 200) {
                            //noinspection ConstantConditions
                            callback.run(response.body().data);
                        }
                    }

                    @Override
                    public void onFailure(
                            @Nullable Call<Wrappers.Single<User>> call,
                            @Nullable Throwable t
                    ) {
                        Log.e(TAG, "Failed to fetch user details for ID " + id + ".", t);
                    }
                });
    }

    abstract protected void joinLiveStream();

    private void recordView(LiveStream liveStream) {
        if (mCall != null) {
            mCall.cancel();
        }

        REST rest = MainApplication.getContainer().get(REST.class);
        mCall = rest.liveStreamsTouch(liveStream.id);
        mCall.enqueue(new Callback<Wrappers.Single<LiveStream>>() {

            @Override
            public void onResponse(
                    @Nullable Call<Wrappers.Single<LiveStream>> call,
                    @Nullable Response<Wrappers.Single<LiveStream>> response
            ) {
                int code = response != null ? response.code() : -1;
                Log.v(TAG, "Recording live stream view on server returned " + code + '.');
            }

            @Override
            public void onFailure(
                    @Nullable Call<Wrappers.Single<LiveStream>> call,
                    @Nullable Throwable t
            ) {
                Log.e(TAG, "Failed when record live stream view on server.", t);
            }
        });
    }

    abstract protected void sendMessage(String text);

    @SuppressLint("SetTextI18n")
    private void setupViews(final LiveStream liveStream, @Nullable final User user) {
        SimpleDraweeView photo = findViewById(R.id.photo);
        photo.setOnClickListener(v -> showProfile(liveStream.user.id));
        if (TextUtils.isEmpty(liveStream.user.photo)) {
            photo.setActualImageResource(R.drawable.photo_placeholder);
        } else {
            photo.setImageURI(liveStream.user.photo);
        }

        TextView username = findViewById(R.id.username);
        username.setOnClickListener(v -> showProfile(liveStream.user.id));
        username.setText('@' + liveStream.user.username);
        findViewById(R.id.verified)
                .setVisibility(liveStream.user.verified ? View.VISIBLE : View.GONE);
        View close = findViewById(R.id.close);
        close.setOnClickListener(v -> {
            if (user != null && liveStream.user.id == user.id) {
                confirmClose();
            } else {
                finish();
            }
        });
        View composer = findViewById(R.id.composer);
        EditText input = composer.findViewById(R.id.input);
        composer.findViewById(R.id.submit)
                .setOnClickListener(v -> {
                    if (user == null) {
                        Toast.makeText(this, R.string.login_required_message, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Editable message = input.getText();
                    if (TextUtils.isEmpty(message)) {
                        return;
                    }

                    sendMessage(message.toString());
                    input.setText(null);
                });
        RecyclerView comments = findViewById(R.id.comments);
        mCommentsAdapter = new CommentsAdapter();
        mCommentsAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {

            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                comments.smoothScrollToPosition(0);
            }
        });
        comments.setAdapter(new SlideInLeftAnimationAdapter(mCommentsAdapter));
        OverScrollDecoratorHelper.setUpOverScroll(
                comments, OverScrollDecoratorHelper.ORIENTATION_VERTICAL);
    }

    protected void showProfile(int id) {
    }

    protected void toggleVideoVisibility(boolean show) {
        findViewById(R.id.video_view_container)
                .setVisibility(show ? View.VISIBLE : View.GONE);
        findViewById(R.id.video_view_placeholder)
                .setVisibility(show ? View.GONE : View.VISIBLE);
    }

    protected void updateViewerCount(int count) {
        TextView viewers = findViewById(R.id.viewers);
        viewers.setText(TextFormatUtil.toShortNumber(count));
    }

    protected void userJoined(int id) {
        fetchUserAndRun(id, user -> {
            Log.v(TAG, "User @" + user.username + " joined.");
            mModel.userComments.add(0, new UserComment(user, getString(R.string.live_stream_user_joined)));
            mCommentsAdapter.notifyItemInserted(0);
        });
    }

    protected void userLeft(int id) {
        fetchUserAndRun(id, user -> {
            Log.v(TAG, "User @" + user.username + " left.");
            mModel.userComments.add(0, new UserComment(user, getString(R.string.live_stream_user_left)));
            mCommentsAdapter.notifyItemInserted(0);
        });
    }

    protected void userSentMessage(int id, String message) {
        fetchUserAndRun(id, user -> {
            Log.v(TAG, "User @" + user.username + " commented:\n" + message);
            mModel.userComments.add(0, new UserComment(user, message));
            mCommentsAdapter.notifyItemInserted(0);
        });
    }

    private class CommentsAdapter extends RecyclerView.Adapter<CommentViewHolder> {

        @Override
        @SuppressLint("SetTextI18n")
        public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
            UserComment comment = mModel.userComments.get(position);
            if (TextUtils.isEmpty(comment.user.photo)) {
                holder.photo.setActualImageResource(R.drawable.photo_placeholder);
            } else {
                holder.photo.setImageURI(comment.user.photo);
            }
            holder.username.setText('@' + comment.user.username);
            holder.verified.setVisibility(comment.user.verified ? View.VISIBLE : View.GONE);
            SocialSpanUtil.apply(holder.text, comment.text, null);
            holder.when.setText(
                    DateUtils.getRelativeTimeSpanString(
                            getApplicationContext(), comment.createdAt.getTime(), true));
            holder.photo.setOnClickListener(v -> showProfile(comment.user.id));
            holder.username.setOnClickListener(v -> showProfile(comment.user.id));
        }

        @Override
        public int getItemCount() {
            return mModel.userComments.size();
        }

        @NonNull
        @Override
        public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_comment_alt, parent, false);
            return new CommentViewHolder(view);
        }
    }

    private static class CommentViewHolder extends RecyclerView.ViewHolder {

        public SimpleDraweeView photo;
        public TextView username;
        public ImageView verified;
        public TextView text;
        public TextView when;

        public CommentViewHolder(@NonNull View root) {
            super(root);
            username = root.findViewById(R.id.username);
            verified = root.findViewById(R.id.verified);
            photo = root.findViewById(R.id.photo);
            text = root.findViewById(R.id.text);
            when = root.findViewById(R.id.when);
        }
    }

    private static class UserComment {

        public final User user;
        public final String text;
        public final Date createdAt = new Date();

        public UserComment(User user, String comment) {
            this.user = user;
            this.text = comment;
        }
    }

    public static class LiveStreamBaseActivityViewModel extends ViewModel {

        public final MutableLiveData<LiveStream> liveStream = new MutableLiveData<>();
        public final MutableLiveData<User> user = new MutableLiveData<>();
        public final List<UserComment> userComments = new ArrayList<>();
        public boolean viewed;
    }

    private interface WithUser {

        void run(@NonNull User user);
    }
}
