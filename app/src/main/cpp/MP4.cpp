//
// Created by djshaji on 3/19/24.
//

#include "MP4.h"

void
MP4::MakeDsi(unsigned int sampling_frequency_index, unsigned int channel_configuration, unsigned char* dsi)
{
    unsigned int object_type = 2; // AAC LC by default
    dsi[0] = (object_type<<3) | (sampling_frequency_index>>1);
    dsi[1] = ((sampling_frequency_index&1)<<7) | (channel_configuration<<3);
}

void MP4::openFile (std::string _filename) {
    filename = _filename ;
    result = AP4_FileByteStream::Create(filename.c_str(), AP4_FileByteStream::STREAM_MODE_WRITE, output);

    if (AP4_FAILED(result)) {
        LOGE("ERROR: cannot open output (%s) %d\n", filename.c_str(), result);
        return ;
    }

    AP4_AacFrame frame;
    result = parser.FindFrame(frame);
    if (AP4_SUCCEEDED(result)) {
        LOGI("AAC frame [%06d]: size = %d, %d kHz, %d ch\n",
                  sample_count,
                  frame.m_Info.m_FrameLength,
                  frame.m_Info.m_SamplingFrequency,
                  frame.m_Info.m_ChannelConfiguration);
    } else {
        HERE LOGE("[mp4] failed to find frame");
        return;
    }

    initialized = true;
    sample_table = new AP4_SyntheticSampleTable();
    AP4_DataBuffer dsi;
    unsigned char aac_dsi[2];
    MakeDsi(frame.m_Info.m_SamplingFrequencyIndex, frame.m_Info.m_ChannelConfiguration, aac_dsi);
    dsi.SetData(aac_dsi, 2);

    AP4_MpegAudioSampleDescription* sample_description =
            new AP4_MpegAudioSampleDescription(
                    AP4_OTI_MPEG4_AUDIO,   // object type
                    frame.m_Info.m_SamplingFrequency,
                    16,                    // sample size
                    frame.m_Info.m_ChannelConfiguration,
                    &dsi,                  // decoder info
                    6144,                  // buffer size
                    256000,                // max bitrate
                    128000);               // average bitrate
    sample_description_index = sample_table->GetSampleDescriptionCount();
    sample_table->AddSampleDescription(sample_description);
    sample_rate = frame.m_Info.m_SamplingFrequency;

    AP4_MemoryByteStream* sample_data = new AP4_MemoryByteStream(frame.m_Info.m_FrameLength);
    frame.m_Source->ReadBytes(sample_data->UseData(), frame.m_Info.m_FrameLength);
    sample_table->AddSample(*sample_data, 0, frame.m_Info.m_FrameLength, 1024, sample_description_index, 0, 0, true);
    sample_data->Release();
    sample_count++;

    movie = new AP4_Movie();
    track = new AP4_Track(AP4_Track::TYPE_AUDIO,
                          sample_table,
                          0,     // track id
                          sample_rate,       // movie time scale
                          sample_count*1024, // track duration
                          sample_rate,       // media time scale
                          sample_count*1024, // media duration
                          "eng", // language
                          0, 0); // width, height

    movie->AddTrack(track);
    file = new AP4_File(movie);

    AP4_UI32 compatible_brands[2] = {
            AP4_FILE_BRAND_ISOM,
            AP4_FILE_BRAND_MP42
    };

    file->SetFileType(AP4_FILE_BRAND_M4A_, 0, compatible_brands, 2);

}

void MP4::writeFile ()
