package cn.laotang.dibudong;

import android.util.Log;

import androidx.annotation.Nullable;

import com.google.android.exoplayer2.decoder.DecoderException;

public class WOKAOHEVCDecoderException extends DecoderException {
    public WOKAOHEVCDecoderException(String message) {
        super(message);
        Log.d("wokaor", "WOKAOHEVCDecoderException:" );
    }


    public WOKAOHEVCDecoderException(String message, @Nullable Throwable cause) {
        super(message, cause);
        Log.d("wokaor", "WOKAOHEVCDecoderException:1" );
    }
}
