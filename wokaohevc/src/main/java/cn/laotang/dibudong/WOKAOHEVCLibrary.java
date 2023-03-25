package cn.laotang.dibudong;
import com.google.android.exoplayer2.ExoPlayerLibraryInfo;
import com.google.android.exoplayer2.util.LibraryLoader;
public class WOKAOHEVCLibrary {

    // Used to load the 'wokaohevc' library on application startup.
    private static final LibraryLoader LOADER =
            new LibraryLoader("wokaohevcdecoder") {
                @Override
                protected void loadLibrary(String name) {
                    System.loadLibrary(name);
                }
            };




    static {
        System.loadLibrary("wokaohevcdecoder");
        ExoPlayerLibraryInfo.registerModule("cn.laotang.wokaohevc");
    }
    public static boolean isAvailable() {
        return true;
    }
    public static native String hevcGetVersion();
    public static native String hevcGetBuildConfig();
    public static native boolean hevcIsSecureDecodeSupported();

}