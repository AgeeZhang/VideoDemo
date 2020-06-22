package com.demo.videodemo;

import androidx.appcompat.app.AppCompatActivity;

import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Window;
import android.view.WindowManager;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.ConcatenatingMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.zzj.videolibrary.AnalysisManger;
import com.zzj.videolibrary.contract.VideoContract;
import com.zzj.videolibrary.model.M3u8;
import com.zzj.videolibrary.model.M3u8Ts;
import com.zzj.videolibrary.utils.TimeFormatUtil;
import com.zzj.videolibrary.utils.VideoUtil;

public class ExoPlayerActivity extends AppCompatActivity {

    private PlayerView mPlayerView;
    private SimpleExoPlayer mSimpleExoPlayer;
    private TextView mCurrentDuration;
    private TextView mTotalDuration;
    private SeekBar mSeekBar;

    private final static String url = "http://cdndaode.inteink.com/Vedios/2020/demo/jh/jh.m3u8";
    private M3u8 mM3u8;
    private boolean mLockLongPressKey = false;  //处理长按事件
    private boolean isForward = false;          //判断前进还是后退
    private Handler mProgressHandler = new Handler();
    private Runnable mProgressRunnable = new Runnable() {

        @Override
        public void run() {
            if (mSimpleExoPlayer.isPlaying()) {
                M3u8Ts m3u8Ts = mM3u8.getTsList().get(mSimpleExoPlayer.getCurrentWindowIndex());
                float proportion = ((float) mSimpleExoPlayer.getCurrentPosition() / (float) Math.round(m3u8Ts.getTotalDuration() * 1000));
                int progress = Math.round(m3u8Ts.getStartTime() + (m3u8Ts.getTotalDuration() * proportion));
                //设置进度条的主要进度，表示当前的播放时间
                mSeekBar.setProgress(progress);
                mCurrentDuration.setText(TimeFormatUtil.formatTime(progress));
                // 设置进度条的次要进度，表示视频的缓冲进度
                float secondaryProportion = ((float) mSimpleExoPlayer.getBufferedPosition() / (float) Math.round(m3u8Ts.getTotalDuration() * 1000));
                int secondaryProgress = Math.round(m3u8Ts.getStartTime() + (m3u8Ts.getTotalDuration() * secondaryProportion));
                mSeekBar.setSecondaryProgress(secondaryProgress);
            }
            mProgressHandler.postDelayed(mProgressRunnable, 1000);
        }
    };
    private Handler mLongPressHandler = new Handler();
    private Runnable mLongPressRunnable = new Runnable() {

        @Override
        public void run() {
            if (mSimpleExoPlayer.isPlaying() && mLockLongPressKey) {
                if (isForward) {
                    jumpProgress(mSeekBar.getProgress() + 3);
                } else {
                    jumpProgress(mSeekBar.getProgress() - 3);
                }
            }
            mLongPressHandler.postDelayed(mLongPressRunnable, 1000);
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_exo_player);
        mPlayerView = findViewById(R.id.video_view);
        mSeekBar = findViewById(R.id.video_progress_bar);
        mCurrentDuration = findViewById(R.id.current_duration);
        mTotalDuration = findViewById(R.id.total_duration);
        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mCurrentDuration.setText(TimeFormatUtil.formatTime(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                mProgressHandler.removeCallbacks(mProgressRunnable);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                jumpProgress(seekBar.getProgress());
            }
        });
        initializePlayer();
    }


    private void initializePlayer() {
        if (mSimpleExoPlayer == null) {
            mSimpleExoPlayer = new SimpleExoPlayer.Builder(this).build();
            mPlayerView.setPlayer(mSimpleExoPlayer);
            mSimpleExoPlayer.setPlayWhenReady(true);
            mSimpleExoPlayer.seekTo(0, 0);
        }
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
                Toast.makeText(getApplicationContext(), "视频解析完成，准备播放！", Toast.LENGTH_LONG).show();
                mM3u8 = m3u8;
                mSeekBar.setMax(Math.round(m3u8.getMaxDuration()));
                mCurrentDuration.setText(TimeFormatUtil.formatTime(0));
                mTotalDuration.setText(TimeFormatUtil.formatTime(m3u8.getMaxDuration()));
                ConcatenatingMediaSource concatenatedSource = new ConcatenatingMediaSource();
                for (int i = 0; i < m3u8.getTsList().size(); i++) {
                    M3u8Ts m3u8Ts = m3u8.getTsList().get(i);
                    MediaSource mediaSource = buildMediaSource(Uri.parse(m3u8.getBasePath() + m3u8Ts.getFilePath()));
                    concatenatedSource.addMediaSource(i, mediaSource);
                }
                mSimpleExoPlayer.prepare(concatenatedSource, true, false);
                mProgressHandler.postDelayed(mProgressRunnable, 1000);
            }
        });
    }

    private MediaSource buildMediaSource(Uri uri) {
        return new ProgressiveMediaSource.Factory(
                new DefaultHttpDataSourceFactory("exoplayer-codelab")).
                createMediaSource(uri);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_LEFT: //向左键
            case KeyEvent.KEYCODE_DPAD_RIGHT:  //向右键
                mProgressHandler.removeCallbacks(mProgressRunnable);
                break;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_LEFT: //向左键
                jumpProgress(mSeekBar.getProgress());
            case KeyEvent.KEYCODE_DPAD_RIGHT:  //向右键
                jumpProgress(mSeekBar.getProgress());
                break;
        }
        return super.onKeyUp(keyCode, event);
    }

    private void jumpProgress(int progress) {
        mProgressHandler.removeCallbacks(mProgressRunnable);
        int windowIndex = VideoUtil.getM3U8TsIndex(mM3u8, progress);
        M3u8Ts m3u8Ts = mM3u8.getTsList().get(windowIndex);
        mSimpleExoPlayer.seekTo(windowIndex, Math.round(progress - m3u8Ts.getStartTime()) * 1000);
        mProgressHandler.postDelayed(mProgressRunnable, 1000);
    }
}