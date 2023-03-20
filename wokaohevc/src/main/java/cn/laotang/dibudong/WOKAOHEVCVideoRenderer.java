package cn.laotang.dibudong;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.util.Log;
import android.view.Surface;

import androidx.annotation.Nullable;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.RendererCapabilities;
import com.google.android.exoplayer2.decoder.CryptoConfig;
import com.google.android.exoplayer2.decoder.Decoder;
import com.google.android.exoplayer2.decoder.DecoderException;
import com.google.android.exoplayer2.decoder.DecoderInputBuffer;
import com.google.android.exoplayer2.decoder.VideoDecoderOutputBuffer;

import com.google.android.exoplayer2.util.MimeTypes;
import com.google.android.exoplayer2.util.TraceUtil;
import com.google.android.exoplayer2.video.DecoderVideoRenderer;
import com.google.android.exoplayer2.video.VideoRendererEventListener;



//MediaCodecVideoRenderer
public class WOKAOHEVCVideoRenderer extends DecoderVideoRenderer {
    private static final String TAG = "WOKAOHEVCVideoRenderer";
    private final int numInputBuffers;
    /**
     * The number of output buffers. The renderer may limit the minimum possible value due to
     * requiring multiple output buffers to be dequeued at a time for it to make progress.
     */
    private final int numOutputBuffers;
    /**
     * The default input buffer size. The value is based on <a
     * href="https://android.googlesource.com/platform/frameworks/av/+/d42b90c5183fbd9d6a28d9baee613fddbf8131d6/media/libstagefright/codecs/on2/dec/SoftVPX.cpp">SoftVPX.cpp</a>.
     */
    private static final int DEFAULT_INPUT_BUFFER_SIZE = 768 * 1024;


    /**
     * @param allowedJoiningTimeMs     The maximum duration in milliseconds for which this video renderer
     *                                 can attempt to seamlessly join an ongoing playback.
     * @param eventHandler             A handler to use when delivering events to {@code eventListener}. May be
     *                                 null if delivery of events is not required.
     * @param eventListener            A listener of events. May be null if delivery of events is not required.
     * @param maxDroppedFramesToNotify The maximum number of frames that can be dropped between
     *                                 invocations of {@link VideoRendererEventListener#onDroppedFrames(int, long)}.
     */
    protected WOKAOHEVCVideoRenderer(long allowedJoiningTimeMs, @Nullable Handler eventHandler, @Nullable VideoRendererEventListener eventListener, int maxDroppedFramesToNotify,int ni,int no) {
        super(allowedJoiningTimeMs, eventHandler, eventListener, maxDroppedFramesToNotify);
        numInputBuffers=ni;
        numOutputBuffers=no;
        Log.d("wokaor", "LibWOKAOHEVCVideoRenderer:" );

    }
    WOKAOHEVCDecoder d=null;

    @Override
    protected Decoder<DecoderInputBuffer, ? extends VideoDecoderOutputBuffer, ? extends DecoderException> createDecoder(Format format, @Nullable CryptoConfig cryptoConfig) throws DecoderException {
        TraceUtil.beginSection("createWOKAOHEVCDecoder");
        this.d=new WOKAOHEVCDecoder(new DecoderInputBuffer[numInputBuffers], new VideoDecoderOutputBuffer[numOutputBuffers]);
        TraceUtil.endSection();
        return this.d;
    }

    @Override
    protected void renderOutputBufferToSurface(VideoDecoderOutputBuffer outputBuffer, Surface surface) throws DecoderException {
        if (d==null) {
            throw new WOKAOHEVCDecoderException(
                    "Failed to render output buffer to surface: decoder is not initialized.");
        }
        d.renderToSurface(outputBuffer, surface);
        outputBuffer.release();
    }

    @SuppressLint("LongLogTag")
    @Override
    protected void setDecoderOutputMode(int outputMode) {
        Log.d(TAG, "setDecoderOutputMode:" + outputMode);
        if (d != null) {
            d.setOutputMode(outputMode);
        }

    }

    @Override
    public String getName() {
        return TAG;
    }




    @Override
    public int supportsFormat(Format format) throws ExoPlaybackException {
        Log.e("wokao q:", format.toString());
        if (!MimeTypes.VIDEO_H265.equalsIgnoreCase(format.sampleMimeType)
                || !WOKAOHEVCLibrary.isAvailable()) {
            return RendererCapabilities.create(C.FORMAT_UNSUPPORTED_TYPE);
        }
        if (format.cryptoType != C.CRYPTO_TYPE_NONE) {
            return RendererCapabilities.create(C.FORMAT_UNSUPPORTED_DRM);
        }
        return RendererCapabilities.create(C.FORMAT_UNSUPPORTED_TYPE);
        //return RendererCapabilities.create(C.FORMAT_HANDLED | ADAPTIVE_SEAMLESS);
    }

    public static String test() {
        return TAG;
    }

}
