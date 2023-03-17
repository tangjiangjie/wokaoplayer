package com.google.exoplayer2.ext.wokaohevc;

import androidx.annotation.Nullable;

import com.google.android.exoplayer2.decoder.DecoderException;

public class WOKAOHEVCDecoderException extends DecoderException {
    public WOKAOHEVCDecoderException(String message) {
        super(message);
    }


    public WOKAOHEVCDecoderException(String message, @Nullable Throwable cause) {
        super(message, cause);
    }
}
