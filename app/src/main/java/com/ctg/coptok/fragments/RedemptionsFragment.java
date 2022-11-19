package com.ctg.coptok.fragments;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
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

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;

import com.ctg.coptok.R;
import com.ctg.coptok.SharedConstants;
import com.ctg.coptok.common.DiffUtilCallback;
import com.ctg.coptok.common.LoadingState;
import com.ctg.coptok.data.RedemptionDataSource;
import com.ctg.coptok.data.models.Redemption;
import com.ctg.coptok.events.GiftRedeemedEvent;

public class RedemptionsFragment extends Fragment {

    private RedemptionsFragmentViewModel mModel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mModel = new ViewModelProvider(this).get(RedemptionsFragmentViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_redemptions, container, false);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onGiftRedeemedEvent(GiftRedeemedEvent event) {
        RedemptionDataSource source = mModel.factory.source.getValue();
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
        RedemptionAdapter adapter = new RedemptionAdapter();
        RecyclerView redemptions = view.findViewById(R.id.redemptions);
        redemptions.setAdapter(adapter);
        redemptions.addItemDecoration(new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL));
        mModel.redemptions.observe(getViewLifecycleOwner(), adapter::submitList);
        SwipeRefreshLayout swipe = view.findViewById(R.id.swipe);
        swipe.setOnRefreshListener(() -> {
            RedemptionDataSource source = mModel.factory.source.getValue();
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
            List<?> list = mModel.redemptions.getValue();
            if (state == LoadingState.LOADING) {
                empty.setVisibility(View.GONE);
            } else {
                empty.setVisibility(list == null || list.isEmpty() ? View.VISIBLE : View.GONE);
            }
            loading.setVisibility(state == LoadingState.LOADING ? View.VISIBLE : View.GONE);
        });
    }

    public static RedemptionsFragment newInstance() {
        return new RedemptionsFragment();
    }

    private class RedemptionAdapter extends PagedListAdapter<Redemption, RedemptionsFragment.RedemptionViewHolder> {

        protected RedemptionAdapter() {
            super(new DiffUtilCallback<>(i -> i.id));
        }

        @Override
        @SuppressLint("SetTextI18n")
        public void onBindViewHolder(@NonNull RedemptionsFragment.RedemptionViewHolder holder, int position) {
            Redemption redemption = getItem(position);
            holder.amount.setText(redemption.amount);
            holder.when.setText(
                    DateUtils.getRelativeTimeSpanString(
                            requireContext(), redemption.createdAt.getTime(), true));
            if (TextUtils.equals(redemption.status, "approved")) {
                holder.status.setText(R.string.redemption_status_approved);
                holder.icon.setImageDrawable(
                        ContextCompat.getDrawable(requireContext(), R.drawable.ic_redemption_approved));
            } else if (TextUtils.equals(redemption.status, "rejected")) {
                holder.status.setText(R.string.redemption_status_rejected);
                holder.icon.setImageDrawable(
                        ContextCompat.getDrawable(requireContext(), R.drawable.ic_redemption_rejected));
            } else {
                holder.status.setText(R.string.redemption_status_pending);
                holder.icon.setImageDrawable(
                        ContextCompat.getDrawable(requireContext(), R.drawable.ic_redemption_pending));
            }
        }

        @NonNull
        @Override
        public RedemptionsFragment.RedemptionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(requireContext())
                    .inflate(R.layout.item_redemption, parent, false);
            return new RedemptionsFragment.RedemptionViewHolder(view);
        }
    }

    public static class RedemptionsFragmentViewModel extends ViewModel {

        public RedemptionsFragmentViewModel() {
            PagedList.Config config = new PagedList.Config.Builder()
                    .setPageSize(SharedConstants.DEFAULT_PAGE_SIZE)
                    .build();
            factory = new RedemptionDataSource.Factory();
            state = Transformations.switchMap(factory.source, input -> input.state);
            redemptions = new LivePagedListBuilder<>(factory, config).build();
        }

        public final LiveData<PagedList<Redemption>> redemptions;
        public final RedemptionDataSource.Factory factory;
        public final LiveData<LoadingState> state;
    }

    private static class RedemptionViewHolder extends RecyclerView.ViewHolder {

        public TextView amount;
        public TextView when;
        public TextView status;
        public ImageView icon;

        public RedemptionViewHolder(@NonNull View root) {
            super(root);
            amount = root.findViewById(R.id.amount);
            when = root.findViewById(R.id.when);
            status = root.findViewById(R.id.status);
            icon = root.findViewById(R.id.icon);
        }
    }
}
