package com.example.testexoplayer;

public class PlaySourceBean {


    /**
     * channelName : ABC
     * encryptionType : 1
     * playUrl : http://192.168.180.15/7495e1e879bc408cb738bf803f8a8caa/7495e1e879bc408cb738bf803f8a8caa.mpd
     * status : 1
     * lcn : 108
     */

    private String channelName;
    private int encryptionType;
    private String playUrl;
    private int status;
    private String lcn;

    public String getChannelName() {
        return channelName;
    }

    public void setChannelName(String channelName) {
        this.channelName = channelName;
    }

    public int getEncryptionType() {
        return encryptionType;
    }

    public void setEncryptionType(int encryptionType) {
        this.encryptionType = encryptionType;
    }

    public String getPlayUrl() {
        return playUrl;
    }

    public void setPlayUrl(String playUrl) {
        this.playUrl = playUrl;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getLcn() {
        return lcn;
    }

    public void setLcn(String lcn) {
        this.lcn = lcn;
    }

    @Override
    public String toString() {
        return "PlaySourceBean{" +
                "channelName='" + channelName + '\'' +
                ", encryptionType=" + encryptionType +
                ", playUrl='" + playUrl + '\'' +
                ", status=" + status +
                ", lcn='" + lcn + '\'' +
                '}';
    }
}
