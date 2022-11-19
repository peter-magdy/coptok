package com.ctg.coptok.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ShareCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.widget.ImageViewCompat;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavBackStackEntry;
import androidx.navigation.NavController;
import androidx.navigation.NavDirections;
import androidx.navigation.fragment.NavHostFragment;
import androidx.paging.LivePagedListBuilder;
import androidx.paging.PagedList;
import androidx.paging.PagedListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.install.InstallStateUpdatedListener;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.InstallStatus;
import com.google.android.play.core.install.model.UpdateAvailability;
import com.google.android.play.core.review.ReviewInfo;
import com.google.android.play.core.review.ReviewManager;
import com.google.android.play.core.review.ReviewManagerFactory;
import com.google.firebase.dynamiclinks.DynamicLink;
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks;
import com.google.firebase.dynamiclinks.ShortDynamicLink;
import com.google.firebase.messaging.FirebaseMessaging;
import com.kaopiz.kprogresshud.KProgressHUD;
import com.pixplicity.easyprefs.library.Prefs;
import com.thefinestartist.finestwebview.FinestWebView;

import org.json.JSONArray;
import org.json.JSONObject;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.ctg.coptok.BuildConfig;
import com.ctg.coptok.MainApplication;
import com.ctg.coptok.MainNavigationDirections;
import com.ctg.coptok.R;
import com.ctg.coptok.SharedConstants;
import com.ctg.coptok.ads.BannerAdProvider;
import com.ctg.coptok.common.DiffUtilCallback;
import com.ctg.coptok.common.LoadingState;
import com.ctg.coptok.common.SharingTarget;
import com.ctg.coptok.data.ItemDataSource;
import com.ctg.coptok.data.api.REST;
import com.ctg.coptok.data.dbs.ClientDatabase;
import com.ctg.coptok.data.models.Advertisement;
import com.ctg.coptok.data.models.Balance;
import com.ctg.coptok.data.models.Clip;
import com.ctg.coptok.data.models.Item;
import com.ctg.coptok.data.models.Promotion;
import com.ctg.coptok.data.models.Token;
import com.ctg.coptok.data.models.UnreadNotifications;
import com.ctg.coptok.data.models.User;
import com.ctg.coptok.data.models.Wrappers;
import com.ctg.coptok.events.RecordingOptionsEvent;
import com.ctg.coptok.events.ResolveRecordingOptionsEvent;
import com.ctg.coptok.fragments.PromotionsDialogFragment;
import com.ctg.coptok.utils.AdsUtil;
import com.ctg.coptok.utils.IntentUtil;
import com.ctg.coptok.utils.LocaleUtil;
import com.ctg.coptok.utils.PackageUtil;
import com.ctg.coptok.utils.TempUtil;
import com.ctg.coptok.utils.VideoUtil;
import com.ctg.coptok.workers.DeviceTokenWorker;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private static final String EXTRA_REGISTERED = "registered";
    public static final String EXTRA_USER = "user";
    private static final String QUERY_REFERRER = "referrer";
    private static final String TAG = "MainActivity";

    private BannerAdProvider mAd;
    private CallbackManager mCallbackManager;
    private boolean mExitRequested;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private MainActivityViewModel mModel;
    private GoogleSignInClient mSignInClient;
    private AppUpdateManager mUpdateManager;
    private final InstallStateUpdatedListener mUpdateListener = state -> {
        if (state.installStatus() == InstallStatus.DOWNLOADED) {
            mUpdateManager.completeUpdate();
        }
    };

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleUtil.wrap(base));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        Log.v(TAG, "Received request: " + requestCode + ", result: " + resultCode + ".");
        if (requestCode == SharedConstants.REQUEST_CODE_LOGIN_GOOGLE && resultCode == RESULT_OK && data != null) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                loginWithGoogle(task.getResult(ApiException.class));
            } catch (ApiException e) {
                Log.e(TAG, "Unable to login with Google account.", e);
            }
        } else if (requestCode == SharedConstants.REQUEST_CODE_LOGIN_PHONE && resultCode == RESULT_OK && data != null) {
            Token token = data.getParcelableExtra(PhoneLoginBaseActivity.EXTRA_TOKEN);
            updateLoginState(token);
        } else if (requestCode == SharedConstants.REQUEST_CODE_LOGIN_EMAIL && resultCode == RESULT_OK && data != null) {
            Token token = data.getParcelableExtra(EmailLoginActivity.EXTRA_TOKEN);
            updateLoginState(token);
        } else if (requestCode == SharedConstants.REQUEST_CODE_PICK_VIDEO && resultCode == RESULT_OK && data != null) {
            proceedToTrimmer(data.getData());
        }

        mCallbackManager.onActivityResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onBackPressed() {
        NavController controller = findNavController();
        if (controller.popBackStack()) {
            return;
        }
        int behavior = getResources().getInteger(R.integer.exit_behavior);
        switch (behavior) {
            case 1:
                if (mExitRequested) {
                    super.onBackPressed();
                    break;
                }
                Toast.makeText(this, R.string.exit_toast_message, Toast.LENGTH_SHORT).show();
                mExitRequested = true;
                mHandler.postDelayed(() -> mExitRequested = false, TimeUnit.SECONDS.toMillis(2));
                break;
            case 2:
                new MaterialAlertDialogBuilder(this)
                        .setMessage(R.string.confirmation_close_app)
                        .setNegativeButton(R.string.cancel_button, (dialog, i) -> dialog.cancel())
                        .setPositiveButton(R.string.close_button, (dialog, i) -> {
                            dialog.dismiss();
                            finish();
                        })
                        .show();
                break;
            default:
                super.onBackPressed();
                break;
        }
    }

    @Override
    @SuppressLint({"NonConstantResourceId", "SetTextI18n"})
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Advertisement ad = AdsUtil.findByLocationAndType("login", "banner");
        if (ad != null) {
            mAd = new BannerAdProvider(ad);
        } else {
            Log.w(TAG, "No ad configured for login page.");
        }

        mUpdateManager = AppUpdateManagerFactory.create(this);
        mModel = new ViewModelProvider(this).get(MainActivityViewModel.class);
        Uri uri = getIntent().getData();
        if (uri != null && uri.getBooleanQueryParameter(QUERY_REFERRER, false)) {
            mModel.referrer = uri.getQueryParameter(QUERY_REFERRER);
        }

        Log.v(TAG, "Received URI in intent is: " + uri + "; referrer: " + mModel.referrer);
        User user = getIntent().getParcelableExtra(EXTRA_USER);
        if (user != null) {
            mModel.user.postValue(user);
            mModel.state1.postValue(LoadingState.LOADED);
        }

        View content = findViewById(R.id.content);
        View loading = findViewById(R.id.loading2);
        mModel.state1.observe(this, state -> {
            content.setVisibility(state == LoadingState.LOADED ? View.VISIBLE : View.GONE);
            loading.setVisibility(state == LoadingState.LOADING ? View.VISIBLE : View.GONE);
        });
        TextView badge = findViewById(R.id.notification_badge);
        mModel.notifications.observe(this, count -> {
            if (count > 99) {
                count = 99;
            }

            if (count > 0) {
                badge.setText(count < 10 ? (count + "+") : String.valueOf(count));
                badge.setVisibility(View.VISIBLE);
            } else {
                badge.setVisibility(View.GONE);
            }
        });
        mModel.user.observe(this, u -> {
            if (u != null) {
                Prefs.putBoolean(SharedConstants.PREF_BUSINESS_USER, u.business);
                refreshUnreadCount();
                boolean registered = getIntent().getBooleanExtra(EXTRA_REGISTERED, false);
                if (registered && getResources().getBoolean(R.bool.profile_edit_after_registration)) {
                    findNavController().navigate(MainNavigationDirections.actionShowEditProfile());
                }
                if (registered && !getResources().getBoolean(R.bool.skip_suggestions_screen)) {
                    startActivity(new Intent(this, SuggestionsActivity.class));
                }
            }
        });
        int launches = Prefs.getInt(SharedConstants.PREF_LAUNCH_COUNT, 0);
        Prefs.putInt(SharedConstants.PREF_LAUNCH_COUNT, ++launches);
        View sheet1 = findViewById(R.id.login_sheet);
        BottomSheetBehavior<View> bsb = BottomSheetBehavior.from(sheet1);
        ImageButton close = sheet1.findViewById(R.id.header_back);
        close.setImageResource(R.drawable.ic_baseline_close_24);
        close.setOnClickListener(view -> bsb.setState(BottomSheetBehavior.STATE_COLLAPSED));
        TextView title = sheet1.findViewById(R.id.header_title);
        title.setText(R.string.login_label);
        sheet1.findViewById(R.id.header_more).setVisibility(View.GONE);
        String pp = getString(R.string.privacy_policy);
        String tou = getString(R.string.term_of_use);
        String text = getString(R.string.privacy_terms_description, pp, tou);
        SpannableString spanned = new SpannableString(text);
        spanned.setSpan(
                createSpan(getString(R.string.link_privacy_policy)),
                text.indexOf(pp),
                text.indexOf(pp) + pp.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );
        spanned.setSpan(
                createSpan(getString(R.string.link_terms_of_use)),
                text.indexOf(tou),
                text.indexOf(tou) + tou.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );
        TextView terms1 = sheet1.findViewById(R.id.terms_notice);
        terms1.setMovementMethod(LinkMovementMethod.getInstance());
        terms1.setText(spanned, TextView.BufferType.SPANNABLE);
        CheckBox terms2 = sheet1.findViewById(R.id.terms_checkbox);
        terms2.setMovementMethod(LinkMovementMethod.getInstance());
        terms2.setText(spanned, TextView.BufferType.SPANNABLE);
        boolean acceptance = getResources().getBoolean(R.bool.explicit_acceptance_required);
        if (acceptance) {
            terms1.setVisibility(View.GONE);
            terms2.setVisibility(View.VISIBLE);
        } else {
            terms1.setVisibility(View.VISIBLE);
            terms2.setVisibility(View.GONE);
        }

        mCallbackManager = CallbackManager.Factory.create();
        LoginManager.getInstance()
                .registerCallback(mCallbackManager, new FacebookCallback<LoginResult>() {

                    @Override
                    public void onCancel() {
                        Log.w(TAG, "Login with Facebook was cancelled.");
                    }

                    @Override
                    public void onError(FacebookException error) {
                        Log.e(TAG, "Login with Facebook returned error.", error);
                        Toast.makeText(MainActivity.this, R.string.error_internet, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onSuccess(LoginResult result) {
                        loginWithFacebook(result);
                    }
                });
        View facebook = sheet1.findViewById(R.id.facebook);
        facebook.setOnClickListener(view -> {
            if (acceptance && !terms2.isChecked()) {
                Toast.makeText(this, R.string.message_accept_policies, Toast.LENGTH_SHORT).show();
                return;
            }

            bsb.setState(BottomSheetBehavior.STATE_COLLAPSED);
            LoginManager lm = LoginManager.getInstance();
            try {
                lm.logOut();
            } catch (Exception ignore) {
            }

            lm.logInWithReadPermissions(
                    MainActivity.this, Collections.singletonList("email"));
        });
        if (!getResources().getBoolean(R.bool.facebook_login_enabled)) {
            facebook.setVisibility(View.GONE);
        }

        GoogleSignInOptions options =
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestEmail()
                        .requestIdToken(getString(R.string.google_client_id))
                        .requestProfile()
                        .build();
        mSignInClient = GoogleSignIn.getClient(this, options);
        View google = sheet1.findViewById(R.id.google);
        google.setOnClickListener(view -> {
            if (acceptance && !terms2.isChecked()) {
                Toast.makeText(this, R.string.message_accept_policies, Toast.LENGTH_SHORT).show();
                return;
            }

            bsb.setState(BottomSheetBehavior.STATE_COLLAPSED);
            startActivityForResult(
                    mSignInClient.getSignInIntent(), SharedConstants.REQUEST_CODE_LOGIN_GOOGLE);
        });
        if (!getResources().getBoolean(R.bool.google_login_enabled)) {
            google.setVisibility(View.GONE);
        }

        View phone = sheet1.findViewById(R.id.phone);
        phone.setOnClickListener(view -> {
            if (acceptance && !terms2.isChecked()) {
                Toast.makeText(this, R.string.message_accept_policies, Toast.LENGTH_SHORT).show();
                return;
            }

            bsb.setState(BottomSheetBehavior.STATE_COLLAPSED);
            Intent intent;
            if (TextUtils.equals(getString(R.string.sms_login_service), "firebase")) {
                intent = new Intent(this, PhoneLoginFirebaseActivity.class);
            } else {
                intent = new Intent(this, PhoneLoginServerActivity.class);
            }

            intent.putExtra(PhoneLoginBaseActivity.EXTRA_REFERRER, mModel.referrer);
            startActivityForResult(intent, SharedConstants.REQUEST_CODE_LOGIN_PHONE);
        });
        if (!getResources().getBoolean(R.bool.sms_login_enabled)) {
            phone.setVisibility(View.GONE);
        }

        View email = sheet1.findViewById(R.id.email);
        email.setOnClickListener(view -> {
            if (acceptance && !terms2.isChecked()) {
                Toast.makeText(this, R.string.message_accept_policies, Toast.LENGTH_SHORT).show();
                return;
            }

            bsb.setState(BottomSheetBehavior.STATE_COLLAPSED);
            startActivityForResult(
                    new Intent(this, EmailLoginActivity.class),
                    SharedConstants.REQUEST_CODE_LOGIN_EMAIL
            );
        });
        if (!getResources().getBoolean(R.bool.email_login_enabled)) {
            email.setVisibility(View.GONE);
        }

        NavController controller = findNavController();
        ImageButton clips = findViewById(R.id.clips);
        clips.setOnClickListener(v ->
                controller.navigate(MainNavigationDirections.actionShowClips()));
        ImageButton discover = findViewById(R.id.discover);
        discover.setOnClickListener(v ->
                controller.navigate(MainNavigationDirections.actionShowDiscover()));
        findViewById(R.id.record).setOnClickListener(v -> {
            if (mModel.isLoggedIn()) {
                if (getResources().getBoolean(R.bool.skip_recording_options)) {
                    String[] permissions = new String[]{
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    };
                    if (EasyPermissions.hasPermissions(MainActivity.this, permissions)) {
                        chooseVideoForUpload();
                    } else {
                        EasyPermissions.requestPermissions(
                                this,
                                getString(R.string.permission_rationale_upload),
                                SharedConstants.REQUEST_CODE_PERMISSIONS_UPLOAD,
                                permissions);
                    }
                } else if (EventBus.getDefault().hasSubscriberForEvent(ResolveRecordingOptionsEvent.class)) {
                    EventBus.getDefault().post(new ResolveRecordingOptionsEvent());
                } else {
                    startActivity(new Intent(this, RecorderActivity.class));
                }
            } else {
                showLoginSheet();
            }
        });
        ImageButton notifications = findViewById(R.id.notifications);
        notifications.setOnClickListener(v -> {
            if (mModel.isLoggedIn()) {
                controller.navigate(MainNavigationDirections.actionShowNotifications());
            } else {
                showLoginSheet();
            }
        });
        ImageButton profile = findViewById(R.id.profile);
        profile.setOnClickListener(v -> {
            if (mModel.isLoggedIn()) {
                controller.navigate(MainNavigationDirections.actionShowProfileSelf());
            } else {
                showLoginSheet();
            }
        });
        int active = ContextCompat.getColor(this, R.color.colorNavigationActive);
        int inactive = ContextCompat.getColor(this, R.color.colorNavigationInactive);
        boolean immersive = getResources().getBoolean(R.bool.immersive_mode_enabled);
        View container = findViewById(R.id.host);
        View toolbar = findViewById(R.id.toolbar);
        controller.addOnDestinationChangedListener((c, destination, arguments) -> {
            NavBackStackEntry previous = controller.getPreviousBackStackEntry();
            if (previous != null && previous.getDestination().getId() == R.id.fragment_notifications && mModel.isLoggedIn()) {
                refreshUnreadCount();
            }

            ImageViewCompat.setImageTintList(
                    clips, ColorStateList.valueOf(destination.getId() == R.id.fragment_player_tabs ? active : inactive));
            ImageViewCompat.setImageTintList(
                    discover, ColorStateList.valueOf(destination.getId() == R.id.fragment_discover ? active : inactive));
            ImageViewCompat.setImageTintList(
                    notifications, ColorStateList.valueOf(destination.getId() == R.id.fragment_notifications ? active : inactive));
            ImageViewCompat.setImageTintList(
                    profile, ColorStateList.valueOf(destination.getId() == R.id.fragment_profile ? active : inactive));
            boolean player = destination.getId() == R.id.fragment_player_slider
                    || destination.getId() == R.id.fragment_player_tabs;
            if (player && immersive) {
                container.setLayoutParams(
                        new RelativeLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT));
                toolbar.setBackgroundResource(android.R.color.transparent);
            } else if (immersive) {
                RelativeLayout.LayoutParams params =
                        new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0);
                params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
                params.addRule(RelativeLayout.ABOVE, R.id.toolbar);
                container.setLayoutParams(params);
                toolbar.setBackgroundResource(R.color.colorNavigationBar);
            }
        });
        View options2 = findViewById(R.id.sharing_sheet);
        BottomSheetBehavior<View> bsb2 = BottomSheetBehavior.from(options2);
        ImageButton close2 = options2.findViewById(R.id.header_back);
        close2.setImageResource(R.drawable.ic_baseline_close_24);
        close2.setOnClickListener(v -> bsb2.setState(BottomSheetBehavior.STATE_COLLAPSED));
        options2.findViewById(R.id.header_more).setVisibility(View.GONE);
        View options3 = findViewById(R.id.gift_sheet);
        BottomSheetBehavior<View> bsb3 = BottomSheetBehavior.from(options3);
        ImageButton close3 = options3.findViewById(R.id.header_back);
        close3.setImageResource(R.drawable.ic_baseline_close_24);
        close3.setOnClickListener(v -> bsb3.setState(BottomSheetBehavior.STATE_COLLAPSED));
        TextView title3 = options3.findViewById(R.id.header_title);
        title3.setText(R.string.send_gift_label);
        options3.findViewById(R.id.header_more).setVisibility(View.GONE);
        if (getResources().getBoolean(R.bool.gifts_enabled)) {
            ItemAdapter adapter = new ItemAdapter();
            RecyclerView items = options3.findViewById(R.id.items);
            items.setAdapter(adapter);
            mModel.items.observe(this, adapter::submitList);
            View loading2 = options3.findViewById(R.id.loading);
            mModel.state2.observe(this, state -> {
                loading2.setVisibility(state == LoadingState.LOADING ? View.VISIBLE : View.GONE);
            });
            TextView amount = options3.findViewById(R.id.amount);
            mModel.total.observe(this, total ->
                    amount.setText(getString(R.string.count_credits, total)));
        }

        View options5 = findViewById(R.id.qr_sheet);
        BottomSheetBehavior<View> bsb5 = BottomSheetBehavior.from(options5);
        ImageButton close5 = options5.findViewById(R.id.header_back);
        close5.setImageResource(R.drawable.ic_baseline_close_24);
        close5.setOnClickListener(v -> bsb5.setState(BottomSheetBehavior.STATE_COLLAPSED));
        TextView title5 = options5.findViewById(R.id.header_title);
        title5.setText(R.string.qr_label);
        options5.findViewById(R.id.header_more).setVisibility(View.GONE);
        syncFcmToken();
        showPromotionsIfAvailable();
        if (getResources().getBoolean(R.bool.in_app_review_enabled)) {
            long review = Prefs.getLong(SharedConstants.PREF_REVIEW_PROMPTED_AT, 0);
            if (review <= 0 && launches == getResources().getInteger(R.integer.in_app_review_delay)) {
                Prefs.putLong(SharedConstants.PREF_REVIEW_PROMPTED_AT, System.currentTimeMillis());
                askForReview();
            } else if (launches % getResources().getInteger(R.integer.in_app_review_interval) == 0) {
                Prefs.putLong(SharedConstants.PREF_REVIEW_PROMPTED_AT, System.currentTimeMillis());
                askForReview();
            }
        }

        boolean update = getResources().getBoolean(R.bool.in_app_update_enabled)
                && launches % getResources().getInteger(R.integer.in_app_update_interval) == 0;
        if (update) {
            checkForUpdate();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mUpdateManager.unregisterListener(mUpdateListener);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRecordingOptionsEvent(RecordingOptionsEvent event) {
        View sheet = findViewById(R.id.recording_options_sheet);
        TextView title = sheet.findViewById(R.id.header_title);
        title.setText(R.string.recording_options_title);
        BottomSheetBehavior<View> bsb = BottomSheetBehavior.from(sheet);
        sheet.findViewById(R.id.header_back).setVisibility(View.GONE);
        ImageButton close = sheet.findViewById(R.id.header_more);
        close.setImageResource(R.drawable.ic_baseline_close_24);
        close.setOnClickListener(v -> bsb.setState(BottomSheetBehavior.STATE_COLLAPSED));
        sheet.findViewById(R.id.solo)
                .setOnClickListener(v -> {
                    bsb.setState(BottomSheetBehavior.STATE_COLLAPSED);
                    startActivity(new Intent(this, RecorderActivity.class));
                });
        sheet.findViewById(R.id.solo_music)
                .setOnClickListener(v -> {
                    bsb.setState(BottomSheetBehavior.STATE_COLLAPSED);
                    submitForUse(event.getClip(), false);
                });
        View duet = sheet.findViewById(R.id.duet);
        if (event.getClip().duet && getResources().getBoolean(R.bool.duet_enabled)) {
            duet.setOnClickListener(v -> {
                bsb.setState(BottomSheetBehavior.STATE_COLLAPSED);
                submitForUse(event.getClip(), true);
            });
            duet.setVisibility(View.VISIBLE);
        } else {
            duet.setVisibility(View.GONE);
        }

        bsb.setState(BottomSheetBehavior.STATE_EXPANDED);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mUpdateManager.registerListener(mUpdateListener);
        com.google.android.play.core.tasks.Task<AppUpdateInfo> task =
                mUpdateManager.getAppUpdateInfo();
        task.addOnSuccessListener(info -> {
            if (info.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                Log.w(TAG, "There is a previous update pending.");
                try {
                    mUpdateManager.startUpdateFlowForResult(
                            info,
                            AppUpdateType.IMMEDIATE,
                            this,
                            SharedConstants.REQUEST_CODE_UPDATE_APP);
                } catch (Exception e) {
                    Log.e(TAG, "Could not initial in-app update.", e);
                }
            }
        });
        if (!mModel.isLoggedIn() && mModel.state1.getValue() != LoadingState.LOADING) {
            reloadProfile();
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

    private void askForReview() {
        ReviewManager manager = ReviewManagerFactory.create(this);
        com.google.android.play.core.tasks.Task<ReviewInfo> task1 = manager.requestReviewFlow();
        task1.addOnSuccessListener(info -> {
            com.google.android.play.core.tasks.Task<Void> task2 =
                    manager.launchReviewFlow(this, info);
            task2.addOnCompleteListener(x ->
                    Log.w(TAG, "Review could be cancelled or submitted, whichever."));
        });
    }

    private void checkForUpdate() {
        com.google.android.play.core.tasks.Task<AppUpdateInfo> task =
                mUpdateManager.getAppUpdateInfo();
        task.addOnSuccessListener(info -> {
            if (info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                    && info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
                Log.v(TAG, "There is an new update available.");
                try {
                    mUpdateManager.startUpdateFlowForResult(
                            info,
                            AppUpdateType.FLEXIBLE,
                            this,
                            SharedConstants.REQUEST_CODE_UPDATE_APP);
                } catch (Exception e) {
                    Log.e(TAG, "Could not initial in-app update.", e);
                }
            }
        });
    }

    @AfterPermissionGranted(SharedConstants.REQUEST_CODE_PERMISSIONS_UPLOAD)
    private void chooseVideoForUpload() {
        IntentUtil.startChooser(
                this,
                SharedConstants.REQUEST_CODE_PICK_VIDEO,
                "video/mp4");
    }

    private ClickableSpan createSpan(String url) {
        return new ClickableSpan() {

            @Override
            public void onClick(@NonNull View widget) {
                showUrlBrowser(url, null, false);
            }

            @Override
            public void updateDrawState(@NonNull TextPaint ds) {
                super.updateDrawState(ds);
                ds.setColor(ds.linkColor);
                ds.setUnderlineText(true);
            }
        };
    }

    public NavController findNavController() {
        NavHostFragment fragment = (NavHostFragment)getSupportFragmentManager()
                .findFragmentById(R.id.host);
        return fragment.getNavController();
    }

    private boolean isResumed() {
        return getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED);
    }

    private void loginWithFacebook(LoginResult result) {
        Log.d(TAG, "User logged in with Facebook ID " + result.getAccessToken().getUserId() + '.');
        REST rest = MainApplication.getContainer().get(REST.class);
        rest.loginFacebook(result.getAccessToken().getToken(), mModel.referrer)
                .enqueue(new Callback<Token>() {

                    @Override
                    public void onResponse(
                            @Nullable Call<Token> call,
                            @Nullable Response<Token> response
                    ) {
                        if (response != null && response.isSuccessful()) {
                            mHandler.post(() -> updateLoginState(response.body()));
                        }
                    }

                    @Override
                    public void onFailure(
                            @Nullable Call<Token> call,
                            @Nullable Throwable t
                    ) {
                        Log.e(TAG, "Login request with Facebook has failed.", t);
                    }
                });
    }

    private void loginWithGoogle(@Nullable GoogleSignInAccount account) {
        if (account == null) {
            Log.v(TAG, "Could not retrieve a Google account after login.");
            return;
        }

        Log.d(TAG, "User logged in with Google ID " + account.getId() + '.');
        REST rest = MainApplication.getContainer().get(REST.class);
        rest.loginGoogle(account.getIdToken(), mModel.referrer)
                .enqueue(new Callback<Token>() {

                    @Override
                    public void onResponse(
                            @Nullable Call<Token> call,
                            @Nullable Response<Token> response
                    ) {
                        if (response != null && response.isSuccessful()) {
                            mHandler.post(() -> updateLoginState(response.body()));
                        }
                    }

                    @Override
                    public void onFailure(
                            @Nullable Call<Token> call,
                            @Nullable Throwable t
                    ) {
                        Log.e(TAG, "Login request with Google has failed.", t);
                    }
                });
    }

    public void logout() {
        KProgressHUD progress = KProgressHUD.create(this)
                .setStyle(KProgressHUD.Style.SPIN_INDETERMINATE)
                .setLabel(getString(R.string.progress_title))
                .setCancellable(false)
                .show();
        FirebaseMessaging.getInstance().deleteToken()
                .addOnCompleteListener(this, task -> {
                    Log.v(TAG, "Firebase token deletion was successful.");
                    if (progress.isShowing()) {
                        progress.dismiss();
                    }

                    Prefs.remove(SharedConstants.PREF_CLIPS_SEEN_UNTIL);
                    Prefs.remove(SharedConstants.PREF_DEVICE_ID);
                    Prefs.remove(SharedConstants.PREF_FCM_TOKEN_SYNCED_AT);
                    Prefs.remove(SharedConstants.PREF_LAUNCH_COUNT);
                    Prefs.remove(SharedConstants.PREF_PREFERRED_LANGUAGES);
                    Prefs.remove(SharedConstants.PREF_PROMOTIONS_SEEN_UNTIL);
                    Prefs.remove(SharedConstants.PREF_SERVER_TOKEN);
                    ClientDatabase database =
                            MainApplication.getContainer().get(ClientDatabase.class);
                    database.drafts().delete();
                    Clip.LIKED.clear();
                    User.FOLLOWING.clear();
                    restartActivity(null, null);
                });
    }

    public void popBackStack() {
        findNavController().popBackStack();
    }

    private void proceedToAdjustment(File file) {
        Intent intent = new Intent(this, AdjustAudioActivity.class);
        intent.putExtra(AdjustAudioActivity.EXTRA_VIDEO, file.getAbsolutePath());
        startActivity(intent);
        finish();
    }

    private void proceedToFilter(File file) {
        Intent intent;
        if (getResources().getBoolean(R.bool.filters_enabled)) {
            intent = new Intent(this, FilterActivity.class);
            intent.putExtra(FilterActivity.EXTRA_VIDEO, file.getAbsolutePath());
        } else {
            intent = new Intent(this, UploadActivity.class);
            intent.putExtra(UploadActivity.EXTRA_VIDEO, file.getAbsolutePath());
        }

        startActivity(intent);
        finish();
    }

    private void proceedToTrimmer(Uri uri) {
        File copy = TempUtil.createCopy(this, uri, ".mp4");
        if (getResources().getBoolean(R.bool.skip_trimming_screen)) {
            if (getResources().getBoolean(R.bool.skip_adjustment_screen)) {
                proceedToAdjustment(copy);
            } else {
                proceedToFilter(copy);
            }
        } else {
            Intent intent = new Intent(this, TrimmerActivity.class);
            intent.putExtra(TrimmerActivity.EXTRA_VIDEO, copy.getAbsolutePath());
            startActivity(intent);
        }

        finish();
    }

    private void refreshUnreadCount() {
        REST rest = MainApplication.getContainer().get(REST.class);
        rest.notificationsUnread()
                .enqueue(new Callback<UnreadNotifications>() {

                    @Override
                    public void onResponse(
                            @Nullable Call<UnreadNotifications> call,
                            @Nullable Response<UnreadNotifications> response
                    ) {
                        int code = response != null ? response.code() : -1;
                        Log.w(TAG, "Fetching unread notifications count returned " + code + ".");
                        if (response != null && response.isSuccessful()) {
                            //noinspection ConstantConditions
                            int count = response.body().count;
                            mModel.notifications.postValue(count);
                        }
                    }

                    @Override
                    public void onFailure(
                            @Nullable Call<UnreadNotifications> call,
                            @Nullable Throwable t
                    ) {
                        Log.e(TAG, "Failed to fetch unread notifications count.", t);
                    }
                });
    }

    private void reloadProfile() {
        mModel.state1.postValue(LoadingState.LOADING);
        REST rest = MainApplication.getContainer().get(REST.class);
        rest.profileShow()
                .enqueue(new Callback<Wrappers.Single<User>>() {

                    @Override
                    public void onResponse(
                            @Nullable Call<Wrappers.Single<User>> call,
                            @Nullable Response<Wrappers.Single<User>> response
                    ) {
                        int code = response != null ? response.code() : -1;
                        Log.w(TAG, "Fetching profile from server returned " + code + ".");
                        if (response != null && response.isSuccessful()) {
                            mModel.user.postValue(response.body().data);
                            mModel.state1.postValue(LoadingState.LOADED);
                        } else {
                            mModel.state1.postValue(LoadingState.LOADED);
                        }
                    }

                    @Override
                    public void onFailure(
                            @Nullable Call<Wrappers.Single<User>> call,
                            @Nullable Throwable t
                    ) {
                        Log.e(TAG, "Failed to fetch profile from server.", t);
                        mModel.state1.postValue(LoadingState.LOADED);
                    }
                });
    }

    public void reportSubject(String type, int id) {
        Intent intent = new Intent(this, ReportActivity.class);
        intent.putExtra(ReportActivity.EXTRA_REPORT_SUBJECT_TYPE, type);
        intent.putExtra(ReportActivity.EXTRA_REPORT_SUBJECT_ID, id);
        startActivity(intent);
    }

    public void restartActivity(@Nullable Token token, @Nullable Uri data) {
        Intent intent = Intent.makeRestartActivityTask(getComponentName());
        if (token != null && !token.existing) {
            intent.putExtra(EXTRA_REGISTERED, true);
        }

        if (data != null) {
            intent.setData(data);
        }

        startActivity(intent);
    }

    private void sendGift(Clip clip) {
        List<Item.CartItem> cart = new ArrayList<>();
        for (Map.Entry<Integer, Integer> entry : mModel.cart.entrySet()) {
            if (entry.getValue() > 0) {
                Item.CartItem item = new Item.CartItem();
                item.id = entry.getKey();
                item.quantity = entry.getValue();
                cart.add(item);
            }
        }

        if (cart.isEmpty()) {
            Toast.makeText(this, R.string.message_empty_cart, Toast.LENGTH_SHORT).show();
            return;
        }

        KProgressHUD progress = KProgressHUD.create(this)
                .setStyle(KProgressHUD.Style.SPIN_INDETERMINATE)
                .setLabel(getString(R.string.progress_title))
                .setCancellable(false)
                .show();
        Map<String, String> items = new HashMap<>();
        for (int i = 0; i < cart.size(); i++) {
            items.put("items[" + i + "][id]", cart.get(i).id + "");
            items.put("items[" + i + "][quantity]", cart.get(i).quantity + "");
        }

        REST rest = MainApplication.getContainer().get(REST.class);
        rest.walletGift(clip.user.id, items)
                .enqueue(new Callback<Wrappers.Single<Balance>>() {

                    @Override
                    public void onResponse(
                            @Nullable Call<Wrappers.Single<Balance>> call,
                            @Nullable Response<Wrappers.Single<Balance>> response
                    ) {
                        int code = response != null ? response.code() : -1;
                        Log.v(TAG, "Sending gift returned " + code + ".");
                        progress.dismiss();
                        if (response != null && response.isSuccessful()) {
                            mModel.cart.clear();
                            mModel.total.setValue(0);
                            ItemDataSource source = mModel.factory.source.getValue();
                            if (source != null) {
                                source.invalidate();
                            }

                            Toast.makeText(MainActivity.this, R.string.gift_sent_message, Toast.LENGTH_SHORT).show();
                        } else if (code == 422 && isResumed()) {
                            try {
                                String body = response.errorBody().string();
                                JSONObject json = new JSONObject(body);
                                if (json.has("errors")) {
                                    JSONObject errors = json.getJSONObject("errors");
                                    if (errors.has("items")) {
                                        JSONArray message = errors.getJSONArray("items");
                                        Toast.makeText(MainActivity.this, message.getString(0), Toast.LENGTH_SHORT).show();
                                    }
                                }
                            } catch (Exception ignore) {
                            }
                        } else {
                            Toast.makeText(MainActivity.this, R.string.error_unknown, Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(
                            @Nullable Call<Wrappers.Single<Balance>> call,
                            @Nullable Throwable t
                    ) {
                        Log.e(TAG, "Failed to send gift to the user.", t);
                        progress.dismiss();
                        Toast.makeText(MainActivity.this, R.string.error_internet, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    public void share(Clip clip, @Nullable SharingTarget target) {
        if (getResources().getBoolean(R.bool.sharing_links_enabled)) {
            shareLink(clip, target);
        } else {
            File clips = new File(getFilesDir(), "clips");
            if (!clips.exists() && !clips.mkdirs()) {
                Log.w(TAG, "Could not create directory at " + clips);
            }

            File fixed = TempUtil.createNewFile(clips, ".mp4");
            if (fixed.exists()) {
                shareVideoFile(this, fixed, target);
                return;
            }

            WorkManager wm = WorkManager.getInstance(this);
            boolean async = getResources().getBoolean(R.bool.sharing_async_enabled);
            OneTimeWorkRequest request;
            if (getResources().getBoolean(R.bool.sharing_watermark_enabled)) {
                File original = TempUtil.createNewFile(this, ".mp4");
                File watermarked = TempUtil.createNewFile(this, ".mp4");
                wm.beginWith(VideoUtil.createDownloadRequest(clip.video, original, async))
                        .then(VideoUtil.createWatermarkRequest(clip, original, watermarked, async))
                        .then(request = VideoUtil.createFastStartRequest(watermarked, fixed))
                        .enqueue();
            } else {
                wm.enqueue(request = VideoUtil.createDownloadRequest(clip.video, fixed, async));
            }

            if (async) {
                Toast.makeText(this, R.string.message_sharing_async, Toast.LENGTH_SHORT).show();
            } else {
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
                        });
            }

            wm.getWorkInfoByIdLiveData(request.getId())
                    .observe(this, info -> {
                        if (info.getState() == WorkInfo.State.SUCCEEDED) {
                            shareVideoFile(this, fixed, target);
                        }
                    });
        }
    }

    private void shareToTarget(Intent intent, @Nullable SharingTarget target, @StringRes int title) {
        if (target != null && PackageUtil.isInstalled(this, target.pkg)) {
            intent.setPackage(target.pkg);
            startActivity(intent);
        } else {
            startActivity(Intent.createChooser(intent, getString(title)));
        }
    }

    private void shareVideoFile(Context context, File file, @Nullable SharingTarget target) {
        Log.v(TAG, "Showing sharing options for " + file);
        Uri uri = FileProvider.getUriForFile(context, context.getPackageName(), file);
        Intent intent = ShareCompat.IntentBuilder.from(this)
                .setStream(uri)
                .setText(getString(R.string.share_clip_text, context.getPackageName()))
                .setType("video/*")
                .getIntent()
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        shareToTarget(intent, target, R.string.share_clip_chooser);
    }

    private void shareLink(Uri uri, @Nullable SharingTarget target, @StringRes int title) {
        Log.v(TAG, "Showing sharing options for " + uri);
        Intent intent = ShareCompat.IntentBuilder.from(this)
                .setText(uri.toString())
                .setType("text/plain")
                .getIntent();
        shareToTarget(intent, target, title);
    }

    private void shareInvite(@Nullable SharingTarget target) {
        User user = mModel.user.getValue();
        if (user == null) {
            return;
        }

        KProgressHUD progress = KProgressHUD.create(this)
                .setStyle(KProgressHUD.Style.SPIN_INDETERMINATE)
                .setLabel(getString(R.string.progress_title))
                .setCancellable(false)
                .show();
        Uri base = Uri.parse(getString(R.string.server_url));
        Uri link = base.buildUpon()
                .path("links/home")
                .appendQueryParameter(QUERY_REFERRER, user.id + "")
                .appendQueryParameter("package", BuildConfig.APPLICATION_ID)
                .build();
        Task<ShortDynamicLink> task = FirebaseDynamicLinks.getInstance()
                .createDynamicLink()
                .setLink(link)
                .setDomainUriPrefix(String.format(Locale.US, "https://%s/", getString(R.string.sharing_links_domain)))
                .setAndroidParameters(
                        new DynamicLink.AndroidParameters.Builder(BuildConfig.APPLICATION_ID)
                                .build())
                .buildShortDynamicLink();
        task.addOnCompleteListener(this, result -> {
            progress.dismiss();
            if (result.isSuccessful()) {
                ShortDynamicLink sdl = result.getResult();
                shareLink(sdl.getShortLink(), target, R.string.share_invite_chooser);
            } else {
                Log.v(TAG, "Could not generate short dynamic link for invite.");
            }
        });
    }

    private void shareLink(Clip clip, @Nullable SharingTarget target) {
        KProgressHUD progress = KProgressHUD.create(this)
                .setStyle(KProgressHUD.Style.SPIN_INDETERMINATE)
                .setLabel(getString(R.string.progress_title))
                .setCancellable(false)
                .show();
        DynamicLink.SocialMetaTagParameters.Builder smtp =
                new DynamicLink.SocialMetaTagParameters.Builder();
        smtp.setDescription(getString(R.string.share_clip_description, getString(R.string.app_name)));
        smtp.setImageUrl(Uri.parse(clip.screenshot));
        if (TextUtils.isEmpty(clip.description)) {
            smtp.setTitle(getString(R.string.share_clip_title, '@' + clip.user.username));
        } else {
            smtp.setTitle(clip.description);
        }

        Uri base = Uri.parse(getString(R.string.server_url));
        Uri link = base.buildUpon()
                .path("links/clips")
                .appendQueryParameter("first", clip.id + "")
                .appendQueryParameter("package", BuildConfig.APPLICATION_ID)
                .build();
        Task<ShortDynamicLink> task = FirebaseDynamicLinks.getInstance()
                .createDynamicLink()
                .setLink(link)
                .setDomainUriPrefix(String.format(Locale.US, "https://%s/", getString(R.string.sharing_links_domain)))
                .setSocialMetaTagParameters(smtp.build())
                .setAndroidParameters(
                        new DynamicLink.AndroidParameters.Builder(BuildConfig.APPLICATION_ID)
                                .build())
                .buildShortDynamicLink();
        task.addOnCompleteListener(this, result -> {
            progress.dismiss();
            if (result.isSuccessful()) {
                ShortDynamicLink sdl = result.getResult();
                shareLink(sdl.getShortLink(), target, R.string.share_clip_chooser);
            } else {
                Log.e(TAG, "Could not generate short dynamic link for clip.", task.getException());
            }
        });
    }

    private void shareLink(User user, @Nullable SharingTarget target) {
        KProgressHUD progress = KProgressHUD.create(this)
                .setStyle(KProgressHUD.Style.SPIN_INDETERMINATE)
                .setLabel(getString(R.string.progress_title))
                .setCancellable(false)
                .show();
        DynamicLink.SocialMetaTagParameters.Builder smtp =
                new DynamicLink.SocialMetaTagParameters.Builder();
        smtp.setDescription(getString(R.string.share_user_description, getString(R.string.app_name)));
        if (!TextUtils.isEmpty(user.photo)) {
            smtp.setImageUrl(Uri.parse(user.photo));
        }

        if (TextUtils.isEmpty(user.bio)) {
            smtp.setTitle(getString(R.string.share_user_title, '@' + user.username));
        } else {
            smtp.setTitle(user.bio);
        }

        Uri base = Uri.parse(getString(R.string.server_url));
        Uri link = base.buildUpon()
                .path("links/users")
                .appendQueryParameter("user", user.id + "")
                .appendQueryParameter("package", BuildConfig.APPLICATION_ID)
                .build();
        Task<ShortDynamicLink> task = FirebaseDynamicLinks.getInstance()
                .createDynamicLink()
                .setLink(link)
                .setDomainUriPrefix(String.format(Locale.US, "https://%s/", getString(R.string.sharing_links_domain)))
                .setSocialMetaTagParameters(smtp.build())
                .setAndroidParameters(
                        new DynamicLink.AndroidParameters.Builder(BuildConfig.APPLICATION_ID)
                                .build())
                .buildShortDynamicLink();
        task.addOnCompleteListener(this, result -> {
            progress.dismiss();
            if (result.isSuccessful()) {
                ShortDynamicLink sdl = result.getResult();
                shareLink(sdl.getShortLink(), target, R.string.share_user_chooser);
            } else {
                Log.e(TAG, "Could not generate short dynamic link for user.", task.getException());
            }
        });
    }

    public void showAbout() {
        NavDirections direction = MainNavigationDirections.actionShowAbout();
        findNavController().navigate(direction);
    }

    public void showClips(String title, Bundle params) {
        NavDirections direction = MainNavigationDirections.actionShowClipsGrid(title, params, true);
        findNavController().navigate(direction);
    }

    public void showCommentsPage(int clip) {
        NavDirections direction = MainNavigationDirections.actionShowComments(clip);
        findNavController().navigate(direction);
    }

    public void showEditClip(int clip) {
        NavDirections direction = MainNavigationDirections.actionShowEditClip(clip);
        findNavController().navigate(direction);
    }

    public void showEditProfile() {
        NavDirections direction = MainNavigationDirections.actionShowEditProfile();
        findNavController().navigate(direction);
    }

    public void showFollowerFollowing(int user, boolean following) {
        NavDirections direction = MainNavigationDirections.actionShowFollowers(user, following);
        findNavController().navigate(direction);
    }

    public void showGiftOptions(Clip clip) {
        View options = findViewById(R.id.gift_sheet);
        BottomSheetBehavior<View> bsb = BottomSheetBehavior.from(options);
        options.findViewById(R.id.send)
                .setOnClickListener(v -> {
                    bsb.setState(BottomSheetBehavior.STATE_COLLAPSED);
                    sendGift(clip);
                });
        bsb.setState(BottomSheetBehavior.STATE_EXPANDED);
    }

    public void showInviteOptions() {
        View options = findViewById(R.id.sharing_sheet);
        TextView title = options.findViewById(R.id.header_title);
        title.setText(R.string.share_invite_chooser);
        BottomSheetBehavior<View> bsb = BottomSheetBehavior.from(options);
        options.findViewById(R.id.facebook)
                .setOnClickListener(v -> {
                    bsb.setState(BottomSheetBehavior.STATE_COLLAPSED);
                    shareInvite(SharingTarget.FACEBOOK);
                });
        options.findViewById(R.id.instagram)
                .setOnClickListener(v -> {
                    bsb.setState(BottomSheetBehavior.STATE_COLLAPSED);
                    shareInvite(SharingTarget.INSTAGRAM);
                });
        options.findViewById(R.id.twitter)
                .setOnClickListener(v -> {
                    bsb.setState(BottomSheetBehavior.STATE_COLLAPSED);
                    shareInvite(SharingTarget.TWITTER);
                });
        options.findViewById(R.id.whatsapp)
                .setOnClickListener(v -> {
                    bsb.setState(BottomSheetBehavior.STATE_COLLAPSED);
                    shareInvite(SharingTarget.WHATSAPP);
                });
        options.findViewById(R.id.other)
                .setOnClickListener(v -> {
                    bsb.setState(BottomSheetBehavior.STATE_COLLAPSED);
                    shareInvite(null);
                });
        bsb.setState(BottomSheetBehavior.STATE_EXPANDED);
    }

    public void showLoginSheet() {
        View sheet = findViewById(R.id.login_sheet);
        if (mAd != null) {
            mAd.create(this, ad -> {
                LinearLayout banner = findViewById(R.id.banner);
                banner.removeAllViews();
                banner.addView(ad);
            });
        }

        BottomSheetBehavior<View> bsb = BottomSheetBehavior.from(sheet);
        bsb.setState(BottomSheetBehavior.STATE_EXPANDED);
    }

    public void showMessages(String title, int thread) {
        NavDirections direction = MainNavigationDirections.actionShowMessages(title, thread);
        findNavController().navigate(direction);
    }

    public void showNews() {
        findNavController().navigate(MainNavigationDirections.actionShowNews());
    }

    public void showPhotoViewer(String title, Uri url) {
        NavDirections direction = MainNavigationDirections.actionShowPhotoViewer(title, url);
        findNavController().navigate(direction);
    }

    public void showPlayerSlider(int clip, Bundle params) {
        NavDirections direction = MainNavigationDirections.actionShowPlayerSlider(clip, params);
        findNavController().navigate(direction);
    }

    public void showProfilePage(int user) {
        NavDirections direction = MainNavigationDirections.actionShowProfile(user);
        findNavController().navigate(direction);
    }

    public void showProfilePage(String username) {
        KProgressHUD progress = KProgressHUD.create(this)
                .setStyle(KProgressHUD.Style.SPIN_INDETERMINATE)
                .setLabel(getString(R.string.progress_title))
                .setCancellable(false)
                .show();
        REST rest = MainApplication.getContainer().get(REST.class);
        rest.usersFind(username)
                .enqueue(new Callback<Wrappers.Single<User>>() {
                    @Override
                    public void onResponse(
                            @Nullable Call<Wrappers.Single<User>> call,
                            @Nullable Response<Wrappers.Single<User>> response
                    ) {
                        int code = response != null ? response.code() : -1;
                        Log.v(TAG, "Finding user returned " + code + '.');
                        if (code == 200) {
                            User user = response.body().data;
                            showProfilePage(user.id);
                        }

                        progress.dismiss();
                    }

                    @Override
                    public void onFailure(
                            @Nullable Call<Wrappers.Single<User>> call,
                            @Nullable Throwable t
                    ) {
                        Log.e(TAG, "Failed to find user with username " + username + ".", t);
                        progress.dismiss();
                    }
                });
    }

    private void showPromotionsIfAvailable() {
        long until = Prefs.getLong(SharedConstants.PREF_PROMOTIONS_SEEN_UNTIL, 0);
        REST rest = MainApplication.getContainer().get(REST.class);
        rest.promotionsIndex(until)
                .enqueue(new Callback<Wrappers.Paginated<Promotion>>() {
                    @Override
                    public void onResponse(
                            @Nullable Call<Wrappers.Paginated<Promotion>> call,
                            @Nullable Response<Wrappers.Paginated<Promotion>> response
                    ) {
                        int code = response != null ? response.code() : -1;
                        Log.v(TAG, "Fetching promotions returned " + code + '.');
                        if (response != null && response.isSuccessful()) {
                            Prefs.putLong(
                                    SharedConstants.PREF_PROMOTIONS_SEEN_UNTIL,
                                    System.currentTimeMillis());
                            List<Promotion> promotions = response.body().data;
                            if (promotions.isEmpty()) {
                                Log.w(TAG, "There are no banners to show.");
                            } else if (isResumed()) {
                                PromotionsDialogFragment.newInstance(promotions)
                                        .show(getSupportFragmentManager(), null);
                            }
                        }
                    }

                    @Override
                    public void onFailure(
                            @Nullable Call<Wrappers.Paginated<Promotion>> call,
                            @Nullable Throwable t
                    ) {
                        Log.e(TAG, "Failed to load promotions data from server.", t);
                    }
                });
    }

    public void showQrSheet(User user) {
        View sheet = findViewById(R.id.qr_sheet);
        BottomSheetBehavior<View> bsb = BottomSheetBehavior.from(sheet);
        sheet.findViewById(R.id.scan).setOnClickListener(v -> {
            bsb.setState(BottomSheetBehavior.STATE_COLLAPSED);
            NavDirections direction = MainNavigationDirections.actionShowQrScanner();
            findNavController().navigate(direction);
        });
        sheet.findViewById(R.id.show).setOnClickListener(v -> {
            bsb.setState(BottomSheetBehavior.STATE_COLLAPSED);
            NavDirections direction = MainNavigationDirections.actionShowQr();
            findNavController().navigate(direction);
        });
        bsb.setState(BottomSheetBehavior.STATE_EXPANDED);
    }

    public void showRequestVerification() {
        NavDirections direction = MainNavigationDirections.actionShowRequestVerification();
        findNavController().navigate(direction);
    }

    public void showSearch() {
        NavDirections direction = MainNavigationDirections.actionShowSearch();
        findNavController().navigate(direction);
    }

    public void showSharingOptions(Clip clip) {
        if (getResources().getBoolean(R.bool.sharing_sheet_enabled)) {
            View options = findViewById(R.id.sharing_sheet);
            TextView title = options.findViewById(R.id.header_title);
            title.setText(R.string.share_clip_chooser);
            BottomSheetBehavior<View> bsb = BottomSheetBehavior.from(options);
            options.findViewById(R.id.facebook)
                    .setOnClickListener(v -> {
                        bsb.setState(BottomSheetBehavior.STATE_COLLAPSED);
                        share(clip, SharingTarget.FACEBOOK);
                    });
            options.findViewById(R.id.instagram)
                    .setOnClickListener(v -> {
                        bsb.setState(BottomSheetBehavior.STATE_COLLAPSED);
                        share(clip, SharingTarget.INSTAGRAM);
                    });
            options.findViewById(R.id.twitter)
                    .setOnClickListener(v -> {
                        bsb.setState(BottomSheetBehavior.STATE_COLLAPSED);
                        share(clip, SharingTarget.TWITTER);
                    });
            options.findViewById(R.id.whatsapp)
                    .setOnClickListener(v -> {
                        bsb.setState(BottomSheetBehavior.STATE_COLLAPSED);
                        share(clip, SharingTarget.WHATSAPP);
                    });
            options.findViewById(R.id.other)
                    .setOnClickListener(v -> {
                        bsb.setState(BottomSheetBehavior.STATE_COLLAPSED);
                        share(clip, null);
                    });
            bsb.setState(BottomSheetBehavior.STATE_EXPANDED);
        } else {
            share(clip, null);
        }
    }

    public void showSharingOptions(User user) {
        if (getResources().getBoolean(R.bool.sharing_sheet_enabled)) {
            View options = findViewById(R.id.sharing_sheet);
            TextView title = options.findViewById(R.id.header_title);
            title.setText(R.string.share_user_chooser);
            BottomSheetBehavior<View> bsb = BottomSheetBehavior.from(options);
            options.findViewById(R.id.facebook)
                    .setOnClickListener(v -> {
                        bsb.setState(BottomSheetBehavior.STATE_COLLAPSED);
                        shareLink(user, SharingTarget.FACEBOOK);
                    });
            options.findViewById(R.id.instagram)
                    .setOnClickListener(v -> {
                        bsb.setState(BottomSheetBehavior.STATE_COLLAPSED);
                        shareLink(user, SharingTarget.INSTAGRAM);
                    });
            options.findViewById(R.id.twitter)
                    .setOnClickListener(v -> {
                        bsb.setState(BottomSheetBehavior.STATE_COLLAPSED);
                        shareLink(user, SharingTarget.TWITTER);
                    });
            options.findViewById(R.id.whatsapp)
                    .setOnClickListener(v -> {
                        bsb.setState(BottomSheetBehavior.STATE_COLLAPSED);
                        shareLink(user, SharingTarget.WHATSAPP);
                    });
            options.findViewById(R.id.other)
                    .setOnClickListener(v -> {
                        bsb.setState(BottomSheetBehavior.STATE_COLLAPSED);
                        shareLink(user, null);
                    });
            bsb.setState(BottomSheetBehavior.STATE_EXPANDED);
        } else {
            shareLink(user, null);
        }
    }

    public void showThreads() {
        findNavController().navigate(MainNavigationDirections.actionShowThreads());
    }

    public void showUrlBrowser(String url, @Nullable String title, boolean internal) {
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "http://" + url;
        }

        if (internal || !getResources().getBoolean(R.bool.external_browser_enabled)) {
            FinestWebView.Builder builder = new FinestWebView.Builder(this);
            if (title != null) {
                builder.titleDefault(title);
            }

            builder.show(url);
        } else {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        }
    }

    public void showWallet() {
        NavDirections direction = MainNavigationDirections.actionShowWallet();
        findNavController().navigate(direction);
    }

    public void submitForUse(Clip clip, boolean duet) {
        KProgressHUD progress = KProgressHUD.create(this)
                .setStyle(KProgressHUD.Style.SPIN_INDETERMINATE)
                .setLabel(getString(R.string.progress_title))
                .setCancellable(false)
                .show();
        File downloaded = TempUtil.createNewFile(this, ".mp4");
        OneTimeWorkRequest request = VideoUtil.createDownloadRequest(clip.video, downloaded, false);
        WorkManager wm = WorkManager.getInstance(this);
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
                        Intent intent;
                        if (duet) {
                            intent = new Intent(this, DuetRecorderActivity.class);
                            intent.putExtra(DuetRecorderActivity.EXTRA_VIDEO, Uri.fromFile(downloaded));
                        } else {
                            intent = new Intent(this, RecorderActivity.class);
                            intent.putExtra(RecorderActivity.EXTRA_AUDIO, Uri.fromFile(downloaded));
                        }

                        startActivity(intent);
                    }
                });
    }

    private void syncFcmToken() {
        String token = Prefs.getString(SharedConstants.PREF_FCM_TOKEN, null);
        if (TextUtils.isEmpty(token)) {
            return;
        }

        long synced = Prefs.getLong(SharedConstants.PREF_FCM_TOKEN_SYNCED_AT, 0);
        if (synced <= 0 || synced < (System.currentTimeMillis() - SharedConstants.SYNC_FCM_INTERVAL)) {
            Prefs.putLong(SharedConstants.PREF_FCM_TOKEN_SYNCED_AT, System.currentTimeMillis());
            WorkRequest request = OneTimeWorkRequest.from(DeviceTokenWorker.class);
            WorkManager.getInstance(this).enqueue(request);
        }
    }

    private int updateCart(Item item, int quantity) {
        int updated = 0;
        if (mModel.cart.containsKey(item.id)) {
            updated = mModel.cart.get(item.id);
        }

        updated += quantity;
        if (updated < 0) {
            updated = quantity = 0;
        }

        mModel.cart.put(item.id, updated);
        int amount = item.price * quantity;
        int total = mModel.total.getValue();
        total += amount;
        mModel.total.postValue(total);
        return updated;
    }

    private void updateLoginState(Token token) {
        Log.v(TAG, "Received token from server i.e., " + token);
        Prefs.putString(SharedConstants.PREF_SERVER_TOKEN, token.token);
        Toast.makeText(this, R.string.login_success, Toast.LENGTH_SHORT).show();
        restartActivity(token, null);
    }

    private class ItemAdapter extends PagedListAdapter<Item, ItemViewHolder> {

        protected ItemAdapter() {
            super(new DiffUtilCallback<>(i -> i.id));
        }

        @Override
        @SuppressLint("SetTextI18n")
        public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
            Item item = getItem(position);
            holder.image.setImageURI(item.image);
            holder.name.setText(item.name);
            holder.price.setText("@ " + item.price);
            holder.plus.setOnClickListener(v -> {
                int updated = updateCart(item, 1);
                holder.count.setText(updated + "");
            });
            holder.minus.setOnClickListener(v -> {
                int updated = updateCart(item, -1);
                holder.count.setText(updated + "");
            });
            int quantity = updateCart(item, 0);
            holder.count.setText(quantity + "");
        }

        @NonNull
        @Override
        public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(MainActivity.this)
                    .inflate(R.layout.item_gift_to_send, parent, false);
            return new ItemViewHolder(view);
        }
    }

    private static class ItemViewHolder extends RecyclerView.ViewHolder {

        public SimpleDraweeView image;
        public TextView name;
        public TextView price;
        public Button minus;
        public TextView count;
        public Button plus;

        public ItemViewHolder(@NonNull View root) {
            super(root);
            image = root.findViewById(R.id.image);
            name = root.findViewById(R.id.name);
            price = root.findViewById(R.id.price);
            minus = root.findViewById(R.id.minus);
            count = root.findViewById(R.id.count);
            plus = root.findViewById(R.id.plus);
        }
    }

    public static class MainActivityViewModel extends ViewModel {

        public MainActivityViewModel() {
            PagedList.Config config = new PagedList.Config.Builder()
                    .setPageSize(SharedConstants.DEFAULT_PAGE_SIZE)
                    .build();
            factory = new ItemDataSource.Factory();
            state2 = Transformations.switchMap(factory.source, input -> input.state);
            items = new LivePagedListBuilder<>(factory, config).build();
        }

        public boolean areThreadsInvalid;
        public final Map<Integer, Integer> cart = new HashMap<>();
        public final ItemDataSource.Factory factory;
        public boolean isProfileInvalid;
        public final LiveData<PagedList<Item>> items;
        public final MutableLiveData<Integer> notifications = new MutableLiveData<>(0);
        public String referrer;
        public final MutableLiveData<String> searchTerm = new MutableLiveData<>();
        public final MutableLiveData<LoadingState> state1 = new MutableLiveData<>(LoadingState.IDLE);
        public final LiveData<LoadingState> state2;
        public final MutableLiveData<Integer> total = new MutableLiveData<Integer>(0);
        public final MutableLiveData<User> user = new MutableLiveData<>();

        public boolean isLoggedIn() {
            return user.getValue() != null;
        }
    }
}
