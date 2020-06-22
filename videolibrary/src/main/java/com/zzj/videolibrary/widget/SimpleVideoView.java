package com.zzj.videolibrary.widget;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.zzj.videolibrary.AnalysisManger;
import com.zzj.videolibrary.DownloadManger;
import com.zzj.videolibrary.R;
import com.zzj.videolibrary.contract.VideoContract;
import com.zzj.videolibrary.model.M3u8;
import com.zzj.videolibrary.model.M3u8Ts;
import com.zzj.videolibrary.utils.StringUtils;
import com.zzj.videolibrary.utils.TimeFormatUtil;
import com.zzj.videolibrary.utils.VideoUtil;

public class SimpleVideoView extends RelativeLayout implements SeekBar.OnSeekBarChangeListener, MediaPlayer.OnCompletionListener {

    private View mRootView;
    private VideoView mMainPlayer;      //主播放器
    private VideoView mSubPlayer;       //副播放器
    private LinearLayout mProgressView; //主要用于控制进度条显示
    private TextView mCurrentDuration;  //主要用于当前时长显示
    private TextView mTotalDuration;    //主要用于总时长显示
    private SeekBar mVideoProgressBar;  //视频进度条控制

    private DownloadManger mDownloadManger;
    private String mVideoSource; //设置视频源
    private String mVideoSourceType = "m3u8"; //视频源类型
    private M3u8 mM3u8; //视频对象
    private boolean isCache; //是否开启缓存
    private int mClipIndex = 0; //视频片段序号（m3u8视频源由多个片段组成）
    private String mCurrentPlayerTag = "main";  //当前使用的播放器标签（main、sub）
    private boolean isReady = true;
    Handler handler = new Handler();
    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            VideoView videoView = getCurrentPlayer();
            if (videoView.isPlaying()) {
                M3u8Ts m3u8Ts = mM3u8.getTsList().get(mClipIndex);
                int currentPosition = videoView.getCurrentPosition();
                int duration = Math.round(m3u8Ts.getTotalDuration() * 1000);
                float proportion = ((float) currentPosition / (float) duration);
                int time = Math.round(m3u8Ts.getStartTime() + (m3u8Ts.getTotalDuration() * proportion));
                mVideoProgressBar.setProgress(time);
                mCurrentDuration.setText(TimeFormatUtil.formatTime(time));
                if (isReady && proportion > 0.9 && mClipIndex < mM3u8.getTsList().size())
                    readyNextPlayer();
            }
            handler.postDelayed(this, 1000);
        }
    };

    public void setVideoSource(String videoSource) {
        this.mVideoSource = videoSource;
        start();
    }

    public void setCache(boolean isCache) {
        this.isCache = isCache;
    }

    public void pause() {
        getCurrentPlayer().stopPlayback();
    }

    public void play() {
        getCurrentPlayer().start();
    }

    public void fastForward() {
        getCurrentPlayer().pause();
        skipProgress(mVideoProgressBar.getProgress() + 3);
    }

    public void fastBack() {
        getCurrentPlayer().pause();
        skipProgress(mVideoProgressBar.getProgress() - 3);
    }

    public SimpleVideoView(Context context) {
        super(context);
    }

    public SimpleVideoView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mRootView = LayoutInflater.from(context).inflate(R.layout.simple_video_view, this);
        initView();
    }

    private void start() {
        initPlayer();
        analysisVideoSource();
    }

    private void initView() {
        mMainPlayer = findViewById(R.id.main_player);
        mSubPlayer = findViewById(R.id.sub_player);
        mProgressView = findViewById(R.id.progress_view);
        mCurrentDuration = findViewById(R.id.current_duration);
        mTotalDuration = findViewById(R.id.total_duration);
        mVideoProgressBar = findViewById(R.id.video_progress_bar);
        mVideoProgressBar.setOnSeekBarChangeListener(this);
    }

    private void initPlayer() {
        Toast.makeText(getContext(), "正在初始化视频播放器！", Toast.LENGTH_LONG).show();
        {
            mMainPlayer.requestFocus();
            mMainPlayer.setOnCompletionListener(this);
            mMainPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    getCurrentPlayer().start();
                    handler.postDelayed(runnable, 1000);
                }
            });
        }
        if (mVideoSourceType.equals("m3u8")) {
            mSubPlayer.requestFocus();
            mSubPlayer.setOnCompletionListener(this);
            mSubPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    getCurrentPlayer().start();
                    handler.postDelayed(runnable, 1000);
                }
            });
        }
    }

    private void analysisVideoSource() {
        if (!StringUtils.isEmpty(this.mVideoSource) && this.mVideoSourceType.equals("m3u8")) {
            AnalysisManger.getInstance().analysisM3u8(this.mVideoSource, new VideoContract.OnAnalysisListener() {
                @Override
                public void onStart() {
                    Toast.makeText(getContext(), "开始视频解析！", Toast.LENGTH_LONG).show();
                }

                @Override
                public void onError(Throwable errorMsg) {
                    Toast.makeText(getContext(), "视频解析失败！", Toast.LENGTH_LONG).show();
                }

                @Override
                public void onSuccess(M3u8 m3u8) {
                    Toast.makeText(getContext(), "视频解析完成！", Toast.LENGTH_LONG).show();
                    mM3u8 = m3u8;
                    mVideoProgressBar.setMax(Math.round(m3u8.getMaxDuration()));
                    mCurrentDuration.setText(TimeFormatUtil.formatTime(0));
                    mTotalDuration.setText(TimeFormatUtil.formatTime(m3u8.getMaxDuration()));
                    if (isCache) {
                        cacheVideo();
                    }
                    playClip(mClipIndex, 0);
                }
            });
        } else {
            Toast.makeText(getContext(), "找不到视频播放源！", Toast.LENGTH_LONG).show();
        }
    }

    private void cacheVideo() {
        if (mDownloadManger == null)
            mDownloadManger = new DownloadManger("1001", this.mM3u8);
        mDownloadManger.download(new VideoContract.OnDownloadListener() {
            @Override
            public void onStart() {

            }

            @Override
            public void onProgress(M3u8Ts m3u8Ts) {
                for (M3u8Ts m3u8Ts1 : mM3u8.getTsList()) {
                    if (m3u8Ts.getFileName().equals(m3u8Ts1.getFileName())) {
                        m3u8Ts1.setCache(m3u8Ts.isCache());
                        m3u8Ts1.setTempFilePath(m3u8Ts.getTempFilePath());
                    }
                }
            }

            @Override
            public void onSuccess() {

            }

            @Override
            public void onError(Throwable errorMsg) {

            }

            @Override
            public void onDownloading(long fileSize, int total, int current) {

            }
        });
    }

    private void setCurrentPlayerTag(String tag) {
        this.mCurrentPlayerTag = tag;
    }

    private VideoView getCurrentPlayer() {
        if (mCurrentPlayerTag.equals("main"))
            return this.mMainPlayer;
        else
            return this.mSubPlayer;
    }

    private VideoView getNextPlayer() {
        if (mCurrentPlayerTag.equals("main"))
            return this.mSubPlayer;
        else
            return this.mMainPlayer;
    }

    private void readyNextPlayer() {
        M3u8Ts m3u8Ts = mM3u8.getTsList().get(mClipIndex + 1);
        VideoView videoView = getNextPlayer();
        if (videoView != null) {
            if (m3u8Ts.isCache())
                videoView.setVideoPath(m3u8Ts.getTempFilePath());
            else
                videoView.setVideoURI(Uri.parse(mM3u8.getBasePath() + m3u8Ts.getFilePath()));
            videoView.seekTo(0);
            videoView.pause();
            isReady = false;
        } else {
            Toast.makeText(getContext(), "视频组件加载失败！", Toast.LENGTH_LONG).show();
        }
    }

    private void skipProgress(int progress) {
        int clipIndex = VideoUtil.getM3U8TsIndex(mM3u8, progress);
        M3u8Ts m3u8Ts = mM3u8.getTsList().get(clipIndex);
        int msec = Math.round(progress - m3u8Ts.getStartTime()) * 1000;
        if (clipIndex == mClipIndex) {
            getCurrentPlayer().seekTo(msec);
            getCurrentPlayer().start();
        } else {
            mClipIndex = clipIndex;
            playClip(mClipIndex, msec);
        }
    }

    private void playClip(int index, int msec) {
        M3u8Ts m3u8Ts = mM3u8.getTsList().get(index);
        VideoView videoView = getCurrentPlayer();
        if (videoView != null) {
            if (m3u8Ts.isCache())
                videoView.setVideoPath(m3u8Ts.getTempFilePath());
            else
                videoView.setVideoURI(Uri.parse(mM3u8.getBasePath() + m3u8Ts.getFilePath()));
            videoView.seekTo(msec);
        } else {
            Toast.makeText(getContext(), "视频组件加载失败！", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        mCurrentDuration.setText(TimeFormatUtil.formatTime(progress));
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        handler.removeCallbacks(runnable);
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        getCurrentPlayer().pause();
        skipProgress(seekBar.getProgress());
        handler.postDelayed(runnable, 1000);
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        getCurrentPlayer().stopPlayback();
        if (mClipIndex < mM3u8.getTsList().size() - 1) {
            getCurrentPlayer().setVisibility(GONE);
            getNextPlayer().setVisibility(VISIBLE);
            getNextPlayer().start();
            setCurrentPlayerTag(mCurrentPlayerTag.equals("main") ? "sub" : "main");
            ++mClipIndex;
            isReady = true;
        }
    }
}
