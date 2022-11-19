package com.ctg.coptok.ads.natives;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.LayoutRes;
import androidx.annotation.Nullable;

import com.mopub.common.MoPub;
import com.mopub.common.SdkConfiguration;
import com.mopub.common.logging.MoPubLog;
import com.mopub.nativeads.MoPubNative;
import com.mopub.nativeads.MoPubStaticNativeAdRenderer;
import com.mopub.nativeads.NativeAd;
import com.mopub.nativeads.NativeErrorCode;
import com.mopub.nativeads.ViewBinder;

import java.util.ArrayList;
import java.util.List;

import com.ctg.coptok.BuildConfig;
import com.ctg.coptok.R;
import com.ctg.coptok.common.LoadingState;
import com.ctg.coptok.data.models.Advertisement;

public class MoPubNativeAdProvider extends NativeAdProvider {

    private static final String TAG = "MoPubNativeAdProvider";

    private final List<NativeAd> mAds = new ArrayList<>();
    private int mLayout = R.layout.view_native_mopub_item;
    private int mPosition = 0;

    public MoPubNativeAdProvider(Advertisement ad, Context context) {
        super(ad);
        //noinspection ConstantConditions
        MoPubNative loader = new MoPubNative(context, ad.unit, new MoPubNative.MoPubNativeNetworkListener() {

            @Override
            public void onNativeLoad(NativeAd ad) {
                Log.v(TAG, "Native ad from MoPub was loaded.");
                mAds.add(ad);
                state.postValue(LoadingState.LOADED);
            }

            @Override
            public void onNativeFail(NativeErrorCode error) {
                Log.e(TAG, "Native ad from MoPub failed to load.\n" + error.toString());
                state.postValue(LoadingState.ERROR);
            }
        });
        ViewBinder binder = new ViewBinder.Builder(mLayout)
                .iconImageId(R.id.icon)
                .titleId(R.id.title)
                .textId(R.id.text)
                .mainImageId(R.id.image)
                .privacyInformationIconImageId(R.id.privacy)
                .sponsoredTextId(R.id.sponsored)
                .build();
        loader.registerAdRenderer(new MoPubStaticNativeAdRenderer(binder));
        if (MoPub.isSdkInitialized()) {
            loader.makeRequest();
        } else {
            //noinspection ConstantConditions
            SdkConfiguration config = new SdkConfiguration.Builder(mAd.unit)
                    .withLogLevel(BuildConfig.DEBUG ? MoPubLog.LogLevel.DEBUG : MoPubLog.LogLevel.NONE)
                    .build();
            MoPub.initializeSdk(context, config, loader::makeRequest);
        }
    }

    @Nullable
    @Override
    public View create(LayoutInflater inflater, @Nullable ViewGroup parent) {
        if (state.getValue() != LoadingState.LOADED) {
            return null;
        }

        int i = mPosition + 1;
        if (i > mAds.size() - 1) {
            i = 0;
        }

        NativeAd ad = mAds.get(mPosition = i);
        View root = ad.createAdView(inflater.getContext(), parent);
        ad.renderAdView(root);
        return root;
    }

    public void setLayout(@LayoutRes int layout) {
        mLayout = layout;
    }
}
