package com.ctg.coptok.ads.natives;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.annotation.Nullable;

import com.facebook.ads.AdError;
import com.facebook.ads.AdOptionsView;
import com.facebook.ads.MediaView;
import com.facebook.ads.NativeAd;
import com.facebook.ads.NativeAdLayout;
import com.facebook.ads.NativeAdsManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.ctg.coptok.R;
import com.ctg.coptok.common.LoadingState;
import com.ctg.coptok.data.models.Advertisement;

@SuppressLint("LongLogTag")
public class FacebookNativeAdProvider extends NativeAdProvider {

    private static final String TAG = "FacebookNativeAdProvider";

    private int mLayout = R.layout.view_native_facebook_item;
    private final NativeAdsManager mManager;

    public FacebookNativeAdProvider(Advertisement ad, Context context, int count) {
        super(ad);
        mManager = new NativeAdsManager(context, ad.unit, count);
        mManager.setListener(new NativeAdsManager.Listener() {

            @Override
            public void onAdsLoaded() {
                Log.v(TAG, "Native ad from Facebook was loaded.");
                state.postValue(LoadingState.LOADED);
            }

            @Override
            public void onAdError(AdError error) {
                Log.e(TAG, "Native ad from Facebook failed to load.\n" + error.getErrorMessage());
                state.postValue(LoadingState.ERROR);
            }
        });
        mManager.loadAds();
    }

    @Nullable
    @Override
    public View create(LayoutInflater inflater, @Nullable ViewGroup parent) {
        if (!mManager.isLoaded()) {
            return null;
        }

        NativeAd ad = mManager.nextNativeAd();
        if (ad == null || ad.isAdInvalidated()) {
            return null;
        }

        ad.unregisterView();
        View root = inflater.inflate(mLayout, parent, false);
        NativeAdLayout adv = root.findViewById(R.id.ad);
        TextView title = root.findViewById(R.id.title);
        TextView context = root.findViewById(R.id.context);
        TextView body = root.findViewById(R.id.body);
        Button cta = root.findViewById(R.id.cta);
        LinearLayout choices = root.findViewById(R.id.choices);
        title.setText(ad.getAdvertiserName());
        context.setText(ad.getAdSocialContext());
        body.setText(ad.getAdBodyText());
        if (ad.hasCallToAction()) {
            cta.setText(ad.getAdCallToAction());
            cta.setVisibility(View.VISIBLE);
        } else {
            cta.setVisibility(View.GONE);
        }
        choices.removeAllViews();
        choices.addView(new AdOptionsView(adv.getContext(), ad, adv));
        MediaView media = root.findViewById(R.id.media);
        MediaView icon = root.findViewById(R.id.icon);
        List<View> views = new ArrayList<>();
        Collections.addAll(views, title, body, cta);
        ad.registerViewForInteraction(adv, media, icon, views);
        return root;
    }

    public void setLayout(@LayoutRes int layout) {
        mLayout = layout;
    }
}
