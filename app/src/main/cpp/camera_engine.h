
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

#include <jni.h>

#include <functional>
#include <thread>

#include "camera_manager.h"

/**
 * CameraAppEngine
 */
class CameraAppEngine {
 public:
  explicit CameraAppEngine(JNIEnv* env, jobject instance, jint w, jint h);
  ~CameraAppEngine();

  // Manage NDKCamera Object
  void CreateCameraSession(jobject surface);
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

private:
  JNIEnv* env_;
  jobject javaInstance_;
  int32_t requestWidth_;
  int32_t requestHeight_;
  jobject surface_;
  NDKCamera* camera_;
  ImageFormat compatibleCameraRes_;

    void createEncoder();

    long long int computePresentationTimeNsec();

    int64_t frame2Time(int64_t frameNo);

    void drainEncoder(bool endOfStream);

    bool writeFrame(int *data, const long long int timestamp);

    void writeEnd();

    void releaseEncoder();
};
#endif  // __CAMERA_ENGINE_H__
