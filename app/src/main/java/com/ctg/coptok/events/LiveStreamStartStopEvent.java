package com.ctg.coptok.events;

public class LiveStreamStartStopEvent {

    public final int id;
    public final String status;

    public LiveStreamStartStopEvent(int id, String status) {
        this.id = id;
        this.status = status;
    }
}
