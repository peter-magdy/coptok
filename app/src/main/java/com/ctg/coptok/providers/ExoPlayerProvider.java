package com.ctg.coptok.providers;

import android.content.Context;

import com.danikula.videocache.HttpProxyCacheServer;
import com.vaibhavpandey.katora.contracts.MutableContainer;
import com.vaibhavpandey.katora.contracts.Provider;

import com.ctg.coptok.SharedConstants;

public class ExoPlayerProvider implements Provider {

    private final Context mContext;

    public ExoPlayerProvider(Context context) {
        mContext = context;
    }

    @Override
    public void provide(MutableContainer container) {
        container.singleton(
                HttpProxyCacheServer.class,
                c -> new HttpProxyCacheServer.Builder(mContext)
                        .maxCacheSize(SharedConstants.CACHE_SIZE)
                        .maxCacheFilesCount(SharedConstants.CACHE_COUNT)
                        .build()
        );
    }
}
