package com.demo.videodemo;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.KeyEvent;

import com.zzj.videolibrary.widget.SimpleVideoView;

public class SimpleVideoActivity extends AppCompatActivity {

    private SimpleVideoView mSimpleVideoView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simple_video);
        mSimpleVideoView = findViewById(R.id.my_simple_video);
        mSimpleVideoView.setVideoSource("http://cdndaode.inteink.com/Vedios/2020/demo/jh/jh.m3u8");
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_LEFT: //向左键
                mSimpleVideoView.fastBack();
                break;
            case KeyEvent.KEYCODE_DPAD_RIGHT:  //向右键
                mSimpleVideoView.fastForward();
                break;
            default:
                break;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        mSimpleVideoView.pause();
        super.onDestroy();
    }
}