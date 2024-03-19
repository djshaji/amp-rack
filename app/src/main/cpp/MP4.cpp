//
// Created by djshaji on 3/19/24.
//

#include "MP4.h"

void MP4::openFile (std::string _filename) {
    filename = _filename ;
    result = AP4_FileByteStream::Create(filename.c_str(), AP4_FileByteStream::STREAM_MODE_WRITE, output);

    if (AP4_FAILED(result)) {
        LOGE("ERROR: cannot open output (%s) %d\n", filename.c_str(), result);
        return ;
    }

    sample_table = new AP4_SyntheticSampleTable();


}