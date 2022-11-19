package com.ctg.coptok.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.slider.Slider;

import org.greenrobot.eventbus.EventBus;

import java.util.Locale;

import com.ctg.coptok.R;
import com.ctg.coptok.events.AudioTargetUpdateEvent;

public class AdjustAudioFragment extends Fragment {

    private static final String ARG_TARGET = "target";

    public static final int TARGET_VIDEO = 60600 + 1;
    public static final int TARGET_SONG = 60600 + 2;

    private AdjustAudioViewModel mModel;
    private int mTarget;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mModel = new ViewModelProvider(this).get(AdjustAudioViewModel.class);
        mTarget = requireArguments().getInt(ARG_TARGET);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_adjust_audio, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Slider delay = view.findViewById(R.id.delay);
        delay.setLabelFormatter(value ->
                String.format(Locale.US, "%dms", (int)value - 2500));
        //noinspection ConstantConditions
        delay.setValue(mModel.delay.getValue());
        delay.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {

            @Override
            public void onStartTrackingTouch(@NonNull Slider slider) { }

            @Override
            public void onStopTrackingTouch(@NonNull Slider slider) {
                mModel.delay.postValue(slider.getValue());
            }
        });
        mModel.delay.observe(getViewLifecycleOwner(), value -> {
            //noinspection ConstantConditions
            EventBus.getDefault()
                    .post(new AudioTargetUpdateEvent(mTarget, value, mModel.volume.getValue()));
        });
        Slider volume = view.findViewById(R.id.volume);
        volume.setLabelFormatter(value -> String.format(Locale.US, "%d%%", (int)value));
        //noinspection ConstantConditions
        volume.setValue(mModel.volume.getValue());
        volume.addOnChangeListener((v, value, user) -> mModel.volume.postValue(value));
        mModel.volume.observe(getViewLifecycleOwner(), value -> {
            EventBus.getDefault()
                    .post(new AudioTargetUpdateEvent(mTarget, mModel.delay.getValue(), value));
        });
    }

    public static AdjustAudioFragment newInstance(int target) {
        Bundle arguments = new Bundle();
        arguments.putInt(ARG_TARGET, target);
        AdjustAudioFragment fragment = new AdjustAudioFragment();
        fragment.setArguments(arguments);
        return fragment;
    }

    public static final class AdjustAudioViewModel extends ViewModel {

        public final MutableLiveData<Float> delay = new MutableLiveData<>(2500f);
        public final MutableLiveData<Float> volume = new MutableLiveData<>(100f);
    }
}
