package com.zzj.videolibrary.contract;

import com.zzj.videolibrary.model.M3u8;
import com.zzj.videolibrary.model.M3u8Ts;

public interface VideoContract {

    interface OnDownloadListener {

        void onStart();

        void onProgress(M3u8Ts m3u8Ts);

        void onSuccess();

        void onError(Throwable errorMsg);

        void onDownloading(long fileSize, int total, int current);
    }

    interface OnAnalysisListener {
        void onStart();

        void onError(Throwable errorMsg);

        void onSuccess(M3u8 m3u8);
    }

}
