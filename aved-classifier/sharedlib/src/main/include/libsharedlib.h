/*
 * MATLAB Compiler: 4.10 (R2009a)
 * Date: Wed Jun 16 17:33:39 2010
 * Arguments: "-B" "macro_default" "-v" "-W" "lib:libsharedlib" "-T" "link:lib"
 * "-I" "/Users/dcline/avedac/aved-classifier/sharedlib/src/main/native/matlab"
 * "-I"
 * "/Users/dcline/avedac/aved-classifier/sharedlib/src/main/native/matlab/netlab
 * " "-g" "-G" "run_tests_ui" "collect_ui" "collect_tests" "collect_class"
 * "assign_class" "test_class" "train_classes_ui" 
 */

#ifndef __libsharedlib_h
#define __libsharedlib_h 1

#if defined(__cplusplus) && !defined(mclmcrrt_h) && defined(__linux__)
#  pragma implementation "mclmcrrt.h"
#endif
#include "mclmcrrt.h"
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

#ifdef EXPORTING_libsharedlib
#define PUBLIC_libsharedlib_C_API __global
#else
#define PUBLIC_libsharedlib_C_API /* No import statement needed. */
#endif

#define LIB_libsharedlib_C_API PUBLIC_libsharedlib_C_API

#elif defined(_HPUX_SOURCE)

#ifdef EXPORTING_libsharedlib
#define PUBLIC_libsharedlib_C_API __declspec(dllexport)
#else
#define PUBLIC_libsharedlib_C_API __declspec(dllimport)
#endif

#define LIB_libsharedlib_C_API PUBLIC_libsharedlib_C_API


#else

#define LIB_libsharedlib_C_API

#endif

/* This symbol is defined in shared libraries. Define it here
 * (to nothing) in case this isn't a shared library. 
 */
#ifndef LIB_libsharedlib_C_API 
#define LIB_libsharedlib_C_API /* No special import/export declaration */
#endif

extern LIB_libsharedlib_C_API 
bool MW_CALL_CONV libsharedlibInitializeWithHandlers(mclOutputHandlerFcn error_handler,
                                                     mclOutputHandlerFcn print_handler);

extern LIB_libsharedlib_C_API 
bool MW_CALL_CONV libsharedlibInitialize(void);

extern LIB_libsharedlib_C_API 
void MW_CALL_CONV libsharedlibTerminate(void);



extern LIB_libsharedlib_C_API 
void MW_CALL_CONV libsharedlibPrintStackTrace(void);


extern LIB_libsharedlib_C_API 
bool MW_CALL_CONV mlxRun_tests_ui(int nlhs, mxArray *plhs[],
                                  int nrhs, mxArray *prhs[]);

extern LIB_libsharedlib_C_API 
bool MW_CALL_CONV mlxCollect_ui(int nlhs, mxArray *plhs[],
                                int nrhs, mxArray *prhs[]);

extern LIB_libsharedlib_C_API 
bool MW_CALL_CONV mlxCollect_tests(int nlhs, mxArray *plhs[],
                                   int nrhs, mxArray *prhs[]);

extern LIB_libsharedlib_C_API 
bool MW_CALL_CONV mlxCollect_class(int nlhs, mxArray *plhs[],
                                   int nrhs, mxArray *prhs[]);

extern LIB_libsharedlib_C_API 
bool MW_CALL_CONV mlxAssign_class(int nlhs, mxArray *plhs[],
                                  int nrhs, mxArray *prhs[]);

extern LIB_libsharedlib_C_API 
bool MW_CALL_CONV mlxTest_class(int nlhs, mxArray *plhs[],
                                int nrhs, mxArray *prhs[]);

extern LIB_libsharedlib_C_API 
bool MW_CALL_CONV mlxTrain_classes_ui(int nlhs, mxArray *plhs[],
                                      int nrhs, mxArray *prhs[]);

extern LIB_libsharedlib_C_API 
long MW_CALL_CONV libsharedlibGetMcrID() ;



extern LIB_libsharedlib_C_API bool MW_CALL_CONV mlfRun_tests_ui(int nargout
                                                                , mxArray** eventids
                                                                , mxArray** majoritywinnerindex
                                                                , mxArray** probabilitywinnerindex
                                                                , mxArray** probability
                                                                , mxArray* kill
                                                                , mxArray* dbroot
                                                                , mxArray* color_space
                                                                , mxArray* testclassname
                                                                , mxArray* trainingalias
                                                                , mxArray* threshold);

extern LIB_libsharedlib_C_API bool MW_CALL_CONV mlfCollect_ui(int nargout
                                                              , mxArray** filenames
                                                              , mxArray** store
                                                              , mxArray* kill
                                                              , mxArray* rawdirct
                                                              , mxArray* sqdirct
                                                              , mxArray* classname
                                                              , mxArray* dbroot
                                                              , mxArray* color_space
                                                              , mxArray* varsclassname
                                                              , mxArray* description);

extern LIB_libsharedlib_C_API bool MW_CALL_CONV mlfCollect_tests(int nargout
                                                                 , mxArray** testmfiles
                                                                 , mxArray* kill
                                                                 , mxArray* testdir
                                                                 , mxArray* dbroot
                                                                 , mxArray* color_space);

extern LIB_libsharedlib_C_API bool MW_CALL_CONV mlfCollect_class(int nargout
                                                                 , mxArray** classfilemetadata
                                                                 , mxArray* kill
                                                                 , mxArray* dbroot
                                                                 , mxArray* classdir);

extern LIB_libsharedlib_C_API bool MW_CALL_CONV mlfAssign_class(int nargout
                                                                , mxArray** classindex
                                                                , mxArray** storeprob
                                                                , mxArray* kill
                                                                , mxArray* file
                                                                , mxArray* thresh
                                                                , mxArray* trainclasses);

extern LIB_libsharedlib_C_API bool MW_CALL_CONV mlfTest_class(int nargout
                                                              , mxArray** testfiles
                                                              , mxArray** finalclassindex
                                                              , mxArray** finalstoreprob
                                                              , mxArray* kill
                                                              , mxArray* dbroot
                                                              , mxArray* testclassname
                                                              , mxArray* trainingalias
                                                              , mxArray* threshold
                                                              , mxArray* colorspace);

extern LIB_libsharedlib_C_API bool MW_CALL_CONV mlfTrain_classes_ui(mxArray* kill
                                                                    , mxArray* dbroot
                                                                    , mxArray* color_space
                                                                    , mxArray* classalias
                                                                    , mxArray* classnames
                                                                    , mxArray* description);

#ifdef __cplusplus
}
#endif

#endif
