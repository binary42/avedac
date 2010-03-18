/*
 * MATLAB Compiler: 4.4 (R2006a)
 * Date: Tue Aug 11 11:20:54 2009
 * Arguments: "-B" "macro_default" "-W" "lib:libAVEDClassifierMatlab" "-T"
 * "link:lib" "-d"
 * "/home/aved/aved/aved-classifier/sharedlib/src/main/native/matlab/lib" "-I"
 * "./" "-g" "-G" "collect" "collect_class" "assign_class" "test_class"
 * "init_classifier" "train_classes" 
 */

#include <stdio.h>
#define EXPORTING_libAVEDClassifierMatlab 1
#include "libAVEDClassifierMatlab.h"
#ifdef __cplusplus
extern "C" {
#endif

extern mclComponentData __MCC_libAVEDClassifierMatlab_component_data;

#ifdef __cplusplus
}
#endif


static HMCRINSTANCE _mcr_inst = NULL;


static int mclDefaultPrintHandler(const char *s)
{
    return fwrite(s, sizeof(char), strlen(s), stdout);
}

static int mclDefaultErrorHandler(const char *s)
{
    int written = 0, len = 0;
    len = strlen(s);
    written = fwrite(s, sizeof(char), len, stderr);
    if (len > 0 && s[ len-1 ] != '\n')
        written += fwrite("\n", sizeof(char), 1, stderr);
    return written;
}


/* This symbol is defined in shared libraries. Define it here
 * (to nothing) in case this isn't a shared library. 
 */
#ifndef LIB_libAVEDClassifierMatlab_C_API 
#define LIB_libAVEDClassifierMatlab_C_API /* No special import/export declaration */
#endif

LIB_libAVEDClassifierMatlab_C_API 
bool MW_CALL_CONV libAVEDClassifierMatlabInitializeWithHandlers(
    mclOutputHandlerFcn error_handler,
    mclOutputHandlerFcn print_handler
)
{
    if (_mcr_inst != NULL)
        return true;
    if (!mclmcrInitialize())
        return false;
    if (!mclInitializeComponentInstance(&_mcr_inst,
                                        &__MCC_libAVEDClassifierMatlab_component_data,
                                        true, NoObjectType, LibTarget,
                                        error_handler, print_handler))
        return false;
    return true;
}

LIB_libAVEDClassifierMatlab_C_API 
bool MW_CALL_CONV libAVEDClassifierMatlabInitialize(void)
{
    return libAVEDClassifierMatlabInitializeWithHandlers(mclDefaultErrorHandler,
                                                         mclDefaultPrintHandler);
}

LIB_libAVEDClassifierMatlab_C_API 
void MW_CALL_CONV libAVEDClassifierMatlabTerminate(void)
{
    if (_mcr_inst != NULL)
        mclTerminateInstance(&_mcr_inst);
}


LIB_libAVEDClassifierMatlab_C_API 
bool MW_CALL_CONV mlxCollect(int nlhs, mxArray *plhs[],
                             int nrhs, mxArray *prhs[])
{
    return mclFeval(_mcr_inst, "collect", nlhs, plhs, nrhs, prhs);
}

LIB_libAVEDClassifierMatlab_C_API 
bool MW_CALL_CONV mlxCollect_class(int nlhs, mxArray *plhs[],
                                   int nrhs, mxArray *prhs[])
{
    return mclFeval(_mcr_inst, "collect_class", nlhs, plhs, nrhs, prhs);
}

LIB_libAVEDClassifierMatlab_C_API 
bool MW_CALL_CONV mlxAssign_class(int nlhs, mxArray *plhs[],
                                  int nrhs, mxArray *prhs[])
{
    return mclFeval(_mcr_inst, "assign_class", nlhs, plhs, nrhs, prhs);
}

LIB_libAVEDClassifierMatlab_C_API 
bool MW_CALL_CONV mlxTest_class(int nlhs, mxArray *plhs[],
                                int nrhs, mxArray *prhs[])
{
    return mclFeval(_mcr_inst, "test_class", nlhs, plhs, nrhs, prhs);
}

LIB_libAVEDClassifierMatlab_C_API 
bool MW_CALL_CONV mlxInit_classifier(int nlhs, mxArray *plhs[],
                                     int nrhs, mxArray *prhs[])
{
    return mclFeval(_mcr_inst, "init_classifier", nlhs, plhs, nrhs, prhs);
}

LIB_libAVEDClassifierMatlab_C_API 
bool MW_CALL_CONV mlxTrain_classes(int nlhs, mxArray *plhs[],
                                   int nrhs, mxArray *prhs[])
{
    return mclFeval(_mcr_inst, "train_classes", nlhs, plhs, nrhs, prhs);
}

LIB_libAVEDClassifierMatlab_C_API 
bool MW_CALL_CONV mlfCollect(int nargout, mxArray** classfilemetadata
                             , mxArray** classdata, mxArray* dbroot
                             , mxArray* dirct_or_filelist, mxArray* classname)
{
    return mclMlfFeval(_mcr_inst, "collect", nargout, 2, 3, classfilemetadata,
                       classdata, dbroot, dirct_or_filelist, classname);
}

LIB_libAVEDClassifierMatlab_C_API 
bool MW_CALL_CONV mlfCollect_class(mxArray* dbroot, mxArray* classdir)
{
    return mclMlfFeval(_mcr_inst, "collect_class", 0, 0, 2, dbroot, classdir);
}

LIB_libAVEDClassifierMatlab_C_API 
bool MW_CALL_CONV mlfAssign_class(int nargout, mxArray** classindex
                                  , mxArray** storeprob, mxArray* file
                                  , mxArray* thresh, mxArray* trainclasses)
{
    return mclMlfFeval(_mcr_inst, "assign_class", nargout, 2, 3,
                       classindex, storeprob, file, thresh, trainclasses);
}

LIB_libAVEDClassifierMatlab_C_API 
bool MW_CALL_CONV mlfTest_class(int nargout, mxArray** classindex
                                , mxArray** storeprob, mxArray* dbroot
                                , mxArray* testclassname
                                , mxArray* trainingclasses
                                , mxArray* threshold)
{
    return mclMlfFeval(_mcr_inst, "test_class", nargout, 2, 4,
                       classindex, storeprob, dbroot,
                       testclassname, trainingclasses, threshold);
}

LIB_libAVEDClassifierMatlab_C_API 
bool MW_CALL_CONV mlfInit_classifier(mxArray* dbroot, mxArray* trainclasses)
{
    return mclMlfFeval(_mcr_inst, "init_classifier", 0,
                       0, 2, dbroot, trainclasses);
}

LIB_libAVEDClassifierMatlab_C_API 
bool MW_CALL_CONV mlfTrain_classes(int nargout, mxArray** ris
                                   , mxArray* classnames
                                   , mxArray* class_mfiles
                                   , mxArray* trainingdatafile)
{
    return mclMlfFeval(_mcr_inst, "train_classes", nargout, 1, 3, ris,
                       classnames, class_mfiles, trainingdatafile);
}
