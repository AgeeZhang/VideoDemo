package com.demo.videodemo;

import androidx.appcompat.app.AppCompatActivity;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.ConcatenatingMediaSource;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.zzj.videolibrary.AnalysisManger;
import com.zzj.videolibrary.contract.VideoContract;
import com.zzj.videolibrary.model.M3u8;
import com.zzj.videolibrary.model.M3u8Ts;
import com.zzj.videolibrary.utils.TimeFormatUtil;

public class ExoPlayerActivity extends AppCompatActivity {

    private PlayerView playerView;
    private TextView mCurrentDuration;
    private TextView mMaxDuration;
    private SeekBar mSeekBar;

    private final static String url = "http://cdndaode.inteink.com/Vedios/2020/demo/jh/jh.m3u8";
    private M3u8 mM3u8;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_exo_player);
        playerView = findViewById(R.id.video_view);
        mSeekBar = findViewById(R.id.video_progress_bar);
        mCurrentDuration = findViewById(R.id.current_duration);
        mMaxDuration = findViewById(R.id.total_duration);
        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                player.seekTo(1, 10);
            }
        });
        initializePlayer();
    }

    private SimpleExoPlayer player;

    private void initializePlayer() {
        if (player == null) {
            player = new SimpleExoPlayer.Builder(this).build();
            playerView.setPlayer(player);
            player.setPlayWhenReady(true);
            player.seekTo(0, 0);
            player.addListener(new ExoPlayer.EventListener() {
                @Override
                public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
//                    if (playbackState == 4) {
//                        MediaSource mediaSource = buildMediaSource(Uri.parse(mM3u8.getBasePath() + mM3u8.getTsList().get(1).getFilePath()));
//                        player.prepare(mediaSource, true, false);
//                    }
                    Log.i("111", playbackState + "");
                }

                @Override
                public void onPositionDiscontinuity(int reason) {
                    Log.i("onPositionDiscontinuity", reason + "");
                }

                @Override
                public void onSeekProcessed() {

                }
            });
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
                Toast.makeText(getApplicationContext(), "视频解析完成！", Toast.LENGTH_LONG).show();
                mM3u8 = m3u8;
                mSeekBar.setMax(Math.round(m3u8.getMaxDuration()));
                mCurrentDuration.setText(TimeFormatUtil.formatTime(0));
                mMaxDuration.setText(TimeFormatUtil.formatTime(m3u8.getMaxDuration()));
                ConcatenatingMediaSource concatenatedSource = new ConcatenatingMediaSource();
                for (int i = 0; i < m3u8.getTsList().size(); i++) {
                    M3u8Ts m3u8Ts = m3u8.getTsList().get(i);
                    MediaSource mediaSource = buildMediaSource(Uri.parse(m3u8.getBasePath() + m3u8Ts.getFilePath()));
                    concatenatedSource.addMediaSource(i, mediaSource);
                }
                player.prepare(concatenatedSource, true, false);
            }
        });
    }

    private MediaSource buildMediaSource(Uri uri) {
        return new ExtractorMediaSource.Factory(
                new DefaultHttpDataSourceFactory("exoplayer-codelab")).
                createMediaSource(uri);
    }
}