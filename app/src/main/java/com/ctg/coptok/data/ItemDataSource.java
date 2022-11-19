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
import com.ctg.coptok.data.models.Item;
import com.ctg.coptok.data.models.Wrappers;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ItemDataSource extends PageKeyedDataSource<Integer, Item> {

    private static final String TAG = "ItemDataSource";

    public final MutableLiveData<LoadingState> state = new MutableLiveData<>(LoadingState.IDLE);

    @Override
    public void loadInitial(@NonNull LoadInitialParams<Integer> params, @NonNull final LoadInitialCallback<Integer, Item> callback) {
        state.postValue(LoadingState.LOADING);
        REST rest = MainApplication.getContainer().get(REST.class);
        rest.itemsIndex(1)
                .enqueue(new Callback<Wrappers.Paginated<Item>>() {

                    @Override
                    public void onResponse(
                            @Nullable Call<Wrappers.Paginated<Item>> call,
                            @Nullable Response<Wrappers.Paginated<Item>> response
                    ) {
                        //noinspection ConstantConditions
                        Log.v(TAG, "Server responded with " + response.code() + " status.");
                        if (response.isSuccessful()) {
                            Wrappers.Paginated<Item> items = response.body();
                            //noinspection ConstantConditions
                            callback.onResult(items.data,null, 2);
                            state.postValue(LoadingState.LOADED);
                        } else {
                            state.postValue(LoadingState.ERROR);
                        }
                    }

                    @Override
                    public void onFailure(
                            @Nullable Call<Wrappers.Paginated<Item>> call,
                            @Nullable Throwable t
                    ) {
                        Log.e(TAG, "Fetching items has failed.", t);
                        state.postValue(LoadingState.ERROR);
                    }
                });
    }

    @Override
    public void loadBefore(
            @NonNull LoadParams<Integer> params,
            @NonNull LoadCallback<Integer, Item> callback
    ) {
    }

    @Override
    public void loadAfter(@NonNull final LoadParams<Integer> params, @NonNull final LoadCallback<Integer, Item> callback) {
        state.postValue(LoadingState.LOADING);
        REST rest = MainApplication.getContainer().get(REST.class);
        rest.itemsIndex(params.key)
                .enqueue(new Callback<Wrappers.Paginated<Item>>() {

                    @Override
                    public void onResponse(
                            @Nullable Call<Wrappers.Paginated<Item>> call,
                            @Nullable Response<Wrappers.Paginated<Item>> response
                    ) {
                        //noinspection ConstantConditions
                        Log.v(TAG, "Server responded with " + response.code() + " status.");
                        if (response.isSuccessful()) {
                            Wrappers.Paginated<Item> items = response.body();
                            //noinspection ConstantConditions
                            callback.onResult(items.data,params.key + 1);
                            state.postValue(LoadingState.LOADED);
                        } else {
                            state.postValue(LoadingState.ERROR);
                        }
                    }

                    @Override
                    public void onFailure(
                            @Nullable Call<Wrappers.Paginated<Item>> call,
                            @Nullable Throwable t
                    ) {
                        Log.e(TAG, "Fetching items has failed.", t);
                        state.postValue(LoadingState.ERROR);
                    }
                });
    }

    public static class Factory extends DataSource.Factory<Integer, Item> {

        public MutableLiveData<ItemDataSource> source = new MutableLiveData<>();

        @NonNull
        @Override
        public DataSource<Integer, Item> create() {
            ItemDataSource source = new ItemDataSource();
            this.source.postValue(source);
            return source;
        }
    }
}
