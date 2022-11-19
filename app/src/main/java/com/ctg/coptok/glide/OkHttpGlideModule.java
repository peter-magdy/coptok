package com.ctg.coptok.glide;

import android.content.Context;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Registry;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.integration.okhttp3.OkHttpUrlLoader;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.module.LibraryGlideModule;

import org.jetbrains.annotations.NotNull;

import java.io.InputStream;

import com.ctg.coptok.MainApplication;
import okhttp3.OkHttpClient;

@GlideModule
public class OkHttpGlideModule extends LibraryGlideModule {

    @Override
    public void registerComponents(@NotNull Context context, @NotNull Glide glide, Registry registry) {
        OkHttpClient client = MainApplication.getContainer()
                .get(OkHttpClient.Builder.class)
                .build();
        registry.replace(GlideUrl.class, InputStream.class, new OkHttpUrlLoader.Factory(client));
    }
}
