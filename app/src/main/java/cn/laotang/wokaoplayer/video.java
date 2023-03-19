package cn.laotang.wokaoplayer;

import android.content.Context;
import android.util.Log;

import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerLibraryInfo;
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

import cn.laotang.dibudong.WOKAOHEVCVideoRenderer;
import cn.laotang.dibudong.WOKAORenderersFactor;

public class video {
    StyledPlayerView mVideView=null;
    MainActivity _act=null;
    public video(MainActivity act,StyledPlayerView view)
    {
        _act=act;
        mVideView=view;
    }

    private RenderersFactory buildRenderersFactory(Context context , Boolean preferExtensionRenderer
    ) {
        int extensionRendererMode = DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON;
        if(preferExtensionRenderer) {
            extensionRendererMode = DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER;
        }

        return new WOKAORenderersFactor(context.getApplicationContext())
                .setExtensionRendererMode(extensionRendererMode)
                .setEnableDecoderFallback(true);
    }

    public void play(String videoUrl,String firstname) {
        Log.e("wokao", WOKAOHEVCVideoRenderer.test());
        Log.e("wokao m", ExoPlayerLibraryInfo.registeredModules());
        //Log.e("wokao m", WOKAOHEVCLibrary.isAvailable()+"");
        Log.e("wokao m", ExoPlayerLibraryInfo.registeredModules());

        ExoPlayer player = new ExoPlayer.Builder(_act.getApplicationContext(), MediaSource.Factory.UNSUPPORTED)
                .setRenderersFactory(buildRenderersFactory(_act.getApplicationContext(), true))
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
        Log.e("wokao m", ExoPlayerLibraryInfo.registeredModules());
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
            _act.mTextView.setText(msg);
        }
        public void psetTitle(ExoPlayer p){
            int w=p.getVideoFormat().width;
            int h=p.getVideoFormat().height;
            _act.mTextView.setText(p.getCurrentMediaItem().mediaId+" wxh:"+w+"x"+h);
        }

        public void onPlayerError(PlaybackException error) {
            psetTitle(error.getMessage());
            Log.e("wokao m", ExoPlayerLibraryInfo.registeredModules());
            Log.d("wokao err:",error.getMessage());
        }

    }
}
