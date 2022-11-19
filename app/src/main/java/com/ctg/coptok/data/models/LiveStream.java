package com.ctg.coptok.data.models;

import android.os.Parcel;
import android.os.Parcelable;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;

public class LiveStream implements Parcelable {

    public int id;
    public String service;
    @JsonProperty("private")
    public boolean _private;
    public Date endsAt;
    public Date createdAt;
    public Date updatedAt;
    public int viewsCount;
    public User user;

    public LiveStream() {
    }

    protected LiveStream(Parcel in) {
        id = in.readInt();
        service = in.readString();
        _private = in.readByte() != 0;
        viewsCount = in.readInt();
        user = in.readParcelable(User.class.getClassLoader());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeString(service);
        dest.writeByte((byte) (_private ? 1 : 0));
        dest.writeInt(viewsCount);
        dest.writeParcelable(user, flags);
    }

    public static final Creator<LiveStream> CREATOR = new Creator<LiveStream>() {

        @Override
        public LiveStream createFromParcel(Parcel in) {
            return new LiveStream(in);
        }

        @Override
        public LiveStream[] newArray(int size) {
            return new LiveStream[size];
        }
    };
}
