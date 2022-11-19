package com.ctg.coptok.fragments;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
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
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ConsumeParams;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetailsParams;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.kaopiz.kprogresshud.KProgressHUD;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.reactivex.rxjava3.disposables.Disposable;
import com.ctg.coptok.MainApplication;
import com.ctg.coptok.R;
import com.ctg.coptok.SharedConstants;
import com.ctg.coptok.activities.MainActivity;
import com.ctg.coptok.activities.PaymentActivity;
import com.ctg.coptok.common.DiffUtilCallback;
import com.ctg.coptok.common.LoadingState;
import com.ctg.coptok.data.CreditDataSource;
import com.ctg.coptok.data.api.REST;
import com.ctg.coptok.data.models.Balance;
import com.ctg.coptok.data.models.Credit;
import com.ctg.coptok.data.models.Redirect;
import com.ctg.coptok.data.models.Wrappers;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WalletFragment extends Fragment implements PurchasesUpdatedListener {

    private static final String TAG = "WalletFragment";

    private BillingClient mBillingClient;
    private final List<Disposable> mDisposables = new ArrayList<>();
    private final Handler mHandler = new Handler();
    private WalletFragmentViewModel mModel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mModel = new ViewModelProvider(this).get(WalletFragmentViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_wallet, container, false);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        for (Disposable disposable : mDisposables) {
            disposable.dispose();
        }

        mDisposables.clear();
    }

    @Override
    public void onPurchasesUpdated(@NonNull BillingResult result, @Nullable List<Purchase> purchases) {
        int code = result.getResponseCode();
        Log.v(TAG, "In-app purchase flow returned with " + code + ".");
        if (code == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (Purchase purchase : purchases) {
                mHandler.postDelayed(() -> submitIabPurchase(purchase), 500);
            }
        } else if (code == BillingClient.BillingResponseCode.USER_CANCELED) {
            Log.v(TAG, "User has cancelled the purchase flow.");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshBalance();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        View back = view.findViewById(R.id.header_back);
        back.setOnClickListener(v -> ((MainActivity)requireActivity()).popBackStack());
        TextView title1 = view.findViewById(R.id.header_title);
        title1.setText(R.string.wallet_label);
        view.findViewById(R.id.header_more).setVisibility(View.GONE);
        View sheet = view.findViewById(R.id.recharge_sheet);
        BottomSheetBehavior<View> bsb = BottomSheetBehavior.from(sheet);
        ImageButton close = sheet.findViewById(R.id.header_back);
        close.setImageResource(R.drawable.ic_baseline_close_24);
        close.setOnClickListener(v -> bsb.setState(BottomSheetBehavior.STATE_COLLAPSED));
        TextView title2 = sheet.findViewById(R.id.header_title);
        title2.setText(R.string.recharge_label);
        sheet.findViewById(R.id.header_more).setVisibility(View.GONE);
        CreditAdapter adapter = new CreditAdapter();
        RecyclerView credits = view.findViewById(R.id.credits);
        credits.setAdapter(adapter);
        credits.addItemDecoration(new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL));
        mModel.credits.observe(getViewLifecycleOwner(), adapter::submitList);
        View loading = view.findViewById(R.id.loading);
        mModel.state.observe(getViewLifecycleOwner(), state ->
                loading.setVisibility(state == LoadingState.LOADING ? View.VISIBLE : View.GONE));
        view.findViewById(R.id.add)
                .setOnClickListener(v -> bsb.setState(BottomSheetBehavior.STATE_EXPANDED));
        ViewPager2 pager = getView().findViewById(R.id.pager);
        pager.setAdapter(new WalletPagerAdapter(this));
        TabLayout tabs = getView().findViewById(R.id.tabs);
        new TabLayoutMediator(tabs, pager, (tab, position) -> {
            switch (position) {
                case 0:
                    tab.setText(R.string.gifts_label);
                    break;
                case 1:
                    tab.setText(R.string.redemptions_label);
                    break;
            }
        }).attach();
    }

    public static WalletFragment newInstance() {
        return new WalletFragment();
    }

    private void rechargeCredit(Credit credit) {
        if (TextUtils.isEmpty(credit.playStoreProductId)) {
            requestExternalRecharge(credit);
            return;
        }

        if (mBillingClient == null) {
            mBillingClient = BillingClient.newBuilder(requireContext())
                    .setListener(this)
                    .enablePendingPurchases()
                    .build();
        }

        if (mBillingClient.isReady()) {
            requestIabRecharge(credit);
        } else {
            mBillingClient.startConnection(new BillingClientStateListener() {

                @Override
                public void onBillingSetupFinished(@NotNull BillingResult result) {
                    Log.v(TAG, "Play store billing setup finished with " + result.getResponseCode() + ".");
                    if (result.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                        requestIabRecharge(credit);
                    } else if (isAdded()) {
                        Toast.makeText(requireContext(), R.string.error_iab_connection, Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onBillingServiceDisconnected() {
                    Log.w(TAG, "Play store billing service was disconnected.");
                }
            });
        }
    }

    private void refreshBalance() {
        KProgressHUD progress = KProgressHUD.create(requireActivity())
                .setStyle(KProgressHUD.Style.SPIN_INDETERMINATE)
                .setLabel(getString(R.string.progress_title))
                .setCancellable(false)
                .show();
        REST rest = MainApplication.getContainer().get(REST.class);
        rest.walletBalance()
                .enqueue(new Callback<Wrappers.Single<Balance>>() {

                    @Override
                    public void onFailure(@Nullable Call<Wrappers.Single<Balance>> call, @Nullable Throwable t) {
                        Log.e(TAG, "Could not fetch wallet balance.", t);
                        progress.dismiss();
                        if (isAdded()) {
                            Toast.makeText(requireContext(), R.string.error_internet, Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    @SuppressLint("SetTextI18n")
                    public void onResponse(
                            @Nullable Call<Wrappers.Single<Balance>> call,
                            @Nullable Response<Wrappers.Single<Balance>> response
                    ) {
                        int code = response != null ? response.code() : -1;
                        Log.v(TAG, "Fetching wallet balance returned " + code + ".");
                        progress.dismiss();
                        if (isAdded()) {
                            int balance = 0;
                            if (response != null && response.isSuccessful()) {
                                balance = response.body().data.balance;
                            }

                            TextView text = getView().findViewById(R.id.balance);
                            text.setText(balance + "");
                        }
                    }
                });
    }

    private void requestExternalRecharge(Credit credit) {
        KProgressHUD progress = KProgressHUD.create(requireActivity())
                .setStyle(KProgressHUD.Style.SPIN_INDETERMINATE)
                .setLabel(getString(R.string.progress_title))
                .setCancellable(false)
                .show();
        REST rest = MainApplication.getContainer().get(REST.class);
        rest.walletRecharge(credit.id)
                .enqueue(new Callback<Wrappers.Single<Redirect>>() {

                    @Override
                    public void onResponse(
                            @Nullable Call<Wrappers.Single<Redirect>> call,
                            @Nullable Response<Wrappers.Single<Redirect>> response
                    ) {
                        int code = response != null ? response.code() : -1;
                        Log.v(TAG, "Submitting recharge request returned " + code + ".");
                        progress.dismiss();
                        if (response != null && response.isSuccessful()) {
                            if (isAdded()) {
                                String redirect = response.body().data.redirect;
                                Log.v(TAG, "Opening " + redirect + " to complete payment.");
                                Intent intent = new Intent(requireContext(), PaymentActivity.class);
                                intent.setData(Uri.parse(redirect));
                                startActivity(intent);
                            }
                        } else {
                            Toast.makeText(requireContext(), R.string.error_unknown, Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(
                            @Nullable Call<Wrappers.Single<Redirect>> call,
                            @Nullable Throwable t
                    ) {
                        Log.e(TAG, "Failed to submit recharge request on server.", t);
                        progress.dismiss();
                        if (isAdded()) {
                            Toast.makeText(requireContext(), R.string.error_internet, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void requestIabRecharge(Credit credit) {
        Log.v(TAG, "Fetching SKU details for " + credit.playStoreProductId);
        SkuDetailsParams params1 = SkuDetailsParams.newBuilder()
                .setSkusList(Collections.singletonList(credit.playStoreProductId))
                .setType(BillingClient.SkuType.INAPP)
                .build();
        mBillingClient.querySkuDetailsAsync(params1, (result, list) -> {
            Log.v(TAG, "Querying SKU details returned " + result.getResponseCode() + ".");
            if (result.getResponseCode() == BillingClient.BillingResponseCode.OK && list != null && !list.isEmpty()) {
                Log.v(TAG, "In-app products API returned " + list.size() + " SKUs.");
                BillingFlowParams params2 = BillingFlowParams.newBuilder()
                        .setSkuDetails(list.get(0))
                        .build();
                int code = mBillingClient.launchBillingFlow(requireActivity(), params2)
                        .getResponseCode();
                Log.v(TAG, "Launch purchase flow returned " + code + ".");
                if (code != BillingClient.BillingResponseCode.OK) {
                    Toast.makeText(requireContext(), R.string.error_iab_connection, Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(requireContext(), R.string.error_unknown, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void submitIabPurchase(Purchase purchase) {
        KProgressHUD progress = KProgressHUD.create(requireActivity())
                .setStyle(KProgressHUD.Style.SPIN_INDETERMINATE)
                .setLabel(getString(R.string.progress_title))
                .setCancellable(false)
                .show();
        Log.v(TAG, "Sending purchase token to server: " + purchase.getPurchaseToken());
        REST rest = MainApplication.getContainer().get(REST.class);
        rest.walletRechargeIab(purchase.getSkus().get(0), purchase.getPurchaseToken())
                .enqueue(new Callback<Wrappers.Single<Balance>>() {

                    @Override
                    public void onResponse(
                            @Nullable Call<Wrappers.Single<Balance>> call,
                            @Nullable Response<Wrappers.Single<Balance>> response
                    ) {
                        int code = response != null ? response.code() : -1;
                        Log.v(TAG, "Submitting IAB purchase returned " + code + ".");
                        progress.dismiss();
                        if (response != null && response.isSuccessful()) {
                            ConsumeParams params = ConsumeParams.newBuilder()
                                    .setPurchaseToken(purchase.getPurchaseToken())
                                    .build();
                            mBillingClient.consumeAsync(params, (result, token) ->
                                    Log.v(TAG, "Consuming in-app purchase " + result.getResponseCode() + "."));
                        } else if (isAdded()) {
                            Toast.makeText(requireContext(), R.string.error_unknown, Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(
                            @Nullable Call<Wrappers.Single<Balance>> call,
                            @Nullable Throwable t
                    ) {
                        Log.e(TAG, "Could not update IAB purchase to server.", t);
                        progress.dismiss();
                    }
                });
    }

    private class CreditAdapter extends PagedListAdapter<Credit, CreditViewHolder> {

        protected CreditAdapter() {
            super(new DiffUtilCallback<>(i -> i.id));
        }

        @Override
        @SuppressLint("SetTextI18n")
        public void onBindViewHolder(@NonNull CreditViewHolder holder, int position) {
            Credit credit = getItem(position);
            holder.title.setText(credit.title);
            holder.value.setText("x" + credit.value);
            holder.price.setText(credit.price);
            holder.buy.setOnClickListener(v -> rechargeCredit(credit));
        }

        @NonNull
        @Override
        public CreditViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(requireContext())
                    .inflate(R.layout.item_credit, parent, false);
            return new CreditViewHolder(view);
        }
    }

    private static class CreditViewHolder extends RecyclerView.ViewHolder {

        public TextView title;
        public TextView price;
        public TextView value;
        public Button buy;

        public CreditViewHolder(@NonNull View root) {
            super(root);
            title = root.findViewById(R.id.title);
            price = root.findViewById(R.id.price);
            value = root.findViewById(R.id.value);
            buy = root.findViewById(R.id.buy);
        }
    }

    public static class WalletFragmentViewModel extends ViewModel {

        public WalletFragmentViewModel() {
            PagedList.Config config = new PagedList.Config.Builder()
                    .setPageSize(SharedConstants.DEFAULT_PAGE_SIZE)
                    .build();
            factory = new CreditDataSource.Factory();
            state = Transformations.switchMap(factory.source, input -> input.state);
            credits = new LivePagedListBuilder<>(factory, config).build();
        }

        public final LiveData<PagedList<Credit>> credits;
        public final CreditDataSource.Factory factory;
        public final LiveData<LoadingState> state;
    }

    private static class WalletPagerAdapter extends FragmentStateAdapter {

        public WalletPagerAdapter(@NonNull Fragment fragment) {
            super(fragment);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            if (position == 0) {
                return GiftsFragment.newInstance();
            }

            return RedemptionsFragment.newInstance();
        }

        @Override
        public int getItemCount() {
            return 2;
        }
    }
}
