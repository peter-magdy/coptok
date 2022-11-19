package com.ctg.coptok.providers;

import android.content.Context;
import android.text.TextUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pixplicity.easyprefs.library.Prefs;
import com.vaibhavpandey.katora.contracts.MutableContainer;
import com.vaibhavpandey.katora.contracts.Provider;

import com.ctg.coptok.R;
import com.ctg.coptok.SharedConstants;
import com.ctg.coptok.data.api.REST;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

public class RetrofitProvider implements Provider {

    private final Context mContext;
    private final String mServerKey;

    public RetrofitProvider(Context context) {
        mContext = context;
        mServerKey = context.getString(R.string.server_api_key);
    }

    @Override
    public void provide(MutableContainer container) {
        container.factory(Retrofit.Builder.class, c -> {
            ObjectMapper om = c.get(ObjectMapper.class);
            return new Retrofit.Builder()
                    .baseUrl(mContext.getString(R.string.server_url))
                    .addConverterFactory(JacksonConverterFactory.create(om));
        });
        container.factory(Retrofit.class, c -> {
            OkHttpClient client = c.get(OkHttpClient.Builder.class)
                    .addInterceptor(chain -> {
                        Request request = chain.request();
                        String token =
                                Prefs.getString(SharedConstants.PREF_SERVER_TOKEN, null);
                        if (!TextUtils.isEmpty(token)) {
                            request = request.newBuilder()
                                    .header("Authorization", "Bearer " + token)
                                    .build();
                        }
                        if (!TextUtils.isEmpty(mServerKey)) {
                            request = request.newBuilder()
                                    .header("X-API-Key", mServerKey)
                                    .build();
                        }
                        return chain.proceed(request);
                    })
                    .build();
            return c.get(Retrofit.Builder.class)
                    .client(client)
                    .build();
        });
        container.singleton(REST.class, c -> c.get(Retrofit.class).create(REST.class));
    }
}
