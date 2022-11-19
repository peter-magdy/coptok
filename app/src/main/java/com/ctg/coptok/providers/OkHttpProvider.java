package com.ctg.coptok.providers;

import android.content.Context;

import com.vaibhavpandey.katora.contracts.MutableContainer;
import com.vaibhavpandey.katora.contracts.Provider;

import java.util.concurrent.TimeUnit;

import com.ctg.coptok.BuildConfig;
import com.ctg.coptok.R;
import com.ctg.coptok.SharedConstants;
import okhttp3.CertificatePinner;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;

public class OkHttpProvider implements Provider {

    private final Context mContext;

    public OkHttpProvider(Context context) {
        mContext = context;
    }

    @Override
    public void provide(MutableContainer container) {
        container.factory(OkHttpClient.Builder.class, c -> {
            OkHttpClient.Builder builder = new OkHttpClient.Builder()
                    .connectTimeout(SharedConstants.TIMEOUT_CONNECT, TimeUnit.MILLISECONDS)
                    .readTimeout(SharedConstants.TIMEOUT_READ, TimeUnit.MILLISECONDS)
                    .writeTimeout(SharedConstants.TIMEOUT_WRITE, TimeUnit.MILLISECONDS);
            if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
                interceptor.setLevel(HttpLoggingInterceptor.Level.BASIC);
                builder.addInterceptor(interceptor);
            }
            if (mContext.getResources().getBoolean(R.bool.ssl_pinning_enabled)) {
                builder.certificatePinner(
                        new CertificatePinner.Builder()
                                .add(mContext.getString(R.string.ssl_pinning_domain),
                                        mContext.getString(R.string.ssl_pinning_certificate))
                                .build());
            }
            return builder;
        });
    }
}
