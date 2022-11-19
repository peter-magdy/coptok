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
import com.ctg.coptok.data.models.Redemption;
import com.ctg.coptok.data.models.Wrappers;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RedemptionDataSource extends PageKeyedDataSource<Integer, Redemption> {

    private static final String TAG = "RedemptionDataSource";

    public final MutableLiveData<LoadingState> state = new MutableLiveData<>(LoadingState.IDLE);

    @Override
    public void loadInitial(@NonNull LoadInitialParams<Integer> params, @NonNull final LoadInitialCallback<Integer, Redemption> callback) {
        state.postValue(LoadingState.LOADING);
        REST rest = MainApplication.getContainer().get(REST.class);
        rest.walletRedemptions(1)
                .enqueue(new Callback<Wrappers.Paginated<Redemption>>() {

                    @Override
                    public void onResponse(
                            @Nullable Call<Wrappers.Paginated<Redemption>> call,
                            @Nullable Response<Wrappers.Paginated<Redemption>> response
                    ) {
                        //noinspection ConstantConditions
                        Log.v(TAG, "Server responded with " + response.code() + " status.");
                        if (response.isSuccessful()) {
                            Wrappers.Paginated<Redemption> redemptions = response.body();
                            //noinspection ConstantConditions
                            callback.onResult(redemptions.data,null, 2);
                            state.postValue(LoadingState.LOADED);
                        } else {
                            state.postValue(LoadingState.ERROR);
                        }
                    }

                    @Override
                    public void onFailure(
                            @Nullable Call<Wrappers.Paginated<Redemption>> call,
                            @Nullable Throwable t
                    ) {
                        Log.e(TAG, "Fetching redemptions has failed.", t);
                        state.postValue(LoadingState.ERROR);
                    }
                });
    }

    @Override
    public void loadBefore(
            @NonNull LoadParams<Integer> params,
            @NonNull LoadCallback<Integer, Redemption> callback
    ) {
    }

    @Override
    public void loadAfter(@NonNull final LoadParams<Integer> params, @NonNull final LoadCallback<Integer, Redemption> callback) {
        state.postValue(LoadingState.LOADING);
        REST rest = MainApplication.getContainer().get(REST.class);
        rest.walletRedemptions(params.key)
                .enqueue(new Callback<Wrappers.Paginated<Redemption>>() {

                    @Override
                    public void onResponse(
                            @Nullable Call<Wrappers.Paginated<Redemption>> call,
                            @Nullable Response<Wrappers.Paginated<Redemption>> response
                    ) {
                        //noinspection ConstantConditions
                        Log.v(TAG, "Server responded with " + response.code() + " status.");
                        if (response.isSuccessful()) {
                            Wrappers.Paginated<Redemption> redemptions = response.body();
                            //noinspection ConstantConditions
                            callback.onResult(redemptions.data,params.key + 1);
                            state.postValue(LoadingState.LOADED);
                        } else {
                            state.postValue(LoadingState.ERROR);
                        }
                    }

                    @Override
                    public void onFailure(
                            @Nullable Call<Wrappers.Paginated<Redemption>> call,
                            @Nullable Throwable t
                    ) {
                        Log.e(TAG, "Fetching redemptions has failed.", t);
                        state.postValue(LoadingState.ERROR);
                    }
                });
    }

    public static class Factory extends DataSource.Factory<Integer, Redemption> {

        public MutableLiveData<RedemptionDataSource> source = new MutableLiveData<>();

        @NonNull
        @Override
        public DataSource<Integer, Redemption> create() {
            RedemptionDataSource source = new RedemptionDataSource();
            this.source.postValue(source);
            return source;
        }
    }
}
