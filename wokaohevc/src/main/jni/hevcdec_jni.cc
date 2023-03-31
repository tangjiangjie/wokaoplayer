
#include <cpu-features.h>
#ifdef __ARM_NEON__
#include <arm_neon.h>
#endif
#include <jni.h>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <sys/stat.h>.
#include <pthread.h>

#include "libyuv.h"
#include "log.h"

#include "openhevcwrapper/openHevcWrapper.h"
#include <android/native_window.h>
#include <android/native_window_jni.h>
//Java_cn_laotang_dibudong_WOKAOHEVCDecoder_hevcInit

#define DECODER_FUNC(RETURN_TYPE, NAME, ...) \
  extern "C" { \
  JNIEXPORT RETURN_TYPE \
    Java_cn_laotang_dibudong_WOKAOHEVCDecoder_ ## NAME \
      (JNIEnv* env, jobject thiz, ##__VA_ARGS__);\
  } \
  JNIEXPORT RETURN_TYPE \
    Java_cn_laotang_dibudong_WOKAOHEVCDecoder_ ## NAME \
      (JNIEnv* env, jobject thiz, ##__VA_ARGS__)\

//Java_cn_laotang_dibudong_WOKAOHEVCLibrary

#define LIBRARY_FUNC(RETURN_TYPE, NAME, ...) \
  extern "C" { \
  JNIEXPORT RETURN_TYPE \
    Java_cn_laotang_dibudong_WOKAOHEVCLibrary_ ## NAME \
      (JNIEnv* env, jobject thiz, ##__VA_ARGS__);\
  } \
  JNIEXPORT RETURN_TYPE \
    Java_cn_laotang_dibudong_WOKAOHEVCLibrary_ ## NAME \
      (JNIEnv* env, jobject thiz, ##__VA_ARGS__)\


const char *hevc_codec_version_str(void) { return libOpenHevcVersion(NULL); }

static const char *const cfg = "-O1";

const char *hevc_codec_build_config(void) { return cfg; }

typedef struct Info {
    int NbFrame;
    int Poc;
    int Tid;
    int Qid;
    int type;
    int size;
} Info;

static Info info;



static int temporal_layer_id = 0;
static int quality_layer_id = 0;

typedef enum {
    YUV = 0,//要和HevcOutputBuffer中的mode的定义保持一致
    RGB
} OutputMode;
//
//typedef enum {
//    PIXFMT_UNKNOWN = 0, //要和HevcOutputBuffer中的PIXFMT保持一致
//    PIXFMT_RGB565,
//    PIXFMT_ARGB8888
//} OutputPixFmt;

#define  PIXFMT_UNKNOWN  0
#define  PIXFMT_RGB565  1
#define  PIXFMT_ARGB8888  2



LIBRARY_FUNC(jboolean, hevcIsSecureDecodeSupported) {
    // Doesn't support
    // Java client should have checked hevcIsSecureDecodeSupported
    // and avoid calling this
    // return -2 (DRM Error)
    return 0;
}

LIBRARY_FUNC(jstring, hevcGetVersion) {
    return env->NewStringUTF(hevc_codec_version_str());
}

LIBRARY_FUNC(jstring, hevcGetBuildConfig) {
    return env->NewStringUTF(hevc_codec_build_config());
}

static void convert_16_to_8_standard(const OpenHevc_Frame* const img,
                                     jbyte* const data, const int32_t uvHeight,
                                     const int32_t yLength,
                                     const int32_t uvLength) {
    // Y
    int sampleY = 0;
    for (int y = 0; y < img->frameInfo.nHeight; y++) {
        const uint16_t* srcBase = reinterpret_cast<uint16_t*>(
                img->pvY + img->frameInfo.nYPitch * y);
        int8_t* destBase = data + img->frameInfo.nYPitch * y;
        for (int x = 0; x < img->frameInfo.nWidth; x++) {
            // Lightweight dither. Carryover the remainder of each 10->8 bit
            // conversion to the next pixel.
            sampleY += *srcBase++;
            *destBase++ = sampleY >> 2;
            sampleY = sampleY & 3;  // Remainder.
        }
    }
    // UV
    int sampleU = 0;
    int sampleV = 0;
    const int32_t uvWidth = (img->frameInfo.nWidth + 1) / 2;
    for (int y = 0; y < uvHeight; y++) {
        const uint16_t* srcUBase = reinterpret_cast<uint16_t*>(
                img->pvU + img->frameInfo.nUPitch * y);
        const uint16_t* srcVBase = reinterpret_cast<uint16_t*>(
                img->pvV + img->frameInfo.nVPitch * y);
        int8_t* destUBase = data + yLength + img->frameInfo.nUPitch * y;
        int8_t* destVBase =
                data + yLength + uvLength + img->frameInfo.nVPitch * y;
        for (int x = 0; x < uvWidth; x++) {
            // Lightweight dither. Carryover the remainder of each 10->8 bit
            // conversion to the next pixel.
            sampleU += *srcUBase++;
            *destUBase++ = sampleU >> 2;
            sampleU = sampleU & 3;  // Remainder.
            sampleV += *srcVBase++;
            *destVBase++ = sampleV >> 2;
            sampleV = sampleV & 3;  // Remainder.
        }
    }
}

static const int FLAG_DECODE_ONLY = 1;
static const int DECODE_NO_ERROR = 0;
static const int DECODE_ERROR = -1;
static const int DECODE_DRM_ERROR = -2;
static const int DECODE_GET_FRAME_ERROR = -3;//把AVFrame的数据拷贝到OutputBuffer出错

// JNI references for HevcOutputBuffer class.



struct dbdframe{
    OpenHevc_Frame f;
    int id;
    const jbyte* pic;
};

struct dibudong {
    jfieldID decoder_private_field;
    jmethodID initForRgbFrame;
    jmethodID initForYuvFrame;
    jfieldID dataField;
    jfieldID timeUsField;
    dibudong(OpenHevc_Handle d):decoder(d) {
        buf_init();
    }
    ~dibudong() {
        if (native_window) {
            ANativeWindow_release(native_window);
        }
        buf_deinit();
    }
    void acquire_native_window(JNIEnv* env, jobject new_surface) {
        if (surface != new_surface) {
            if (native_window) {
                ANativeWindow_release(native_window);
            }
            native_window = ANativeWindow_fromSurface(env, new_surface);
            surface = new_surface;
            width = 0;
        }
    }
    OpenHevc_Handle decoder = NULL;
    ANativeWindow* native_window = NULL;
    jobject surface = NULL;
    int width = 0;
    int height = 0;

    //buffer manager
#define MAX_FRAMES      32
#define FRAME_BASEID    0X100000
    pthread_mutex_t mutex;
    void buf_init()
    {
        pthread_mutex_init(&mutex, NULL);
        memset(frame,0,sizeof(frame));
        bitmap32=0;
        curidx=0;
    }
    void buf_deinit()
    {
    }
    dbdframe* getframe(int id){
        return &frame[id-FRAME_BASEID];
    }
    dbdframe* getframe(){
        dbdframe* ret=0;
        pthread_mutex_lock(&mutex);
        if(bitmap32!=0xffffffff){
            while(true){
                if(frame[curidx].id==0){
                    frame[curidx].id=FRAME_BASEID+curidx;
                    ret=&frame[curidx];
                    bitmap32|=1<<curidx;
                    curidx++;
                    break;
                }
                curidx++;
                curidx%=MAX_FRAMES;
            }
        }
        pthread_mutex_unlock(&mutex);
        return ret;
    }
    void releaseframe(int id){
        releaseframe(&frame[id-FRAME_BASEID]);
    }

    void releaseframe(dbdframe* f){
        pthread_mutex_lock(&mutex);
        int idx=frame[curidx].id-FRAME_BASEID;
        bitmap32&=~(1<<idx);
        memset(f,0,sizeof(dbdframe));
        pthread_mutex_unlock(&mutex);
    }
    unsigned int bitmap32;
    int curidx;
    dbdframe frame[MAX_FRAMES];

};


DECODER_FUNC(jlong, hevcInit, jobject extraData, jint len) {
    OpenHevc_Handle ohevc = nullptr;
    dibudong* ctx=0;
    int mode = 1;
    int nb_pthreads = (android_getCpuCount() + 1) / 2;
    int layer_id = 0;

    uint8_t * data = nullptr;

    ohevc = libOpenHevcInit(nb_pthreads, mode);

    memset(&info, 0, sizeof(Info) );

    uint8_t arm_neon = 0;
#ifdef __ARM_NEON__
    arm_neon = 1;
#endif
    ALOGI("[%s] libOpenHevcInit thd=%d neon=%d", __func__, nb_pthreads, arm_neon);

    if (NULL == ohevc) {
        ALOGE("[%s] libOpenHevcInit err", __func__);
        goto LABEL_RETURN;
    }

    libOpenHevcSetCheckMD5(ohevc, 0);
    libOpenHevcSetDebugMode(ohevc, 0);

    data = reinterpret_cast<uint8_t *>(env->GetDirectBufferAddress(extraData));
    libOpenHevcCopyExtraData(ohevc, data, len);

    if (libOpenHevcStartDecoder(ohevc) < 0) {
        ALOGE("[%s] libOpenHevcStartDecoder err", __func__);
        goto LABEL_RETURN;
    }
    libOpenHevcSetTemporalLayer_id(ohevc, temporal_layer_id);
    libOpenHevcSetActiveDecoders(ohevc, quality_layer_id);
    libOpenHevcSetViewLayers(ohevc, quality_layer_id);
    ctx=new dibudong(ohevc);
    {
        // Populate JNI References.
        //com.google.android.exoplayer2.decoder
        const jclass outputBufferClass = env->FindClass(
                "cn/laotang/dibudong/VideoDecoderOutputBufferX");
                //"com/google/android/exoplayer2/decoder/VideoDecoderOutputBuffer");
        ctx->decoder_private_field =
                env->GetFieldID(outputBufferClass, "decoderPrivate", "I");
        ctx->initForYuvFrame = env->GetMethodID(outputBufferClass, "initForYuvFrame",
                                           "(IIIII)Z");
        ctx->initForRgbFrame = env->GetMethodID(outputBufferClass, "initForRgbFrame",
                                           "(III)I");
        ctx->dataField = env->GetFieldID(outputBufferClass, "data",
                                    "Ljava/nio/ByteBuffer;");
        ctx->timeUsField = env->GetFieldID(outputBufferClass, "timeUs", "J");
    }

LABEL_RETURN:
    return (jlong) ctx;
}

DECODER_FUNC(jlong, hevcClose, jlong jHandle) {
    dibudong* ctx=(dibudong*)jHandle;
    OpenHevc_Handle ohevc = ctx->decoder;
    libOpenHevcClose(ohevc);
    ALOGI("total frame decoded %d", info.NbFrame);
    delete ctx;
    return 0;
}


int getYUVFrame(JNIEnv* env,dibudong* ctx, jobject jOutputBuffer, OpenHevc_Frame& hevcFrame) {
    ALOGI("getYUVFrame %d", hevcFrame.frameInfo.chromat_format);
    // yuv
    const int kColorspaceUnknown = 0;
    const int kColorspaceBT601 = 1;
    const int kColorspaceBT709 = 2;
    const int kColorspaceBT2020 = 3;

    int colorspace = kColorspaceUnknown;
    switch (hevcFrame.frameInfo.colorspace) {
        case AVCOL_SPC_BT470BG:
        case AVCOL_SPC_SMPTE170M:
            colorspace = kColorspaceBT601;
            break;
        case AVCOL_SPC_BT709:
            colorspace = kColorspaceBT709;
            break;
        case AVCOL_SPC_BT2020_NCL:
        case AVCOL_SPC_BT2020_CL:
            colorspace = kColorspaceBT2020;
            break;
        default:
            break;
    }

    // resize buffer if required.
    jboolean initResult = env->CallBooleanMethod(
            jOutputBuffer, ctx->initForYuvFrame, hevcFrame.frameInfo.nWidth, hevcFrame.frameInfo.nHeight,
            hevcFrame.frameInfo.nYPitch, hevcFrame.frameInfo.nUPitch, colorspace);
    if (env->ExceptionCheck() || !initResult) {
        ALOGE("ERROR: %s yuvmode failed", __func__);
        return DECODE_GET_FRAME_ERROR;
    }
    // get pointer to the data buffer.
    const jobject dataObject = env->GetObjectField(jOutputBuffer, ctx->dataField);
    jbyte* const data =
            reinterpret_cast<jbyte*>(env->GetDirectBufferAddress(dataObject));

    const int32_t uvHeight = (hevcFrame.frameInfo.nHeight + 1) / 2;
    const uint64_t yLength = hevcFrame.frameInfo.nYPitch * hevcFrame.frameInfo.nHeight;
    const uint64_t uvLength = hevcFrame.frameInfo.nUPitch * uvHeight;

    if (hevcFrame.frameInfo.chromat_format == YUV420) {  // HBD planar 420.
        // Note: The stride for BT2020 is twice of what we use so this is wasting
        // memory. The long term goal however is to upload half-float/short so
        // it's not important to optimize the stride at this time.
        int converted = 0;
#ifdef __ARM_NEON__
       // converted = convert_16_to_8_neon(&hevcFrame, data, uvHeight, yLength, uvLength);
#endif  // __ARM_NEON__
        if (!converted) {
            convert_16_to_8_standard(&hevcFrame, data, uvHeight, yLength, uvLength);
        }
    } else {
        // todo to support other formats
        ALOGW("WARN: %s unrecognized pixfmt %d", __func__ ,hevcFrame.frameInfo.chromat_format);
//        memcpy(data, hevcFrame.pvY, yLength);
//        memcpy(data + yLength, hevcFrame.pvU, uvLength);
//        memcpy(data + yLength + uvLength, hevcFrame.pvV, uvLength);
        return DECODE_GET_FRAME_ERROR;
    }

    return DECODE_NO_ERROR;
}

void saveFrame(JNIEnv* env, jbyte* data,OpenHevc_Frame& hevcFrame,uint64_t ylen,uint64_t uvlen) {
    FILE* outFile = nullptr;
    const char* path = "/data/data/cn.laotang.wokaoplayer/files";
    struct stat st;
    int res = stat(path, &st);
    if(0 == res && (st.st_mode & S_IFDIR)) {
        char outFilePath[512];
        sprintf(outFilePath, "%s/rvideo_%dx%d.yuv", path, hevcFrame.frameInfo.nWidth, hevcFrame.frameInfo.nHeight);
        outFile = fopen(outFilePath, "ab+");
        ALOGV("save yuv to %s", outFilePath);

        // write Y
        int format = hevcFrame.frameInfo.chromat_format == YUV420 ? 1 : 0;
        int pixlen=1;
        for (int h = 0; h < hevcFrame.frameInfo.nHeight; h++) {
            const jbyte* srcBase1 = data + hevcFrame.frameInfo.nYPitch * h;
            fwrite(srcBase1, pixlen, hevcFrame.frameInfo.nWidth, outFile);
        }
        // write UV
        int uvLineSize = hevcFrame.frameInfo.nWidth >> format;
        for (int h = 0; h < hevcFrame.frameInfo.nHeight / 2; h++) {
            const jbyte* srcBase2 = data +ylen+ hevcFrame.frameInfo.nUPitch * h;
            fwrite(srcBase2, pixlen , uvLineSize, outFile);
        }
        for (int h = 0; h < hevcFrame.frameInfo.nHeight / 2 ; h++) {
            const jbyte* srcBase3 = data+ylen +uvlen+ hevcFrame.frameInfo.nVPitch * h;
            fwrite(srcBase3, pixlen , uvLineSize, outFile);
        }
        fflush(outFile);
        fclose(outFile);
    } else {
        ALOGW("WARN: %s is not a valid dir", path);
    }
}


void saveFrame(JNIEnv* env, jstring jStr, OpenHevc_Frame& hevcFrame) {
    FILE* outFile = nullptr;
    const char* path = env->GetStringUTFChars(jStr, nullptr);
    struct stat st;
    int res = stat(path, &st);
    if(0 == res && (st.st_mode & S_IFDIR)) {
        char outFilePath[512];
        sprintf(outFilePath, "%s/video_%dx%d.yuv", path, hevcFrame.frameInfo.nWidth, hevcFrame.frameInfo.nHeight);
        outFile = fopen(outFilePath, "ab+");
        ALOGV("save yuv to %s", outFilePath);

        // write Y
        int format = hevcFrame.frameInfo.chromat_format == YUV420 ? 1 : 0;
        int pixlen=(hevcFrame.frameInfo.nBitDepth+7)/8;
        for (int h = 0; h < hevcFrame.frameInfo.nHeight; h++) {
            const uint8_t* srcBase1 = hevcFrame.pvY + hevcFrame.frameInfo.nYPitch * h;
            fwrite(srcBase1, pixlen, hevcFrame.frameInfo.nWidth, outFile);
        }
        // write UV
        int uvLineSize = hevcFrame.frameInfo.nWidth >> format;
        for (int h = 0; h < hevcFrame.frameInfo.nHeight / 2; h++) {
            const uint8_t* srcBase2 = hevcFrame.pvU + hevcFrame.frameInfo.nUPitch * h;
            fwrite(srcBase2, pixlen , uvLineSize, outFile);
        }
        for (int h = 0; h < hevcFrame.frameInfo.nHeight / 2 ; h++) {
            const uint8_t* srcBase3 = hevcFrame.pvV + hevcFrame.frameInfo.nVPitch * h;
            fwrite(srcBase3, pixlen , uvLineSize, outFile);
        }
        fflush(outFile);
        fclose(outFile);
    } else {
        ALOGW("WARN: %s is not a valid dir", path);
    }
}
void dbgframeinfo(OpenHevc_FrameInfo f)
{
    /*
    int         nYPitch;
    int         nUPitch;
    int         nVPitch;
    int         nBitDepth;
    int         nWidth;
    int         nHeight;
    int        chromat_format;
    OpenHevc_Rational  sample_aspect_ratio;
    OpenHevc_Rational  frameRate;
    int         display_picture_number;
    int         flag; //progressive, interlaced, interlaced top field first, interlaced bottom field first.
    int64_t     nTimeStamp;
    enum AVColorSpace colorspace;
    int64_t     pts;
     */

    ALOGE("frame wxh= %d x %d fmt=%d", f.nWidth,f.nHeight,f.chromat_format);
    ALOGE("frame nBitDepth %d ", f.nBitDepth);
    ALOGE("frame frameRate %d %d", f.frameRate.den,f.frameRate.num);
    ALOGE("frame yuvPitch %d %d %d", f.nYPitch,f.nUPitch,f.nVPitch);
    ALOGE("frame nTimeStamp %lld %lld", f.nTimeStamp,f.pts);
}

DECODER_FUNC(jint, hevcDecode, jlong jHandle, jobject encoded, jint len, int64_t pts,
             jobject jOutputBuffer, jint outputMode, jint flush, jstring jStr) {
    dibudong* ctx=(dibudong*)jHandle;
    OpenHevc_Handle ohevc = ctx->decoder;
    const uint8_t *const buffer =
            reinterpret_cast<const uint8_t *>(env->GetDirectBufferAddress(encoded));
    int got_pic;


    //Zero if no frame could be decompressed, otherwise, it is nonzero.
    //-1 indicates error occurred
    got_pic = libOpenHevcDecode(ohevc, buffer, len, pts, flush);
    ALOGD("DEBUG: %d frame decoded got_pic %d ", info.NbFrame, got_pic);
    info.NbFrame++;
    if (got_pic < 0) {
        ALOGE("ERROR: hevcDecode failed, status= %d", got_pic);
        return DECODE_ERROR;
    }

    if(got_pic == 0) {
        ALOGD("[%s] decode only frame", __func__);
        return FLAG_DECODE_ONLY;
    }
    dbdframe* dbd=ctx->getframe();
    memset(&dbd->f, 0, sizeof(OpenHevc_Frame));
    //libOpenHevcGetPictureInfo(ohevc, &hevcFrame.frameInfo);
    got_pic = libOpenHevcGetOutput(ohevc, 1, &dbd->f);
    if(got_pic < 0) {
        ALOGE("ERROR: %s failed getoutput", __func__);
        ctx->releaseframe(dbd);
        return DECODE_GET_FRAME_ERROR; // indicate error
    }
    dbgframeinfo(dbd->f.frameInfo);

    //设置OutputBuffer的pts
    env->SetLongField(jOutputBuffer, ctx->timeUsField, dbd->f.frameInfo.pts);
    env->SetIntField(jOutputBuffer, ctx->decoder_private_field, dbd->id);
    got_pic = getYUVFrame(env, ctx,jOutputBuffer, dbd->f);

    const jobject dataObject = env->GetObjectField(jOutputBuffer, ctx->dataField);
    //jbyte* const data =
     dbd->pic=reinterpret_cast<jbyte*>(env->GetDirectBufferAddress(dataObject));

    //for debug, to save frame to file
    if(jStr != nullptr) {
       saveFrame(env, jStr, dbd->f);
    }
    return got_pic;
}
// Android YUV format. See:
// https://developer.android.com/reference/android/graphics/ImageFormat.html#YV12.
//YV12 is a 4:2:0 YCrCb planar format comprised of a WxH Y plane followed by (W/2) x (H/2) Cr and Cb planes.
static const int FormatYV12 = 0x32315659;

DECODER_FUNC(void, hevcReleaseFrame, jlong jHandle, jobject jOutputBuffer) {
    dibudong* ctx=(dibudong*)jHandle;
    const int id = env->GetIntField(jOutputBuffer, ctx->decoder_private_field);
    env->SetIntField(jOutputBuffer, ctx->decoder_private_field, -1);
    ctx->releaseframe(id);
}

DECODER_FUNC(jint, hevcRenderFrame, jlong jHandle, jobject jSurface,
             jobject jOutputBuffer) {

    dibudong* ctx=(dibudong*)jHandle;
    const int id = env->GetIntField(jOutputBuffer, ctx->decoder_private_field);
    dbdframe* dbdf=ctx->getframe(id);

    ctx->acquire_native_window(env, jSurface);

    if (ctx->native_window == NULL) {
        return 1;
    }

    if (ctx->width != dbdf->f.frameInfo.nWidth || ctx->height != dbdf->f.frameInfo.nHeight) {
        ANativeWindow_setBuffersGeometry(ctx->native_window, dbdf->f.frameInfo.nWidth,
                                         dbdf->f.frameInfo.nHeight, FormatYV12);
        ctx->width = dbdf->f.frameInfo.nWidth;
        ctx->height = dbdf->f.frameInfo.nHeight;
    }

    ANativeWindow_Buffer buffer;
    int result = ANativeWindow_lock(ctx->native_window, &buffer, NULL);
    if (buffer.bits == NULL || result) {
        return -1;
    }
    const jobject dataObject = env->GetObjectField(jOutputBuffer, ctx->dataField);
    jbyte* const data =reinterpret_cast<jbyte*>(env->GetDirectBufferAddress(dataObject));

    ALOGD("hevcRenderFrame %p %p",data,dbdf->pic);

    // Y
    const size_t src_y_stride = dbdf->f.frameInfo.nYPitch;
    int stride = dbdf->f.frameInfo.nWidth;
    const uint8_t* src_base =
            reinterpret_cast<uint8_t*>(data);
    uint8_t* dest_base = (uint8_t*)buffer.bits;
    for (int y = 0; y < dbdf->f.frameInfo.nHeight; y++) {
        memcpy(dest_base, src_base, stride);
        src_base += src_y_stride;
        dest_base += buffer.stride;
    }
    const int32_t uvHeight = (dbdf->f.frameInfo.nHeight + 1) / 2;
    const uint64_t yLength = dbdf->f.frameInfo.nYPitch * dbdf->f.frameInfo.nHeight;
    const uint64_t uvLength = dbdf->f.frameInfo.nUPitch * uvHeight;

   // saveFrame(env,data,dbdf->f,yLength,uvLength);

    // UV
    const int src_uv_stride = dbdf->f.frameInfo.nUPitch;
    const int dest_uv_stride = (buffer.stride / 2 + 15) & (~15);
    const int32_t buffer_uv_height = (buffer.height + 1) / 2;
    const int32_t height =
            fmin((int32_t)(dbdf->f.frameInfo.nHeight + 1) / 2, buffer_uv_height);
    stride = (dbdf->f.frameInfo.nWidth + 1) / 2;
    src_base = reinterpret_cast<uint8_t*>(data + yLength);
    const uint8_t* src_v_base =
            reinterpret_cast<uint8_t*>(data + yLength+uvLength);
    uint8_t* dest_v_base =
            ((uint8_t*)buffer.bits) + buffer.stride * buffer.height;
    dest_base = dest_v_base + buffer_uv_height * dest_uv_stride;
    for (int y = 0; y < height; y++) {
        memcpy(dest_base, src_base, stride);
        memcpy(dest_v_base, src_v_base, stride);
        src_base += src_uv_stride;
        src_v_base += src_uv_stride;
        dest_base += dest_uv_stride;
        dest_v_base += dest_uv_stride;
    }

    return ANativeWindow_unlockAndPost(ctx->native_window);
}
