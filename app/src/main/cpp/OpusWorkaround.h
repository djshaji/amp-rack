#ifndef OPUS_WORKAROUND_H
#define OPUS_WORKAROUND_H

OpusProjectionEncoder *opus_projection_ambisonics_encoder_create(
        opus_int32 Fs, int channels, int mapping_family, int *streams,
        int *coupled_streams, int application, int *error) ;


#endif //OPUS_WORKAROUND_H
