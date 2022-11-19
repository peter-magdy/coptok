package com.ctg.coptok.fragments;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.paging.LivePagedListBuilder;
import androidx.paging.PagedList;
import androidx.paging.PagedListAdapter;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.facebook.drawee.view.SimpleDraweeView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.kaopiz.kprogresshud.KProgressHUD;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Collections;
import java.util.List;

import com.ctg.coptok.MainApplication;
import com.ctg.coptok.R;
import com.ctg.coptok.SharedConstants;
import com.ctg.coptok.common.DiffUtilCallback;
import com.ctg.coptok.common.LoadingState;
import com.ctg.coptok.data.GiftDataSource;
import com.ctg.coptok.data.api.REST;
import com.ctg.coptok.data.models.Gift;
import com.ctg.coptok.data.models.Wrappers;
import com.ctg.coptok.events.GiftRedeemedEvent;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class GiftsFragment extends Fragment {

    private static final String TAG = "GiftsFragment";

    private GiftsFragmentViewModel mModel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mModel = new ViewModelProvider(this).get(GiftsFragmentViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_gifts, container, false);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onGiftRedeemedEvent(GiftRedeemedEvent event) {
        GiftDataSource source = mModel.factory.source.getValue();
        if (source != null) {
            source.invalidate();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        GiftAdapter adapter = new GiftAdapter();
        RecyclerView gifts = view.findViewById(R.id.gifts);
        gifts.setAdapter(adapter);
        gifts.addItemDecoration(new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL));
        mModel.gifts.observe(getViewLifecycleOwner(), adapter::submitList);
        SwipeRefreshLayout swipe = view.findViewById(R.id.swipe);
        swipe.setOnRefreshListener(() -> {
            GiftDataSource source = mModel.factory.source.getValue();
            if (source != null) {
                source.invalidate();
            }
        });
        View empty = view.findViewById(R.id.empty);
        View loading = view.findViewById(R.id.loading);
        mModel.state.observe(getViewLifecycleOwner(), state -> {
            if (state != LoadingState.LOADING) {
                swipe.setRefreshing(false);
            }
            List<?> list = mModel.gifts.getValue();
            if (state == LoadingState.LOADING) {
                empty.setVisibility(View.GONE);
            } else {
                empty.setVisibility(list == null || list.isEmpty() ? View.VISIBLE : View.GONE);
            }
            loading.setVisibility(state == LoadingState.LOADING ? View.VISIBLE : View.GONE);
        });
    }

    private void confirmAndRedeem(Gift gift) {
        new MaterialAlertDialogBuilder(requireContext())
                .setMessage(R.string.confirmation_redeem_gift)
                .setNegativeButton(R.string.cancel_button, (dialog, i) -> dialog.cancel())
                .setPositiveButton(R.string.yes_button, (dialog, i) -> {
                    dialog.dismiss();
                    redeemGift(gift);
                })
                .show();
    }

    public static GiftsFragment newInstance() {
        return new GiftsFragment();
    }

    private void redeemGift(Gift gift) {
        KProgressHUD progress = KProgressHUD.create(requireActivity())
                .setStyle(KProgressHUD.Style.SPIN_INDETERMINATE)
                .setLabel(getString(R.string.progress_title))
                .setCancellable(false)
                .show();
        REST rest = MainApplication.getContainer().get(REST.class);
        rest.walletRedeem(Collections.singletonList(gift.item.id))
                .enqueue(new Callback<Wrappers.Paginated<Gift>>() {

                    @Override
                    public void onResponse(
                            @Nullable Call<Wrappers.Paginated<Gift>> call,
                            @Nullable Response<Wrappers.Paginated<Gift>> response
                    ) {
                        int code = response != null ? response.code() : -1;
                        Log.v(TAG, "Redeeming received gift returned " + code + ".");
                        progress.dismiss();
                        if (response != null && response.isSuccessful()) {
                            EventBus.getDefault().post(new GiftRedeemedEvent());
                            if (isAdded()) {
                                Toast.makeText(requireContext(), R.string.message_redemption_submitted, Toast.LENGTH_SHORT).show();
                            }
                        } else if (code == 422 && isAdded()) {
                            try {
                                String body = response.errorBody().string();
                                JSONObject json = new JSONObject(body);
                                if (json.has("errors")) {
                                    JSONObject errors = json.getJSONObject("errors");
                                    if (errors.has("items")) {
                                        JSONArray message = errors.getJSONArray("items");
                                        Toast.makeText(requireContext(), message.getString(0), Toast.LENGTH_SHORT).show();
                                    }
                                }
                            } catch (Exception ignore) {
                            }
                        } else if (isAdded()) {
                            Toast.makeText(requireContext(), R.string.error_unknown, Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(
                            @Nullable Call<Wrappers.Paginated<Gift>> call,
                            @Nullable Throwable t
                    ) {
                        Log.e(TAG, "Could not redeem received gift.", t);
                        progress.dismiss();
                        if (isAdded()) {
                            Toast.makeText(requireContext(), R.string.error_internet, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private class GiftAdapter extends PagedListAdapter<Gift, GiftViewHolder> {

        protected GiftAdapter() {
            super(new DiffUtilCallback<>(i -> i.item.id));
        }

        @Override
        @SuppressLint("SetTextI18n")
        public void onBindViewHolder(@NonNull GiftViewHolder holder, int position) {
            Gift gift = getItem(position);
            holder.image.setImageURI(gift.item.image);
            holder.name.setText(gift.item.name);
            holder.count.setText("x" + gift.balance);
            holder.value.setText(gift.value);
            holder.redeem.setOnClickListener(v -> confirmAndRedeem(gift));
            holder.redeem.setEnabled(gift.balance >= gift.item.minimum);
        }

        @NonNull
        @Override
        public GiftViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(requireContext())
                    .inflate(R.layout.item_gift_received, parent, false);
            return new GiftViewHolder(view);
        }
    }

    public static class GiftsFragmentViewModel extends ViewModel {

        public GiftsFragmentViewModel() {
            PagedList.Config config = new PagedList.Config.Builder()
                    .setPageSize(SharedConstants.DEFAULT_PAGE_SIZE)
                    .build();
            factory = new GiftDataSource.Factory();
            state = Transformations.switchMap(factory.source, input -> input.state);
            gifts = new LivePagedListBuilder<>(factory, config).build();
        }

        public final LiveData<PagedList<Gift>> gifts;
        public final GiftDataSource.Factory factory;
        public final LiveData<LoadingState> state;
    }

    private static class GiftViewHolder extends RecyclerView.ViewHolder {

        public SimpleDraweeView image;
        public TextView name;
        public TextView count;
        public TextView value;
        public Button redeem;

        public GiftViewHolder(@NonNull View root) {
            super(root);
            image = root.findViewById(R.id.image);
            name = root.findViewById(R.id.name);
            count = root.findViewById(R.id.count);
            value = root.findViewById(R.id.value);
            redeem = root.findViewById(R.id.redeem);
        }
    }
}
