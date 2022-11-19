package com.ctg.coptok.agora;

import java.util.Map;

import io.agora.rtm.RtmClientListener;
import io.agora.rtm.RtmFileMessage;
import io.agora.rtm.RtmImageMessage;
import io.agora.rtm.RtmMediaOperationProgress;
import io.agora.rtm.RtmMessage;

public class DefaultRtmClientListener implements RtmClientListener {

    @Override
    public void onConnectionStateChanged(int i, int i1) {
    }

    @Override
    public void onMessageReceived(RtmMessage rtmMessage, String s) {
    }

    @Override
    public void onImageMessageReceivedFromPeer(RtmImageMessage rtmImageMessage, String s) {
    }

    @Override
    public void onFileMessageReceivedFromPeer(RtmFileMessage rtmFileMessage, String s) {
    }

    @Override
    public void onMediaUploadingProgress(RtmMediaOperationProgress rtmMediaOperationProgress, long l) {
    }

    @Override
    public void onMediaDownloadingProgress(RtmMediaOperationProgress rtmMediaOperationProgress, long l) {
    }

    @Override
    public void onTokenExpired() {
    }

    @Override
    public void onPeersOnlineStatusChanged(Map<String, Integer> map) {
    }
}
