package cn.laotang.dibudong;
import com.google.android.exoplayer2.ExoPlayerLibraryInfo;
import com.google.android.exoplayer2.util.LibraryLoader;
public class WOKAOHEVCLibrary {

    // Used to load the 'wokaohevc' library on application startup.
    static {
        //System.loadLibrary("wokaohevc");
    }
    static {
        ExoPlayerLibraryInfo.registerModule("goog.exo.wokaohevc");
    }
    public static boolean isAvailable() {
        return true;
    }

    /**
     * A native method that is implemented by the 'wokaohevc' native library,
     * which is packaged with this application.
     */
    public String stringFromJNI()
    {
        return "1234";
    }
}