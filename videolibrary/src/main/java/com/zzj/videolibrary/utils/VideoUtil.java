package com.zzj.videolibrary.utils;

import android.util.Log;

import com.zzj.videolibrary.model.M3u8;
import com.zzj.videolibrary.model.M3u8Ts;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class VideoUtil {

    /**
     * 从链接中分析出M3U8对象
     *
     * @param url
     * @return
     * @throws IOException
     */
    public static M3u8 analysisM3u8(String url) throws IOException {

        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
            String realUrl = conn.getURL().toString();
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String basePath = realUrl.substring(0, realUrl.lastIndexOf("/") + 1);
            M3u8 m3u8 = new M3u8();
            m3u8.setBasePath(basePath);

            String line;
            float duration = 0;
            float seconds = 0;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("#")) {
                    if (line.startsWith("#EXTINF:")) {
                        line = line.substring(8);
                        if (line.endsWith(",")) {
                            line = line.substring(0, line.length() - 1);
                        }
                        seconds = Float.parseFloat(line);
                    }
                    continue;
                }
                if (line.endsWith("m3u8")) {
                    return analysisM3u8(basePath + line);
                }
                M3u8Ts m3u8Ts = new M3u8Ts(line, seconds);
                m3u8Ts.setStartTime(duration);
                m3u8Ts.setEndTime(duration + seconds);
                m3u8.addTs(m3u8Ts);
                duration += seconds;
                seconds = 0;
            }
            m3u8.setMaxDuration(duration);
            reader.close();
            return m3u8;
        } else {
            return null;
        }
    }

    /**
     * 将M3U8对象的所有ts切片合并为1个
     *
     * @param m3u8
     * @param tofile
     * @throws IOException
     */
    public static void merge(M3u8 m3u8, String tofile, String basePath) throws IOException {
        List<M3u8Ts> mergeList = getLimitM3U8Ts(m3u8);
        File saveFile = new File(tofile);
        FileOutputStream fos = new FileOutputStream(saveFile);
        File file;
        for (M3u8Ts ts : mergeList) {
            file = new File(basePath, ts.getFileName());
            ts.setTempFilePath(file.getPath());
            ts.setCache(true);
            if (file.isFile() && file.exists()) {
                IOUtils.copyLarge(new FileInputStream(file), fos);
            }
        }
        fos.close();
    }

    /**
     * 移动文件
     *
     * @param sFile
     * @param tFile
     */
    public static void moveFile(String sFile, String tFile) {
        try {
            FileUtils.moveFile(new File(sFile), new File(tFile));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 清空文件夹
     */
    public static void clearDir(File dir) {
        if (dir.exists()) {// 判断文件是否存在
            if (dir.isFile()) {// 判断是否是文件
                dir.delete();// 删除文件
            } else if (dir.isDirectory()) {// 否则如果它是一个目录
                File[] files = dir.listFiles();// 声明目录下所有的文件 files[];
                for (int i = 0; i < files.length; i++) {// 遍历目录下所有的文件
                    clearDir(files[i]);// 把每个文件用这个方法进行迭代
                }
                dir.delete();// 删除文件夹
            }
        }
    }

    /**
     * 获取指定区间的M3U8切片
     *
     * @param m3u8
     * @return
     */
    public static List<M3u8Ts> getLimitM3U8Ts(M3u8 m3u8) {
        List<M3u8Ts> downList = new ArrayList<>();

        if (m3u8.getStartDownloadTime() < m3u8.getStartTime() || m3u8.getEndDownloadTime() > m3u8.getEndTime()) {
            downList = m3u8.getTsList();
            return downList;
        }


        if ((m3u8.getStartDownloadTime() == -1 && m3u8.getEndDownloadTime() == -1) || m3u8.getEndDownloadTime() <= m3u8.getStartDownloadTime()) {
            downList = m3u8.getTsList();
        } else if (m3u8.getStartDownloadTime() == -1 && m3u8.getEndDownloadTime() > -1) {
            for (final M3u8Ts ts : m3u8.getTsList()) { //从头下到指定时间
                if (ts.getLongDate() <= m3u8.getEndDownloadTime()) {
                    downList.add(ts);
                }
            }
        } else if (m3u8.getStartDownloadTime() > -1 && m3u8.getEndDownloadTime() == -1) {
            for (final M3u8Ts ts : m3u8.getTsList()) { //从指定时间下到尾部
                if (ts.getLongDate() >= m3u8.getStartDownloadTime()) {
                    downList.add(ts);
                }
            }
        } else {//从指定开始时间下载到指定结束时间
            for (final M3u8Ts ts : m3u8.getTsList()) {
                if (m3u8.getStartDownloadTime() <= ts.getLongDate() && ts.getLongDate() <= m3u8.getEndDownloadTime()) {
                    downList.add(ts);//指定区间的ts
                }
            }
        }
        Log.e("hdltag", "getLimitM3U8Ts(MUtils.java:152):" + downList);
        return downList;
    }

    public static int getM3U8TsIndex(M3u8 m3u8, int progress) {
        int index = -1;
        for (int i = 0; i < m3u8.getTsList().size(); i++) {
            M3u8Ts m3u8Ts = m3u8.getTsList().get(i);
            if (progress > m3u8Ts.getStartTime() && progress < m3u8Ts.getEndTime()) {
                index = i;
                break;
            }
        }
        return index;
    }
}
