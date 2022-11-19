package com.ctg.coptok.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.text.TextUtils;

import androidx.preference.PreferenceManager;

import java.util.Locale;

import com.ctg.coptok.SharedConstants;

final public class LocaleUtil {

    public static void override(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String custom = prefs.getString(SharedConstants.PREF_APP_LOCALE, null);
        if (TextUtils.isEmpty(custom)) {
            return;
        }

        Locale changed = new Locale(custom);
        Locale.setDefault(changed);
        Configuration configuration = new Configuration();
        configuration.setLocale(changed);
        configuration.setLayoutDirection(changed);
        Resources resources1 = context.getResources();
        resources1.updateConfiguration(configuration, resources1.getDisplayMetrics());
        if (context != context.getApplicationContext()) {
            Resources resources2 = context.getApplicationContext().getResources();
            resources2.updateConfiguration(configuration, resources2.getDisplayMetrics());
        }
    }

    public static Context wrap(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String custom = prefs.getString(SharedConstants.PREF_APP_LOCALE, null);
        if (TextUtils.isEmpty(custom)) {
            return context;
        }

        Locale changed = new Locale(custom);
        Locale.setDefault(changed);
        Configuration configuration = new Configuration();
        configuration.setLocale(changed);
        configuration.setLayoutDirection(changed);
        return context.createConfigurationContext(configuration);
    }
}
