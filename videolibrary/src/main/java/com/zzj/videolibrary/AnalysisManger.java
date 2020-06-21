package com.zzj.videolibrary;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.zzj.videolibrary.contract.VideoContract;
import com.zzj.videolibrary.model.M3u8;
import com.zzj.videolibrary.utils.VideoUtil;

import java.io.IOException;

public class AnalysisManger {

    private static AnalysisManger singleton;
    private VideoContract.OnAnalysisListener onAnalysisListener;
    private static final int WHAT_ON_ERROR = 1101;
    private static final int WHAT_ON_SUCCESS = 1102;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case WHAT_ON_ERROR:
                    onAnalysisListener.onError((Throwable) msg.obj);
                    break;
                case WHAT_ON_SUCCESS:
                    onAnalysisListener.onSuccess((M3u8) msg.obj);
                    break;
            }
        }
    };

    private AnalysisManger() {
    }

    public static AnalysisManger getInstance() {
        synchronized (AnalysisManger.class) {
            if (singleton == null) {
                singleton = new AnalysisManger();
            }
        }
        return singleton;
    }

    public synchronized void analysisM3u8(final String url, VideoContract.OnAnalysisListener onAnalysisListener) {
        this.onAnalysisListener = onAnalysisListener;
        onAnalysisListener.onStart();
        new Thread() {
            @Override
            public void run() {
                try {
                    M3u8 m3u8 = VideoUtil.analysisM3u8(url);
                    Log.i("m3u8", m3u8.toString());
                    handlerSuccess(m3u8);
                } catch (IOException e) {
                    e.printStackTrace();
                    handlerError(e);
                }
            }
        }.start();
    }

    private void handlerSuccess(M3u8 m3u8) {
        Message msg = mHandler.obtainMessage();
        msg.obj = m3u8;
        msg.what = WHAT_ON_SUCCESS;
        mHandler.sendMessage(msg);
    }

    private void handlerError(Throwable e) {
        Message msg = mHandler.obtainMessage();
        msg.obj = e;
        msg.what = WHAT_ON_ERROR;
        mHandler.sendMessage(msg);
    }


}
