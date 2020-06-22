package com.zzj.videolibrary;

import android.widget.VideoView;

import com.zzj.videolibrary.contract.VideoContract;
import com.zzj.videolibrary.model.M3u8;

public class VideoController {

    private static VideoController singleton;
    private DownloadManger mDownloadManger;

    public static synchronized VideoController getInstance() {
        if (singleton == null) {
            singleton = new VideoController();
        }
        return singleton;
    }


}
