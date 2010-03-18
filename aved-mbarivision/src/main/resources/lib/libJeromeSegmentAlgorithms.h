/*
 * MATLAB Compiler: 4.4 (R2006a)
 * Date: Thu Feb 15 10:00:15 2007
 * Arguments: "-B" "macro_default" "-W" "lib:libJeromeSegmentAlgorithms" "-T"
 * "link:lib" "-d" "/usr/lib/aved" "-g" "-G" "applyRectangleMask"
 * "applyRectangleImageMask" "homomorphicCanny" "adaptiveThresholding"
 * "backgroundCanny" "graphCut/extractForegroundBW"
 * "graphCut/getBackgroundMean" "readTmp" 
 */

#ifndef __libJeromeSegmentAlgorithms_h
#define __libJeromeSegmentAlgorithms_h 1

#if defined(__cplusplus) && !defined(mclmcr_h) && defined(__linux__)
#  pragma implementation "mclmcr.h"
#endif
#include "mclmcr.h"
#ifdef __cplusplus
extern "C" {
#endif

#if defined(__SUNPRO_CC)
/* Solaris shared libraries use __global, rather than mapfiles
 * to define the API exported from a shared library. __global is
 * only necessary when building the library -- files including
 * this header file to use the library do not need the __global
 * declaration; hence the EXPORTING_<library> logic.
 */

#ifdef EXPORTING_libJeromeSegmentAlgorithms
#define PUBLIC_libJeromeSegmentAlgorithms_C_API __global
#else
#define PUBLIC_libJeromeSegmentAlgorithms_C_API /* No import statement needed. */
#endif

#define LIB_libJeromeSegmentAlgorithms_C_API PUBLIC_libJeromeSegmentAlgorithms_C_API

#elif defined(_HPUX_SOURCE)

#ifdef EXPORTING_libJeromeSegmentAlgorithms
#define PUBLIC_libJeromeSegmentAlgorithms_C_API __declspec(dllexport)
#else
#define PUBLIC_libJeromeSegmentAlgorithms_C_API __declspec(dllimport)
#endif

#define LIB_libJeromeSegmentAlgorithms_C_API PUBLIC_libJeromeSegmentAlgorithms_C_API


#else

#define LIB_libJeromeSegmentAlgorithms_C_API

#endif

/* This symbol is defined in shared libraries. Define it here
 * (to nothing) in case this isn't a shared library. 
 */
#ifndef LIB_libJeromeSegmentAlgorithms_C_API 
#define LIB_libJeromeSegmentAlgorithms_C_API /* No special import/export declaration */
#endif

extern LIB_libJeromeSegmentAlgorithms_C_API 
bool MW_CALL_CONV libJeromeSegmentAlgorithmsInitializeWithHandlers(mclOutputHandlerFcn error_handler,
                                                                   mclOutputHandlerFcn print_handler);

extern LIB_libJeromeSegmentAlgorithms_C_API 
bool MW_CALL_CONV libJeromeSegmentAlgorithmsInitialize(void);

extern LIB_libJeromeSegmentAlgorithms_C_API 
void MW_CALL_CONV libJeromeSegmentAlgorithmsTerminate(void);


extern LIB_libJeromeSegmentAlgorithms_C_API 
bool MW_CALL_CONV mlxApplyRectangleMask(int nlhs, mxArray *plhs[],
                                        int nrhs, mxArray *prhs[]);

extern LIB_libJeromeSegmentAlgorithms_C_API 
bool MW_CALL_CONV mlxApplyRectangleImageMask(int nlhs, mxArray *plhs[],
                                             int nrhs, mxArray *prhs[]);

extern LIB_libJeromeSegmentAlgorithms_C_API 
bool MW_CALL_CONV mlxHomomorphicCanny(int nlhs, mxArray *plhs[],
                                      int nrhs, mxArray *prhs[]);

extern LIB_libJeromeSegmentAlgorithms_C_API 
bool MW_CALL_CONV mlxAdaptiveThresholding(int nlhs, mxArray *plhs[],
                                          int nrhs, mxArray *prhs[]);

extern LIB_libJeromeSegmentAlgorithms_C_API 
bool MW_CALL_CONV mlxBackgroundCanny(int nlhs, mxArray *plhs[],
                                     int nrhs, mxArray *prhs[]);

extern LIB_libJeromeSegmentAlgorithms_C_API 
bool MW_CALL_CONV mlxExtractForegroundBW(int nlhs, mxArray *plhs[],
                                         int nrhs, mxArray *prhs[]);

extern LIB_libJeromeSegmentAlgorithms_C_API 
bool MW_CALL_CONV mlxGetBackgroundMean(int nlhs, mxArray *plhs[],
                                       int nrhs, mxArray *prhs[]);

extern LIB_libJeromeSegmentAlgorithms_C_API 
bool MW_CALL_CONV mlxReadTmp(int nlhs, mxArray *plhs[],
                             int nrhs, mxArray *prhs[]);


extern LIB_libJeromeSegmentAlgorithms_C_API bool MW_CALL_CONV mlfApplyRectangleMask(mxArray* input_path, mxArray* x, mxArray* y, mxArray* height, mxArray* width);

extern LIB_libJeromeSegmentAlgorithms_C_API bool MW_CALL_CONV mlfApplyRectangleImageMask(mxArray* input_path, mxArray* mask_path);

extern LIB_libJeromeSegmentAlgorithms_C_API bool MW_CALL_CONV mlfHomomorphicCanny(mxArray* input_path, mxArray* type);

extern LIB_libJeromeSegmentAlgorithms_C_API bool MW_CALL_CONV mlfAdaptiveThresholding(mxArray* input_path, mxArray* type);

extern LIB_libJeromeSegmentAlgorithms_C_API bool MW_CALL_CONV mlfBackgroundCanny(mxArray* input_path, mxArray* type);

extern LIB_libJeromeSegmentAlgorithms_C_API bool MW_CALL_CONV mlfExtractForegroundBW(mxArray* image, mxArray* backgroundMean);

extern LIB_libJeromeSegmentAlgorithms_C_API bool MW_CALL_CONV mlfGetBackgroundMean(mxArray* folder, mxArray* input, mxArray* background);

extern LIB_libJeromeSegmentAlgorithms_C_API bool MW_CALL_CONV mlfReadTmp(mxArray* index);

#ifdef __cplusplus
}
#endif

#endif
