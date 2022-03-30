#include <cstdlib>
#include <cstddef>
#include "opus.h"
#include "opus_projection.h"

OpusProjectionEncoder *opus_projection_ambisonics_encoder_create(
        opus_int32 Fs, int channels, int mapping_family, int *streams,
        int *coupled_streams, int application, int *error)
{
    int size;
    int ret;
    OpusProjectionEncoder *st;

    /* Allocate space for the projection encoder. */
    size = opus_projection_ambisonics_encoder_get_size(channels, mapping_family);
    if (!size) {
        if (error)
            *error = OPUS_ALLOC_FAIL;
        return NULL;
    }
    st = (OpusProjectionEncoder *)malloc(size);
    if (!st)
    {
        if (error)
            *error = OPUS_ALLOC_FAIL;
        return NULL;
    }

    /* Initialize projection encoder with provided settings. */
    ret = opus_projection_ambisonics_encoder_init(st, Fs, channels,
                                                  mapping_family, streams, coupled_streams, application);
    if (ret != OPUS_OK)
    {
        free(st);
        st = NULL;
    }
    if (error)
        *error = ret;
    return st;
}
