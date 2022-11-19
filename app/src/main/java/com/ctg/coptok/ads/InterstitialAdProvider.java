package com.ctg.coptok.ads;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.adcolony.sdk.AdColony;
import com.adcolony.sdk.AdColonyInterstitial;
import com.adcolony.sdk.AdColonyInterstitialListener;
import com.adcolony.sdk.AdColonyZone;
import com.facebook.ads.Ad;
import com.facebook.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.mopub.common.MoPub;
import com.mopub.common.SdkConfiguration;
import com.mopub.common.logging.MoPubLog;
import com.mopub.mobileads.MoPubErrorCode;
import com.mopub.mobileads.MoPubInterstitial;

import com.ctg.coptok.BuildConfig;
import com.ctg.coptok.activities.InterstitialAdActivity;
import com.ctg.coptok.data.models.Advertisement;

public class InterstitialAdProvider {

    private static final String TAG = "InterstitialAdProvider";

    private final Advertisement mAd;

    public InterstitialAdProvider(Advertisement ad) {
        mAd = ad;
    }

    @Nullable
    public Runnable create(Activity activity) {
        switch (mAd.network) {
            case "adcolony": {
                Holder<AdColonyInterstitial> ad = new Holder<>();
                //noinspection ConstantConditions
                AdColony.requestInterstitial(mAd.unit, new AdColonyInterstitialListener() {

                    public void onClosed(AdColonyInterstitial ad) {
                        AdColony.requestInterstitial(ad.getZoneID(), this);
                    }

                    @Override
                    public void onRequestNotFilled(AdColonyZone zone) {
                        Log.e(TAG, "Interstitial ad from AdColony could not be filled.");
                    }

                    @Override
                    public void onRequestFilled(AdColonyInterstitial interstitial) {
                        Log.v(TAG, "Interstitial ad from AdColony was filled.");
                        ad.set(interstitial);
                    }
                });
                return () -> {
                    AdColonyInterstitial aci = ad.get();
                    Log.v(TAG, "Showing interstitial ad; loaded: " + (aci != null));
                    if (aci != null) {
                        aci.show();
                    }
                };
            }
            case "admob": {
                Holder<InterstitialAd> holder = new Holder<>();
                //noinspection ConstantConditions
                InterstitialAd.load(
                        activity,
                        mAd.unit,
                        new AdRequest.Builder().build(),
                        new InterstitialAdLoadCallback() {

                            @Override
                            public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                                Log.v(TAG, "Interstitial ad from AdMob was loaded.");
                                holder.set(interstitialAd);
                            }

                            @Override
                            public void onAdFailedToLoad(@NonNull LoadAdError error) {
                                Log.e(TAG, "Interstitial ad from AdMob failed to load.\n" + error.toString());
                            }
                        });
                return () -> {
                    InterstitialAd ad = holder.get();
                    Log.v(TAG, "Showing interstitial ad; loaded: " + (ad != null));
                    if (ad != null) {
                        ad.show(activity);
                    }
                };
            }
            case "facebook": {
                com.facebook.ads.InterstitialAd interstitial =
                        new com.facebook.ads.InterstitialAd(activity, mAd.unit);
                com.facebook.ads.AbstractAdListener listener =
                        new com.facebook.ads.AbstractAdListener() {

                            @Override
                            public void onInterstitialDismissed(Ad ad) {
                                interstitial.loadAd(
                                        interstitial.buildLoadAdConfig()
                                                .withAdListener(this)
                                                .build());
                            }

                            @Override
                            public void onAdLoaded(Ad ad) {
                                Log.v(TAG, "Interstitial ad from Facebook was loaded.");
                            }

                            @Override
                            public void onError(Ad ad, AdError error) {
                                Log.e(TAG, "Interstitial ad from Facebook failed to load.\n" + error.getErrorMessage());
                            }
                        };
                interstitial.loadAd(
                        interstitial.buildLoadAdConfig()
                                .withAdListener(listener)
                                .build());
                return () -> {
                    Log.v(TAG, "Showing interstitial ad; loaded: " + interstitial.isAdLoaded());
                    if (interstitial.isAdLoaded() && !interstitial.isAdInvalidated()) {
                        interstitial.show();
                    }
                };
            }
            case "mopub": {
                //noinspection ConstantConditions
                MoPubInterstitial interstitial = new MoPubInterstitial(activity, mAd.unit);
                interstitial.setInterstitialAdListener(new MoPubInterstitial.InterstitialAdListener() {

                    @Override
                    public void onInterstitialLoaded(MoPubInterstitial interstitial) {
                        Log.v(TAG, "Interstitial ad from MoPub was loaded.");
                    }

                    @Override
                    public void onInterstitialFailed(MoPubInterstitial interstitial, MoPubErrorCode error) {
                        Log.e(TAG, "Interstitial ad from MoPub failed to load.\n" + error.toString());
                    }

                    @Override
                    public void onInterstitialShown(MoPubInterstitial interstitial) {
                        interstitial.forceRefresh();
                    }

                    @Override
                    public void onInterstitialClicked(MoPubInterstitial interstitial) {
                    }

                    @Override
                    public void onInterstitialDismissed(MoPubInterstitial interstitial) {
                    }
                });
                if (MoPub.isSdkInitialized()) {
                    interstitial.load();
                } else {
                    SdkConfiguration config = new SdkConfiguration.Builder(mAd.unit)
                            .withLogLevel(BuildConfig.DEBUG ? MoPubLog.LogLevel.DEBUG : MoPubLog.LogLevel.NONE)
                            .build();
                    MoPub.initializeSdk(activity, config, interstitial::load);
                }
                return () -> {
                    Log.v(TAG, "Showing interstitial ad; loaded: " + interstitial.isReady());
                    if (interstitial.isReady()) {
                        interstitial.show();
                    }
                };
            }
            case "custom":
                return () -> {
                    Intent intent = new Intent(activity, InterstitialAdActivity.class);
                    intent.putExtra(InterstitialAdActivity.EXTRA_IMAGE, mAd.image);
                    intent.putExtra(InterstitialAdActivity.EXTRA_LINK, mAd.link);
                    activity.startActivity(intent);
                };
            default:
                return null;
        }
    }

    public final int getInterval() {
        return mAd.getInterval();
    }

    private static class Holder<T> {

        private T mValue;

        public T get() {
            return mValue;
        }

        public void set(T value) {
            mValue = value;
        }
    }
}
