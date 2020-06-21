package com.zzj.videolibrary;

import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;

import com.zzj.videolibrary.contract.VideoContract;
import com.zzj.videolibrary.model.M3u8;
import com.zzj.videolibrary.model.M3u8Ts;
import com.zzj.videolibrary.utils.VideoUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 多线程
 */
public class DownloadManger {

    private VideoContract.OnDownloadListener mOnDownloadListener;
    private static final int WHAT_ON_ERROR = 1001;
    private static final int WHAT_ON_PROGRESS = 1002;
    private static final int WHAT_ON_SUCCESS = 1003;

    private String mTempDir = Environment.getExternalStorageDirectory() + File.separator + "m3u8temp";    //临时下载目录
    private String mSaveFilePath = Environment.getExternalStorageDirectory().getPath() + File.separator + "m3u8";    //最终文件保存的路径
    private static int mCountTsSize = 0;    //总文件数
    private static int mCurrentTsSize = 0; //当前下载完成的文件个数
    private static long mItemFileSize = 0;    //单个文件的大小
    private long mCurrentLenght = 0; //当前已经在下完成的大小
    private boolean isRunning = false; //是否有任务正在运行中
    private String mTaskId = "0"; //任务id，用于断点续传.
    private boolean isClearTempDir = true; //是否清楚临时目录，默认清除
    private int mMaxThreadCount = 3;        //线程池最大线程数，默认为3
    private ExecutorService mExecutorService;   //线程池

    private int mReadTimeout = 30 * 60 * 1000; //读取超时时间
    private int mConnTimeout = 10 * 1000; //链接超时时间
    private M3u8 mM3u8;

    private Handler mHandler = new Handler() {

        @Override
        public void handleMessage(@NonNull Message msg) {
            switch (msg.what) {
                case WHAT_ON_ERROR:
                    mOnDownloadListener.onError((Throwable) msg.obj);
                    break;
                case WHAT_ON_PROGRESS:
                    mOnDownloadListener.onDownloading(mItemFileSize, mCountTsSize, mCurrentTsSize);
                    break;
                case WHAT_ON_SUCCESS:
                    mOnDownloadListener.onSuccess();
                    break;
            }
            super.handleMessage(msg);
        }
    };


    public boolean isRunning() {
        return isRunning;
    }

    public void setThreadCount(int threadCount) {
        this.mMaxThreadCount = threadCount;
    }

    public void setSaveFilePath(String saveFilePath) {
        this.mSaveFilePath = saveFilePath;
    }


    public DownloadManger(String taskId, M3u8 m3u8) {
        this.mTaskId = taskId;
        this.mM3u8 = m3u8;
        //需要加上当前时间作为文件夹（由于合并时是根据文件夹来合并的，合并之后需要删除所有的ts文件，这里用到了多线程，所以需要按文件夹来存ts）
        mTempDir += File.separator + System.currentTimeMillis() / (1000 * 60 * 60 * 24) + "-" + taskId;
    }

    public void download(VideoContract.OnDownloadListener onDownloadListener) {
        this.mOnDownloadListener = onDownloadListener;
        if (!isRunning()) {
            mOnDownloadListener.onStart();
            isRunning = true;
            startDownload(mM3u8);
            if (mExecutorService != null) {
                mExecutorService.shutdown();//下载完成之后要关闭线程池
            }
            try {
                while (mExecutorService != null && !mExecutorService.isTerminated()) {
                    Thread.sleep(100);
                }
                if (isRunning) {
//                    String saveFileName = mSaveFilePath.substring(mSaveFilePath.lastIndexOf("/") + 1);
//                    String tempSaveFile = mTempDir + File.separator + saveFileName;//生成临时文件
//                    VideoUtil.merge(mM3u8, tempSaveFile, mTempDir);//合并ts
//                    VideoUtil.moveFile(tempSaveFile, mSaveFilePath);//移动到指定文件夹
//                    if (isClearTempDir) {
//                        mHandler.postDelayed(new Runnable() {
//                            @Override
//                            public void run() {
//                                VideoUtil.clearDir(new File(mTempDir));//清空一下临时文件
//                            }
//                        }, 20 * 1000);//20s之后再删除
//                    }
                    mHandler.sendEmptyMessage(WHAT_ON_SUCCESS);
                    isRunning = false;
                }
            } catch (Exception e) {
                handlerError(e);
            }
        } else {
            handlerError(new Throwable("Task running"));
        }
    }

    private void startDownload(final M3u8 m3u8) {
        if (m3u8 == null) {
            handlerError(new Throwable("M3U8 is null"));
            return;
        }
        final File dir = new File(mTempDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        mCountTsSize = m3u8.getTsList().size();
        if (mExecutorService != null && mExecutorService.isTerminated()) {
            mExecutorService.shutdownNow();
            mExecutorService = null;
        }
        mExecutorService = Executors.newFixedThreadPool(mMaxThreadCount);
        final String basePath = m3u8.getBasePath();
        for (final M3u8Ts m3u8Ts : m3u8.getTsList()) {//循环下载
            mExecutorService.execute(new Runnable() {
                @Override
                public void run() {
                    File file = new File(dir + File.separator + m3u8Ts.getFileName());
                    if (!file.exists()) {//下载过的就不管了
                        FileOutputStream fos = null;
                        InputStream inputStream = null;
                        try {
                            String urlPath;
                            if ("http".equals(m3u8Ts.getFilePath().substring(0, 4))) {
                                urlPath = m3u8Ts.getFilePath();
                            } else {
                                urlPath = basePath + m3u8Ts.getFilePath();
                            }
                            URL url = new URL(urlPath);
                            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                            conn.setConnectTimeout(mReadTimeout);
                            conn.setReadTimeout(mReadTimeout);
                            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                                inputStream = conn.getInputStream();
                                fos = new FileOutputStream(file);//会自动创建文件
                                int len = 0;
                                byte[] buf = new byte[8 * 1024 * 1024];
                                while ((len = inputStream.read(buf)) != -1) {
                                    mCurrentLenght += len;
                                    fos.write(buf, 0, len);//写入流中
                                }
                                Log.i("DownloadManger", file.getPath());
                                m3u8Ts.setCache(true);
                                m3u8Ts.setTempFilePath(file.getPath());
                                mOnDownloadListener.onProgress(m3u8Ts);
                            } else {
                                handlerError(new Throwable(String.valueOf(conn.getResponseCode())));
                            }
                        } catch (MalformedURLException e) {
                            handlerError(e);
                        } catch (IOException e) {
                            handlerError(e);
                        } finally {
                            try {
                                if (inputStream != null) inputStream.close();
                                if (fos != null) fos.close();
                            } catch (IOException e) {
//                                    e.printStackTrace();
                            }
                        }
                        mCurrentTsSize++;
                        if (mCurrentTsSize == 3) {
                            mItemFileSize = file.length();
                        }
                        mHandler.sendEmptyMessage(WHAT_ON_PROGRESS);
                    } else {
                        m3u8Ts.setCache(true);
                        m3u8Ts.setTempFilePath(file.getPath());
                        mOnDownloadListener.onProgress(m3u8Ts);
                    }
                }
            });
        }

    }

    /**
     * 通知异常
     *
     * @param e
     */
    private void handlerError(Throwable e) {
        if (!"Task running".equals(e.getMessage())) {
            stop();
        }
        //不提示被中断的情况
        if ("thread interrupted".equals(e.getMessage())) {
            return;
        }
        Message msg = mHandler.obtainMessage();
        msg.obj = e;
        msg.what = WHAT_ON_ERROR;
        mHandler.sendMessage(msg);
    }

    /**
     * 停止任务
     */
    public void stop() {
        isRunning = false;
        if (mExecutorService != null) {
            mExecutorService.shutdownNow();
        }
    }
}
