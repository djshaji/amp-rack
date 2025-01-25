// Copyright 2021-2022 David Robillard <d@drobilla.net>
// SPDX-License-Identifier: ISC

#ifndef ZIX_ATTRIBUTES_H
#define ZIX_ATTRIBUTES_H

/**
   @defgroup zix_attributes Attributes
   @ingroup zix_utilities
   @{
*/

// Public declaration scope
#ifdef __cplusplus
#  define ZIX_BEGIN_DECLS extern "C" {
#  define ZIX_END_DECLS }
#else
#  define ZIX_BEGIN_DECLS ///< Begin public API definitions
#  define ZIX_END_DECLS   ///< End public API definitions
#endif

// ZIX_API must be used to decorate things in the public API
#ifndef ZIX_API
#  if defined(_WIN32) && !defined(ZIX_STATIC) && defined(ZIX_INTERNAL)
#    define ZIX_API __declspec(dllexport)
#  elif defined(_WIN32) && !defined(ZIX_STATIC)
#    define ZIX_API __declspec(dllimport)
#  elif defined(__GNUC__)
#    define ZIX_API __attribute__((visibility("default")))
#  else
#    define ZIX_API
#  endif
#endif

// GCC function attributes
#ifdef __GNUC__
#  define ZIX_ALWAYS_INLINE_FUNC __attribute__((always_inline))
#  define ZIX_PURE_FUNC __attribute__((pure))
#  define ZIX_CONST_FUNC __attribute__((const))
#  define ZIX_MALLOC_FUNC __attribute__((malloc))
#  define ZIX_NODISCARD __attribute__((warn_unused_result))
#else
#  define ZIX_ALWAYS_INLINE_FUNC ///< Should absolutely always be inlined
#  define ZIX_PURE_FUNC          ///< Only reads memory
#  define ZIX_CONST_FUNC         ///< Only reads its parameters
#  define ZIX_MALLOC_FUNC        ///< Allocates memory with no pointers in it
#  define ZIX_NODISCARD          ///< Returns a value that must be used
#endif

/// A pure function in the public API that only reads memory
#define ZIX_PURE_API ZIX_API ZIX_PURE_FUNC ZIX_NODISCARD

/// A const function in the public API that is pure and only reads parameters
#define ZIX_CONST_API ZIX_API ZIX_CONST_FUNC ZIX_NODISCARD

/// A malloc function in the public API that returns allocated memory
#define ZIX_MALLOC_API ZIX_API ZIX_MALLOC_FUNC ZIX_NODISCARD

// Printf-like format functions
#ifdef __GNUC__
#  define ZIX_LOG_FUNC(fmt, arg1) __attribute__((format(printf, fmt, arg1)))
#else
#  define ZIX_LOG_FUNC(fmt, arg1) ///< A function with printf-like parameters
#endif

// Unused parameter macro to suppresses warnings and make it impossible to use
#if defined(__cplusplus)
#  define ZIX_UNUSED(name)
#elif defined(__GNUC__) || defined(__clang__)
#  define ZIX_UNUSED(name) name##_unused __attribute__((__unused__))
#elif defined(_MSC_VER)
#  define ZIX_UNUSED(name) __pragma(warning(suppress : 4100)) name
#else
#  define ZIX_UNUSED(name) name ///< An unused parameter
#endif

// Clang nullability annotations
#if defined(__clang__) && __clang_major__ >= 7
#  define ZIX_NONNULL _Nonnull
#  define ZIX_NULLABLE _Nullable
#  define ZIX_ALLOCATED _Null_unspecified
#  define ZIX_UNSPECIFIED _Null_unspecified
#else
#  define ZIX_NONNULL     ///< A non-null pointer
#  define ZIX_NULLABLE    ///< A nullable pointer
#  define ZIX_ALLOCATED   ///< An allocated (possibly null) pointer
#  define ZIX_UNSPECIFIED ///< A pointer with unspecified nullability
#endif

/**
   @}
*/

#endif /* ZIX_ATTRIBUTES_H */
