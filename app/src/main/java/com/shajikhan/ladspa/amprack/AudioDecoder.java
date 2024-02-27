package com.shajikhan.ladspa.amprack;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.HashMap;

public class AudioDecoder {
    MainActivity mainActivity ;
    long MAX_BUFFER = 512 ;
    String TAG = "Moffin Decoder yeah" ;
    int sampleRate = 48000 ;
    AudioDecoder (MainActivity _MainActivity) {
        mainActivity = _MainActivity;
    }
    public static byte[] readFileByBytes(File file) {

        byte[] tempBuf = new byte[(int) file.length()];
        int byteRead;
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        try {
            BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(file));
            while ((byteRead = bufferedInputStream.read(tempBuf)) != -1) {
                byteArrayOutputStream.write(tempBuf, 0, byteRead);
            }
            bufferedInputStream.close();
            return byteArrayOutputStream.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    float [] decode (Uri uri, String mime, int _sampleRate) throws IOException {
        if (uri == null) {
            Log.e(TAG, "decode: null uri passed", null);
        }
        ParcelFileDescriptor pfd =
                mainActivity.getContentResolver().
                        openFileDescriptor(uri, "r");

        FileDescriptor fileDescriptor = pfd.getFileDescriptor();
        float [] samples = decode(fileDescriptor, mime, _sampleRate);
        pfd.close();
        return samples;
    }

    float[] decode (FileDescriptor fileDescriptor, String mimeType, int _sampleRate) throws IOException {
        MediaExtractor extractor;
        float [] decoded = new float[0];
        int decodedIdx = 0;

        MediaMetadataRetriever metadataRetriever = new MediaMetadataRetriever();

        MediaCodec codec;
        ByteBuffer[] codecInputBuffers;
        ByteBuffer[] codecOutputBuffers;
        extractor = new MediaExtractor();
        metadataRetriever.setDataSource(fileDescriptor);

        HashMap <Integer, String> metadata = new HashMap();
        int dataKeys [] = {
                MediaMetadataRetriever.METADATA_KEY_MIMETYPE,
                MediaMetadataRetriever.METADATA_KEY_NUM_TRACKS,
                MediaMetadataRetriever.METADATA_KEY_BITS_PER_SAMPLE,
                MediaMetadataRetriever.METADATA_KEY_BITRATE,
                MediaMetadataRetriever.METADATA_KEY_SAMPLERATE
        } ;

        for (int key: dataKeys) {
            metadata.put(key, metadataRetriever.extractMetadata(key));
            Log.d(TAG, "decode: metadata" +
                    String.format("%d: %s", key, metadataRetriever.extractMetadata(key)));
        }

        sampleRate = Integer.parseInt(metadata.get(MediaMetadataRetriever.METADATA_KEY_SAMPLERATE));
        extractor.setDataSource(fileDescriptor);
        float scaleFactor = 48000 / sampleRate ;
        MediaFormat format = extractor.getTrackFormat(0);
        int channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
        String mime = format.getString(MediaFormat.KEY_MIME);
        Log.d(TAG, String.format ("mimetype: %s", mimeType));
        if (_sampleRate != -1) {
            codec = MediaCodec.createEncoderByType(mimeType);
            Log.d(TAG, String.format ("converting to: %s [%d]", mimeType, sampleRate));
        } else {
            codec = MediaCodec.createDecoderByType(mime);
            Log.d(TAG, String.format ("converting to: %s [%d]", mime, sampleRate));
        }

        Log.i(TAG, "decode: detected format " + mime);
        if (_sampleRate != -1)
            format.setInteger(MediaFormat.KEY_SAMPLE_RATE, sampleRate);

        codec.configure(format, null /* surface */, null /* crypto */, 0 /* flags */);
        codec.start();
        codecInputBuffers = codec.getInputBuffers();
        codecOutputBuffers = codec.getOutputBuffers();
        extractor.selectTrack(0);
        // start decoding
        final long kTimeOutUs = 5000;
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        boolean sawInputEOS = false;
        boolean sawOutputEOS = false;
        int noOutputCounter = 0;
        boolean reconfigure = true ;
        long bufferCount = 0 ;
        while (!sawOutputEOS && noOutputCounter < 50) {
            bufferCount ++ ;
//            Log.d(TAG, String.format ("buffer count %d: maxBuffer %d", bufferCount, MAX_BUFFER));
            if (bufferCount > MAX_BUFFER) {
                sawInputEOS = true ;
                sawOutputEOS = true ;
                break ;
            }

            noOutputCounter++;
            if (!sawInputEOS) {
                int inputBufIndex = codec.dequeueInputBuffer(kTimeOutUs);
                if (inputBufIndex >= 0) {
                    ByteBuffer dstBuf = codecInputBuffers[inputBufIndex];
                    int sampleSize =
                            extractor.readSampleData(dstBuf, 0 /* offset */);
                    long presentationTimeUs = 0;
                    if (sampleSize < 0) {
                        Log.d(TAG, "saw input EOS.");
                        sawInputEOS = true;
                        sampleSize = 0;
                    } else {
                        presentationTimeUs = extractor.getSampleTime();
                    }
                    codec.queueInputBuffer(
                            inputBufIndex,
                            0 /* offset */,
                            sampleSize,
                            presentationTimeUs,
                            sawInputEOS ? MediaCodec.BUFFER_FLAG_END_OF_STREAM : 0);
                    if (!sawInputEOS) {
                        extractor.advance();
                    }
                }
            }
            int res = codec.dequeueOutputBuffer(info, kTimeOutUs);
            if (res >= 0) {
                //Log.d(TAG, "got frame, size " + info.size + "/" + info.presentationTimeUs);
                if (info.size > 0) {
                    noOutputCounter = 0;
                }

                if (info.size > 0 && reconfigure) {
                    // once we've gotten some data out of the decoder, reconfigure it again
                    reconfigure = false;
                    extractor.seekTo(0, MediaExtractor.SEEK_TO_NEXT_SYNC);
                    sawInputEOS = false;
                    codec.stop();
                    codec.configure(format, null /* surface */, null /* crypto */, 0 /* flags */);
                    codec.start();
                    codecInputBuffers = codec.getInputBuffers();
                    codecOutputBuffers = codec.getOutputBuffers();
                    continue;
                }

                int outputBufIndex = res;
                ByteBuffer buf = codecOutputBuffers[outputBufIndex];
                if (decodedIdx + (info.size / 2) >= decoded.length) {
                    decoded = Arrays.copyOf(decoded, decodedIdx + (info.size / 2));
                }
                for (int i = 0; i < info.size; i += 2) {
                    decoded[decodedIdx++] = (float) (buf.getShort(i) / 32768.0);
                }
                codec.releaseOutputBuffer(outputBufIndex, false /* render */);
                if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    Log.d(TAG, "saw output EOS.");
                    sawOutputEOS = true;
                }
            } else if (res == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                codecOutputBuffers = codec.getOutputBuffers();
                Log.d(TAG, "output buffers have changed.");
            } else if (res == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                MediaFormat oformat = codec.getOutputFormat();
                Log.d(TAG, "output format has changed to " + oformat);
//                codec.stop();
//                codec.configure(format, null /* surface */, null /* crypto */, 0 /* flags */);
//                codec.start();
            } else {
                Log.d(TAG, "dequeueOutputBuffer returned " + res);
            }
        }
        codec.stop();
        codec.release();
        Log.i(TAG, "decode: returning " +
                String.format("%d audio samples", decoded.length));

        if (channels > 1) {
            Log.d(TAG, String.format ("channels: %d, converting to mono", channels));
            float[] mono = new float [(decoded.length/channels) + 1];
            for (int i = 0, k = 0 ; i < decoded.length ; i += channels, k ++) {
                mono [k] = decoded [i];
            }

            decoded = mono;
        }

        if (sampleRate == 48000)
            return decoded;

        ByteBuffer bb = ByteBuffer.allocateDirect(decoded.length * 4);
        ByteBuffer bb2 = ByteBuffer.allocateDirect(decoded.length * 10);
        FloatBuffer floatBuffer = bb.asFloatBuffer(), resampled = bb2.asFloatBuffer();
        floatBuffer.put(decoded);
        floatBuffer.position(0);

        Resampler resampler = new Resampler(true,0.1,30);
        boolean result = resampler.process((double)48000.0/sampleRate,floatBuffer,true,resampled);
        float [] res = new float[resampled.limit()];
//                resampled.position(0);
        for (int i = 0 ; i < resampled.limit(); i ++) {
            res [i] = resampled.get(i);
        }

        return res;
    }
}
