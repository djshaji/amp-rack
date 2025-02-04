//
// Created by djshaji on 1/29/25.
//

#ifndef AMP_RACK_ATOM_H
#define AMP_RACK_ATOM_H


#include "lv2/lv2plug.in/ns/ext/patch/patch.h"
#include <lv2/atom/atom.h>
#include <lv2/atom/util.h>
#include <lv2/atom/forge.h>
#include <lv2/core/lv2.h>
#include <lv2/urid/urid.h>
#include <lv2/atom/forge.h>
#include <cstring>
#include <stdexcept>
#include <stdint.h>
#include "logging_macros.h"
#include "zix/ring.h"

typedef struct {
  uint32_t index;
  uint32_t protocol;
  unsigned long size;
  // Followed immediately by size bytes of data
} ControlChange;

// Define custom URIs for our host
struct HostURIs {
    LV2_URID atom_Sequence;
    LV2_URID atom_Object;
    LV2_URID atom_String;
    LV2_URID atom_Path;
    LV2_URID atom_URID;
    LV2_URID atom_eventTransfer;
    LV2_URID patch_Set;
    LV2_URID patch_property;
//    LV2_URID xlv2_model;
    LV2_URID patch_value;
    LV2_URID filename_URI;
};

class AmpAtom {
private:
    HostURIs uris;
    LV2_Atom_Forge forge;

    // Buffer for atom sequence
    uint8_t * buffer;

public:
    LV2_URID_Map* urid_map = nullptr;
    ZixRing * ring = zix_ring_new(NULL, 10000);
    AmpAtom(LV2_URID_Map *map, int _size);

    void sendFilenameToPlugin(LV2_Atom_Sequence *output_port, const char *filename);

    LV2_Atom *createFilenameMessage(const char *filename);

    void mapURIs();

    void release();

    void send_filename_to_plugin(LV2_URID_Map *map, const char *filename, uint8_t *atom_port_buffer,
                                 size_t atom_port_buffer_size);

    void setControl(LV2_Atom_Sequence * control, char *path);

    int write_control_change(const void *const header,
                             const uint32_t header_size, const void *const body,
                             const uint32_t body_size);

    int write_event( const uint32_t port_index, const uint32_t size,
                    const LV2_URID type, const void *const body);

    void son_of_a(LV2_Atom_Sequence *control, const char *filename);

//    void write_control(LV2_Atom_Sequence *control, int, int32_t, const char *filename);

    void resetAtom(LV2_Atom_Sequence *control, int portSize);

    bool has_file_path(LV2_Atom_Sequence *port);

    void
    write_control(LV2_Atom_Sequence *control, int portSize, uint32_t uri, const char *filename);
};


#endif //AMP_RACK_ATOM_H
