package cn.laotang.dibudong;

import android.util.Log;
import android.view.Surface;

import androidx.annotation.Nullable;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.decoder.DecoderInputBuffer;
import com.google.android.exoplayer2.decoder.SimpleDecoder;
import com.google.android.exoplayer2.decoder.VideoDecoderOutputBuffer;

import java.nio.ByteBuffer;

public class WOKAOHEVCDecoder extends SimpleDecoder<DecoderInputBuffer, VideoDecoderOutputBuffer, WOKAOHEVCDecoderException> {
    /**
     * @param inputBuffers  An array of nulls that will be used to store references to input buffers.
     * @param outputBuffers An array of nulls that will be used to store references to output buffers.
     */
    private final long d;
    protected WOKAOHEVCDecoder(DecoderInputBuffer[] inputBuffers, VideoDecoderOutputBuffer[] outputBuffers) {
        super(inputBuffers, outputBuffers);
        Log.d("wokaor", "WOKAOHEVCDecoder:" );
        //init decoder handel
        d=1;
        //todo create decode handle
    }
    @Override
    public void release() {
        super.release();
        //todo close
        //vpxClose(d);
    }

    private volatile @C.VideoOutputMode int outputMode;
    void setOutputMode(int om) {
        outputMode=om;
    }
    void renderToSurface(VideoDecoderOutputBuffer outputBuffer, Surface surface){

    }
    @Nullable
    @Override
    protected WOKAOHEVCDecoderException decode(DecoderInputBuffer inputBuffer, VideoDecoderOutputBuffer outputBuffer, boolean reset) {
        //todo decode video
        Log.d("wokaor", "WOKAOHEVCDecoder:" );
        return null;
    }

    @Override
    protected DecoderInputBuffer createInputBuffer() {
        return new DecoderInputBuffer(DecoderInputBuffer.BUFFER_REPLACEMENT_MODE_DIRECT);
    }

    @Override
    protected VideoDecoderOutputBuffer createOutputBuffer() {
        return new VideoDecoderOutputBuffer(this::releaseOutputBuffer);
    }
    @Override
    protected void releaseOutputBuffer(VideoDecoderOutputBuffer outputBuffer) {
        // Decode only frames do not acquire a reference on the internal decoder buffer and thus do not
        // require a call to vpxReleaseFrame.
        if (outputMode == C.VIDEO_OUTPUT_MODE_SURFACE_YUV && !outputBuffer.isDecodeOnly()) {
           // WOKAOHEVCReleaseFrame(d, outputBuffer);
        }
        super.releaseOutputBuffer(outputBuffer);
    }
    /**
     * Releases the frame. Used with {@link C#VIDEO_OUTPUT_MODE_SURFACE_YUV} only.
     *
     * param context Decoder context.
     * param outputBuffer Output buffer.
     */
    //private native void WOKAOHEVCReleaseFrame(long context, VideoDecoderOutputBuffer outputBuffer);
    @Override
    protected WOKAOHEVCDecoderException createUnexpectedDecodeException(Throwable error) {
        return new WOKAOHEVCDecoderException("Unexpected decode error", error);
    }


    @Override
    public String getName() {
        return "WOKAOHEVC";
    }
    private native long hevcInit(ByteBuffer buffer, int length);
    private native long hevcClose(long context);

    //VideoDecoderOutputBuffer
    private native int hevcDecode(long context, ByteBuffer encoded, int length, long pts,
                                  VideoDecoderOutputBuffer outputBuffer, int outputMode, boolean flush, String outpath);

}
