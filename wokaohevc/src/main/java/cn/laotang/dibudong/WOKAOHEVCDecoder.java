package cn.laotang.dibudong;

import android.util.Log;

import androidx.annotation.Nullable;

import com.google.android.exoplayer2.decoder.DecoderInputBuffer;
import com.google.android.exoplayer2.decoder.SimpleDecoder;
import com.google.android.exoplayer2.decoder.VideoDecoderOutputBuffer;

public class WOKAOHEVCDecoder extends SimpleDecoder<DecoderInputBuffer, VideoDecoderOutputBuffer, WOKAOHEVCDecoderException> {
    /**
     * @param inputBuffers  An array of nulls that will be used to store references to input buffers.
     * @param outputBuffers An array of nulls that will be used to store references to output buffers.
     */
    protected WOKAOHEVCDecoder(DecoderInputBuffer[] inputBuffers, VideoDecoderOutputBuffer[] outputBuffers) {
        super(inputBuffers, outputBuffers);
        Log.d("wokaor", "WOKAOHEVCDecoder:" );
        //todo create decode handle
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
    protected WOKAOHEVCDecoderException createUnexpectedDecodeException(Throwable error) {
        return new WOKAOHEVCDecoderException("Unexpected decode error", error);
    }


    @Override
    public String getName() {
        return "libWOKAOHEVC";
    }
}
