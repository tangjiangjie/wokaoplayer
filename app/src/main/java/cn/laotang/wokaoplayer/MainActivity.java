package cn.laotang.wokaoplayer;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;
import android.widget.TextView;

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

    StyledPlayerView mVideView=null;
    TextView mTextView=null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mTextView=(TextView)findViewById(R.id.videomsg);
        mVideView=(StyledPlayerView) findViewById(R.id.video_view);
        String url="http://10.0.0.104/test.mp4";
        String name="test not hdr";
        if(true){
            url="http://10.0.0.104/city.mkv";
            name="city hdr";
        }
        play(url,name);
    }
    private RenderersFactory buildRenderersFactory(Context context , Boolean preferExtensionRenderer
    ) {
        int extensionRendererMode = DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON;
        if(preferExtensionRenderer) {
            extensionRendererMode = DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER;
        }

        return new DefaultRenderersFactory(context.getApplicationContext())
                .setExtensionRendererMode(extensionRendererMode)
                .setEnableDecoderFallback(true);
    }

    public void play(String videoUrl,String firstname) {
        ExoPlayer player = new ExoPlayer.Builder(getApplicationContext(), MediaSource.Factory.UNSUPPORTED)
                .setRenderersFactory(buildRenderersFactory(getApplicationContext(), true))
                .build();


        mVideView.setPlayer(player);
        mVideView.setShowPreviousButton(false);
        mVideView.setShowNextButton(false);

        mVideView.setKeepScreenOn(true);

        MediaItem mediaItem = new MediaItem.Builder().setMediaId(firstname).setUri(videoUrl).build();

        player.setMediaSource(mi2ms(mediaItem));

        // player.setRepeatMode()

        player.prepare();
        player.addListener(new wkListener());
        //player.setRepeatMode(REPEAT_MODE_ALL);
        player.play();
    }
    MediaSource mi2ms(MediaItem ite){

        Map<String ,String> heads=new HashMap<String,String>();
        //heads.put("Connection","Keep-Alive");
        //heads.put("Connection","close");
        //heads.put("Keep-Alive","timeout=1800, max=1000");
        DefaultHttpDataSource.Factory fc=new DefaultHttpDataSource.Factory();
        //fc.setDefaultRequestProperties(heads);
        return new ProgressiveMediaSource.Factory(fc).createMediaSource(ite);
    }
    class wkListener implements Player.Listener {
        public void onIsPlayingChanged(boolean isPlaying) {
            ExoPlayer p=(ExoPlayer)mVideView.getPlayer();
            if(!isPlaying){
                if(p!=null) psetTitle(p);
            }
            else{
                psetTitle("");
            }

        }
        void psetTitle(String msg){
            mTextView.setText(msg);
        }
        public void psetTitle(ExoPlayer p){
            int w=p.getVideoFormat().width;
            int h=p.getVideoFormat().height;
            mTextView.setText(p.getCurrentMediaItem().mediaId+" wxh:"+w+"x"+h);
        }

        public void onPlayerError(PlaybackException error) {
            setTitle(error.getMessage());
            Log.d("wokao err:",error.getMessage());
        }

    }
}