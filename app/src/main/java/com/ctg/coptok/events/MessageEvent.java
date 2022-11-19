package com.ctg.coptok.events;

public class MessageEvent {

    private final int mThread;

    public MessageEvent(int thread) {
        mThread = thread;
    }

    public int getThread() {
        return mThread;
    }
}
