package com.ctg.coptok.events;

public class PlaybackEndedEvent {

    private final int mClip;

    public PlaybackEndedEvent(int clip) {
        mClip = clip;
    }

    public int getClip() {
        return mClip;
    }
}
