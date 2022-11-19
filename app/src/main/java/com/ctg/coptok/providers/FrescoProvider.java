package com.ctg.coptok.providers;

import android.content.Context;

import com.facebook.imagepipeline.backends.okhttp3.OkHttpImagePipelineConfigFactory;
import com.facebook.imagepipeline.core.ImagePipelineConfig;
import com.vaibhavpandey.katora.contracts.MutableContainer;
import com.vaibhavpandey.katora.contracts.Provider;

import okhttp3.OkHttpClient;

public class FrescoProvider implements Provider {

    private final Context mContext;

    public FrescoProvider(Context context) {
        mContext = context;
    }

    @Override
    public void provide(MutableContainer container) {
        container.factory(ImagePipelineConfig.class, c -> {
            OkHttpClient client = c.get(OkHttpClient.Builder.class)
                    .build();
            return OkHttpImagePipelineConfigFactory.newBuilder(mContext, client)
                    .build();
        });
    }
}
