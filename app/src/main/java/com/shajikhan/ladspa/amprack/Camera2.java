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
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.CamcorderProfile;
import android.media.ImageReader;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.media.MediaRecorder;
import android.os.Build;
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
import java.util.NoSuchElementException;

public class Camera2 {
    final String TAG = getClass().getSimpleName();
    int sampleRate = 48000;
    // parameters for the encoder
    private static final String MIME_TYPE = "video/avc";    // H.264 Advanced Video Coding
    private static final int FRAME_RATE = 30;               // 15fps
    private static final int IFRAME_INTERVAL = 1;          // 10 seconds between I-frames
    private int mWidth = -1;
    public long presentationTimeUs = 0, firstAudioFrame = -1 ;
    long frame = 0 ;
    private int mHeight = -1;
    // bit rate, in bits per second
    private int mBitRate = -1;
    public MediaCodec mEncoder, audioEncoder = null;
    ByteBuffer[] audioInputBuffers ;

    private Surface mInputSurface;
    class Timestamp {
        long start = 0;
        long vidstart = 0;

        Timestamp () {
            start = System.nanoTime() / 1000 ;
        }

        long get () {
            return (System.nanoTime() / 1000) - start;
        }
    }

    Timestamp timestamp = null ;
    public MediaMuxer mMuxer;
    public int mTrackIndex = -1, audioTrackIndex = -1;
    public int audioIndex ;
    public boolean mMuxerStarted;

    // allocate one of these up front so we don't need to do it every time
    public MediaCodec.BufferInfo mBufferInfo;

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

    // Start Audio Record
    static final int AUDIO_SOURCE = MediaRecorder.AudioSource.MIC; // for raw audio, use MediaRecorder.AudioSource.UNPROCESSED, see note in MediaRecorder section
    static final int SAMPLE_RATE = 48000;
    static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_FLOAT;
    static final int BUFFER_SIZE_RECORDING = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
    AudioRecord audioRecord;

    // End Audio Record

    Camera2(MainActivity mainActivity_) {
        mainActivity = mainActivity_;
        textureView = mainActivity_.rack.videoTexture;
        sampleRate = AudioEngine.getSampleRate() ;
        if (sampleRate == 0)
            sampleRate = 48000 ;

        timestamp = new Timestamp();
    }

    public void openCamera() {
        manager = (CameraManager) mainActivity.getSystemService(mainActivity.CAMERA_SERVICE);
        cameras = new ArrayList<>();
        cameraCharacteristicsHashMap = new HashMap<>();

        Log.e(TAG, "is camera open");
        try {
            for (String s: manager.getCameraIdList()) {
                Log.d(TAG, String.format("found camera: %s", s));
                CameraCharacteristics characteristics = manager.getCameraCharacteristics(s);
                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                int front = 0;
                if (mainActivity.rack.swapCamera.isChecked())
                    front = 1;
                if (facing == front)
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
        mMuxerStarted = false;
        if (null != cameraDevice) {
            cameraDevice.close();
            cameraDevice = null;
        }

        releaseEncoder();
    }

    private void prepareEncoder() {
        mBufferInfo = new MediaCodec.BufferInfo();
        ///| Todo: fixme: get width and height from camera
        mWidth = imageDimension.getWidth();
        mHeight = imageDimension.getHeight();
        mBitRate = 1000000;
        MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, mWidth, mHeight);

        // Set some properties.  Failing to specify some of these can cause the MediaCodec
        // configure() call to throw an unhelpful exception.
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        format.setInteger(MediaFormat.KEY_BIT_RATE, mBitRate);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE);
        try {
            format.setInteger(MediaFormat.KEY_ROTATION, manager.getCameraCharacteristics(cameraId).get(CameraCharacteristics.SENSOR_ORIENTATION));
        } catch (CameraAccessException e) {
            throw new RuntimeException(e);
        }
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, IFRAME_INTERVAL);
        Log.d(TAG, "format: " + format);
        Log.d(TAG, "rotation: " + format.getInteger(MediaFormat.KEY_ROTATION));

//        MediaFormat outputFormat = MediaFormat.createAudioFormat("audio/mp4a-latm", sampleRate, 1);
        MediaFormat outputFormat = new MediaFormat();
        outputFormat.setString(MediaFormat.KEY_MIME, "audio/mp4a-latm");
        outputFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
        outputFormat.setInteger(MediaFormat.KEY_BIT_RATE, 160000);
        outputFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16384);
        outputFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 1);
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
//            outputFormat.setInteger(MediaFormat.KEY_PCM_ENCODING, AudioFormat.ENCODING_PCM_32BIT);
//        }

        outputFormat.setInteger(MediaFormat.KEY_SAMPLE_RATE, 48000);

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

        // audio record start
//        audioRecord = new AudioRecord(AUDIO_SOURCE, SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT, BUFFER_SIZE_RECORDING);
//
//        if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) { // check for proper initialization
//            Log.e(TAG, "error initializing " );
//            return;
//        }

        // audio record end

        mEncoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        audioEncoder.configure(outputFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        Log.d(TAG, "[audio] prepareEncoder: configured format: " + outputFormat.toString());

        mInputSurface = mEncoder.createInputSurface();
//        audioEncoder.setCallback(new EncoderCallback(false));
        mEncoder.setCallback(new EncoderCallback(true));
        audioEncoder.setCallback(new MediaCodec.Callback() {
            @Override
            public void onInputBufferAvailable(@NonNull MediaCodec codec, int index) {
                if (mainActivity.avBuffer.size() == 0) {
                    codec.queueInputBuffer(index, 0, 0, timestamp.get(), 0);
                    return;
                }

                // the following is always true. why?
                // fixme
                try {
                    if (mainActivity.avBuffer.size() > 1) {
                        MainActivity.AVBuffer avBuffer = mainActivity.avBuffer.pop();
                        ByteBuffer buffer = codec.getInputBuffer(index);

                        for (int i = 0; i < avBuffer.size; i++) {
                            if (avBuffer.bytes[i] > 1.0f)
                                avBuffer.bytes[i] = 0.99f;
                            if (avBuffer.bytes[i] < -1.0f)
                                avBuffer.bytes[i] = -0.99f;

                            buffer.putShort((short) (avBuffer.bytes[i] * 32767.0));
                        }

                        codec.queueInputBuffer(index, 0, avBuffer.size * 2, timestamp.get(), 0);
                        avBuffer.bytes = null ;
//                        avBuffer.size = 0 ;
                        avBuffer = null ;
                    } else
                        codec.queueInputBuffer(index, 0, 0, timestamp.get(), 0);
                } catch (NoSuchElementException e) {
                    Log.e(TAG, "[audio] onInputBufferAvailable: no element even though size > 1", e);
                    codec.queueInputBuffer(index, 0, 0, timestamp.get(), 0);

                }

                /*
                float [] data = new float[BUFFER_SIZE_RECORDING/2]; // assign size so that bytes are read in in chunks inferior to AudioRecord internal buffer size
                int read = audioRecord.read(data, 0, data.length, AudioRecord.READ_NON_BLOCKING);
                ByteBuffer buffer = codec.getInputBuffer(index);
                buffer.rewind();
                if (read > 0) {
                    for (int i = 0 ; i < read; i ++)
                        buffer.putShort((short) (data [i] * 32768.0));
                }
                else
                    Log.e(TAG, "[audioRecord]: read returned " + read);
                long time = timestamp.get() ;
                codec.queueInputBuffer(index, 0, read * 2, time, 0);

                 */
            }

            @Override
            public void onOutputBufferAvailable(@NonNull MediaCodec codec, int index, @NonNull MediaCodec.BufferInfo info) {
                ByteBuffer buffer = codec.getOutputBuffer(index);
                buffer.rewind();
                if (mMuxerStarted)
                    mMuxer.writeSampleData(audioTrackIndex, buffer, info);
//                Log.d(TAG, String.format ("[audioOutput]: %d | %d", info.size, info.presentationTimeUs));
                codec.releaseOutputBuffer(index, false);
            }

            @Override
            public void onError(@NonNull MediaCodec codec, @NonNull MediaCodec.CodecException e) {

            }

            @Override
            public void onOutputFormatChanged(@NonNull MediaCodec codec, @NonNull MediaFormat format) {
                audioTrackIndex = mMuxer.addTrack(codec.getOutputFormat());
                Log.d(TAG, String.format("[audio]: added audio track [%d] with format %s",
                        audioTrackIndex, codec.getOutputFormat()));
            }
        });

//        audioRecord.startRecording();

        // Output filename.  Ideally this would use Context.getFilesDir() rather than a
        // hard-coded output directory.
        audioEncoder.start();
        mEncoder.start();

        timestamp = new Timestamp();

        SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy_HH.mm.ss");
        Date date = new Date();
        mainActivity.lastRecordedFileName =
                String.format("%s/%s.mp4",
                        mainActivity.getExternalFilesDir(Environment.DIRECTORY_MUSIC).getAbsolutePath(),
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
        int videoRecordOrientation = Integer.parseInt(mainActivity.defaultSharedPreferences.getString("camera_orientation", "0"));
        videoRecordOrientation += cameraCharacteristicsHashMap.get(cameraId).get(CameraCharacteristics.SENSOR_ORIENTATION) ;
        if (videoRecordOrientation >= 360)
            videoRecordOrientation = 0 ;
        mMuxer.setOrientationHint(videoRecordOrientation);
        Log.d(TAG, String.format ("set orientation hint: %d", cameraCharacteristicsHashMap.get(cameraId).get(CameraCharacteristics.SENSOR_ORIENTATION)));

        presentationTimeUs = System.nanoTime()/1000;

    }

    /**
     * Releases encoder resources.  May be called after partial / failed initialization.
     */
    private void releaseEncoder() {
        Log.d(TAG, "releaseEncoder: stopping encoder");
        timestamp = null ;

//        if (audioRecord != null)
//            audioRecord.stop();

        if (mMuxer != null) {
            if (mMuxerStarted)
                mMuxer.stop();
            mMuxerStarted = false;
            mMuxer.release();
            mMuxer = null;
        }

        if (mEncoder != null) {
            mEncoder.signalEndOfInputStream();
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

        mTrackIndex = -1 ;
        audioTrackIndex = -1 ;
        frame = 0 ;
        presentationTimeUs = 0 ;
        firstAudioFrame = -1 ;
    }

    class EncoderCallback extends MediaCodec.Callback {
        ByteBuffer outPutByteBuffer, inputByteBuffer, audioBuffer;
        MainActivity.AVBuffer floatBuffer;
        boolean isVideo ;
        MediaCodec.BufferInfo bufferInfo ;

        EncoderCallback (boolean video) {
            isVideo = video;
            bufferInfo = new MediaCodec.BufferInfo();

        }

        @Override
        public void onInputBufferAvailable(@NonNull MediaCodec codec, int index) {

        }

        @Override
        public void onOutputBufferAvailable(@NonNull MediaCodec codec, int index, @NonNull MediaCodec.BufferInfo info) {
            if (mTrackIndex == -1) {
                MediaFormat newFormat = mEncoder.getOutputFormat();
//                Log.d(TAG, "encoder output format changed: " + newFormat);

                // now that we have the Magic Goodies, start the muxer
//                if (mTrackIndex == -1)
                mTrackIndex = mMuxer.addTrack(newFormat);

//                if (audioTrackIndex == -1)
//                    return;

//                Log.d(TAG, "onOutputBufferAvailable: starting muxer");
//                mMuxer.start();
//                mMuxerStarted = true;
                presentationTimeUs = info.presentationTimeUs;
                timestamp.vidstart = info.presentationTimeUs;
            }

            outPutByteBuffer = codec.getOutputBuffer(index);
            info.presentationTimeUs = timestamp.get();
            if (mMuxerStarted)
                mMuxer.writeSampleData(mTrackIndex, outPutByteBuffer, info);
            codec.releaseOutputBuffer(index, false);

//            int bytesWritten = 0 ;
//            ByteBuffer buffer = ByteBuffer.allocate(info.size * 2);
//            buffer.rewind();

            /*
            while (mainActivity.avBuffer.size() > 0) {
                MainActivity.AVBuffer avBuffer = mainActivity.avBuffer.pop();
                bytesWritten += avBuffer.size ;
                for (int i = 0; i < avBuffer.size; i++)
                    buffer.putChar(avBuffer.bytes[i]);
                if (bytesWritten + avBuffer.size > info.size)
                    break ;
            }

            bufferInfo.set(0, bytesWritten, info.presentationTimeUs, 0);
            mMuxer.writeSampleData(audioTrackIndex, buffer, bufferInfo);
            Log.d(TAG, String.format ("[muxer]: (%d) %d {%d:%d}", info.size, bufferInfo.size, (int) buffer .get(0), (int) buffer.get(bufferInfo.size)));

             */
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

    void startRecording () {
        Log.d(TAG, "startRecording: ");
        mMuxer.start();
        mMuxerStarted = true;
    }

    void stopRecording () {
        Log.d(TAG, "stopRecording: ");
        mMuxerStarted = false;
        mMuxer.stop();
    }
}
