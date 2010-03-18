/*
 * MATLAB Compiler: 4.4 (R2006a)
 * Date: Tue Aug 11 11:20:54 2009
 * Arguments: "-B" "macro_default" "-W" "lib:libAVEDClassifierMatlab" "-T"
 * "link:lib" "-d"
 * "/home/aved/aved/aved-classifier/sharedlib/src/main/native/matlab/lib" "-I"
 * "./" "-g" "-G" "collect" "collect_class" "assign_class" "test_class"
 * "init_classifier" "train_classes" 
 */

#ifndef __libAVEDClassifierMatlab_h
#define __libAVEDClassifierMatlab_h 1

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

#ifdef EXPORTING_libAVEDClassifierMatlab
#define PUBLIC_libAVEDClassifierMatlab_C_API __global
#else
#define PUBLIC_libAVEDClassifierMatlab_C_API /* No import statement needed. */
#endif

#define LIB_libAVEDClassifierMatlab_C_API PUBLIC_libAVEDClassifierMatlab_C_API

#elif defined(_HPUX_SOURCE)

#ifdef EXPORTING_libAVEDClassifierMatlab
#define PUBLIC_libAVEDClassifierMatlab_C_API __declspec(dllexport)
#else
#define PUBLIC_libAVEDClassifierMatlab_C_API __declspec(dllimport)
#endif

#define LIB_libAVEDClassifierMatlab_C_API PUBLIC_libAVEDClassifierMatlab_C_API


#else

#define LIB_libAVEDClassifierMatlab_C_API

#endif

/* This symbol is defined in shared libraries. Define it here
 * (to nothing) in case this isn't a shared library. 
 */
#ifndef LIB_libAVEDClassifierMatlab_C_API 
#define LIB_libAVEDClassifierMatlab_C_API /* No special import/export declaration */
#endif

extern LIB_libAVEDClassifierMatlab_C_API 
bool MW_CALL_CONV libAVEDClassifierMatlabInitializeWithHandlers(mclOutputHandlerFcn error_handler,
                                                                mclOutputHandlerFcn print_handler);

extern LIB_libAVEDClassifierMatlab_C_API 
bool MW_CALL_CONV libAVEDClassifierMatlabInitialize(void);

extern LIB_libAVEDClassifierMatlab_C_API 
void MW_CALL_CONV libAVEDClassifierMatlabTerminate(void);


extern LIB_libAVEDClassifierMatlab_C_API 
bool MW_CALL_CONV mlxCollect(int nlhs, mxArray *plhs[],
                             int nrhs, mxArray *prhs[]);

extern LIB_libAVEDClassifierMatlab_C_API 
bool MW_CALL_CONV mlxCollect_class(int nlhs, mxArray *plhs[],
                                   int nrhs, mxArray *prhs[]);

extern LIB_libAVEDClassifierMatlab_C_API 
bool MW_CALL_CONV mlxAssign_class(int nlhs, mxArray *plhs[],
                                  int nrhs, mxArray *prhs[]);

extern LIB_libAVEDClassifierMatlab_C_API 
bool MW_CALL_CONV mlxTest_class(int nlhs, mxArray *plhs[],
                                int nrhs, mxArray *prhs[]);

extern LIB_libAVEDClassifierMatlab_C_API 
bool MW_CALL_CONV mlxInit_classifier(int nlhs, mxArray *plhs[],
                                     int nrhs, mxArray *prhs[]);

extern LIB_libAVEDClassifierMatlab_C_API 
bool MW_CALL_CONV mlxTrain_classes(int nlhs, mxArray *plhs[],
                                   int nrhs, mxArray *prhs[]);


extern LIB_libAVEDClassifierMatlab_C_API bool MW_CALL_CONV mlfCollect(int nargout
                                                                      , mxArray** classfilemetadata
                                                                      , mxArray** classdata
                                                                      , mxArray* dbroot
                                                                      , mxArray* dirct_or_filelist
                                                                      , mxArray* classname);

extern LIB_libAVEDClassifierMatlab_C_API bool MW_CALL_CONV mlfCollect_class(mxArray* dbroot
                                                                            , mxArray* classdir);

extern LIB_libAVEDClassifierMatlab_C_API bool MW_CALL_CONV mlfAssign_class(int nargout
                                                                           , mxArray** classindex
                                                                           , mxArray** storeprob
                                                                           , mxArray* file
                                                                           , mxArray* thresh
                                                                           , mxArray* trainclasses);

extern LIB_libAVEDClassifierMatlab_C_API bool MW_CALL_CONV mlfTest_class(int nargout
                                                                         , mxArray** classindex
                                                                         , mxArray** storeprob
                                                                         , mxArray* dbroot
                                                                         , mxArray* testclassname
                                                                         , mxArray* trainingclasses
                                                                         , mxArray* threshold);

extern LIB_libAVEDClassifierMatlab_C_API bool MW_CALL_CONV mlfInit_classifier(mxArray* dbroot
                                                                              , mxArray* trainclasses);

extern LIB_libAVEDClassifierMatlab_C_API bool MW_CALL_CONV mlfTrain_classes(int nargout
                                                                            , mxArray** ris
                                                                            , mxArray* classnames
                                                                            , mxArray* class_mfiles
                                                                            , mxArray* trainingdatafile);

#ifdef __cplusplus
}
#endif

#endif
