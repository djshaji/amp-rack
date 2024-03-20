//
// Created by djshaji on 3/19/24.
//

#ifndef AMP_RACK_MP4_H
#define AMP_RACK_MP4_H

#include <stdlib.h>
#include <string>
#include "bento4/Ap4.h"
#include "bento4/Ap4AdtsParser.h"
#include "logging_macros.h"

class MP4 {
public:
    std::string lastRecordedFile, tmp ;
    FILE * tmpfile = noll;

    MP4(std::string _filename) {
        lastRecordedFile = _filename ;
        lastRecordedFile = "/sdcard/Android/data/com.shajikhan.ladspa.amprack/files/Music/test.mp4";
        tmp = lastRecordedFile + ".aac" ;
        tmpfile = fopen (tmp.c_str(), "wb");
        LOGD("[mp4] opened tmp file %s", tmp.c_str());
    }

    ~MP4() {
//        remove (tmp.c_str());
    }

    void MakeDsi(unsigned int sampling_frequency_index, unsigned int channel_configuration,
                 unsigned char *dsi);

    void aacToMP4();

    void write(unsigned char *data, int nframes);
};


#endif //AMP_RACK_MP4_H
