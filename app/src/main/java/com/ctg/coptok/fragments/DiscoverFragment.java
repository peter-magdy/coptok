package com.ctg.coptok.fragments;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.paging.LivePagedListBuilder;
import androidx.paging.PagedList;
import androidx.paging.PagedListAdapter;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.request.RequestOptions;
import com.facebook.drawee.view.SimpleDraweeView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.kaopiz.kprogresshud.KProgressHUD;
import com.tbuonomo.viewpagerdotsindicator.WormDotsIndicator;

import org.apache.commons.lang3.StringUtils;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

import jp.wasabeef.recyclerview.adapters.SlideInBottomAnimationAdapter;
import jp.wasabeef.recyclerview.adapters.SlideInLeftAnimationAdapter;
import com.ctg.coptok.MainApplication;
import com.ctg.coptok.R;
import com.ctg.coptok.SharedConstants;
import com.ctg.coptok.activities.LiveStreamAgoraActivity;
import com.ctg.coptok.activities.MainActivity;
import com.ctg.coptok.ads.BannerAdProvider;
import com.ctg.coptok.common.DiffUtilCallback;
import com.ctg.coptok.common.LoadingState;
import com.ctg.coptok.data.ClipDataSource;
import com.ctg.coptok.data.ClipItemDataSource;
import com.ctg.coptok.data.ClipSectionDataSource;
import com.ctg.coptok.data.LiveStreamDataSource;
import com.ctg.coptok.data.api.REST;
import com.ctg.coptok.data.models.Advertisement;
import com.ctg.coptok.data.models.Challenge;
import com.ctg.coptok.data.models.Clip;
import com.ctg.coptok.data.models.ClipSection;
import com.ctg.coptok.data.models.LiveStream;
import com.ctg.coptok.data.models.User;
import com.ctg.coptok.data.models.Wrappers;
import com.ctg.coptok.events.LiveStreamStartStopEvent;
import com.ctg.coptok.utils.AdsUtil;
import com.ctg.coptok.utils.TextFormatUtil;
import me.everything.android.ui.overscroll.OverScrollDecoratorHelper;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DiscoverFragment extends Fragment {

    private static final String TAG = "DiscoverFragment";

    private BannerAdProvider mAd;
    private final Handler mHandler = new Handler();
    private DiscoverFragmentViewModel mModel1;
    private MainActivity.MainActivityViewModel mModel2;
    private ViewPager2 mPager;
    private final Runnable mAutoScrollRunnable = new Runnable() {

        @Override
        public void run() {
            int pages = mPager.getAdapter().getItemCount();
            int page = (mPager.getCurrentItem() + 1) % pages;
            mPager.setCurrentItem(page);
            int interval = getResources().getInteger(R.integer.challenges_auto_scroll_interval);
            mHandler.postDelayed(this, interval);
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Advertisement ad = AdsUtil.findByLocationAndType("discover", "banner");
        if (ad != null) {
            mAd = new BannerAdProvider(ad);
        }
        mModel1 = new ViewModelProvider(this).get(DiscoverFragmentViewModel.class);
        mModel2 = new ViewModelProvider(requireActivity())
                .get(MainActivity.MainActivityViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_discover, container, false);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onLiveStreamStartStopEvent(LiveStreamStartStopEvent event) {
        reloadLiveStreams();
    }

    @Override
    public void onPause() {
        super.onPause();
        mHandler.removeCallbacks(mAutoScrollRunnable);
    }

    @Override
    public void onResume() {
        super.onResume();
        LoadingState state = mModel1.state3.getValue();
        if (state != LoadingState.LOADED && state != LoadingState.LOADING) {
            loadChallenges();
        }
        reloadLiveStreams();
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
    @SuppressLint("SetTextI18n")
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ImageView news = view.findViewById(R.id.header_back);
        news.setImageDrawable(
                ContextCompat.getDrawable(requireContext(), R.drawable.ic_baseline_travel_explore_24));
        news.setOnClickListener(v -> ((MainActivity)requireActivity()).showNews());
        if (!getResources().getBoolean(R.bool.news_enabled)) {
            news.setVisibility(View.INVISIBLE);
        }
        TextView title = view.findViewById(R.id.header_title);
        title.setText(R.string.discover_label);
        ImageButton search = view.findViewById(R.id.header_more);
        search.setImageDrawable(
                ContextCompat.getDrawable(requireContext(), R.drawable.ic_baseline_search_24));
        search.setOnClickListener(v -> ((MainActivity)requireActivity()).showSearch());
        /// live-streams >>>
        boolean liveStreamingEnabled = getResources().getBoolean(R.bool.live_streaming_enabled);
        view.findViewById(R.id.live_streams_container)
                .setVisibility(liveStreamingEnabled ? View.VISIBLE : View.GONE);
        if (liveStreamingEnabled) {
            View liveStreamSelf = view.findViewById(R.id.live_stream_self);
            if (mModel2.isLoggedIn()) {
                liveStreamSelf.setVisibility(View.VISIBLE);
                SimpleDraweeView photo = view.findViewById(R.id.photo);
                photo.setOnClickListener(v -> confirmStartLiveStream());
                mModel2.user.observe(getViewLifecycleOwner(), user -> {
                    TextView username = view.findViewById(R.id.username);
                    if (user != null && !TextUtils.isEmpty(user.photo)) {
                        photo.setImageURI(user.photo);
                        username.setText(user.username);
                    }

                    view.findViewById(R.id.go_live).setVisibility(View.VISIBLE);
                });
            } else {
                liveStreamSelf.setVisibility(View.VISIBLE);
            }
            RecyclerView liveStreams = view.findViewById(R.id.live_streams);
            LinearLayoutManager llm =
                    new LinearLayoutManager(
                            requireContext(), LinearLayoutManager.HORIZONTAL, false);
            liveStreams.setLayoutManager(llm);
            OverScrollDecoratorHelper.setUpOverScroll(
                    liveStreams, OverScrollDecoratorHelper.ORIENTATION_HORIZONTAL);
            LiveStreamsAdapter adapter = new LiveStreamsAdapter();
            liveStreams.setAdapter(new SlideInLeftAnimationAdapter(adapter));
            mModel1.liveStreams.observe(getViewLifecycleOwner(), adapter::submitList);
            View loading = view.findViewById(R.id.loading);
            View empty = view.findViewById(R.id.empty);
            mModel1.state2.observe(getViewLifecycleOwner(), state -> {
                List<?> list = mModel1.liveStreams.getValue();
                if (state == LoadingState.LOADING) {
                    empty.setVisibility(View.GONE);
                } else {
                    empty.setVisibility(list == null || list.isEmpty() ? View.VISIBLE : View.GONE);
                }
                loading.setVisibility(state == LoadingState.LOADING ? View.VISIBLE : View.GONE);
            });
        }
        /// <<< live-streams
        /// sections >>>
        RecyclerView sections = view.findViewById(R.id.sections);
        SectionsAdapter adapter = new SectionsAdapter();
        sections.setAdapter(new SlideInLeftAnimationAdapter(adapter));
        SwipeRefreshLayout swipe = view.findViewById(R.id.swipe);
        swipe.setOnRefreshListener(() -> {
            ClipSectionDataSource source = mModel1.factory1.source.getValue();
            if (source != null) {
                source.invalidate();
            }
        });
        mModel1.sections.observe(getViewLifecycleOwner(), adapter::submitList);
        View loading2 = view.findViewById(R.id.loading2);
        mModel1.state1.observe(getViewLifecycleOwner(), state -> {
            if (state != LoadingState.LOADING) {
                swipe.setRefreshing(false);
            }
            loading2.setVisibility(state == LoadingState.LOADING ? View.VISIBLE : View.GONE);
        });
        /// <<< live-streams
        /// challenges >>>
        mModel1.challenges.observe(getViewLifecycleOwner(), challenges -> {
            if (challenges != null && !challenges.isEmpty()) {
                showChallengesSlider(challenges);
            }
        });
        mModel1.state3.observe(getViewLifecycleOwner(), state ->
                Log.v(TAG, "Loading challenges state is " + state + "."));
        /// <<< challenges
        if (mAd != null) {
            mAd.create(requireContext(), ad -> {
                LinearLayout banner = view.findViewById(R.id.banner);
                banner.removeAllViews();
                banner.addView(ad);
            });
        }
    }

    private void joinLiveStream(LiveStream liveStream) {
        Log.v(TAG, "User wants to join " + liveStream.service + " live stream #" + liveStream.id + ".");
        if (StringUtils.equals(liveStream.service, "agora")) {
            joinLiveStreamAgora(liveStream);
        }
    }

    private void joinLiveStreamAgora(LiveStream liveStream) {
        User user = mModel2.user.getValue();
        Intent intent = new Intent(requireContext(), LiveStreamAgoraActivity.class);
        intent.putExtra(LiveStreamAgoraActivity.EXTRA_LIVE_STREAM, liveStream);
        intent.putExtra(LiveStreamAgoraActivity.EXTRA_USER, user);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private void loadChallenges() {
        mModel1.state3.postValue(LoadingState.LOADING);
        REST rest = MainApplication.getContainer().get(REST.class);
        rest.challengesIndex()
                .enqueue(new Callback<Wrappers.Paginated<Challenge>>() {

                    @Override
                    public void onResponse(
                            @Nullable Call<Wrappers.Paginated<Challenge>> call,
                            @Nullable Response<Wrappers.Paginated<Challenge>> response
                    ) {
                        int code = response != null ? response.code() : -1;
                        Log.v(TAG, "Loading challenges from server returned " + code + ".");
                        if (response != null && response.isSuccessful()) {
                            //noinspection ConstantConditions
                            List<Challenge> challenges = response.body().data;
                            mModel1.challenges.postValue(challenges);
                            mModel1.state3.postValue(LoadingState.LOADED);
                        } else {
                            mModel1.state3.postValue(LoadingState.ERROR);
                        }
                    }

                    @Override
                    public void onFailure(
                            @Nullable Call<Wrappers.Paginated<Challenge>> call,
                            @Nullable Throwable t
                    ) {
                        Log.e(TAG, "Failed to load challenges from server.", t);
                        mModel1.state3.postValue(LoadingState.ERROR);
                    }
                });
    }

    public static DiscoverFragment newInstance() {
        return new DiscoverFragment();
    }

    private void showChallengesSlider(List<Challenge> challenges) {
        View container = getView().findViewById(R.id.challenges);
        container.setVisibility(View.VISIBLE);
        mPager = getView().findViewById(R.id.pager);
        mPager.setAdapter(new ChallengePagerAdapter(this, challenges));
        mPager.setOffscreenPageLimit(1);
        float npx = getResources()
                .getDimension(R.dimen.viewpager_adjacent_visibility);
        float cpx = getResources()
                .getDimension(R.dimen.viewpager_current_margin);
        mPager.addItemDecoration(new RecyclerView.ItemDecoration() {

            private final int mMargin = Math.round(cpx);

            @Override
            public void getItemOffsets(
                    @NonNull Rect out,
                    @NonNull View view,
                    @NonNull RecyclerView parent,
                    @NonNull RecyclerView.State state) {
                out.left = mMargin;
                out.right = mMargin;
            }
        });
        ViewPager2.PageTransformer transformer = (page, position) -> {
            page.setTranslationX(-(npx + cpx) * position);
            page.setScaleY(1 - (0.25f * Math.abs(position)));
            page.setAlpha(0.25f + (1 - Math.abs(position)));
        };
        mPager.setPageTransformer(transformer);
        WormDotsIndicator indicator = getView().findViewById(R.id.indicator);
        indicator.setViewPager2(mPager);
        if (getResources().getBoolean(R.bool.challenges_auto_scroll)) {
            int interval = getResources().getInteger(R.integer.challenges_auto_scroll_interval);
            mHandler.postDelayed(mAutoScrollRunnable, interval);
        }
    }

    private void confirmStartLiveStream() {
        Log.v(TAG, "User wants to start live streaming.");
        new MaterialAlertDialogBuilder(requireActivity())
                .setTitle(R.string.live_stream_privacy_title)
                .setMessage(R.string.live_stream_privacy_message)
                .setNeutralButton(R.string.live_stream_privacy_neutral, (dialog, i) -> dialog.cancel())
                .setNegativeButton(R.string.live_stream_privacy_negative, (dialog, i) -> {
                    dialog.cancel();
                    startLiveStream(true);
                })
                .setPositiveButton(R.string.live_stream_privacy_positive, (dialog, i) -> {
                    dialog.dismiss();
                    startLiveStream(false);
                })
                .show();
    }

    private void reloadLiveStreams() {
        if (getResources().getBoolean(R.bool.live_streaming_enabled)) {
            LiveStreamDataSource source = mModel1.factory2.source.getValue();
            if (source != null) {
                source.invalidate();
            }
        }
    }

    private void startLiveStream(boolean _private) {
        Log.v(TAG, "User wants to start a (private: " + _private + ") live stream.");
        KProgressHUD progress = KProgressHUD.create(requireActivity())
                .setStyle(KProgressHUD.Style.SPIN_INDETERMINATE)
                .setLabel(getString(R.string.progress_title))
                .setCancellable(false)
                .show();
        REST rest = MainApplication.getContainer().get(REST.class);
        rest.liveStreamsCreate(_private ? 1 : 0)
                .enqueue(new Callback<Wrappers.Single<LiveStream>>() {

                    @Override
                    public void onResponse(
                            @Nullable Call<Wrappers.Single<LiveStream>> call,
                            @Nullable Response<Wrappers.Single<LiveStream>> response
                    ) {
                        progress.dismiss();
                        if (response != null && response.isSuccessful()) {
                            //noinspection ConstantConditions
                            LiveStream liveStream = response.body().data;
                            Log.v(TAG, "Created live stream with ID #" + liveStream.id);
                            joinLiveStream(liveStream);
                        } else {
                            Toast.makeText(requireContext(), R.string.error_server, Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(
                            @Nullable Call<Wrappers.Single<LiveStream>> call,
                            @Nullable Throwable t
                    ) {
                        Log.e(TAG, "Failed when trying to create live stream.", t);
                        progress.dismiss();
                        Toast.makeText(requireContext(), R.string.error_internet, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showPlayerSlider(int clip, int section) {
        ArrayList<Integer> sections = new ArrayList<>();
        sections.add(section);
        Bundle params = new Bundle();
        params.putIntegerArrayList(ClipDataSource.PARAM_SECTIONS, sections);
        ((MainActivity) requireActivity()).showPlayerSlider(clip, params);
    }

    private void showSection(String name, int id) {
        ArrayList<Integer> sections = new ArrayList<>();
        sections.add(id);
        Bundle params = new Bundle();
        params.putIntegerArrayList(ClipDataSource.PARAM_SECTIONS, sections);
        ((MainActivity) requireActivity()).showClips(name, params);
    }

    private static class ChallengePagerAdapter extends FragmentStateAdapter {

        private final List<Challenge> mChallenges;

        public ChallengePagerAdapter(@NonNull Fragment fragment, List<Challenge> challenges) {
            super(fragment);
            mChallenges = challenges;
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            return ChallengeFragment.newInstance(mChallenges.get(position));
        }

        @Override
        public int getItemCount() {
            return mChallenges.size();
        }
    }

    private class LiveStreamsAdapter extends PagedListAdapter<LiveStream, LiveStreamViewHolder> {

        protected LiveStreamsAdapter() {
            super(new DiffUtilCallback<>(i -> i.id));
        }

        @NonNull
        @Override
        public LiveStreamViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(requireContext())
                    .inflate(R.layout.item_live_stream_publisher, parent, false);
            return new LiveStreamViewHolder(view);
        }

        @Override
        @SuppressLint("SetTextI18n")
        public void onBindViewHolder(@NonNull LiveStreamViewHolder holder, int position) {
            LiveStream liveStream = getItem(position);
            //noinspection ConstantConditions
            if (TextUtils.isEmpty(liveStream.user.photo)) {
                holder.photo.setActualImageResource(R.drawable.photo_placeholder);
            } else {
                holder.photo.setImageURI(liveStream.user.photo);
            }

            Log.v(TAG, "Live-stream publisher's username is @" + liveStream.user.username);
            holder.username.setText(liveStream.user.username);
            holder.username.setVisibility(View.VISIBLE);
            holder.itemView.setOnClickListener(v -> joinLiveStream(liveStream));
        }
    }

    private class ClipsAdapter extends PagedListAdapter<Clip, ClipViewHolder> {

        private final int mSection;

        protected ClipsAdapter(int section) {
            super(new DiffUtilCallback<>(i -> i.id));
            mSection = section;
        }

        @NonNull
        @Override
        public ClipViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(requireContext())
                    .inflate(R.layout.item_clip_discover, parent, false);
            return new ClipViewHolder(view);
        }

        @Override
        @SuppressLint("SetTextI18n")
        public void onBindViewHolder(@NonNull ClipViewHolder holder, int position) {
            Clip clip = getItem(position);
            holder.likes.setText(TextFormatUtil.toShortNumber(clip.likesCount));
            if (getResources().getBoolean(R.bool.discover_previews_enabled)) {
                //noinspection unchecked
                Glide.with(requireContext())
                        .asGif()
                        .load(clip.preview)
                        .thumbnail(new RequestBuilder[]{
                                Glide.with(requireContext()).load(clip.screenshot).centerCrop()
                        })
                        .apply(RequestOptions.placeholderOf(R.drawable.image_placeholder).centerCrop())
                        .into(holder.preview);
            } else {
                holder.preview.setImageURI(clip.screenshot);
            }

            holder.itemView.setOnClickListener(v -> showPlayerSlider(clip.id, mSection));
        }
    }

    private class SectionsAdapter extends PagedListAdapter<ClipSection, SectionViewHolder> {

        protected SectionsAdapter() {
            super(new DiffUtilCallback<>(i -> i.id));
        }

        @Override
        public void onBindViewHolder(@NonNull SectionViewHolder holder, int position) {
            ClipSection section = getItem(position);
            holder.all.setOnClickListener(v -> showSection(section.name, section.id));
            holder.all.setText(getString(R.string.see_all_label, TextFormatUtil.toShortNumber(section.clipsCount)));
            holder.title.setOnClickListener(v -> showSection(section.name, section.id));
            holder.title.setText(section.name);
            holder.load(section.id);
        }

        @NonNull
        @Override
        public SectionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View root = LayoutInflater.from(requireContext())
                    .inflate(R.layout.item_discover_section, parent, false);
            return new SectionViewHolder(root);
        }
    }

    public static class DiscoverFragmentViewModel extends ViewModel {

        public DiscoverFragmentViewModel() {
            PagedList.Config config = new PagedList.Config.Builder()
                    .setPageSize(SharedConstants.DEFAULT_PAGE_SIZE)
                    .build();
            factory1 = new ClipSectionDataSource.Factory();
            factory2 = new LiveStreamDataSource.Factory();
            state1 = Transformations.switchMap(factory1.source, input -> input.state);
            state2 = Transformations.switchMap(factory2.source, input -> input.state);
            sections = new LivePagedListBuilder<>(factory1, config).build();
            liveStreams = new LivePagedListBuilder<>(factory2, config).build();
        }

        public final ClipSectionDataSource.Factory factory1;
        public final LiveStreamDataSource.Factory factory2;
        public final LiveData<LoadingState> state1;
        public final LiveData<LoadingState> state2;
        public final MutableLiveData<LoadingState> state3 = new MutableLiveData<>(LoadingState.IDLE);
        public final LiveData<PagedList<ClipSection>> sections;
        public final LiveData<PagedList<LiveStream>> liveStreams;
        public final MutableLiveData<List<Challenge>> challenges = new MutableLiveData<>();
    }

    private static class LiveStreamViewHolder extends RecyclerView.ViewHolder {

        public SimpleDraweeView photo;
        public TextView username;

        public LiveStreamViewHolder(@NonNull View root) {
            super(root);
            photo = root.findViewById(R.id.photo);
            username = root.findViewById(R.id.username);
            root.findViewById(R.id.live).setVisibility(View.VISIBLE);
        }
    }

    private static class ClipViewHolder extends RecyclerView.ViewHolder {

        public SimpleDraweeView preview;
        public TextView likes;

        public ClipViewHolder(@NonNull View root) {
            super(root);
            preview = root.findViewById(R.id.preview);
            likes = root.findViewById(R.id.likes);
        }
    }

    private class SectionViewHolder extends RecyclerView.ViewHolder {

        public TextView title;
        public TextView all;
        public ProgressBar loading;
        public RecyclerView clips;

        public LiveData<PagedList<Clip>> items;
        public LiveData<LoadingState> state;

        public SectionViewHolder(@NonNull View root) {
            super(root);
            title = root.findViewById(R.id.title);
            all = root.findViewById(R.id.all);
            clips = root.findViewById(R.id.clips);
            loading = root.findViewById(R.id.loading);
            LinearLayoutManager llm =
                    new LinearLayoutManager(
                            requireContext(), LinearLayoutManager.HORIZONTAL, false);
            clips.setLayoutManager(llm);
            OverScrollDecoratorHelper.setUpOverScroll(
                    clips, OverScrollDecoratorHelper.ORIENTATION_HORIZONTAL);
        }

        public void load(int section) {
            ClipsAdapter adapter = new ClipsAdapter(section);
            clips.setAdapter(new SlideInBottomAnimationAdapter(adapter));
            PagedList.Config config = new PagedList.Config.Builder()
                    .setPageSize(SharedConstants.DEFAULT_PAGE_SIZE)
                    .build();
            ArrayList<Integer> sections = new ArrayList<>();
            sections.add(section);
            Bundle params = new Bundle();
            params.putIntegerArrayList(ClipDataSource.PARAM_SECTIONS, sections);
            ClipItemDataSource.Factory factory = new ClipItemDataSource.Factory(params);
            state = Transformations.switchMap(factory.source, input -> input.state);
            state.observe(getViewLifecycleOwner(), state ->
                    loading.setVisibility(state == LoadingState.LOADING ? View.VISIBLE : View.GONE));
            items = new LivePagedListBuilder<>(factory, config).build();
            items.observe(getViewLifecycleOwner(), adapter::submitList);
        }
    }
}
