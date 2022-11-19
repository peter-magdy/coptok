package com.ctg.coptok.events;

import androidx.annotation.Nullable;

import com.ctg.coptok.data.models.Clip;

public class RecordingOptionsEvent {

    private final Clip mClip;

    public RecordingOptionsEvent(@Nullable Clip clip) {
        mClip = clip;
    }

    public Clip getClip() {
        return mClip;
    }
}
