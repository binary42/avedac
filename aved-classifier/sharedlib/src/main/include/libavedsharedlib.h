/*
 * MATLAB Compiler: 4.17 (R2012a)
 * Date: Fri Jun  8 17:20:35 2012
 * Arguments: "-B" "macro_default" "-v" "-W" "lib:libavedsharedlib" "-T"
 * "link:lib" "-I"
 * "/Users/dcline/NetBeansProjects/avedac/aved-classifier/sharedlib/src/main/nat
 * ive/matlab" "-I"
 * "/Users/dcline/NetBeansProjects/avedac/aved-classifier/sharedlib/src/main/nat
 * ive/matlab/netlab" "-g" "-G" "run_tests_ui" "collect" "collect_ui"
 * "collect_tests" "collect_class" "assign_class" "test_class"
 * "train_classes_ui" 
 */

#ifndef __libavedsharedlib_h
#define __libavedsharedlib_h 1

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

#ifdef EXPORTING_libavedsharedlib
#define PUBLIC_libavedsharedlib_C_API __global
#else
#define PUBLIC_libavedsharedlib_C_API /* No import statement needed. */
#endif

#define LIB_libavedsharedlib_C_API PUBLIC_libavedsharedlib_C_API

#elif defined(_HPUX_SOURCE)

#ifdef EXPORTING_libavedsharedlib
#define PUBLIC_libavedsharedlib_C_API __declspec(dllexport)
#else
#define PUBLIC_libavedsharedlib_C_API __declspec(dllimport)
#endif

#define LIB_libavedsharedlib_C_API PUBLIC_libavedsharedlib_C_API


#else

#define LIB_libavedsharedlib_C_API

#endif

/* This symbol is defined in shared libraries. Define it here
 * (to nothing) in case this isn't a shared library. 
 */
#ifndef LIB_libavedsharedlib_C_API 
#define LIB_libavedsharedlib_C_API /* No special import/export declaration */
#endif

extern LIB_libavedsharedlib_C_API 
bool MW_CALL_CONV libavedsharedlibInitializeWithHandlers(
       mclOutputHandlerFcn error_handler, 
       mclOutputHandlerFcn print_handler);

extern LIB_libavedsharedlib_C_API 
bool MW_CALL_CONV libavedsharedlibInitialize(void);

extern LIB_libavedsharedlib_C_API 
void MW_CALL_CONV libavedsharedlibTerminate(void);



extern LIB_libavedsharedlib_C_API 
void MW_CALL_CONV libavedsharedlibPrintStackTrace(void);

extern LIB_libavedsharedlib_C_API 
bool MW_CALL_CONV mlxRun_tests_ui(int nlhs, mxArray *plhs[], int nrhs, mxArray *prhs[]);

extern LIB_libavedsharedlib_C_API 
bool MW_CALL_CONV mlxCollect(int nlhs, mxArray *plhs[], int nrhs, mxArray *prhs[]);

extern LIB_libavedsharedlib_C_API 
bool MW_CALL_CONV mlxCollect_ui(int nlhs, mxArray *plhs[], int nrhs, mxArray *prhs[]);

extern LIB_libavedsharedlib_C_API 
bool MW_CALL_CONV mlxCollect_tests(int nlhs, mxArray *plhs[], int nrhs, mxArray *prhs[]);

extern LIB_libavedsharedlib_C_API 
bool MW_CALL_CONV mlxCollect_class(int nlhs, mxArray *plhs[], int nrhs, mxArray *prhs[]);

extern LIB_libavedsharedlib_C_API 
bool MW_CALL_CONV mlxAssign_class(int nlhs, mxArray *plhs[], int nrhs, mxArray *prhs[]);

extern LIB_libavedsharedlib_C_API 
bool MW_CALL_CONV mlxTest_class(int nlhs, mxArray *plhs[], int nrhs, mxArray *prhs[]);

extern LIB_libavedsharedlib_C_API 
bool MW_CALL_CONV mlxTrain_classes_ui(int nlhs, mxArray *plhs[], int nrhs, mxArray 
                                      *prhs[]);



extern LIB_libavedsharedlib_C_API bool MW_CALL_CONV mlfRun_tests_ui(int nargout, mxArray** eventids, mxArray** majoritywinnerindex, mxArray** probabilitywinnerindex, mxArray** maxwinnerindex, mxArray** probability, mxArray* kill, mxArray* dbroot, mxArray* color_space, mxArray* testclassname, mxArray* trainingalias, mxArray* threshold);

extern LIB_libavedsharedlib_C_API bool MW_CALL_CONV mlfCollect(int nargout, mxArray** filenames, mxArray** resolfiles, mxArray** datafiles, mxArray* kill, mxArray* dirct, mxArray* pattern, mxArray* dbroot, mxArray* color_space);

extern LIB_libavedsharedlib_C_API bool MW_CALL_CONV mlfCollect_ui(int nargout, mxArray** filenames, mxArray* kill, mxArray* rawdirct, mxArray* sqdirct, mxArray* classname, mxArray* dbroot, mxArray* color_space, mxArray* predictedclassname, mxArray* description);

extern LIB_libavedsharedlib_C_API bool MW_CALL_CONV mlfCollect_tests(int nargout, mxArray** testmfiles, mxArray* kill, mxArray* testdir, mxArray* dbroot, mxArray* color_space);

extern LIB_libavedsharedlib_C_API bool MW_CALL_CONV mlfCollect_class(int nargout, mxArray** classfilemetadata, mxArray* kill, mxArray* dbroot, mxArray* classdir);

extern LIB_libavedsharedlib_C_API bool MW_CALL_CONV mlfAssign_class(int nargout, mxArray** classindex, mxArray** storeprob, mxArray* kill, mxArray* file, mxArray* thresh, mxArray* trainclasses);

extern LIB_libavedsharedlib_C_API bool MW_CALL_CONV mlfTest_class(int nargout, mxArray** testfiles, mxArray** finalclassindex, mxArray** finalstoreprob, mxArray* kill, mxArray* dbroot, mxArray* testclassname, mxArray* trainingalias, mxArray* threshold, mxArray* colorspace);

extern LIB_libavedsharedlib_C_API bool MW_CALL_CONV mlfTrain_classes_ui(mxArray* kill, mxArray* dbroot, mxArray* color_space, mxArray* classalias, mxArray* classnames, mxArray* description);

#ifdef __cplusplus
}
#endif
#endif
