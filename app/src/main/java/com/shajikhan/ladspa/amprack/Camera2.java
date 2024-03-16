package com.shajikhan.ladspa.amprack;

import android.hardware.camera2.TotalCaptureResult;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.CamcorderProfile;
import android.media.ImageReader;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class Camera2 {
    final String TAG = getClass().getSimpleName();
    // parameters for the encoder
    private static final String MIME_TYPE = "video/avc";    // H.264 Advanced Video Coding
    private static final int FRAME_RATE = 30;               // 15fps
    private static final int IFRAME_INTERVAL = 1;          // 10 seconds between I-frames
    private int mWidth = -1;
    public long presentationTimeUs = 0 ;
    private int mHeight = -1;
    // bit rate, in bits per second
    private int mBitRate = -1;
    public MediaCodec mEncoder, audioEncoder;
    ByteBuffer[] audioInputBuffers ;

    private Surface mInputSurface;
    private MediaMuxer mMuxer;
    private int mTrackIndex = -1, audioTrackIndex = -1;
    public int audioIndex ;
    public boolean mMuxerStarted;

    // allocate one of these up front so we don't need to do it every time
    private MediaCodec.BufferInfo mBufferInfo;

    private TextureView textureView;
    MainActivity mainActivity;
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    CameraManager manager;

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    public String cameraId;
    protected CameraDevice cameraDevice;
    protected CameraCaptureSession cameraCaptureSessions;
    protected CaptureRequest captureRequest;
    protected CaptureRequest.Builder captureRequestBuilder;
    private Size imageDimension;
    private ImageReader imageReader;
    ArrayList<String> cameras;
    HashMap<String, CameraCharacteristics> cameraCharacteristicsHashMap;
    private boolean mFlashSupported;
    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;

    Camera2(MainActivity mainActivity_) {
        mainActivity = mainActivity_;
        textureView = mainActivity_.rack.videoTexture;
    }

    public void openCamera() {
        CameraManager manager = (CameraManager) mainActivity.getSystemService(mainActivity.CAMERA_SERVICE);
        cameras = new ArrayList<>();
        cameraCharacteristicsHashMap = new HashMap<>();

        Log.e(TAG, "is camera open");
        try {
            for (String s: manager.getCameraIdList()) {
                Log.d(TAG, String.format("found camera: %s", s));
                CameraCharacteristics characteristics = manager.getCameraCharacteristics(s);
                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing == CameraCharacteristics.LENS_FACING_FRONT)
                    cameraId = s ;

                cameras.add(s);
                cameraCharacteristicsHashMap.put(s, characteristics);
            }

//            cameraId = manager.getCameraIdList()[0];
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            assert map != null;
            imageDimension = map.getOutputSizes(SurfaceTexture.class)[0];
            ///| fixme: get this from somewhere else
            imageDimension = new Size(720, 1280);
            if (ActivityCompat.checkSelfPermission(mainActivity, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            manager.openCamera(cameraId, stateCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        Log.e(TAG, "openCamera X");
    }

    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            //This is called when the camera is open
            Log.e(TAG, "onOpened");
            cameraDevice = camera;
            createCameraPreview();
            mainActivity.setTextureTransform(mainActivity.camera2.cameraCharacteristicsHashMap.get(mainActivity.camera2.cameraId));
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            cameraDevice.close();
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            cameraDevice.close();
            cameraDevice = null;
        }
    };
    final CameraCaptureSession.CaptureCallback captureCallbackListener = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
            createCameraPreview();
        }
    };

    protected void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("Camera Background");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    protected void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    public void createCameraPreview() {
        try {
            SurfaceTexture texture = textureView.getSurfaceTexture();
            assert texture != null;
            // fixme: change this!
            imageDimension = new Size (1280, 720);
            texture.setDefaultBufferSize(imageDimension.getWidth(), imageDimension.getHeight());
            Log.d(TAG, "createCameraPreview: created surface with dimensions" + ":".format (" %d x %d", imageDimension.getWidth(), imageDimension.getHeight()));
            Surface surface = new Surface(texture);
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(surface);

            prepareEncoder();
            captureRequestBuilder.addTarget(mInputSurface);
            List<Surface> surfaceList = new ArrayList<>();
            surfaceList.add(surface);
            surfaceList.add(mInputSurface);

            cameraDevice.createCaptureSession(Arrays.asList(surface,mInputSurface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    //The camera is already closed
                    if (null == cameraDevice) {
                        return;
                    }
                    // When the session is ready, we start displaying the preview.
                    cameraCaptureSessions = cameraCaptureSession;
                    updatePreview();
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    MainActivity.toast("Failed to start camera 😓");
                }
            }, null);
        } catch (CameraAccessException e) {
            MainActivity.alert("Failed to start camera \uD83D\uDE13", e.getMessage());
            e.printStackTrace();
        }
    }

    protected void updatePreview() {
        if (null == cameraDevice) {
            Log.e(TAG, "updatePreview error, return");
        }
//        captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
        try {
            cameraCaptureSessions.setRepeatingRequest(captureRequestBuilder.build(), null, mBackgroundHandler);
//            cameraCaptureSessions.setRepeatingRequest(videoBuilder.build(), null, encoderHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public void closeCamera() {
        if (null != cameraDevice) {
            cameraDevice.close();
            cameraDevice = null;
        }

        releaseEncoder();
    }

    private void prepareEncoder() {
        mBufferInfo = new MediaCodec.BufferInfo();
        ///| Todo: fixme: get width and height from camera
        mWidth = imageDimension.getWidth() ;
        mHeight = imageDimension.getHeight() ;
        mBitRate = 1000000 ;
        MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, mWidth, mHeight);

        // Set some properties.  Failing to specify some of these can cause the MediaCodec
        // configure() call to throw an unhelpful exception.
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        format.setInteger(MediaFormat.KEY_BIT_RATE, mBitRate);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, IFRAME_INTERVAL);
        Log.d(TAG, "format: " + format);

        MediaFormat outputFormat = MediaFormat.createAudioFormat("audio/mp4a-latm",AudioEngine.getSampleRate(), 1);
        outputFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
        outputFormat.setInteger(MediaFormat.KEY_BIT_RATE, 160000);
        outputFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16384);

        // Create a MediaCodec encoder, and configure it with our format.  Get a Surface
        // we can use for input and wrap it with a class that handles the EGL work.
        //
        // If you want to have two EGL contexts -- one for display, one for recording --
        // you will likely want to defer instantiation of CodecInputSurface until after the
        // "display" EGL context is created, then modify the eglCreateContext call to
        // take eglGetCurrentContext() as the share_context argument.
        try {
            mEncoder = MediaCodec.createEncoderByType(MIME_TYPE);
            audioEncoder = MediaCodec.createEncoderByType("audio/mp4a-latm");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        mEncoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        audioEncoder.configure(outputFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);

        mInputSurface = mEncoder.createInputSurface();
        mEncoder.setCallback(new EncoderCallback(true));
//        audioEncoder.setCallback(new EncoderCallback(false));

        audioEncoder.start();
        mEncoder.start();

        // Output filename.  Ideally this would use Context.getFilesDir() rather than a
        // hard-coded output directory.
        SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy_HH.mm.ss");
        Date date = new Date();
        mainActivity.lastRecordedFileName =
                String.format("%s/%s.mp4",
                        mainActivity.getExternalFilesDir(Environment.DIRECTORY_MOVIES).getAbsolutePath(),
                        formatter.format(date));
        String outputPath = mainActivity.lastRecordedFileName;
        Log.d(TAG, String.format ("recording video to file: %s", mainActivity.lastRecordedFileName));

        // Create a MediaMuxer.  We can't add the video track and start() the muxer here,
        // because our MediaFormat doesn't have the Magic Goodies.  These can only be
        // obtained from the encoder after it has started processing data.
        //
        // We're not actually interested in multiplexing audio.  We just want to convert
        // the raw H.264 elementary stream we get from MediaCodec into a .mp4 file.
        try {
            mMuxer = new MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        } catch (IOException ioe) {
            throw new RuntimeException("MediaMuxer creation failed", ioe);
        }

        mTrackIndex = -1;
        mMuxerStarted = false;
    }

    /**
     * Releases encoder resources.  May be called after partial / failed initialization.
     */
    private void releaseEncoder() {
        Log.d(TAG, "releaseEncoder: stopping encoder");
        mEncoder.signalEndOfInputStream();

        if (mEncoder != null) {
            mEncoder.stop();
            mEncoder.release();
            mEncoder = null;
        }

        if (audioEncoder != null) {
            audioEncoder.stop();
            audioEncoder.release();
            audioEncoder = null;
        }

        if (mInputSurface != null) {
            mInputSurface.release();
            mInputSurface = null;
        }
        if (mMuxer != null) {
            mMuxer.stop();
            mMuxer.release();
            mMuxer = null;
            mMuxerStarted = false;
        }

        mTrackIndex = -1 ;
        audioTrackIndex = -1 ;
    }

    class EncoderCallback extends MediaCodec.Callback {
        ByteBuffer outPutByteBuffer, inputByteBuffer, audioBuffer;
        MainActivity.AVBuffer floatBuffer;
        boolean isVideo ;

        EncoderCallback (boolean video) {
            isVideo = video;
        }

        @Override
        public void onInputBufferAvailable(@NonNull MediaCodec codec, int index) {
            Log.d(TAG, String.format ("track: %b", isVideo));
            if (! isVideo) return;
            int eos = 0 ;

            if (audioTrackIndex == -1) {
                Log.d(TAG, "onInputBufferAvailable: added audio track");
                audioTrackIndex = mMuxer.addTrack(audioEncoder.getOutputFormat());
            }

            if (! mainActivity.videoRecording)
                eos = MediaCodec.BUFFER_FLAG_END_OF_STREAM ;

            floatBuffer = mainActivity.avBuffer.pop() ;
            inputByteBuffer = codec.getInputBuffer(index);
            inputByteBuffer.asFloatBuffer().put(floatBuffer.floatBuffer);
            presentationTimeUs = 1000000l * index / 48000;
            mainActivity.camera2.audioEncoder.queueInputBuffer(index, 0, floatBuffer.size, presentationTimeUs, eos);;

            if (eos != 0) {
                audioEncoder.signalEndOfInputStream();
                audioEncoder.stop();
            }
        }

        @Override
        public void onOutputBufferAvailable(@NonNull MediaCodec codec, int index, @NonNull MediaCodec.BufferInfo info) {
            if (! mMuxerStarted) {
                MediaFormat newFormat = mEncoder.getOutputFormat();
                Log.d(TAG, "encoder output format changed: " + newFormat);

                // now that we have the Magic Goodies, start the muxer
                if (mTrackIndex == -1)
                    mTrackIndex = mMuxer.addTrack(newFormat);
                int aIndex = audioEncoder.dequeueOutputBuffer(mBufferInfo, 5000) ;
                if (aIndex >= 0) {
                    Log.d(TAG, "onInputBufferAvailable: added audio track");
                    MediaFormat format = audioEncoder.getOutputFormat();
                    Log.d(TAG, String.format("format: %s", format.toString()));
                    audioTrackIndex = mMuxer.addTrack(format);
                } else {
                    Log.e(TAG, "onOutputBufferAvailable: dequeue input buffer: " + aIndex, null);
                    return;
                }

                mMuxer.setOrientationHint(cameraCharacteristicsHashMap.get(cameraId).get(CameraCharacteristics.SENSOR_ORIENTATION));
                mMuxer.start();
                mMuxerStarted = true;
            }

            outPutByteBuffer = codec.getOutputBuffer(index);

            mMuxer.writeSampleData(mTrackIndex, outPutByteBuffer, info);
            codec.releaseOutputBuffer(index, false);
//            byte[] outDate = new byte[info.size];
//            outPutByteBuffer.get(outDate);



            int aIndex = audioEncoder.dequeueOutputBuffer(mBufferInfo, 5000) ;
            if (aIndex > 0) {
                audioBuffer = audioEncoder.getOutputBuffer(aIndex);
                mMuxer.writeSampleData(audioTrackIndex, audioBuffer, mBufferInfo);
            }
        }

        @Override
        public void onError(@NonNull MediaCodec codec, @NonNull MediaCodec.CodecException e) {
            Log.e(TAG, "onError: encoder callback error", e);
        }

        @Override
        public void onOutputFormatChanged(@NonNull MediaCodec codec, @NonNull MediaFormat format) {
            Log.d(TAG, String.format ("encoder format changed: %s", format.toString()));
        }
    }
}
