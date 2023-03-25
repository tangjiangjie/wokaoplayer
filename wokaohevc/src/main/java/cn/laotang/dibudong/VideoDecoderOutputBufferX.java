package cn.laotang.dibudong;

import android.util.Log;

import com.google.android.exoplayer2.decoder.VideoDecoderOutputBuffer;

import java.nio.ByteBuffer;

public class VideoDecoderOutputBufferX extends VideoDecoderOutputBuffer {

    public static final int PIXFMT_RGB565 = 1;
    public static final int PIXFMT_ARGB8888 = 2;
    public int pixfmt;
    static String TAG="VideoDecoderOutputBufferX";
    /**
     * Creates VideoDecoderOutputBuffer.
     *
     * @param owner Buffer owner.
     */
    public VideoDecoderOutputBufferX(Owner<VideoDecoderOutputBuffer> owner) {
        super(owner);
    }

    /**
     * Resizes the buffer based on the given dimensions. Called via JNI after decoding completes.
     * @return the resized buffer size, negative indicates resize error
     */
    public int initForRgbFrame(int width, int height, int pixfmt) {
        this.width = width;
        this.height = height;
        this.yuvPlanes = null;
        int bytesPerPixel;
        switch (pixfmt) {
            case PIXFMT_RGB565:
                bytesPerPixel = 2;
                break;
            case PIXFMT_ARGB8888:
                bytesPerPixel = 4;
                break;
            default:
                Log.e(TAG, "[initForRgbFrame] unrecognized pixfmt, " + pixfmt);
                return -1;
        }
        if (!isSafeToMultiply(width, height) || !isSafeToMultiply(width * height, bytesPerPixel)) {
            Log.e(TAG, "[initForRgbFrame] cant handle size " + width + "x" + height + "x" + bytesPerPixel);
            return -2;
        }
        this.pixfmt = pixfmt;
        int bytes = width * height * bytesPerPixel;
        try {
            initData(bytes);
        } catch(OutOfMemoryError err) {
            Log.e(TAG, "[initForRgbFrame]  error allocate memory" + bytes, err);
            return -3;
        }
        return bytes;
    }
    private void initData(int size) {
        if (data == null || data.capacity() < size) {
            data = ByteBuffer.allocateDirect(size);
        } else {
            data.position(0);
            data.limit(size);
        }
    }
    private static boolean isSafeToMultiply(int a, int b) {
        return a >= 0 && b >= 0 && !(b > 0 && a >= Integer.MAX_VALUE / b);
    }
}
