package com.google.exoplayer2.ext.wokaohevc;

public class WOKAOHEVCLibrary {

    // Used to load the 'wokaohevc' library on application startup.
    static {
        //System.loadLibrary("wokaohevc");
    }

    public static boolean isAvailable() {
        return true;
    }

    /**
     * A native method that is implemented by the 'wokaohevc' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();
}