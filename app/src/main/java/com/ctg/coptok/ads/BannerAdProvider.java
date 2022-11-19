package com.ctg.coptok.ads;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;

import com.adcolony.sdk.AdColony;
import com.adcolony.sdk.AdColonyAdSize;
import com.adcolony.sdk.AdColonyAdView;
import com.adcolony.sdk.AdColonyAdViewListener;
import com.adcolony.sdk.AdColonyZone;
import com.bumptech.glide.Glide;
import com.facebook.ads.Ad;
import com.facebook.ads.AdError;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.mopub.common.MoPub;
import com.mopub.common.SdkConfiguration;
import com.mopub.common.logging.MoPubLog;
import com.mopub.mobileads.DefaultBannerAdListener;
import com.mopub.mobileads.MoPubErrorCode;
import com.mopub.mobileads.MoPubView;

import com.ctg.coptok.BuildConfig;
import com.ctg.coptok.data.models.Advertisement;
import com.ctg.coptok.utils.SizeUtil;

public class BannerAdProvider {

    private static final String TAG = "BannerAdProvider";

    private final Advertisement mAd;

    public BannerAdProvider(Advertisement ad) {
        mAd = ad;
    }

    public void create(Context context, Listener listener) {
        switch (mAd.network) {
            case "adcolony":
                AdColony.requestAdView(mAd.unit, new AdColonyAdViewListener() {

                    @Override
                    public void onRequestFilled(AdColonyAdView ad) {
                        Log.v(TAG, "Banner ad from AdColony was loaded.");
                        listener.onBannerView(ad);
                    }

                    @Override
                    public void onRequestNotFilled(AdColonyZone zone) {
                        Log.v(TAG, "Banner ad from AdColony failed to load.");
                    }

                }, AdColonyAdSize.BANNER);
                break;
            case "admob": {
                AdView ad = new AdView(context);
                ad.setAdListener(new AdListener() {

                    public void onAdFailedToLoad(LoadAdError error) {
                        Log.e(TAG, "Banner ad from AdMob failed to load.\n" + error.toString());
                    }

                    public void onAdLoaded() {
                        Log.v(TAG, "Banner ad from AdMob was loaded.");
                    }
                });
                ad.setAdSize(AdSize.BANNER);
                ad.setAdUnitId(mAd.unit);
                ad.loadAd(new AdRequest.Builder().build());
                listener.onBannerView(ad);
                break;
            }
            case "facebook": {
                String placement = BuildConfig.DEBUG ? "IMG_16_9_APP_INSTALL#" + mAd.unit : mAd.unit;
                com.facebook.ads.AdView adv =
                        new com.facebook.ads.AdView(
                                context, placement, com.facebook.ads.AdSize.BANNER_HEIGHT_50);
                com.facebook.ads.AdListener listener2 = new com.facebook.ads.AbstractAdListener() {

                    @Override
                    public void onAdLoaded(Ad ad) {
                        Log.v(TAG, "Banner ad from Facebook was loaded.");
                    }

                    @Override
                    public void onError(Ad ad, AdError error) {
                        Log.e(TAG, "Banner ad from Facebook failed to load.\n" + error.getErrorMessage());
                    }
                };
                adv.loadAd(adv.buildLoadAdConfig().withAdListener(listener2).build());
                listener.onBannerView(adv);
                break;
            }
            case "mopub": {
                MoPubView ad = new MoPubView(context);
                ad.setAdUnitId(mAd.unit);
                ad.setAdSize(MoPubView.MoPubAdSize.HEIGHT_50);
                ad.setBannerAdListener(new DefaultBannerAdListener() {

                    @Override
                    public void onBannerLoaded(@NonNull MoPubView banner) {
                        Log.v(TAG, "Banner ad from MoPub was loaded.");
                    }

                    @Override
                    public void onBannerFailed(MoPubView banner, MoPubErrorCode error) {
                        Log.e(TAG, "Banner ad from MoPub failed to load.\n" + error);
                    }
                });
                if (MoPub.isSdkInitialized()) {
                    ad.loadAd();
                } else {
                    SdkConfiguration config = new SdkConfiguration.Builder(mAd.unit)
                            .withLogLevel(BuildConfig.DEBUG ? MoPubLog.LogLevel.DEBUG : MoPubLog.LogLevel.NONE)
                            .build();
                    MoPub.initializeSdk(context, config, ad::loadAd);
                }
                listener.onBannerView(ad);
                break;
            }
            case "custom": {
                AppCompatImageView ad = new AppCompatImageView(context);
                ad.setLayoutParams(
                        new ViewGroup.LayoutParams(
                                SizeUtil.toPx(context.getResources(), 320),
                                SizeUtil.toPx(context.getResources(), 50)));
                ad.setScaleType(ImageView.ScaleType.FIT_CENTER);
                //noinspection ConstantConditions
                if (mAd.image.endsWith(".gif")) {
                    Glide.with(context)
                            .asGif()
                            .load(mAd.image)
                            .into(ad);
                } else {
                    Glide.with(context)
                            .load(mAd.image)
                            .into(ad);
                }

                ad.setOnClickListener(v ->
                        context.startActivity(
                                new Intent(Intent.ACTION_VIEW, Uri.parse(mAd.link))));
                listener.onBannerView(ad);
                break;
            }
            default:
                break;
        }
    }

    public interface Listener {

        void onBannerView(View view);
    }
}
