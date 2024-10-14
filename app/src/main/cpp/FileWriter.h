#ifndef FILE_WRITER_H
#define FILE_WRITER_H

#define JC_MAX(a,b) (((a)>(b))?(a):(b))
#define JC_MIN(a,b) (((a)<(b))?(a):(b))

#define ALIGN_UP(value,alignment) (((uintptr_t)value + alignment - 1) & -alignment)
#define ALIGN_UP_DOUBLE(p) ALIGN_UP(p,sizeof(double)) // Using double because double should always be very large.

#include <string>
#include <ctime>
#include <thread>
#include "logging_macros.h"

#ifdef __linux__
#include "sndfile.h"
#endif

#include "opus.h"
#include "opus_multistream.h"
#include "opus_projection.h"
#include "opusenc.h"
#include "lame.h"
#include "LockFreeQueue.h"

// TIL you can do this here also
#ifdef __cplusplus
extern "C" {
#include "vringbuffer.h"
}
#endif


typedef struct buffer_t{
    int overruns;
    int pos;
//    float data[];
    float *data;
    int size ;
} buffer_t;

typedef struct staticBuffer_t{
    int overruns;
    float pos;
    float data[512];
//    float *data;
} staticBuffer_t;

typedef enum  {
    WAV = 0,
    OPUS = 1,
    MP3 = 2
} FileType;

#define MAX_PACKET_SIZE (3*1276)

class FileWriter {
    // for removing pops and clicks
    static int total_overruns;
    static long disk_writes ;
    static long processed ;
    int total_xruns = 0;
    static int buffer_write_index  ;
    static int unreported_overruns;

    #ifdef __linux__
    SF_INFO sf_info ;
    #endif
    
    public: int bitRate = 64000 ;
    static OggOpusComments *comments;
    static lame_t lame ;

#ifndef LOCK_FREE_SIZE
#define LOCK_FREE_SIZE 4096
#endif
    static LockFreeQueue<buffer_t *, LOCK_FREE_SIZE> lockFreeQueue ;

    static int num_channels;
    static OpusEncoder *encoder;
    static OggOpusEnc * oggOpusEnc ;
    static opus_int16 opusIn[960 * 2];
    static unsigned char opusOut[MAX_PACKET_SIZE];
    static int opusRead ;

    static FILE * outputFile ;//for formats other than sndfile

    static bool ready  ;
    static FileType fileType;
    bool buffer_interleaved = true ;
    static vringbuffer_t * vringbuffer ;
    static int jack_samplerate ;
    static int buffer_size_in_bytes ;
    static float  min_buffer_time ,
        max_buffer_time ;

    float *empty_buffer;
    static std::thread fileWriteThread ;

    static int block_size;
    int default_block_size = 384 ;
    static int
    autoincrease_callback(vringbuffer_t *vrb, bool first_call, int reading_size, int writing_size);

    int64_t seconds_to_frames(float seconds);

    float frames_to_seconds(int frames);

    int seconds_to_blocks(float seconds);

    int seconds_to_buffers(float seconds);

    static void *my_calloc(size_t size1, size_t size2);


public:
    FileWriter ();
    ~FileWriter ();
    static int disk_write(AudioBuffer * buffer);
    int disk_write_callback(float *data, size_t frames);

    static void * mp3_buffer ;
    int buffer_index = 0 ;
    std::string filename ;
    void setBufferSize(int bufferSize);

    void setSampleRate(int sampleRate);

    void setFileName(std::string name);

    void openFile();

    void closeFile();

    void startRecording();

    static enum vringbuffer_receiver_callback_return_t disk_callback(vringbuffer_t *vrb,bool first_time,void *element){
//        IN
//        staticBuffer_t * sbuffer  = (staticBuffer_t * ) element ;
        buffer_t *buffer=(buffer_t*)element;
        buffer_t ** doubleBuffer = static_cast<buffer_t **>(element);

        if (first_time==true) {
//            LOGD( "first time out");
//            OUT
            return static_cast<vringbuffer_receiver_callback_return_t>(true);
        }

//        disk_write(buffer->data, buffer->pos);
        return VRB_CALLBACK_USED_BUFFER;
        if (!useStaticBuffer) {
            if (bufferUsed < MAX_STATIC_BUFFER) {
                for (int i = 0 ; i < buffer->pos ; i ++)
                    buffers[bufferUsed].data [i]  = buffer->data [i];

                buffers[bufferUsed].pos = buffer->pos;
                bufferUsed ++ ;
            } else {
//                LOGD("buffer used: %d", bufferUsed);
//                for (int i = 0 ; i < bufferUsed ; i ++)
//                    disk_write(buffers[i].data, buffers [i].pos);
//
//                disk_write(buffer->data, buffer->pos);
                bufferUsed = 0;
            }
        }
        else {
//            LOGD("buffer used: %d", bufferUsed);
//            for (int i = 0 ; i < bufferUsed; i ++) {
//                disk_write(doubleBuffer [i] -> data,doubleBuffer [i] -> pos);
//            }

            bufferUsed = 0 ;
        }
//        OUT
        return VRB_CALLBACK_USED_BUFFER;
    }

//    static int disk_write(SNDFILE *soundfile, void *data, size_t frames);

//    static SNDFILE * soundfile ;

    void stopRecording();

    #ifdef __linux__
    static SNDFILE *soundfile;
    #endif

    static int process(int nframes, const float *arg);

    static void process_fill_buffers(void *data, int samples);

    static bool process_new_current_buffer(int frames_left);

    static bool useStaticBuffer  ;
    static buffer_t *current_buffer;
    static buffer_t *bg_buffer;
    static int MAX_STATIC_BUFFER  ;
    static staticBuffer_t buffers [1025] ;
    static int bufferUsed ;
    static void send_buffer_to_disk_thread(buffer_t *buffer);

    static void process_fill_buffer(float **in, buffer_t *buffer, int i, int end);

    static float buffers_to_seconds(int buffers);

    static float blocks_to_seconds(int blocks);

    void setFileType(int fType);

    void setChannels(int channels);

    void writeLoop();

    void setLamePreset(int preset);
};


#endif //FILE_WRITER_H
