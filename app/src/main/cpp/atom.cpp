//
// Created by djshaji on 1/29/25.
//

#include "atom.h"

void AmpAtom::release () {
    if (! buffer)
        return;

    free (buffer);
    buffer = nullptr;
}

void AmpAtom::mapURIs() {
    // Map all required URIs
    IN
    if (urid_map == nullptr) {
        LOGE ("urid_map is null! owowow");
        abort();
    }

    uris.atom_Sequence = urid_map->map(urid_map->handle, LV2_ATOM__Sequence);
    uris.atom_String = urid_map->map(urid_map->handle, LV2_ATOM__String);
    uris.atom_Path = urid_map->map(urid_map->handle, LV2_ATOM__Path);
    uris.atom_URID = urid_map->map(urid_map->handle, LV2_ATOM__URID);
    uris.atom_eventTransfer = urid_map->map(urid_map->handle, LV2_ATOM__eventTransfer);
    uris.patch_Set = urid_map->map(urid_map->handle, LV2_PATCH__Set);
    uris.patch_property = urid_map->map(urid_map->handle, LV2_PATCH__property);
    uris.patch_value = urid_map->map(urid_map->handle, LV2_PATCH__value);
    uris.filename_URI = urid_map->map(urid_map->handle,
                                      "http://lv2plugin.com/ns/filename");
    OUT
}

LV2_Atom* AmpAtom::createFilenameMessage(const char* filename) {
    IN
    // Set up forge buffer
    lv2_atom_forge_set_buffer(&forge, buffer, sizeof(buffer));

    // Start a sequence
    LV2_Atom_Forge_Frame seq_frame;
    lv2_atom_forge_sequence_head(&forge, &seq_frame, 0);

    // Start a new frame for the Set message
    LV2_Atom_Forge_Frame frame;
    lv2_atom_forge_frame_time(&forge, 0);
    lv2_atom_forge_object(&forge, &frame, 0, uris.patch_Set);

    // Add property URI
    lv2_atom_forge_key(&forge, uris.patch_property);
    lv2_atom_forge_urid(&forge, uris.filename_URI);

    // Add the filename value
    lv2_atom_forge_key(&forge, uris.patch_value);
    lv2_atom_forge_path(&forge, filename, strlen(filename));

    // Close frames
    lv2_atom_forge_pop(&forge, &frame);
    lv2_atom_forge_pop(&forge, &seq_frame);

    OUT
    return (LV2_Atom*)buffer;
}

void AmpAtom::sendFilenameToPlugin(LV2_Atom_Sequence* output_port, const char* filename) {
    IN
    if (!output_port || !filename) {
        throw std::runtime_error("Invalid parameters");
    }

    // Create the message
    LV2_Atom* msg = createFilenameMessage(filename);

    // Copy message to output port
    const uint32_t space = output_port->atom.size;
    if (msg->size > space) {
        throw std::runtime_error("Output buffer too small");
    }

    memcpy(output_port, msg, msg->size + sizeof(LV2_Atom));
    OUT
}

AmpAtom::AmpAtom(LV2_URID_Map* map, int _size) {
        IN
        if (!map) {
            throw std::runtime_error("URID map not provided");
        }

        urid_map = map ;

        // Initialize URIs
        mapURIs();

        // Initialize forge
        lv2_atom_forge_init(&forge, urid_map);
        buffer = static_cast<uint8_t *>(malloc(_size));
        OUT
}

void atom_send_message () {
    // Assuming we have these from the host
    LV2_URID_Map* map = nullptr;  // Should be provided by host
    LV2_Atom_Sequence* output_port = nullptr;  // Should be provided by host

    try {
        // Create host instance
        AmpAtom host(map, 1024);

        // Send a filename to the plugin
        const char* filename = "/path/to/your/file.wav";
        host.sendFilenameToPlugin(output_port, filename);

    } catch (const std::exception& e) {
        fprintf(stderr, "Error: %s\n", e.what());
    }
}

