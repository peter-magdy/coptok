package com.ctg.coptok.utils;

import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Nullable;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pixplicity.easyprefs.library.Prefs;

import java.util.ArrayList;
import java.util.List;

import com.ctg.coptok.MainApplication;
import com.ctg.coptok.SharedConstants;
import com.ctg.coptok.data.models.Advertisement;

final public class AdsUtil {

    private static final String TAG = "AdsUtil";

    private static List<Advertisement> findAll() {
        String json = Prefs.getString(SharedConstants.PREF_ADS_CONFIG, null);
        if (!TextUtils.isEmpty(json)) {
            ObjectMapper om = MainApplication.getContainer().get(ObjectMapper.class);
            try {
                return om.readValue(json, new TypeReference<List<Advertisement>>(){});
            } catch (JsonProcessingException e) {
                Log.e(TAG, "Could not parse ads config from shared preferences.", e);
            }
        }

        return new ArrayList<>();
    }

    @Nullable
    public static Advertisement findByLocationAndType(String location, String type) {
        List<Advertisement> all = findAll();
        for (Advertisement ad : all) {
            if (TextUtils.equals(ad.location, location) && TextUtils.equals(ad.type, type)) {
                return ad;
            }
        }

        return null;
    }

    public static List<Advertisement> findByNetwork(String network) {
        List<Advertisement> ads = new ArrayList<>();
        List<Advertisement> all = findAll();
        for (Advertisement ad : all) {
            if (TextUtils.equals(ad.location, network)) {
                ads.add(ad);
            }
        }

        return ads;
    }
}
