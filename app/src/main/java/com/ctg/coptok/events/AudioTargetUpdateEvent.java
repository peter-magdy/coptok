package com.ctg.coptok.events;

public class AudioTargetUpdateEvent {

    private final float mDelay;
    private final int mTarget;
    private final float mVolume;

    public AudioTargetUpdateEvent(int target, float delay, float volume) {
        mTarget = target;
        mDelay = delay;
        mVolume = volume;
    }

    public float getDelay() {
        return mDelay;
    }

    public int getTarget() {
        return mTarget;
    }

    public float getVolume() {
        return mVolume;
    }
}
