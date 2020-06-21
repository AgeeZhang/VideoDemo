package com.zzj.videolibrary.model;

public class M3u8Ts {

    private String filePath; //（文件路径）
    private float startTime; //片段开始位置
    private float endTime;   //片段结束位置
    private float totalDuration;  //片段总时长（秒）
    private boolean isCache; //是否缓存完毕
    private String tempFilePath;//缓存地址

    public M3u8Ts(String filePath, float totalDuration) {
        this.filePath = filePath;
        this.totalDuration = totalDuration;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public float getStartTime() {
        return startTime;
    }

    public void setStartTime(float startTime) {
        this.startTime = startTime;
    }

    public float getEndTime() {
        return endTime;
    }

    public void setEndTime(float endTime) {
        this.endTime = endTime;
    }

    public float getTotalDuration() {
        return totalDuration;
    }

    public void setTotalDuration(float totalDuration) {
        this.totalDuration = totalDuration;
    }

    public boolean isCache() {
        return isCache;
    }

    public void setCache(boolean cache) {
        isCache = cache;
    }

    public String getTempFilePath() {
        return tempFilePath;
    }

    public void setTempFilePath(String tempFilePath) {
        this.tempFilePath = tempFilePath;
    }
    /**
     * 获取文件名字，只取***.ts
     *
     * @return
     */
    public String getFileName() {
        String fileName = filePath.substring(filePath.lastIndexOf("/") + 1);
        if (fileName.contains("?")) {
            return fileName.substring(0, fileName.indexOf("?"));
        }
        return fileName;
    }

    /**
     * 获取时间
     */
    public long getLongDate() {
        try {
            return Long.parseLong(filePath.substring(0, filePath.lastIndexOf(".")));
        } catch (Exception e) {
            return 0;
        }
    }
}
