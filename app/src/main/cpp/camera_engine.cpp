/**
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/** Description
 *   Demonstrate NDK Camera interface added to android-24
 */

#include "camera_engine.h"

#include <cstdio>
#include <cstring>

//#include "image_reader.h"
#include "native_debug.h"
#include "logging_macros.h"

CameraAppEngine::CameraAppEngine(JNIEnv* env, jobject instance, jint w, jint h)
    : env_(env),
      javaInstance_(instance),
      requestWidth_(w),
      requestHeight_(h),
      surface_(nullptr),
      camera_(nullptr) {
  IN
  memset(&compatibleCameraRes_, 0, sizeof(compatibleCameraRes_));
  camera_ = new NDKCamera();
  ASSERT(camera_, "Failed to Create CameraObject");
  camera_->MatchCaptureSizeRequest(requestWidth_, requestHeight_,
                                   &compatibleCameraRes_);
  imageReader = new ImageReader(&compatibleCameraRes_, AIMAGE_FORMAT_YUV_420_888);
  camera_ -> videoRecordWindow = imageReader -> GetNativeWindow();
  if (camera_ -> videoRecordWindow == nullptr) {
    HERE LOGE("video record  native window nulL!\n");
  }
  OUT
}

CameraAppEngine::~CameraAppEngine() {
  if (camera_) {
    delete camera_;
    camera_ = nullptr;
  }

  if (surface_) {
    env_->DeleteGlobalRef(surface_);
    surface_ = nullptr;
  }
}

/**
 * Create a capture session with given Java Surface Object
 * @param surface a {@link Surface} object.
 */
void CameraAppEngine::CreateCameraSession(jobject surface, ANativeWindow * window) {
  if (surface == nullptr) {
    camera_->CreateSessionVideoCapture(imageReader->GetNativeWindow ());
  } else {
    surface_ = env_->NewGlobalRef(surface);
//    camera_->CreateSession(nullptr);
    camera_->CreateSession(ANativeWindow_fromSurface(env_, surface), nullptr, window, false, 0);
  }
}

/**
 * @return cached {@link Surface} object
 */
jobject CameraAppEngine::GetSurfaceObject() { return surface_; }

/**
 *
 * @return saved camera preview resolution for this session
 */
const ImageFormat& CameraAppEngine::GetCompatibleCameraRes() const {
  return compatibleCameraRes_;
}

int CameraAppEngine::GetCameraSensorOrientation(int32_t requestFacing) {
  ASSERT(requestFacing == ACAMERA_LENS_FACING_BACK,
         "Only support rear facing camera");
  int32_t facing = 0, angle = 0;
  if (camera_->GetSensorOrientation(&facing, &angle) ||
      facing == requestFacing) {
    return angle;
  }
  ASSERT(false, "Failed for GetSensorOrientation()");
  return 0;
}

void CameraAppEngine::createEncoder (std::string _filename) {
    IN
    LOGD("creating encoder with filename %s [%d x %d] %d fps", _filename.c_str (), requestWidth_, requestHeight_, mFPS);
  filename = std::string (_filename) ;
  format = AMediaFormat_new() ;
  AMediaFormat_setInt32(format,AMEDIAFORMAT_KEY_WIDTH,1920);
  AMediaFormat_setInt32(format,AMEDIAFORMAT_KEY_HEIGHT,1080);

  AMediaFormat_setString(format,AMEDIAFORMAT_KEY_MIME,"video/avc"); // H.264 Advanced Video Coding
//  AMediaFormat_setInt32(format, AMEDIAFORMAT_KEY_COLOR_FORMAT, 21); // #21 COLOR_FormatYUV420SemiPlanar (NV12)
//    AMediaFormat_setInt32(format, AMEDIAFORMAT_KEY_COLOR_FORMAT, 0x7f420888 /*COLOR_FormatYUV420Flexible*/);
    AMediaFormat_setInt32(format, AMEDIAFORMAT_KEY_COLOR_FORMAT, 19);

    AMediaFormat_setInt32(format,AMEDIAFORMAT_KEY_BIT_RATE,500000);
  AMediaFormat_setFloat(format,AMEDIAFORMAT_KEY_FRAME_RATE,30);
  AMediaFormat_setInt32(format,AMEDIAFORMAT_KEY_I_FRAME_INTERVAL,1);

  mEncoder = AMediaCodec_createEncoderByType("video/avc");
  if(mEncoder == nullptr){
    LOGE("Unable to create encoder");
  }

  bool exitStatus = true;
  media_status_t err = AMediaCodec_configure(mEncoder, format, NULL, NULL, AMEDIACODEC_CONFIGURE_FLAG_ENCODE);
  if(err != AMEDIA_OK){
      exitStatus = false;
    LOGE( "Error occurred creating encoder: %d", err );
  } else {
      LOGD("Encoder created [ok]");
  }

  err = AMediaCodec_start(mEncoder);
  if(err != AMEDIA_OK){
      exitStatus = false;
    LOGE( "Error occurred: %d", err);
  } else {
      LOGD("encoder started [ok]");
  }

  fd = fileno (fopen (filename.c_str(), "w"));
  mMuxer = AMediaMuxer_new(fd, AMEDIAMUXER_OUTPUT_FORMAT_MPEG_4);

  if(mMuxer == nullptr){
      exitStatus = false;
    LOGE("Unable to create Muxer");
  } else {
      LOGD("Muxer created [ok]");
  }

  mTrackIndex = -1;
    AMediaFormat* newFormat = AMediaCodec_getOutputFormat(mEncoder);

    if(newFormat == nullptr){
        LOGE ("Unable to set new format.");
    }

    LOGD( "encoder output format changed: %s",  AMediaFormat_toString(newFormat));

    // now that we have the Magic Goodies, start the muxer
    mTrackIndex = AMediaMuxer_addTrack(mMuxer, newFormat);
    err = AMediaMuxer_start(mMuxer);

    if(err != AMEDIA_OK){
        LOGE( "Muxer cannot be started: %s" , err);
    }

    mMuxerStarted = true;
  mFrameCounter = 0;
  isRunning = true;
  LOGD ("Encoder ready!");
  if (! exitStatus) LOGF("[danger] something has gone wrong!");
  imageReader -> mediaMuxer = mMuxer ;
  imageReader -> mediaCodec = mEncoder ;
  createDecoder();
  OUT
}


int64_t CameraAppEngine::frame2Time(int64_t frameNo){
  return (frameNo*1000000)/mFPS;
}

long long CameraAppEngine::computePresentationTimeNsec() {
  mFrameCounter++;
  double timePerFrame = 1000000.0/mFPS;
  return static_cast<long long>(mFrameCounter*timePerFrame);
}


void CameraAppEngine::writeEnd(){
  LOGD("End of recording called!");
  // Send the termination frame
  ssize_t inBufferIdx = AMediaCodec_dequeueInputBuffer(mEncoder, TIMEOUT_USEC);
  size_t out_size;
  uint8_t* inBuffer = AMediaCodec_getInputBuffer(mEncoder, inBufferIdx, &out_size);
  int64_t presentationTimeNs = computePresentationTimeNsec();
  LOGD( "Sending EOS");
  media_status_t status = AMediaCodec_queueInputBuffer(mEncoder, inBufferIdx, 0, out_size, presentationTimeNs, AMEDIACODEC_BUFFER_FLAG_END_OF_STREAM);
  // send end-of-stream to encoder, and drain remaining output

  drainEncoder(true);

  releaseEncoder();

  // To test the result, open the file with MediaExtractor, and get the format.  Pass
  // that into the MediaCodec decoder configuration, along with a SurfaceTexture surface,
  // and examine the output with glReadPixels.
}

void CameraAppEngine::releaseEncoder() {
  LOGW( "releasing encoder objects");
  if (mEncoder != nullptr) {
    AMediaCodec_stop(mEncoder);
  }

  if (mMuxer != nullptr) {
    AMediaMuxer_stop(mMuxer);
  }

  if (mEncoder != nullptr) {
    AMediaCodec_delete(mEncoder);
    mEncoder = nullptr;
  }

  if (mMuxer != nullptr) {
    AMediaMuxer_delete(mMuxer);
    mMuxer = nullptr;
  }

  isRunning = false;
  LOGD("recording finished");
}

bool CameraAppEngine::writeFrame(AImage * image){
  // Feed any pending encoder output into the muxer.
  IN
//  if (mEncoder == nullptr) {
//      LOGD("first time running, trying to create encoder ...");
//      createEncoder(filename);
//  }

  drainEncoder(false);

 // Generate a new frame of input.

  /**
                * Get the index of the next available input buffer. An app will typically use this with
                * getInputBuffer() to get a pointer to the buffer, then copy the data to be encoded or decoded
                * into the buffer before passing it to the codec.
                */
    LOGD("dequeue buffer ...");
  ssize_t inBufferIdx = AMediaCodec_dequeueInputBuffer(mEncoder, TIMEOUT_USEC);

  /**
                * Get an input buffer. The specified buffer index must have been previously obtained from
                * dequeueInputBuffer, and not yet queued.
                */
  size_t out_size;
  uint8_t* inBuffer = AMediaCodec_getInputBuffer(mEncoder, inBufferIdx, &out_size);
    int dataSize = 0;
    AImage_getPlaneData(image, 0, &inBuffer, &dataSize);

    int64_t timestamp = 0;
    AImage_getTimestamp(image, &timestamp);

  // here we actually copy the data.
//  memcpy(inBuffer, data, out_size);

  /**
        * Send the specified buffer to the codec for processing.
        */
  //int64_t presentationTimeNs = timestamp;
  int64_t presentationTimeNs = computePresentationTimeNsec();

  media_status_t status = AMediaCodec_queueInputBuffer(mEncoder, inBufferIdx, 0, timestamp, presentationTimeNs, 0);

  if(status == AMEDIA_OK){
    //qDebug() << "Successfully pushed frame to input buffer";
  }
  else{
    LOGW("Something went wrong while pushing frame to input buffer: %d", status);
    OUT
    return false;
  }

  // Submit it to the encoder.  The eglSwapBuffers call will block if the input
  // is full, which would be bad if it stayed full until we dequeued an output
  // buffer (which we can't do, since we're stuck here).  So long as we fully drain
  // the encoder before supplying additional input, the system guarantees that we
  // can supply another frame without blocking.
  //qDebug() << "sending frame " << i << " to encoder";
  //AMediaCodec_flush(mEncoder);
    drainEncoder(false);
    AMediaCodec_flush(mEncoder);
  OUT
  return true;
}

void CameraAppEngine::drainEncoder(bool endOfStream) {
    IN
    LOGD("end of stream: %d", endOfStream);
  if (endOfStream) {
    LOGD( "Draining encoder to EOS");
    // only API >= 26
    // Send an empty frame with the end-of-stream flag set.
    // AMediaCodec_signalEndOfInputStream();
    // Instead, we construct that frame manually.
  }




  while (true) {
      LOGD("dequeue output buffer...");
      if (mEncoder != nullptr) LOGE("mEncoder is NULL");

    ssize_t encoderStatus = AMediaCodec_dequeueOutputBuffer(mEncoder, &mBufferInfo, TIMEOUT_USEC);
      LOGD("encoder status: %d", encoderStatus);

    if (encoderStatus == AMEDIACODEC_INFO_TRY_AGAIN_LATER) {
      // no output available yet
      if (!endOfStream) {
          OUT
        return;
        //break;      // out of while
      }
      if(endOfStream){
        LOGD("no output available, spinning to await EOS");
        OUT
        return;
      }

    } else if (encoderStatus == AMEDIACODEC_INFO_OUTPUT_BUFFERS_CHANGED) {
      // not expected for an encoder
    } else if (encoderStatus == AMEDIACODEC_INFO_OUTPUT_FORMAT_CHANGED) {
      // should happen before receiving buffers, and should only happen once
      if (mMuxerStarted) {
        LOGW( "ERROR: format changed twice");
      }

      AMediaFormat* newFormat = AMediaCodec_getOutputFormat(mEncoder);

      if(newFormat == nullptr){
        LOGW( "Unable to set new format.");
      }

      LOGW( "%s", std::string ("encoder output format changed: " + std::string (AMediaFormat_toString(newFormat))).c_str());

      // now that we have the Magic Goodies, start the muxer
      mTrackIndex = AMediaMuxer_addTrack(mMuxer, newFormat);
      media_status_t err = AMediaMuxer_start(mMuxer);

      if(err != AMEDIA_OK){
        LOGW( "Error occurred: %d", err );
      }

      mMuxerStarted = true;
    } else if (encoderStatus < 0) {
      LOGW( "unexpected result from encoder.dequeueOutputBuffer: %d", encoderStatus);
      // let's ignore it
    } else {

      size_t out_size;
      uint8_t* encodedData = AMediaCodec_getOutputBuffer(mEncoder, encoderStatus, &out_size);

      if(out_size <= 0){
        LOGW( "Encoded data of size 0.");
      }

      if (encodedData == nullptr) {
        LOGW("encoderOutputBuffer was null");
      }


      if ((mBufferInfo.flags & AMEDIACODEC_BUFFER_FLAG_CODEC_CONFIG) != 0) {
        // The codec config data was pulled out and fed to the muxer when we got
        // the INFO_OUTPUT_FORMAT_CHANGED status.  Ignore it.
        LOGW( "ignoring BUFFER_FLAG_CODEC_CONFIG");
        mBufferInfo.size = 0;
      }

      if (mBufferInfo.size != 0) {
        if (!mMuxerStarted) {
          LOGW ( "muxer hasn't started");
        }


        // adjust the ByteBuffer values to match BufferInfo (not needed?)
        //encodedData.position(mBufferInfo.offset);
        //encodedData.limit(mBufferInfo.offset + mBufferInfo.size);

        AMediaMuxer_writeSampleData(mMuxer, mTrackIndex, encodedData, &mBufferInfo);
        //qDebug() << "sent " + QString::number(mBufferInfo.size) + " bytes to muxer";
      }
      else{
        LOGW( "mBufferInfo empty %d" , mBufferInfo.size);
      }

      AMediaCodec_releaseOutputBuffer(mEncoder, encoderStatus, false);

      if ((mBufferInfo.flags & AMEDIACODEC_BUFFER_FLAG_END_OF_STREAM) != 0) {
        if (!endOfStream) {
          LOGW( "reached end of stream unexpectedly");
        } else {
          LOGD( "end of stream reached");

        }
        break;      // out of while
      }
    }
  }

  OUT
}

void CameraAppEngine::test () {
    IN
    camera_ -> TakePhoto();
//    AImage * i = imageReader -> GetLatestImage() ;
//    imageReader ->WriteFile(i);
    OUT
}

/**
 *
 * @param start is true to start preview, false to stop preview
 * @return  true if preview started, false when error happened
 */
void CameraAppEngine::StartPreview(bool start) { camera_->StartPreview(start); }

/**
 * MAX_BUF_COUNT:
 *   Max buffers in this ImageReader.
 */
#define MAX_BUF_COUNT 4

/**
 * ImageReader listener: called by AImageReader for every frame captured
 * We pass the event to ImageReader class, so it could do some housekeeping
 * about
 * the loaded queue. For example, we could keep a counter to track how many
 * buffers are full and idle in the queue. If camera almost has no buffer to
 * capture
 * we could release ( skip ) some frames by AImageReader_getNextImage() and
 * AImageReader_delete().
 */
void OnImageCallback(void *ctx, AImageReader *reader) {
    reinterpret_cast<ImageReader *>(ctx)->ImageCallback(reader);
}

/**
 * Constructor
 */
ImageReader::ImageReader(ImageFormat *res, enum AIMAGE_FORMATS format)
        : presentRotation_(0), reader_(nullptr) {
    callback_ = nullptr;
    callbackCtx_ = nullptr;

    media_status_t status = AImageReader_new(res->width, res->height, format,
                                             MAX_BUF_COUNT, &reader_);
    ASSERT(reader_ && status == AMEDIA_OK, "Failed to create AImageReader");

    AImageReader_ImageListener listener{
            .context = this,
            .onImageAvailable = OnImageCallback,
    };
    AImageReader_setImageListener(reader_, &listener);
}

ImageReader::~ImageReader() {
    ASSERT(reader_, "NULL Pointer to %s", __FUNCTION__);
    AImageReader_delete(reader_);
}

void ImageReader::RegisterCallback(
        void *ctx, std::function<void(void *ctx, const char *fileName)> func) {
    callbackCtx_ = ctx;
    callback_ = func;
}

void ImageReader::ImageCallback(AImageReader *reader) {
    IN
    int32_t format;
    media_status_t status = AImageReader_getFormat(reader, &format);
    ASSERT(status == AMEDIA_OK, "Failed to get the media format");
//  if (format == AIMAGE_FORMAT_JPEG)
//  {
    AImage *image = nullptr;
    status = AImageReader_acquireNextImage(reader, &image);
    ASSERT(status == AMEDIA_OK && image, "Image is not available");
    CameraAppEngine * _app = (CameraAppEngine *) app ;
    _app -> writeFrame (image);
    /*
    size_t bufferSize = 0;
    int inputBufferIdx = AMediaCodec_dequeueInputBuffer(mediaCodec, -1);
    uint8_t *inputBuffer = AMediaCodec_getInputBuffer(mediaCodec, inputBufferIdx, &bufferSize);

    if (inputBufferIdx >= 0) {
      LOGI("dequeue input buffer (size: %u)", bufferSize);
      int dataSize = 0;
      AImage_getPlaneData(image, 0, &inputBuffer, &dataSize);
      LOGI("copying image buffer (size: %d)", dataSize);

      int64_t timestamp = 0;
      AImage_getTimestamp(image, &timestamp);

      AMediaCodec_queueInputBuffer(mediaCodec, inputBufferIdx, 0, bufferSize, timestamp, 0);
    }

    AMediaCodecBufferInfo bufferInfo;
    int idx = AMediaCodec_dequeueOutputBuffer(mediaCodec, &bufferInfo, -1);

    LOGI("dequeue output buffer idx: %d", idx);

    if (idx == AMEDIACODEC_INFO_OUTPUT_FORMAT_CHANGED) {
      AMediaFormat *format = AMediaCodec_getOutputFormat(mediaCodec);

      tidx = AMediaMuxer_addTrack(mediaMuxer, format);
      LOGI("added track tidx: %d (format: %s)", tidx, AMediaFormat_toString(format));

      AMediaMuxer_start(mediaMuxer);
      AMediaFormat_delete(format);

    } else if (idx >= 0) {

      uint8_t *outputBuffer = AMediaCodec_getOutputBuffer(mediaCodec, idx, &bufferSize);

      if (tidx >= 0 && bufferInfo.size > 0) {
        pthread_mutex_lock(&lock);

        LOGI("sending buffer to tidx: %d ptr: %p (info->offset: %d, info->size: %d)", tidx, outputBuffer, bufferInfo.offset, bufferInfo.size);
        AMediaMuxer_writeSampleData(mediaMuxer, tidx, outputBuffer, &bufferInfo);

        pthread_mutex_unlock(&lock);
      }

      AMediaCodec_releaseOutputBuffer(mediaCodec, idx, false);
    }
     */

    // Create a thread and write out the jpeg files
//    std::thread writeFileHandler(&ImageReader::WriteFile, this, image);
//    writeFileHandler.detach();
//  }
//  else
//  {
//    LOGW("format %04x", format);
//  }

    HERE
    OUT
}

ANativeWindow *ImageReader::GetNativeWindow(void) {
    if (!reader_) return nullptr;
    ANativeWindow *nativeWindow;
    media_status_t status = AImageReader_getWindow(reader_, &nativeWindow);
    ASSERT(status == AMEDIA_OK, "Could not get ANativeWindow");

    return nativeWindow;
}

/**
 * GetNextImage()
 *   Retrieve the next image in ImageReader's bufferQueue, NOT the last image so
 * no image is skipped. Recommended for batch/background processing.
 */
AImage *ImageReader::GetNextImage(void) {
    AImage *image;
    media_status_t status = AImageReader_acquireNextImage(reader_, &image);
    if (status != AMEDIA_OK) {
        return nullptr;
    }
    return image;
}

/**
 * GetLatestImage()
 *   Retrieve the last image in ImageReader's bufferQueue, deleting images in
 * in front of it on the queue. Recommended for real-time processing.
 */
AImage *ImageReader::GetLatestImage(void) {
    AImage *image;
    media_status_t status = AImageReader_acquireLatestImage(reader_, &image);
    if (status != AMEDIA_OK) {
        return nullptr;
    }
    return image;
}

/**
 * Delete Image
 * @param image {@link AImage} instance to be deleted
 */
void ImageReader::DeleteImage(AImage *image) {
    if (image) AImage_delete(image);
}

/**
 * Helper function for YUV_420 to RGB conversion. Courtesy of Tensorflow
 * ImageClassifier Sample:
 * https://github.com/tensorflow/tensorflow/blob/master/tensorflow/examples/android/jni/yuv2rgb.cc
 * The difference is that here we have to swap UV plane when calling it.
 */
#ifndef MAX
#define MAX(a, b)           \
  ({                        \
    __typeof__(a) _a = (a); \
    __typeof__(b) _b = (b); \
    _a > _b ? _a : _b;      \
  })
#define MIN(a, b)           \
  ({                        \
    __typeof__(a) _a = (a); \
    __typeof__(b) _b = (b); \
    _a < _b ? _a : _b;      \
  })
#endif

// This value is 2 ^ 18 - 1, and is used to clamp the RGB values before their
// ranges
// are normalized to eight bits.
static const int kMaxChannelValue = 262143;

static inline uint32_t YUV2RGB(int nY, int nU, int nV) {
    nY -= 16;
    nU -= 128;
    nV -= 128;
    if (nY < 0) nY = 0;

    // This is the floating point equivalent. We do the conversion in integer
    // because some Android devices do not have floating point in hardware.
    // nR = (int)(1.164 * nY + 1.596 * nV);
    // nG = (int)(1.164 * nY - 0.813 * nV - 0.391 * nU);
    // nB = (int)(1.164 * nY + 2.018 * nU);

    int nR = (int)(1192 * nY + 1634 * nV);
    int nG = (int)(1192 * nY - 833 * nV - 400 * nU);
    int nB = (int)(1192 * nY + 2066 * nU);

    nR = MIN(kMaxChannelValue, MAX(0, nR));
    nG = MIN(kMaxChannelValue, MAX(0, nG));
    nB = MIN(kMaxChannelValue, MAX(0, nB));

    nR = (nR >> 10) & 0xff;
    nG = (nG >> 10) & 0xff;
    nB = (nB >> 10) & 0xff;

    return 0xff000000 | (nR << 16) | (nG << 8) | nB;
}

/**
 * Convert yuv image inside AImage into ANativeWindow_Buffer
 * ANativeWindow_Buffer format is guaranteed to be
 *      WINDOW_FORMAT_RGBX_8888
 *      WINDOW_FORMAT_RGBA_8888
 * @param buf a {@link ANativeWindow_Buffer } instance, destination of
 *            image conversion
 * @param image a {@link AImage} instance, source of image conversion.
 *            it will be deleted via {@link AImage_delete}
 */
bool ImageReader::DisplayImage(ANativeWindow_Buffer *buf, AImage *image) {
    ASSERT(buf->format == WINDOW_FORMAT_RGBX_8888 ||
           buf->format == WINDOW_FORMAT_RGBA_8888,
           "Not supported buffer format");

    int32_t srcFormat = -1;
    AImage_getFormat(image, &srcFormat);
    ASSERT(AIMAGE_FORMAT_YUV_420_888 == srcFormat, "Failed to get format");
    int32_t srcPlanes = 0;
    AImage_getNumberOfPlanes(image, &srcPlanes);
    ASSERT(srcPlanes == 3, "Is not 3 planes");

    switch (presentRotation_) {
        case 0:
            PresentImage(buf, image);
            break;
        case 90:
            PresentImage90(buf, image);
            break;
        case 180:
            PresentImage180(buf, image);
            break;
        case 270:
            PresentImage270(buf, image);
            break;
        default:
            ASSERT(0, "NOT recognized display rotation: %d", presentRotation_);
    }

    AImage_delete(image);

    return true;
}

/*
 * PresentImage()
 *   Converting yuv to RGB
 *   No rotation: (x,y) --> (x, y)
 *   Refer to:
 * https://mathbits.com/MathBits/TISection/Geometry/Transformations2.htm
 */
void ImageReader::PresentImage(ANativeWindow_Buffer *buf, AImage *image) {
    AImageCropRect srcRect;
    AImage_getCropRect(image, &srcRect);

    int32_t yStride, uvStride;
    uint8_t *yPixel, *uPixel, *vPixel;
    int32_t yLen, uLen, vLen;
    AImage_getPlaneRowStride(image, 0, &yStride);
    AImage_getPlaneRowStride(image, 1, &uvStride);
    AImage_getPlaneData(image, 0, &yPixel, &yLen);
    AImage_getPlaneData(image, 1, &vPixel, &vLen);
    AImage_getPlaneData(image, 2, &uPixel, &uLen);
    int32_t uvPixelStride;
    AImage_getPlanePixelStride(image, 1, &uvPixelStride);

    int32_t height = MIN(buf->height, (srcRect.bottom - srcRect.top));
    int32_t width = MIN(buf->width, (srcRect.right - srcRect.left));

    uint32_t *out = static_cast<uint32_t *>(buf->bits);
    for (int32_t y = 0; y < height; y++) {
        const uint8_t *pY = yPixel + yStride * (y + srcRect.top) + srcRect.left;

        int32_t uv_row_start = uvStride * ((y + srcRect.top) >> 1);
        const uint8_t *pU = uPixel + uv_row_start + (srcRect.left >> 1);
        const uint8_t *pV = vPixel + uv_row_start + (srcRect.left >> 1);

        for (int32_t x = 0; x < width; x++) {
            const int32_t uv_offset = (x >> 1) * uvPixelStride;
            out[x] = YUV2RGB(pY[x], pU[uv_offset], pV[uv_offset]);
        }
        out += buf->stride;
    }
}

/*
 * PresentImage90()
 *   Converting YUV to RGB
 *   Rotation image anti-clockwise 90 degree -- (x, y) --> (-y, x)
 */
void ImageReader::PresentImage90(ANativeWindow_Buffer *buf, AImage *image) {
    AImageCropRect srcRect;
    AImage_getCropRect(image, &srcRect);

    int32_t yStride, uvStride;
    uint8_t *yPixel, *uPixel, *vPixel;
    int32_t yLen, uLen, vLen;
    AImage_getPlaneRowStride(image, 0, &yStride);
    AImage_getPlaneRowStride(image, 1, &uvStride);
    AImage_getPlaneData(image, 0, &yPixel, &yLen);
    AImage_getPlaneData(image, 1, &vPixel, &vLen);
    AImage_getPlaneData(image, 2, &uPixel, &uLen);
    int32_t uvPixelStride;
    AImage_getPlanePixelStride(image, 1, &uvPixelStride);

    int32_t height = MIN(buf->width, (srcRect.bottom - srcRect.top));
    int32_t width = MIN(buf->height, (srcRect.right - srcRect.left));

    uint32_t *out = static_cast<uint32_t *>(buf->bits);
    out += height - 1;
    for (int32_t y = 0; y < height; y++) {
        const uint8_t *pY = yPixel + yStride * (y + srcRect.top) + srcRect.left;

        int32_t uv_row_start = uvStride * ((y + srcRect.top) >> 1);
        const uint8_t *pU = uPixel + uv_row_start + (srcRect.left >> 1);
        const uint8_t *pV = vPixel + uv_row_start + (srcRect.left >> 1);

        for (int32_t x = 0; x < width; x++) {
            const int32_t uv_offset = (x >> 1) * uvPixelStride;
            // [x, y]--> [-y, x]
            out[x * buf->stride] = YUV2RGB(pY[x], pU[uv_offset], pV[uv_offset]);
        }
        out -= 1;  // move to the next column
    }
}

/*
 * PresentImage180()
 *   Converting yuv to RGB
 *   Rotate image 180 degree: (x, y) --> (-x, -y)
 */
void ImageReader::PresentImage180(ANativeWindow_Buffer *buf, AImage *image) {
    AImageCropRect srcRect;
    AImage_getCropRect(image, &srcRect);

    int32_t yStride, uvStride;
    uint8_t *yPixel, *uPixel, *vPixel;
    int32_t yLen, uLen, vLen;
    AImage_getPlaneRowStride(image, 0, &yStride);
    AImage_getPlaneRowStride(image, 1, &uvStride);
    AImage_getPlaneData(image, 0, &yPixel, &yLen);
    AImage_getPlaneData(image, 1, &vPixel, &vLen);
    AImage_getPlaneData(image, 2, &uPixel, &uLen);
    int32_t uvPixelStride;
    AImage_getPlanePixelStride(image, 1, &uvPixelStride);

    int32_t height = MIN(buf->height, (srcRect.bottom - srcRect.top));
    int32_t width = MIN(buf->width, (srcRect.right - srcRect.left));

    uint32_t *out = static_cast<uint32_t *>(buf->bits);
    out += (height - 1) * buf->stride;
    for (int32_t y = 0; y < height; y++) {
        const uint8_t *pY = yPixel + yStride * (y + srcRect.top) + srcRect.left;

        int32_t uv_row_start = uvStride * ((y + srcRect.top) >> 1);
        const uint8_t *pU = uPixel + uv_row_start + (srcRect.left >> 1);
        const uint8_t *pV = vPixel + uv_row_start + (srcRect.left >> 1);

        for (int32_t x = 0; x < width; x++) {
            const int32_t uv_offset = (x >> 1) * uvPixelStride;
            // mirror image since we are using front camera
            out[width - 1 - x] = YUV2RGB(pY[x], pU[uv_offset], pV[uv_offset]);
            // out[x] = YUV2RGB(pY[x], pU[uv_offset], pV[uv_offset]);
        }
        out -= buf->stride;
    }
}

/*
 * PresentImage270()
 *   Converting image from YUV to RGB
 *   Rotate Image counter-clockwise 270 degree: (x, y) --> (y, x)
 */
void ImageReader::PresentImage270(ANativeWindow_Buffer *buf, AImage *image) {
    AImageCropRect srcRect;
    AImage_getCropRect(image, &srcRect);

    int32_t yStride, uvStride;
    uint8_t *yPixel, *uPixel, *vPixel;
    int32_t yLen, uLen, vLen;
    AImage_getPlaneRowStride(image, 0, &yStride);
    AImage_getPlaneRowStride(image, 1, &uvStride);
    AImage_getPlaneData(image, 0, &yPixel, &yLen);
    AImage_getPlaneData(image, 1, &vPixel, &vLen);
    AImage_getPlaneData(image, 2, &uPixel, &uLen);
    int32_t uvPixelStride;
    AImage_getPlanePixelStride(image, 1, &uvPixelStride);

    int32_t height = MIN(buf->width, (srcRect.bottom - srcRect.top));
    int32_t width = MIN(buf->height, (srcRect.right - srcRect.left));

    uint32_t *out = static_cast<uint32_t *>(buf->bits);
    for (int32_t y = 0; y < height; y++) {
        const uint8_t *pY = yPixel + yStride * (y + srcRect.top) + srcRect.left;

        int32_t uv_row_start = uvStride * ((y + srcRect.top) >> 1);
        const uint8_t *pU = uPixel + uv_row_start + (srcRect.left >> 1);
        const uint8_t *pV = vPixel + uv_row_start + (srcRect.left >> 1);

        for (int32_t x = 0; x < width; x++) {
            const int32_t uv_offset = (x >> 1) * uvPixelStride;
            out[(width - 1 - x) * buf->stride] =
                    YUV2RGB(pY[x], pU[uv_offset], pV[uv_offset]);
        }
        out += 1;  // move to the next column
    }
}
void ImageReader::SetPresentRotation(int32_t angle) {
    presentRotation_ = angle;
}

/**
 * Write out jpeg files to kDirName directory
 * @param image point capture jpg image
 */
void ImageReader::WriteFile(AImage *image) {
    IN
    int planeCount;
    media_status_t status = AImage_getNumberOfPlanes(image, &planeCount);
//    ASSERT(status == AMEDIA_OK && planeCount == 1,
//           "Error: getNumberOfPlanes() planeCount = %d", planeCount);
    uint8_t *data = nullptr;
    int len = 0;
    AImage_getPlaneData(image, 0, &data, &len);

    /*
    DIR *dir = opendir(kDirName);
    if (dir) {
        closedir(dir);
    } else {
        std::string cmd = "mkdir -p ";
        cmd += kDirName;
        system(cmd.c_str());
    }
     */

    struct timespec ts {
            0, 0
    };
    clock_gettime(CLOCK_REALTIME, &ts);
    struct tm localTime;
    localtime_r(&ts.tv_sec, &localTime);
    CameraAppEngine * _app = (CameraAppEngine *) app ;
    std::string fileName = _app -> filename;
    /*
    std::string dash("-");
    fileName += kFileName + std::to_string(localTime.tm_mon) +
                std::to_string(localTime.tm_mday) + dash +
                std::to_string(localTime.tm_hour) +
                std::to_string(localTime.tm_min) +
                std::to_string(localTime.tm_sec) + ".jpg";
                */

    FILE *file = fopen(fileName.c_str(), "wb");
    if (file && data && len) {
        fwrite(data, 1, len, file);
        fclose(file);

        if (callback_) {
            callback_(callbackCtx_, fileName.c_str());
        }
    } else {
        if (file) fclose(file);
    }
    AImage_delete(image);
    LOGD("Wrote file %s", fileName.c_str ());
    OUT
}

void CameraAppEngine::createDecoder () {
    IN
//    AAssetManager* mgr = AAssetManager_fromJava(env, assetManager);

    int fd = fileno (fopen ("/storage/emulated/0/Android/data/com.shajikhan.ladspa.amprack/files/Movies/testfile.mp4", "r"));
    if (fd < 0) {
        LOGE("failed to open file: clips/testfile.mp4 %d (%s)",  fd, strerror(errno));
        OUT
        return;
    } else {
        LOGV("opened fd %d", fd);
    }

    AMediaExtractor *ex = AMediaExtractor_new();
    media_status_t err = AMediaExtractor_setDataSourceFd(
            ex, fd, 0, 120);

    if (err != AMEDIA_OK) {
        LOGV("setDataSource error: %d", err);
        OUT
        return ;
    } else {
        LOGV("media extractor [ok]");
    }

    int numtracks = AMediaExtractor_getTrackCount(ex);
    AMediaCodec *codec = NULL;

    LOGV("input has %d tracks", numtracks);
    for (int i = 0; i < numtracks; i++) {
        AMediaFormat *format = AMediaExtractor_getTrackFormat(ex, i);
        const char *s = AMediaFormat_toString(format);
        LOGV("track %d format: %s", i, s);
        const char *mime;
        if (!AMediaFormat_getString(format, AMEDIAFORMAT_KEY_MIME, &mime)) {
            LOGV("no mime type");
            OUT
            return ;
        } else if (!strncmp(mime, "video/", 6)) {
            // Omitting most error handling for clarity.
            // Production code should check for errors.
            AMediaExtractor_selectTrack(ex, i);
            codec = AMediaCodec_createDecoderByType(mime);
            AMediaCodec_configure(codec, format, NULL, NULL, 0);
            AMediaCodec_start(codec);
        }

        AMediaFormat_delete(format);
    }

    OUT
}