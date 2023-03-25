package cn.laotang.wokaoplayer;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.PlaybackException;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.RenderersFactory;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.ui.StyledPlayerView;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    video v=null;
    TextView mTextView=null;
    private PowerManager.WakeLock mWakeLock = null;
//    public void requestPermission() {
//        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
//                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
//
//            Toast.makeText(this, "申请权限", Toast.LENGTH_SHORT).show();
//
//
//            ActivityCompat.requestPermissions(this, new String[]{
//                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
//
//                    Manifest.permission.READ_EXTERNAL_STORAGE}, 100);
//        }
//    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        acquireWakeLock();
        String localpath=this.getFilesDir().getAbsolutePath();
        //Log.e("path",localpath);
//        requestPermission();

        mTextView=(TextView)findViewById(R.id.videomsg);
        v=new video(this,(StyledPlayerView) findViewById(R.id.video_view));

        String url="http://10.0.0.104/test.mp4";
        String name="test not hdr";
        if(true){
            url="http://10.0.0.104/city.mkv";
            name="city hdr";
        }
        v.play(url,name);

    }

    private void acquireWakeLock() {
        if(mWakeLock == null) {
            PowerManager pm = (PowerManager)getSystemService(POWER_SERVICE);
            mWakeLock = pm.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK |
                            PowerManager.ACQUIRE_CAUSES_WAKEUP,this.getClass().getCanonicalName());

        }
        mWakeLock.acquire();
    }
    private void releaseWakeLock() {
        if(mWakeLock != null) {
            mWakeLock.release();
            ///mWakeLock = null;
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
        acquireWakeLock();
    }
    @Override
    protected void onPause() {

        super.onPause();
        releaseWakeLock();
    }

}