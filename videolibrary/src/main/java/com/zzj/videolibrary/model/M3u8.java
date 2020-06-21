package com.zzj.videolibrary.model;

import java.util.ArrayList;
import java.util.List;

public class M3u8 {

    private String basePath;
    private List<M3u8Ts> tsList = new ArrayList<>(); //分片列表
    private float maxDuration; //视频最大时长
    private long startTime;//开始时间
    private long endTime;//结束时间
    private long startDownloadTime;//开始下载时间
    private long endDownloadTime;//结束下载时间

    public String getBasePath() {
        return basePath;
    }

    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }

    public float getMaxDuration() {
        return maxDuration;
    }

    public void setMaxDuration(float maxDuration) {
        this.maxDuration = maxDuration;
    }

    public List<M3u8Ts> getTsList() {
        return tsList;
    }

    public void setTsList(List<M3u8Ts> tsList) {
        this.tsList = tsList;
    }

    public void addTs(M3u8Ts ts) {
        this.tsList.add(ts);
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public long getStartDownloadTime() {
        return startDownloadTime;
    }

    public void setStartDownloadTime(long startDownloadTime) {
        this.startDownloadTime = startDownloadTime;
    }

    public long getEndDownloadTime() {
        return endDownloadTime;
    }

    public void setEndDownloadTime(long endDownloadTime) {
        this.endDownloadTime = endDownloadTime;
    }


}
