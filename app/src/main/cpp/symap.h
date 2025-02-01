// Copyright 2011-2022 David Robillard <d@drobilla.net>
// SPDX-License-Identifier: ISC

/**
   @file symap.h API for Symap, a basic symbol map (string interner).

   Particularly useful for implementing LV2 URI mapping.

   @see <a href="http://lv2plug.in/ns/ext/urid">LV2 URID</a>
*/

#ifndef SYMAP_H
#define SYMAP_H

#include "zix/attributes.h"

#include <stdint.h>
#include "logging_macros.h"
#ifdef __cplusplus
extern "C" {
#endif

typedef struct SymapImpl Symap;

/// Create a new symbol map
ZIX_MALLOC_FUNC
Symap *
symap_new(void);

/// Free a symbol map
void
symap_free(Symap *map);

/// Map a string to a symbol if it is already mapped, otherwise return 0
ZIX_PURE_FUNC
uint32_t
symap_try_map(const Symap *map, const char *sym);

/// Map a string to a symbol
uint32_t
symap_map(Symap *map, const char *sym);

/// Unmap a symbol back to a string if possible, otherwise return NULL
ZIX_PURE_FUNC
const char *
symap_unmap(const Symap *map, uint32_t id);

#ifdef __cplusplus
}
#endif

#endif // SYMAP_H
