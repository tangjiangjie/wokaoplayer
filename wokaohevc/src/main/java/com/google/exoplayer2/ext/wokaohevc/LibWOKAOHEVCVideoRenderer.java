package com.google.exoplayer2.ext.wokaohevc;

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
import com.google.android.exoplayer2.video.DecoderVideoRenderer;
import com.google.android.exoplayer2.video.VideoRendererEventListener;

public class LibWOKAOHEVCVideoRenderer extends DecoderVideoRenderer {
    private static final String TAG = "LibWOKAOHEVCVideoRenderer";

    /**
     * @param allowedJoiningTimeMs     The maximum duration in milliseconds for which this video renderer
     *                                 can attempt to seamlessly join an ongoing playback.
     * @param eventHandler             A handler to use when delivering events to {@code eventListener}. May be
     *                                 null if delivery of events is not required.
     * @param eventListener            A listener of events. May be null if delivery of events is not required.
     * @param maxDroppedFramesToNotify The maximum number of frames that can be dropped between
     *                                 invocations of {@link VideoRendererEventListener#onDroppedFrames(int, long)}.
     */
    protected LibWOKAOHEVCVideoRenderer(long allowedJoiningTimeMs, @Nullable Handler eventHandler, @Nullable VideoRendererEventListener eventListener, int maxDroppedFramesToNotify) {
        super(allowedJoiningTimeMs, eventHandler, eventListener, maxDroppedFramesToNotify);
    }

    @Override
    protected Decoder<DecoderInputBuffer, ? extends VideoDecoderOutputBuffer, ? extends DecoderException> createDecoder(Format format, @Nullable CryptoConfig cryptoConfig) throws DecoderException {
        return null;
    }

    @Override
    protected void renderOutputBufferToSurface(VideoDecoderOutputBuffer outputBuffer, Surface surface) throws DecoderException {

    }

    @SuppressLint("LongLogTag")
    @Override
    protected void setDecoderOutputMode(int outputMode) {
        Log.d(TAG, "setDecoderOutputMode:" + outputMode);

    }

    @Override
    public String getName() {
        return TAG;
    }

    @Override
    public void setPlaybackSpeed(float currentPlaybackSpeed, float targetPlaybackSpeed) throws ExoPlaybackException {
        super.setPlaybackSpeed(currentPlaybackSpeed, targetPlaybackSpeed);
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
        return RendererCapabilities.create(
                C.FORMAT_HANDLED | ADAPTIVE_SEAMLESS);
    }

    public static String test() {
        return TAG;
    }

}
