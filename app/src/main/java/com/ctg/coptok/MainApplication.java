package com.ctg.coptok;

import android.app.Application;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;
import androidx.multidex.MultiDex;

import com.adcolony.sdk.AdColony;
import com.arthenica.mobileffmpeg.Config;
import com.facebook.ads.AdSettings;
import com.facebook.ads.AudienceNetworkAds;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.imagepipeline.core.ImagePipelineConfig;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.RequestConfiguration;
import com.mopub.common.MoPub;
import com.mopub.common.SdkConfiguration;
import com.mopub.common.logging.MoPubLog;
import com.google.android.libraries.places.api.Places;
import com.pixplicity.easyprefs.library.Prefs;
import com.vaibhavpandey.katora.Container;
import com.vaibhavpandey.katora.contracts.ImmutableContainer;
import com.vanniktech.emoji.EmojiManager;
import com.vanniktech.emoji.facebook.FacebookEmojiProvider;
import com.vanniktech.emoji.google.GoogleEmojiProvider;
import com.vanniktech.emoji.ios.IosEmojiProvider;
import com.vanniktech.emoji.twitter.TwitterEmojiProvider;

import java.util.Collections;
import java.util.List;

import io.sentry.android.core.SentryAndroid;
import com.ctg.coptok.data.models.Advertisement;
import com.ctg.coptok.providers.ExoPlayerProvider;
import com.ctg.coptok.providers.FrescoProvider;
import com.ctg.coptok.providers.JacksonProvider;
import com.ctg.coptok.providers.OkHttpProvider;
import com.ctg.coptok.providers.RetrofitProvider;
import com.ctg.coptok.providers.RoomProvider;
import com.ctg.coptok.utils.AdsUtil;
import com.ctg.coptok.utils.LocaleUtil;
import com.ctg.coptok.utils.TempUtil;

public class MainApplication extends Application {

    private static final Container CONTAINER = new Container();
    private static final String TAG = "MainApplication";

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleUtil.wrap(base));
        MultiDex.install(this);
    }

    public static ImmutableContainer getContainer() {
        return CONTAINER;
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressWarnings("SameParameterValue")
    private void createChannel(String id, String name, int visibility, int importance) {
        NotificationChannel channel = new NotificationChannel(id, name, importance);
        channel.enableLights(true);
        channel.setLightColor(ContextCompat.getColor(this, R.color.colorPrimary));
        channel.setLockscreenVisibility(visibility);
        if (importance == NotificationManager.IMPORTANCE_LOW) {
            channel.setShowBadge(false);
        }

        NotificationManager nm =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nm.createNotificationChannel(channel);
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration config) {
        super.onConfigurationChanged(config);
        LocaleUtil.override(this);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        CONTAINER.install(new ExoPlayerProvider(this));
        CONTAINER.install(new FrescoProvider(this));
        CONTAINER.install(new JacksonProvider());
        CONTAINER.install(new OkHttpProvider(this));
        CONTAINER.install(new RetrofitProvider(this));
        CONTAINER.install(new RoomProvider(this));
        String dsn = getString(R.string.sentry_dsn);
        if (!TextUtils.isEmpty(dsn)) {
            SentryAndroid.init(this, options -> options.setDsn(dsn));
        }

        Config.enableLogCallback(message -> Log.d(TAG, message.getText()));
        Config.enableStatisticsCallback(stats ->
                Log.d(TAG, String.format(
                        "FFmpeg frame: %d, time: %d", stats.getVideoFrameNumber(), stats.getTime())));
        Fresco.initialize(this, getContainer().get(ImagePipelineConfig.class));
        if (BuildConfig.DEBUG) {
            RequestConfiguration configuration = new RequestConfiguration.Builder()
                    .setTestDeviceIds(Collections.singletonList(getString(R.string.admob_test_device_id)))
                    .build();
            MobileAds.setRequestConfiguration(configuration);
        }

        MobileAds.initialize(this, status -> { /* eaten */ });
        int emoji = getResources().getInteger(R.integer.emoji_variant);
        switch (emoji) {
            case 1:
                EmojiManager.install(new GoogleEmojiProvider());
                break;
            case 2:
                EmojiManager.install(new FacebookEmojiProvider());
                break;
            case 3:
                EmojiManager.install(new TwitterEmojiProvider());
                break;
            default:
                EmojiManager.install(new IosEmojiProvider());
                break;
        }

        if (getResources().getBoolean(R.bool.locations_enabled)) {
            Places.initialize(this, getString(R.string.locations_api_key));
        }

        new Prefs.Builder()
                .setContext(this)
                .setUseDefaultSharedPreference(true)
                .build();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createChannel(
                    getString(R.string.notification_channel_id),
                    getString(R.string.notification_channel_name),
                    Notification.VISIBILITY_PUBLIC,
                    NotificationManager.IMPORTANCE_DEFAULT
            );
        }

        TempUtil.cleanupStaleFiles(getApplicationContext());
        setupAds();
    }

    private void setupAds() {
        List<Advertisement> ads;
        ads = AdsUtil.findByNetwork("adcolony");
        if (!ads.isEmpty()) {
            String[] zones = new String[ads.size()];
            for (int i = 0; i < zones.length; i++) {
                zones[i] = ads.get(i).unit;
            }
            AdColony.configure(this, getString(R.string.adcolony_app_id), zones);
        }
        ads = AdsUtil.findByNetwork("facebook");
        if (!ads.isEmpty()) {
            AudienceNetworkAds.initialize(this);
            String device = getString(R.string.facebook_test_device_id);
            if (!TextUtils.isEmpty(device)) {
                AdSettings.addTestDevice(device);
            }
        }
        ads = AdsUtil.findByNetwork("mopub");
        if (!ads.isEmpty()) {
            Advertisement ad = ads.get(0);
            SdkConfiguration config = new SdkConfiguration.Builder(ad.unit)
                    .withLogLevel(BuildConfig.DEBUG ? MoPubLog.LogLevel.DEBUG : MoPubLog.LogLevel.NONE)
                    .build();
            MoPub.initializeSdk(this, config, () ->
                    Log.v(TAG, "MoPub SDK was initialized successfully."));
        }
    }
}
