
/*
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

#ifndef __CAMERA_ENGINE_H__
#define __CAMERA_ENGINE_H__
#include <android/native_activity.h>
#include <android/native_window.h>
#include <android/native_window_jni.h>
#include "media/NdkMediaCodec.h"
#include "media/NdkMediaError.h"
#include "media/NdkMediaFormat.h"
#include "media/NdkMediaMuxer.h"
#include <media/NdkImageReader.h>

#include <jni.h>

#include <functional>
#include <thread>

#include "camera_manager.h"
//#include "image_reader.h"



class ImageReader {
public:
    /**
     * Ctor and Dtor()
     */
    explicit ImageReader(ImageFormat* res, enum AIMAGE_FORMATS format);
    void WriteFile(AImage* image);
    AMediaMuxer* mediaMuxer;
    AMediaCodec* mediaCodec;

    ~ImageReader();

    /**
     * Report cached ANativeWindow, which was used to create camera's capture
     * session output.
     */
    ANativeWindow* GetNativeWindow(void);

    /**
     * Retrieve Image on the top of Reader's queue
     */
    AImage* GetNextImage(void);

    /**
     * Retrieve Image on the back of Reader's queue, dropping older images
     */
    AImage* GetLatestImage(void);

    /**
     * Delete Image
     * @param image {@link AImage} instance to be deleted
     */
    void DeleteImage(AImage* image);

    /**
     * AImageReader callback handler. Called by AImageReader when a frame is
     * captured
     * (Internal function, not to be called by clients)
     */
    void ImageCallback(AImageReader* reader);

    /**
     * DisplayImage()
     *   Present camera image to the given display buffer. Avaliable image is
     * converted
     *   to display buffer format. Supported display format:
     *      WINDOW_FORMAT_RGBX_8888
     *      WINDOW_FORMAT_RGBA_8888
     *   @param buf {@link ANativeWindow_Buffer} for image to display to.
     *   @param image a {@link AImage} instance, source of image conversion.
     *            it will be deleted via {@link AImage_delete}
     *   @return true on success, false on failure
     */
    bool DisplayImage(ANativeWindow_Buffer* buf, AImage* image);
    /**
     * Configure the rotation angle necessary to apply to
     * Camera image when presenting: all rotations should be accumulated:
     *    CameraSensorOrientation + Android Device Native Orientation +
     *    Human Rotation (rotated degree related to Phone native orientation
     */
    void SetPresentRotation(int32_t angle);

    /**
     * regsiter a callback function for client to be notified that jpeg already
     * written out.
     * @param ctx is client context when callback is invoked
     * @param callback is the actual callback function
     */
    void RegisterCallback(void* ctx,
                          std::function<void(void* ctx, const char* fileName)>);

private:
    int32_t presentRotation_;
    AImageReader* reader_;
    int tidx = -1 ;
    void * app = nullptr;

    std::function<void(void* ctx, const char* fileName)> callback_;
    void* callbackCtx_;

    void PresentImage(ANativeWindow_Buffer* buf, AImage* image);
    void PresentImage90(ANativeWindow_Buffer* buf, AImage* image);
    void PresentImage180(ANativeWindow_Buffer* buf, AImage* image);
    void PresentImage270(ANativeWindow_Buffer* buf, AImage* image);

};
/**
 * CameraAppEngine
 */
class CameraAppEngine {
 public:
  explicit CameraAppEngine(JNIEnv* env, jobject instance, jint w, jint h);
  ~CameraAppEngine();

  // Manage NDKCamera Object
  void CreateCameraSession(jobject surface, ANativeWindow * window);
  void StartPreview(bool start);
  const ImageFormat& GetCompatibleCameraRes() const;
  int32_t GetCameraSensorOrientation(int32_t facing);
  jobject GetSurfaceObject();
  AMediaFormat* format = NULL ;
  AMediaCodec* mEncoder;
  AMediaMuxer* mMuxer;
  AMediaCodecBufferInfo mBufferInfo;
  int mTrackIndex;
  bool mMuxerStarted;
  const static int TIMEOUT_USEC = 10000;
  int mFPS = 30;
  int mFrameCounter = 0;
  std::string filename ;
  bool isRunning = false;
  int fd = -1 ;
  void createEncoder(std::string _filename);
    void test();
    bool writeFrame(AImage * image);

    ImageReader * imageReader  = nullptr;
private:
  JNIEnv* env_;
  jobject javaInstance_;
  int32_t requestWidth_;
  int32_t requestHeight_;
  jobject surface_;
  NDKCamera* camera_;
  ImageFormat compatibleCameraRes_;

    long long int computePresentationTimeNsec();

    int64_t frame2Time(int64_t frameNo);

    void drainEncoder(bool endOfStream);

    void writeEnd();

    void releaseEncoder();

};


#endif  // __CAMERA_ENGINE_H__
