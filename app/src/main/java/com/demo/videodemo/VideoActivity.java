package com.demo.videodemo;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.MediaController;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.zzj.videolibrary.AnalysisManger;
import com.zzj.videolibrary.DownloadManger;
import com.zzj.videolibrary.contract.VideoContract;
import com.zzj.videolibrary.model.M3u8;
import com.zzj.videolibrary.model.M3u8Ts;
import com.zzj.videolibrary.utils.TimeFormatUtil;
import com.zzj.videolibrary.utils.VideoUtil;

public class VideoActivity extends AppCompatActivity {

    private final static String url = "http://cdndaode.inteink.com/Vedios/2020/demo/jh/jh.m3u8";
    private VideoView mVideoView;
    private TextView mCurrentDuration;
    private TextView mMaxDuration;
    private SeekBar mSeekBar;

    private DownloadManger mDownloadManger;
    private M3u8 mM3u8;
    private int mPlayIndex = 0;

    private Handler handler = new Handler();
    private Runnable runnable = new Runnable() {

        int currentPosition, duration;

        public void run() {
            // 获得当前播放时间和当前视频的长度
            if (mVideoView.isPlaying()) {
                M3u8Ts m3u8Ts = mM3u8.getTsList().get(mPlayIndex);
                currentPosition = mVideoView.getCurrentPosition();
                duration = Math.round(m3u8Ts.getTotalDuration() * 1000);
                float proportion = ((float) currentPosition / (float) duration);
                int time = Math.round(m3u8Ts.getStartTime() + (m3u8Ts.getTotalDuration() * proportion));
                // 设置进度条的主要进度，表示当前的播放时间
                mSeekBar.setProgress(time);
                mCurrentDuration.setText(TimeFormatUtil.formatTime(time));
            }
            // 设置进度条的次要进度，表示视频的缓冲进度
//            buffer = mVideoView.getBufferPercentage();
//            mSeekBar.setSecondaryProgress(buffer);
            handler.postDelayed(runnable, 1000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);
        mVideoView = findViewById(R.id.mVideoView);
        mSeekBar = findViewById(R.id.mSeekBar);
        mCurrentDuration = findViewById(R.id.mCurrentDuration);
        mMaxDuration = findViewById(R.id.mMaxDuration);
        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mCurrentDuration.setText(TimeFormatUtil.formatTime(progress));
                Log.i("VideoActivity", progress + "");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                handler.removeCallbacks(runnable);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                Toast.makeText(getApplicationContext(), "拖动进度条！", Toast.LENGTH_LONG).show();
                mVideoView.pause();
                int progress = seekBar.getProgress();
                mPlayIndex = VideoUtil.getM3U8TsIndex(mM3u8, progress);
                M3u8Ts m3u8Ts = mM3u8.getTsList().get(mPlayIndex);
                playClip(mPlayIndex, Math.round(progress - m3u8Ts.getStartTime()) * 1000);
                handler.postDelayed(runnable, 1000);
            }
        });
        initVideoView();
    }

    private void initVideoView() {
        AnalysisManger.getInstance().analysisM3u8(url, new VideoContract.OnAnalysisListener() {
            @Override
            public void onStart() {
                Toast.makeText(getApplicationContext(), "开始视频解析！", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onError(Throwable errorMsg) {
                Toast.makeText(getApplicationContext(), "视频解析失败！", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onSuccess(M3u8 m3u8) {
                Toast.makeText(getApplicationContext(), "视频解析完成！", Toast.LENGTH_LONG).show();
                mM3u8 = m3u8;
                mSeekBar.setMax(Math.round(m3u8.getMaxDuration()));
                mCurrentDuration.setText(TimeFormatUtil.formatTime(0));
                mMaxDuration.setText(TimeFormatUtil.formatTime(m3u8.getMaxDuration()));
                playVideo();
            }
        });
        mVideoView.setZOrderOnTop(true);
        mVideoView.requestFocus();
        mVideoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                mVideoView.stopPlayback();
                playClip(++mPlayIndex, 0);
            }
        });
        mVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                Toast.makeText(getApplicationContext(), "设置进度！！！！", Toast.LENGTH_LONG).show();
                mVideoView.start();
            }
        });

    }

    private void playVideo() {
        if (mDownloadManger == null)
            mDownloadManger = new DownloadManger("1001", this.mM3u8);
        mDownloadManger.download(new VideoContract.OnDownloadListener() {
            @Override
            public void onStart() {
                handler.postDelayed(runnable, 1000);
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
        playClip(mPlayIndex, 0);
    }


    private void playClip(int index, int seekTo) {
        if (index > mM3u8.getTsList().size() - 1) {
            Toast.makeText(this, "视频播放完成！", Toast.LENGTH_LONG).show();
            return;
        }
        M3u8Ts m3u8Ts = mM3u8.getTsList().get(index);
        if (mVideoView != null) {
            if (m3u8Ts.isCache())
                mVideoView.setVideoPath(m3u8Ts.getTempFilePath());
            else
                mVideoView.setVideoURI(Uri.parse(mM3u8.getBasePath() + m3u8Ts.getFilePath()));
            mVideoView.seekTo(seekTo);
//            mVideoView.start();
        } else {
            Toast.makeText(this, "视频组件加载失败！", Toast.LENGTH_LONG).show();
        }
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Toast.makeText(this, "按键监听！", Toast.LENGTH_LONG).show();
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_LEFT: //向左键
            {
                mVideoView.pause();
                int progress = mSeekBar.getProgress() - 3;
                mPlayIndex = VideoUtil.getM3U8TsIndex(mM3u8, progress);
                M3u8Ts m3u8Ts = mM3u8.getTsList().get(mPlayIndex);
                playClip(mPlayIndex, Math.round(progress - m3u8Ts.getStartTime()) * 1000);
            }
            break;
            case KeyEvent.KEYCODE_DPAD_RIGHT:  //向右键
            {
                mVideoView.pause();
                int progress = mSeekBar.getProgress() + 3;
                mPlayIndex = VideoUtil.getM3U8TsIndex(mM3u8, progress);
                M3u8Ts m3u8Ts = mM3u8.getTsList().get(mPlayIndex);
                playClip(mPlayIndex, Math.round(progress - m3u8Ts.getStartTime()) * 1000);
            }
            break;
            default:
                break;
        }
        return super.onKeyDown(keyCode, event);
    }
}
