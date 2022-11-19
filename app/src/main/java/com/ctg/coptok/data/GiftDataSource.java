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
import com.ctg.coptok.data.models.Gift;
import com.ctg.coptok.data.models.Wrappers;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class GiftDataSource extends PageKeyedDataSource<Integer, Gift> {

    private static final String TAG = "GiftDataSource";

    public final MutableLiveData<LoadingState> state = new MutableLiveData<>(LoadingState.IDLE);

    @Override
    public void loadInitial(@NonNull LoadInitialParams<Integer> params, @NonNull final LoadInitialCallback<Integer, Gift> callback) {
        state.postValue(LoadingState.LOADING);
        REST rest = MainApplication.getContainer().get(REST.class);
        rest.walletGifts(1)
                .enqueue(new Callback<Wrappers.Paginated<Gift>>() {

                    @Override
                    public void onResponse(
                            @Nullable Call<Wrappers.Paginated<Gift>> call,
                            @Nullable Response<Wrappers.Paginated<Gift>> response
                    ) {
                        //noinspection ConstantConditions
                        Log.v(TAG, "Server responded with " + response.code() + " status.");
                        if (response.isSuccessful()) {
                            Wrappers.Paginated<Gift> gifts = response.body();
                            //noinspection ConstantConditions
                            callback.onResult(gifts.data,null, null);
                            state.postValue(LoadingState.LOADED);
                        } else {
                            state.postValue(LoadingState.ERROR);
                        }
                    }

                    @Override
                    public void onFailure(
                            @Nullable Call<Wrappers.Paginated<Gift>> call,
                            @Nullable Throwable t
                    ) {
                        Log.e(TAG, "Fetching gifts has failed.", t);
                        state.postValue(LoadingState.ERROR);
                    }
                });
    }

    @Override
    public void loadBefore(
            @NonNull LoadParams<Integer> params,
            @NonNull LoadCallback<Integer, Gift> callback
    ) {
    }

    @Override
    public void loadAfter(
            @NonNull final LoadParams<Integer> params,
            @NonNull final LoadCallback<Integer, Gift> callback
    ) {
    }

    public static class Factory extends DataSource.Factory<Integer, Gift> {

        public MutableLiveData<GiftDataSource> source = new MutableLiveData<>();

        @NonNull
        @Override
        public DataSource<Integer, Gift> create() {
            GiftDataSource source = new GiftDataSource();
            this.source.postValue(source);
            return source;
        }
    }
}
