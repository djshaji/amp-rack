#include <cstdlib>
#include <cstring>
#include <chrono>
#include "FileWriter.h"

bool FileWriter:: useStaticBuffer = false ;
staticBuffer_t FileWriter::buffers [1025] ;
/* the reason this has to be less than buffer size is that arrays start at zero
 * why this worked at all on some devices amazes me
 */
int FileWriter::MAX_STATIC_BUFFER  = 1024;
int FileWriter::bufferUsed = 0;
int FileWriter::unreported_overruns = 0 ;
int FileWriter::total_overruns = 0;
SNDFILE * FileWriter::soundfile = NULL;
buffer_t *FileWriter::current_buffer;
buffer_t *FileWriter::bg_buffer;
int FileWriter::num_channels = 1 ;
int FileWriter::block_size = 384 ;
float FileWriter::min_buffer_time = -1.0f,
        FileWriter::max_buffer_time = 40.0f ;
int FileWriter::jack_samplerate ;
int FileWriter::buffer_size_in_bytes;
bool FileWriter::ready = false ;
vringbuffer_t * FileWriter::vringbuffer ;
FileType FileWriter::fileType = MP3 ;
OpusEncoder *FileWriter::encoder ;
opus_int16 FileWriter::opusIn[960 * 2];
int FileWriter::opusRead = 0;
unsigned char FileWriter::opusOut[MAX_PACKET_SIZE];
FILE * FileWriter::outputFile = NULL;
OggOpusEnc * FileWriter:: oggOpusEnc = NULL;
OggOpusComments *FileWriter::comments = NULL;
lame_t FileWriter::lame = NULL ;

static char * extensions [] = {
        ".wav",
        ".ogg",
        ".mp3"
} ;

int FileWriter::autoincrease_callback(vringbuffer_t *vrb, bool first_call, int reading_size, int writing_size) {
    if(buffers_to_seconds(writing_size) < min_buffer_time) {
        return 2; // autoincrease_callback is called approx. at every block. So it should not be necessary to return a value higher than 2. Returning a very low number might also theoretically put a lower constant strain on the memory bus, thus theoretically lower the chance of xruns.
    }

    return 0 ;
}

int64_t FileWriter::seconds_to_frames(float seconds){
    return (int64_t) (((long double)seconds)*((long double)jack_samplerate));
}


float FileWriter::frames_to_seconds(int frames){
    return ((float)frames)/jack_samplerate;
}

// round up.
int FileWriter::seconds_to_blocks(float seconds){
    return (int)ceilf((seconds*jack_samplerate/(float)block_size));
}

void* FileWriter:: my_calloc(size_t size1,size_t size2){
    size_t size = size1*size2;
    void*  ret  = malloc(size);
    if(ret==NULL){
        fprintf(stderr,"\nOut of memory. Try a smaller buffer.\n");
        return NULL; }
    memset(ret,0,size);
    return ret;
}

// same return value as seconds_to_blocks
int FileWriter::seconds_to_buffers(float seconds){
    return seconds_to_blocks(seconds);
}

FileWriter::~FileWriter () {
    free (empty_buffer);
    vringbuffer_delete(vringbuffer);
}
FileWriter::FileWriter () {
    IN

    OUT
}

void FileWriter::openFile () {
    IN
    LOGD("opening file [%s] using sample rate: [%d]\tchannels: [%d]", filename.c_str(), jack_samplerate, num_channels);
    memset(&sf_info,0,sizeof(SF_INFO));
    memset (&opusIn, 0, 960*2);
    memset (&opusOut, 0, 3*1276);

    sf_info.channels = num_channels ;
    // why the below?
//    sf_info.samplerate = jack_samplerate * num_channels ;
    sf_info.samplerate = jack_samplerate ;
    sf_info.format = SF_FORMAT_WAV ;
    sf_info.format |= SF_FORMAT_FLOAT ;

    if(sf_format_check(&sf_info)==0){
        LOGE ("\nFileformat not supported by libsndfile. Try other options.\n");
        return ;
    }

    if (fileType == WAV) {
        LOGD("[%s] %d %d %d", __PRETTY_FUNCTION__, sf_info.samplerate, sf_info.channels,
             sf_info.format);
        FileWriter::soundfile = sf_open(filename.c_str(), SFM_WRITE, &sf_info);
        if (soundfile == NULL) { // || ai==10){
            HERE
            LOGF ("\nCan not open sndfile \"%s\" for output (%s)\n", filename.c_str(),
                  sf_strerror(NULL));
        } else {
            LOGD("[%s] Opened file %s", __PRETTY_FUNCTION__, filename.c_str());
        }
    }

    else if (fileType == OPUS) {
        int err;
        encoder = opus_encoder_create(jack_samplerate, num_channels, OPUS_APPLICATION_AUDIO, &err);
        if (err<0) {
            HERE LOGF("failed to create an encoder: %s\n", opus_strerror(err));
        }

        err = opus_encoder_ctl(encoder, OPUS_SET_BITRATE(bitRate));
        if (err<0) {
            LOGF("failed to set bitrate: %s\n", opus_strerror(err));
        }

//        outputFile = fopen(filename.c_str(), "wb");
        outputFile = NULL ;
        int error = 0 ;
        LOGD("going to create opus encoder with filename: %s", filename.c_str());
        comments = ope_comments_create() ;
        ope_comments_add(comments, "TITLE", "AmpRack Demo");

        oggOpusEnc = ope_encoder_create_file(filename.c_str(), comments, jack_samplerate, num_channels, 0, &error) ;
        if (error != NULL) {
            HERE LOGF("cannot create encoder: %s", ope_strerror(error));
        }
        err = ope_encoder_ctl(oggOpusEnc, OPUS_SET_BITRATE(bitRate));

    } else if (fileType == MP3) {
        LOGD("init lame encoder");
        lame = lame_init();
        lame_set_in_samplerate(lame, jack_samplerate);
        lame_set_VBR(lame, vbr_default);
        lame_set_out_samplerate(lame, jack_samplerate);
        lame_set_num_channels(lame, num_channels);
        /*
        lame_set_brate(lame, 64);
         */
        lame_set_preset(lame, MEDIUM);
        int retval = lame_init_params(lame);
        if (retval < 0) {
            HERE LOGF("Unable to intialize lame parameters: %d", retval);
        }
        outputFile = fopen(filename.c_str(), "wb");
    }

    OUT
}

void FileWriter::setChannels (int channels) {
    num_channels = channels ;
}

void FileWriter::closeFile () {
    IN
    if (fileType == MP3) {
        unsigned char       *mp3buf  ;
        mp3buf = (unsigned char *) malloc (8192*3);
        lame_encode_flush(lame, mp3buf, 8192*3);
        fwrite (mp3buf, 8192*3, sizeof (unsigned char), outputFile);
        free (mp3buf);
        lame_close(lame) ;
        fclose(outputFile);
        outputFile = NULL;
        lame = NULL ;
    }

    else if (fileType == WAV && outputFile) {
        fclose(outputFile);
        outputFile = NULL;
    }

    else if (fileType == OPUS )  {
        if (oggOpusEnc) {
            ope_encoder_drain(oggOpusEnc);
            ope_encoder_destroy(oggOpusEnc);
            ope_comments_destroy(comments);
            oggOpusEnc = NULL ;
            comments = NULL ;
        }
    }

    if (soundfile) {
        sf_close(soundfile);
        soundfile = NULL;
    }

    OUT
}

int FileWriter::disk_write_callback(float *arg,size_t nframes) {
    if (bufferUsed < MAX_STATIC_BUFFER) {
        for (int i = 0; i < nframes; i++) {
            buffers[bufferUsed].data [i] = arg[i];
        }

        buffers[bufferUsed].pos = nframes;
        bufferUsed ++ ;
        return 0;

    } else {
        disk_write(arg, nframes);
        bufferUsed = 0;
    }

    return 0 ;
}

int FileWriter::disk_write(float *data,size_t frames) {

    /*
    LOGD("----------| %d  |-----------", std::chrono::system_clock::now()) ;
    for (int i = 0 ; i < frames ; i ++) {
        LOGD("%f\t", data [i]) ;
    }
     return 0 ;
     */



//    IN
    if (fileType == MP3) {
        void * mp3_buffer =  malloc ((frames * 1.25) + 7200);
        int write = lame_encode_buffer_ieee_float(lame, data, NULL, frames, (unsigned char *) mp3_buffer, (frames * 1.25) + 7200);
        if (write < 0) {
            LOGF("unable to encode mp3 stream: %d", write);
        } else {
            fwrite(mp3_buffer, write, 1, outputFile);
        }

        free(mp3_buffer);
//        OUT
        return 0 ;
    }

    if (fileType== OPUS) {
        ope_encoder_write_float(oggOpusEnc, data, frames);
//        OUT
        return 1;
    }

    if (fileType == 987) {
        if (opusRead < 960) {
            float * f = static_cast<float *>(data);
            for (int x = 0 ; x < frames ; x ++) {
                opusIn[opusRead] = (opus_int )(size_t) f ;
                opusRead ++ ;
                *f ++ ;
            }

//            OUT
            return 1 ;

        } else {

            int nbBytes = opus_encode(encoder, opusIn, 960, opusOut, MAX_PACKET_SIZE);
            if (nbBytes<0) {
                LOGF("encode failed: %s\n", opus_strerror(nbBytes));
            } else {
                fwrite(opusOut, sizeof (unsigned char ), opusRead, outputFile) ;
                opusRead = 0 ;
//                OUT
                return 1 ;
            }


        }
    }

    if((size_t)sf_writef_float(FileWriter::soundfile, data,frames) != frames){
        LOGF("Error. Can not write sndfile (%s)\n",
                      sf_strerror(FileWriter::soundfile)
        );
//        OUT
        return 0;
    }

//    OUT
    return 1;
}

void FileWriter::startRecording () {
    IN
    openFile();
    vringbuffer_set_receiver_callback(vringbuffer,disk_callback);
    ready = true ;
    OUT
}

void FileWriter::stopRecording () {
    IN
    // because we use a large buffer, there can be samples in the buffer which have not been written yet
    if (useStaticBuffer)
        vringbuffer_return_writing(vringbuffer,buffers);

    vringbuffer_stop_callbacks(vringbuffer);
    closeFile();
    LOGD("recording stopped: %d buffer underruns", total_overruns);
    OUT
}

void FileWriter::setBufferSize (int bufferSize) {
    IN
    block_size = bufferSize ;
    buffer_size_in_bytes = ALIGN_UP_DOUBLE(sizeof(buffer_t) + block_size*num_channels*sizeof(float ));

    LOGD("setting buffer size: %d from block size: %d", buffer_size_in_bytes, block_size);

    vringbuffer = vringbuffer_create(JC_MAX(4,seconds_to_buffers(min_buffer_time)),
                                     JC_MAX(4,seconds_to_buffers(max_buffer_time)),
                                     (size_t) buffer_size_in_bytes);

    if(vringbuffer == NULL){
        HERE LOGF ("Unable to create ringbuffer!") ;
        OUT
        return ;
    }

    /// TODO: Free this memory!
    vringbuffer_set_autoincrease_callback(vringbuffer,autoincrease_callback,0);
    current_buffer = static_cast<buffer_t *>(vringbuffer_get_writing(vringbuffer));
    bg_buffer = static_cast<buffer_t *>(calloc(1, sizeof(buffer_t)));
//    HERE LOGD("using buffer size %d", bufferSize);
    bg_buffer->data = static_cast<float *>(malloc(bufferSize));
    empty_buffer   = static_cast<float *>(my_calloc(sizeof(float), block_size * num_channels));
//    for (int i = 0 ; i < 32 ; i ++) {
//        buffers[i] = static_cast<buffer_t *>(malloc(sizeof(buffer_t)));
//        buffers [i] -> data = static_cast<float *>(malloc(block_size));
//    }

    OUT
}

void FileWriter::setSampleRate (int sampleRate) {
    IN
    if (sampleRate == 0) {
        HERE LOGE("sample rate passed as 0!") ;
        jack_samplerate = 48000 ;
    } else
        jack_samplerate = sampleRate ;
}

void FileWriter::setFileName (std::string name) {
    IN
    filename = std::string (name) + std::string (extensions [fileType]) ;
    LOGD("[%s] filename set to %s", __PRETTY_FUNCTION__ , name.c_str());
    OUT
}

void FileWriter::process_fill_buffer(float *in[], buffer_t *buffer, int i, int end){
    float *data=buffer->data;
    int pos=buffer->pos*num_channels;

    buffer->pos=pos/num_channels;
}

bool FileWriter::process_new_current_buffer(int frames_left){
    current_buffer=(buffer_t*)vringbuffer_get_writing(vringbuffer);
    if(current_buffer==NULL){
        total_overruns++;
        unreported_overruns += frames_left;
        return false;
    }

    current_buffer->pos=0;
    return true;
}

void FileWriter::send_buffer_to_disk_thread(buffer_t *buffer){
    buffer->overruns = unreported_overruns;
    vringbuffer_return_writing(vringbuffer,buffer);
    unreported_overruns = 0;
}

void FileWriter::process_fill_buffers(void *data, int samples){
    if(current_buffer==NULL && process_new_current_buffer(samples)==false) {
        LOGD("no buffer and no samples!") ;
        return;
    }

    // fixme: why do I always take void* as argument type and cast it to float?
    current_buffer->data = (float*) data ;
    current_buffer->pos = samples ;
    send_buffer_to_disk_thread(current_buffer);
    current_buffer = NULL;
    return ;

    float *in[num_channels];
    int i = 0 ;
    while(i<samples){
        int size=JC_MIN(samples - i,
                        block_size - current_buffer->pos
        );

        process_fill_buffer(in,current_buffer,i,i+size);

        LOGD("%d %d %d %d", i, samples, size, current_buffer->pos) ;
        i+=size;

        if(current_buffer->pos == block_size){
            send_buffer_to_disk_thread(current_buffer);
            if(process_new_current_buffer(samples-i)==false)
                return;
        } else {
            LOGD("pos != block size") ;
        }

    }
}

int FileWriter::process(int nframes, const float *arg) {
//    IN
//    LOGD("file writer process");
    if (!ready) {
        HERE LOGW("not ready, return");
//        OUT
        return 0 ;
    }
//    process_fill_buffers(arg, nframes);
//  simplified the following 26-06-22
//  don't know the consequences yet
//    if ((buffer_t*)vringbuffer_get_writing(vringbuffer) == NULL)
//        return 0;

    if (useStaticBuffer) {
//        LOGD("use static buffer");
        if (bufferUsed < MAX_STATIC_BUFFER) {
//            LOGD("buffer used: %d", bufferUsed);
//            LOGD("bufferUsed: %d, nframes: %d",  bufferUsed, nframes) ;
            for (int i = 0; i < nframes; i++) {
                buffers[bufferUsed].data [i] = arg[i];
//                LOGD("buffers[bufferUsed].data [%d] = arg[%d];", i, i);
            }

            buffers[bufferUsed].pos = nframes;
            bufferUsed ++ ;
//            OUT
            return 0;

        } else {
//            LOGD("bufferUsed = MAX_STATIC_BUFFER, vringbuffer_return_writing") ;
            vringbuffer_return_writing(vringbuffer,buffers);
            bufferUsed = 0;
        }
    } else {
//        bg_buffer->data = (float *) arg;

//        LOGD("frames: %d", nframes);

        for (int i = 0 ; i < nframes ; i ++) {
            if (i >= block_size) {
                HERE LOGF("more samples than we can handle! [%d]", nframes) ;
                break ;
            }

            if (arg [i] < -10.0)
                bg_buffer->data[i] = -10.0 ;
            else if (arg [i] > 10.0)
                bg_buffer->data[i] = 10.0 ;
            else
                bg_buffer->data[i] = arg [i] ;
        }

        bg_buffer->pos = nframes;
//        bg_buffer->pos = nframes;
//        current_buffer->data = (float *) arg;
//        current_buffer->pos = nframes;
        vringbuffer_return_writing(vringbuffer,bg_buffer);
    }



    /// does the following do ANYTHING?
    vringbuffer_trigger_autoincrease_callback(vringbuffer);
//    OUT
    return 0 ;
}

float FileWriter::buffers_to_seconds(int buffers){
    return blocks_to_seconds(buffers);
}

float FileWriter::blocks_to_seconds(int blocks){
    return (float)blocks*(float)block_size/jack_samplerate;
}

void FileWriter::setFileType (int fType) {
    fileType = static_cast<FileType>(fType);
}

