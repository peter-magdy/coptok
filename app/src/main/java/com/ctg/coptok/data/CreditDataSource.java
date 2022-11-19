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
import com.ctg.coptok.data.models.Credit;
import com.ctg.coptok.data.models.Wrappers;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CreditDataSource extends PageKeyedDataSource<Integer, Credit> {

    private static final String TAG = "CreditDataSource";

    public final MutableLiveData<LoadingState> state = new MutableLiveData<>(LoadingState.IDLE);

    @Override
    public void loadInitial(@NonNull LoadInitialParams<Integer> params, @NonNull final LoadInitialCallback<Integer, Credit> callback) {
        state.postValue(LoadingState.LOADING);
        REST rest = MainApplication.getContainer().get(REST.class);
        rest.creditsIndex(1)
                .enqueue(new Callback<Wrappers.Paginated<Credit>>() {

                    @Override
                    public void onResponse(
                            @Nullable Call<Wrappers.Paginated<Credit>> call,
                            @Nullable Response<Wrappers.Paginated<Credit>> response
                    ) {
                        //noinspection ConstantConditions
                        Log.v(TAG, "Server responded with " + response.code() + " status.");
                        if (response.isSuccessful()) {
                            Wrappers.Paginated<Credit> credits = response.body();
                            //noinspection ConstantConditions
                            callback.onResult(credits.data,null, null);
                            state.postValue(LoadingState.LOADED);
                        } else {
                            state.postValue(LoadingState.ERROR);
                        }
                    }

                    @Override
                    public void onFailure(
                            @Nullable Call<Wrappers.Paginated<Credit>> call,
                            @Nullable Throwable t
                    ) {
                        Log.e(TAG, "Fetching credits has failed.", t);
                        state.postValue(LoadingState.ERROR);
                    }
                });
    }

    @Override
    public void loadBefore(
            @NonNull LoadParams<Integer> params,
            @NonNull LoadCallback<Integer, Credit> callback
    ) {
    }

    @Override
    public void loadAfter(
            @NonNull final LoadParams<Integer> params,
            @NonNull final LoadCallback<Integer, Credit> callback
    ) {
    }

    public static class Factory extends DataSource.Factory<Integer, Credit> {

        public MutableLiveData<CreditDataSource> source = new MutableLiveData<>();

        @NonNull
        @Override
        public DataSource<Integer, Credit> create() {
            CreditDataSource source = new CreditDataSource();
            this.source.postValue(source);
            return source;
        }
    }
}
