package cn.laotang.dibudong;

import android.util.Log;
import android.view.Surface;

import androidx.annotation.Nullable;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.decoder.DecoderInputBuffer;
import com.google.android.exoplayer2.decoder.SimpleDecoder;
import com.google.android.exoplayer2.decoder.VideoDecoderOutputBuffer;

import java.nio.ByteBuffer;
import java.util.List;

public class WOKAOHEVCDecoder extends SimpleDecoder<DecoderInputBuffer, VideoDecoderOutputBuffer, WOKAOHEVCDecoderException> {
    /**
     * @param inputBuffers  An array of nulls that will be used to store references to input buffers.
     * @param outputBuffers An array of nulls that will be used to store references to output buffers.
     */
    private final long d;
    protected WOKAOHEVCDecoder(DecoderInputBuffer[] inputBuffers, VideoDecoderOutputBuffer[] outputBuffers, Format format) throws WOKAOHEVCDecoderException {
        super(inputBuffers, outputBuffers);
        Log.d("wokaor", "WOKAOHEVCDecoder:" );
        //init decoder handel
        ByteBuffer buffer = wrapBytes(format.initializationData);
        d = hevcInit(buffer, buffer.capacity());

        if (d == 0) {
            throw new WOKAOHEVCDecoderException("Failed to initialize decoder");
        }
        //setInitialInputBufferSize(initialInputBufferSize);
    }
    private ByteBuffer wrapBytes(List<byte[]> bytesList) {
        int size = 0;
        for (byte[] it : bytesList) {
            size += it.length;
        }
        ByteBuffer buffer = ByteBuffer.allocateDirect(size);
        byte[] data = new byte[size];

        int i = 0;
        for (byte[] it : bytesList) {
            System.arraycopy(it, 0, data, i, it.length);
            i += it.length;
        }
        buffer.put(data);
        return buffer;
    }
    @Override
    public void release() {
        super.release();
        hevcClose(d);
    }

    private volatile @C.VideoOutputMode int outputMode;
    void setOutputMode(int om) {
        outputMode=om;
    }
    void renderToSurface(VideoDecoderOutputBuffer outputBuffer, Surface surface) throws WOKAOHEVCDecoderException {
        if (outputBuffer.mode != C.VIDEO_OUTPUT_MODE_SURFACE_YUV) {
            throw new WOKAOHEVCDecoderException("Invalid output mode.");
        }
        if (hevcRenderFrame(d, surface, outputBuffer) == -1) {
            throw new WOKAOHEVCDecoderException("Buffer render error: " );
        }
    }
    // DO NOT CHANGE THESE CONSTANTS WITHOUT CHANGE IT'S NATIVE COUNTERPART
    public static final int  DECODE_ONLY = 1;
    private static final int NO_ERROR = 0;
    private static final int DECODE_ERROR = -1;
    private static final int DRM_ERROR = -2;
    public static final int GET_FRAME_ERROR = -3;


    @Nullable
    @Override
    protected WOKAOHEVCDecoderException decode(DecoderInputBuffer inputBuffer, VideoDecoderOutputBuffer outputBuffer, boolean reset) {
        //todo decode video
        Log.d("wokaor", "WOKAOHEVCDecoder:" );
        if(inputBuffer.isEncrypted() && !WOKAOHEVCLibrary.hevcIsSecureDecodeSupported()) {
            return new WOKAOHEVCDecoderException(WOKAOHEVCLibrary.hevcGetVersion() + " does not support secure decode.");
        }

        ByteBuffer inputData = inputBuffer.data;
        int inputSize = inputData.limit();

        final long result = hevcDecode(d, inputData, inputSize, inputBuffer.timeUs,
                outputBuffer, outputMode, reset, null);

        if (result == DECODE_ONLY) {
            outputBuffer.addFlag(C.BUFFER_FLAG_DECODE_ONLY);
        } else if (result != NO_ERROR) {
            return new WOKAOHEVCDecoderException("Decode error " + result);
        }

        //outputBuffer.colorInfo = inputBuffer.colorInfo;
        outputBuffer.mode = outputMode;
        return null;
    }

    @Override
    protected DecoderInputBuffer createInputBuffer() {
        return new DecoderInputBuffer(DecoderInputBuffer.BUFFER_REPLACEMENT_MODE_DIRECT);
    }

    @Override
    protected VideoDecoderOutputBuffer createOutputBuffer() {
        return new VideoDecoderOutputBufferX(this::releaseOutputBuffer);
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
    private native long hevcRenderFrame(long context, Surface surface,VideoDecoderOutputBuffer outputBuffer);

    //VideoDecoderOutputBuffer
    private native int hevcDecode(long context, ByteBuffer encoded, int length, long pts,
                                  VideoDecoderOutputBuffer outputBuffer, int outputMode, boolean flush, String outpath);

}
