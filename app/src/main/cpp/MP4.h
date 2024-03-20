//
// Created by djshaji on 3/19/24.
//

#ifndef AMP_RACK_MP4_H
#define AMP_RACK_MP4_H

#include <string>
#include "bento4/Ap4.h"
#include "bento4/Ap4AdtsParser.h"
#include "logging_macros.h"

class MP4 {
    std::string filename ;
    AP4_Result result;
    AP4_ByteStream* input = NULL;
    AP4_ByteStream* output = NULL;
    AP4_SyntheticSampleTable* sample_table;
    AP4_AdtsParser parser;
    bool           initialized = false;
    unsigned int   sample_description_index = 0;

    // read from the input, feed, and get AAC frames
    AP4_UI32     sample_rate = 0;
    AP4_Cardinal sample_count = 0;
    bool eos = false;

    AP4_Movie* movie = nullptr ;
    AP4_Track* track = nullptr ;
    AP4_File* file = nullptr ;

    void openFile(std::string _filename);

    void MakeDsi(unsigned int sampling_frequency_index, unsigned int channel_configuration,
                 unsigned char *dsi);

    void writeFile(unsigned char *data);
};


#endif //AMP_RACK_MP4_H
