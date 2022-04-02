#ifndef FILE_WRITER_H
#define FILE_WRITER_H

#define JC_MAX(a,b) (((a)>(b))?(a):(b))
#define JC_MIN(a,b) (((a)<(b))?(a):(b))

#define ALIGN_UP(value,alignment) (((uintptr_t)value + alignment - 1) & -alignment)
#define ALIGN_UP_DOUBLE(p) ALIGN_UP(p,sizeof(double)) // Using double because double should always be very large.

#include <string>
#include "logging_macros.h"
#include "sndfile.h"
#include "opus.h"
#include "opus_multistream.h"
#include "opus_projection.h"
#include "opusenc.h"
#include "lame.h"

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
} buffer_t;

typedef enum  {
    WAV = 0,
    OPUS = 1,
    MP3 = 2
} FileType;

#define MAX_PACKET_SIZE (3*1276)

class FileWriter {
    SF_INFO sf_info ;
    int bitRate = 64000 ;
    static OggOpusComments *comments;
    static lame_t lame ;

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
    static int disk_write(void *data, size_t frames);

    std::string filename ;
    void setBufferSize(int bufferSize);

    void setSampleRate(int sampleRate);

    void setFileName(std::string name);

    void openFile();

    void closeFile();

    void startRecording();

    static enum vringbuffer_receiver_callback_return_t disk_callback(vringbuffer_t *vrb,bool first_time,void *element){
        static bool printed_receive_message=false;
        buffer_t *buffer=(buffer_t*)element;

        if (first_time==true) {
            return static_cast<vringbuffer_receiver_callback_return_t>(true);
        }

        disk_write(buffer->data,buffer->pos);
        return VRB_CALLBACK_USED_BUFFER;
    }

//    static int disk_write(SNDFILE *soundfile, void *data, size_t frames);

//    static SNDFILE * soundfile ;

    void stopRecording();

    static SNDFILE *soundfile;

    static int process(float nframes, void *arg);

    static void process_fill_buffers(void *data, int samples);

    static bool process_new_current_buffer(int frames_left);

    static buffer_t *current_buffer;
    static void send_buffer_to_disk_thread(buffer_t *buffer);

    static void process_fill_buffer(float **in, buffer_t *buffer, int i, int end);

    static float buffers_to_seconds(int buffers);

    static float blocks_to_seconds(int blocks);

    void setFileType(int fType);
};


#endif //FILE_WRITER_H