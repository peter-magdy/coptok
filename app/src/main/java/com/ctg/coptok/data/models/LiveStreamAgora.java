package com.ctg.coptok.data.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public class LiveStreamAgora {

    public String channel;
    @JsonProperty("token_rtc")
    public String tokenRtc;
    @JsonProperty("token_rtm")
    public String tokenRtm;
}
