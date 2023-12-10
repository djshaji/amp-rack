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

#include "image_reader.h"
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
void CameraAppEngine::CreateCameraSession(jobject surface) {
  if (surface == nullptr) {
    camera_->CreateSessionVideoCapture(imageReader->GetNativeWindow ());
  } else {
    surface_ = env_->NewGlobalRef(surface);
    camera_->CreateSession(ANativeWindow_fromSurface(env_, surface));
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
  filename = _filename ;
  format = AMediaFormat_new() ;
  AMediaFormat_setInt32(format,AMEDIAFORMAT_KEY_WIDTH,requestWidth_);
  AMediaFormat_setInt32(format,AMEDIAFORMAT_KEY_HEIGHT,requestHeight_);

  AMediaFormat_setString(format,AMEDIAFORMAT_KEY_MIME,"video/avc"); // H.264 Advanced Video Coding
  AMediaFormat_setInt32(format, AMEDIAFORMAT_KEY_COLOR_FORMAT, 21); // #21 COLOR_FormatYUV420SemiPlanar (NV12)
  AMediaFormat_setInt32(format,AMEDIAFORMAT_KEY_BIT_RATE,500000);
  AMediaFormat_setFloat(format,AMEDIAFORMAT_KEY_FRAME_RATE,mFPS);
  AMediaFormat_setInt32(format,AMEDIAFORMAT_KEY_I_FRAME_INTERVAL,5);

  mEncoder = AMediaCodec_createEncoderByType("video/avc");
  if(mEncoder == nullptr){
    LOGE("Unable to create encoder");
  }

  media_status_t err = AMediaCodec_configure(mEncoder, format, NULL, NULL, AMEDIACODEC_CONFIGURE_FLAG_ENCODE);
  if(err != AMEDIA_OK){
    LOGE( "Error occurred: %d", err );
  }

  err = AMediaCodec_start(mEncoder);
  if(err != AMEDIA_OK){
    LOGE( "Error occurred: %d", err);
  }

  fd = fileno (fopen (filename.c_str(), "w"));
  mMuxer = AMediaMuxer_new(fd, AMEDIAMUXER_OUTPUT_FORMAT_MPEG_4);

  if(mMuxer == nullptr){
    LOGE("Unable to create Muxer");
  }

  mTrackIndex = -1;
  mMuxerStarted = false;
  mFrameCounter = 0;
  isRunning = true;
  LOGD ("Encoder ready!");
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

bool CameraAppEngine::writeFrame(int * data, const long long timestamp){
  // Feed any pending encoder output into the muxer.
  drainEncoder(false);

 // Generate a new frame of input.

  /**
                * Get the index of the next available input buffer. An app will typically use this with
                * getInputBuffer() to get a pointer to the buffer, then copy the data to be encoded or decoded
                * into the buffer before passing it to the codec.
                */
  ssize_t inBufferIdx = AMediaCodec_dequeueInputBuffer(mEncoder, TIMEOUT_USEC);

  /**
                * Get an input buffer. The specified buffer index must have been previously obtained from
                * dequeueInputBuffer, and not yet queued.
                */
  size_t out_size;
  uint8_t* inBuffer = AMediaCodec_getInputBuffer(mEncoder, inBufferIdx, &out_size);

  // here we actually copy the data.
  memcpy(inBuffer, data, out_size);

  /**
        * Send the specified buffer to the codec for processing.
        */
  //int64_t presentationTimeNs = timestamp;
  int64_t presentationTimeNs = computePresentationTimeNsec();

  media_status_t status = AMediaCodec_queueInputBuffer(mEncoder, inBufferIdx, 0, out_size, presentationTimeNs, data == NULL ? AMEDIACODEC_BUFFER_FLAG_END_OF_STREAM : 0);

  if(status == AMEDIA_OK){
    //qDebug() << "Successfully pushed frame to input buffer";
  }
  else{
    LOGW("Something went wrong while pushing frame to input buffer: %d", status);
    return false;
  }

  // Submit it to the encoder.  The eglSwapBuffers call will block if the input
  // is full, which would be bad if it stayed full until we dequeued an output
  // buffer (which we can't do, since we're stuck here).  So long as we fully drain
  // the encoder before supplying additional input, the system guarantees that we
  // can supply another frame without blocking.
  //qDebug() << "sending frame " << i << " to encoder";
  //AMediaCodec_flush(mEncoder);
  return true;
}

void CameraAppEngine::drainEncoder(bool endOfStream) {

  if (endOfStream) {
    LOGD( "Draining encoder to EOS");
    // only API >= 26
    // Send an empty frame with the end-of-stream flag set.
    // AMediaCodec_signalEndOfInputStream();
    // Instead, we construct that frame manually.
  }




  while (true) {
    ssize_t encoderStatus = AMediaCodec_dequeueOutputBuffer(mEncoder, &mBufferInfo, TIMEOUT_USEC);


    if (encoderStatus == AMEDIACODEC_INFO_TRY_AGAIN_LATER) {
      // no output available yet
      if (!endOfStream) {
        return;
        //break;      // out of while
      }
      if(endOfStream){
        LOGD("no output available, spinning to await EOS");
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
}

void CameraAppEngine::test () {
    IN
    AImage * i = imageReader -> GetLatestImage() ;
    imageReader ->WriteFile(i);
    OUT
}

/**
 *
 * @param start is true to start preview, false to stop preview
 * @return  true if preview started, false when error happened
 */
void CameraAppEngine::StartPreview(bool start) { camera_->StartPreview(start); }
