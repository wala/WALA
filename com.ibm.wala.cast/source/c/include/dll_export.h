#ifndef CAST_DLL_EXPORT
#define CAST_DLL_EXPORT

#if _WIN32
#ifdef BUILD_CAST_DLL
#define DLLEXPORT __declspec(dllexport)
#else // !BUILD_CAST_DLL
#define DLLEXPORT __declspec(dllimport)
#endif // !BUILD_CAST_DLL
#else // !_WIN32
#define DLLEXPORT
#endif // !_WIN32

#endif // !CAST_DLL_EXPORT
