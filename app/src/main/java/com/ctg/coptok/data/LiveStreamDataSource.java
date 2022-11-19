package com.ctg.coptok.data;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;
import androidx.paging.DataSource;
import androidx.paging.PageKeyedDataSource;

import com.ctg.coptok.MainApplication;
import com.ctg.coptok.common.LoadingState;
import com.ctg.coptok.data.api.REST;
import com.ctg.coptok.data.models.LiveStream;
import com.ctg.coptok.data.models.Wrappers;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LiveStreamDataSource extends PageKeyedDataSource<Integer, LiveStream> {

    private static final String TAG = "LiveStreamDataSource";

    public final MutableLiveData<LoadingState> state = new MutableLiveData<>(LoadingState.IDLE);

    public LiveStreamDataSource() {
    }

    @Override
    public void loadInitial(
            @NonNull LoadInitialParams<Integer> params,
            @NonNull final LoadInitialCallback<Integer, LiveStream> callback
    ) {
        state.postValue(LoadingState.LOADING);
        REST rest = MainApplication.getContainer().get(REST.class);
        rest.liveStreamsIndex(1)
                .enqueue(new Callback<Wrappers.Paginated<LiveStream>>() {

                    @Override
                    public void onResponse(
                            @Nullable Call<Wrappers.Paginated<LiveStream>> call,
                            @Nullable Response<Wrappers.Paginated<LiveStream>> response
                    ) {
                        //noinspection ConstantConditions
                        Log.v(TAG, "Server responded with " + response.code() + " status.");
                        if (response.isSuccessful()) {
                            Wrappers.Paginated<LiveStream> users = response.body();
                            //noinspection ConstantConditions
                            callback.onResult(users.data,null, 2);
                            state.postValue(LoadingState.LOADED);
                        } else {
                            state.postValue(LoadingState.ERROR);
                        }
                    }

                    @Override
                    public void onFailure(
                            @Nullable Call<Wrappers.Paginated<LiveStream>> call,
                            @Nullable Throwable t
                    ) {
                        Log.e(TAG, "Fetching live streams has failed.", t);
                        state.postValue(LoadingState.ERROR);
                    }
                });
    }

    @Override
    public void loadBefore(
            @NonNull LoadParams<Integer> params,
            @NonNull LoadCallback<Integer, LiveStream> callback
    ) {
    }

    @Override
    public void loadAfter(
            @NonNull final LoadParams<Integer> params,
            @NonNull final LoadCallback<Integer, LiveStream> callback
    ) {
        state.postValue(LoadingState.LOADING);
        REST rest = MainApplication.getContainer().get(REST.class);
        rest.liveStreamsIndex(params.key)
                .enqueue(new Callback<Wrappers.Paginated<LiveStream>>() {

                    @Override
                    public void onResponse(
                            @Nullable Call<Wrappers.Paginated<LiveStream>> call,
                            @Nullable Response<Wrappers.Paginated<LiveStream>> response
                    ) {
                        //noinspection ConstantConditions
                        Log.v(TAG, "Server responded with " + response.code() + " status.");
                        if (response.isSuccessful()) {
                            Wrappers.Paginated<LiveStream> users = response.body();
                            //noinspection ConstantConditions
                            callback.onResult(users.data,params.key + 1);
                            state.postValue(LoadingState.LOADED);
                        } else {
                            state.postValue(LoadingState.ERROR);
                        }
                    }

                    @Override
                    public void onFailure(
                            @Nullable Call<Wrappers.Paginated<LiveStream>> call,
                            @Nullable Throwable t
                    ) {
                        Log.e(TAG, "Fetching live streams has failed.", t);
                        state.postValue(LoadingState.ERROR);
                    }
                });
    }

    public static class Factory extends DataSource.Factory<Integer, LiveStream> {

        public MutableLiveData<LiveStreamDataSource> source = new MutableLiveData<>();

        public Factory() {
        }

        @NonNull
        @Override
        public DataSource<Integer, LiveStream> create() {
            LiveStreamDataSource source = new LiveStreamDataSource();
            this.source.postValue(source);
            return source;
        }
    }
}
