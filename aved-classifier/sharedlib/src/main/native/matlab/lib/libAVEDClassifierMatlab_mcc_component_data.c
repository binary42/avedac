/*
 * MATLAB Compiler: 4.4 (R2006a)
 * Date: Tue Aug 11 11:20:54 2009
 * Arguments: "-B" "macro_default" "-W" "lib:libAVEDClassifierMatlab" "-T"
 * "link:lib" "-d"
 * "/home/aved/aved/aved-classifier/sharedlib/src/main/native/matlab/lib" "-I"
 * "./" "-g" "-G" "collect" "collect_class" "assign_class" "test_class"
 * "init_classifier" "train_classes" 
 */

#include "mclmcr.h"

#ifdef __cplusplus
extern "C" {
#endif
const unsigned char __MCC_libAVEDClassifierMatlab_session_key[] = {
        '7', '5', 'A', '0', 'B', '5', '3', 'D', '4', 'A', '1', 'B', '9', 'E',
        'A', 'A', '8', '4', 'F', 'D', '1', '9', '3', 'D', 'F', 'C', '6', '2',
        'E', '3', '2', '7', '7', '6', 'B', 'B', '5', '9', 'D', '2', '9', 'F',
        '0', 'A', 'A', 'F', '6', 'C', '7', '6', 'B', '0', '2', '7', '5', '7',
        '5', 'C', 'A', '2', 'A', 'D', '2', 'A', '0', '0', '1', '0', '1', '8',
        '1', 'B', '0', '2', '8', '1', 'D', '3', '9', '4', '2', 'A', '0', 'E',
        '0', 'A', '6', '2', 'E', 'C', '5', '1', 'E', 'B', '2', 'C', '6', 'D',
        'C', '7', 'E', 'F', 'F', 'F', '3', '6', '0', '7', '2', 'D', '5', '4',
        '9', '8', 'C', '1', 'D', '4', '5', '8', 'B', '3', '5', 'D', 'F', '8',
        '6', '0', '8', '3', 'C', 'F', 'A', '0', '9', '1', '9', '8', 'D', 'F',
        '5', 'C', 'C', 'F', '5', '4', '1', '4', '8', 'E', '5', 'C', '1', 'F',
        'C', 'F', '0', '5', '9', '1', '4', '1', 'F', '0', 'F', '3', '4', '7',
        '1', '6', '5', 'C', '3', 'C', 'F', '3', 'F', '1', 'A', '7', '9', 'D',
        '3', 'A', 'A', '1', '2', 'F', '6', '8', 'C', 'C', '0', 'A', '3', '6',
        '4', '5', '9', 'C', 'F', '7', '9', '5', '5', '8', 'F', 'A', '7', '6',
        '4', '6', '1', '2', 'C', '7', 'F', '1', '2', 'B', '6', '8', '3', '2',
        'A', '2', '7', 'D', '1', '1', 'E', 'D', '4', '4', '1', '2', '4', '5',
        '4', '0', 'D', '9', 'F', '3', '3', '8', 'D', '8', '6', '0', '8', 'C',
        '8', '9', '7', 'C', '\0'};

const unsigned char __MCC_libAVEDClassifierMatlab_public_key[] = {
        '3', '0', '8', '1', '9', 'D', '3', '0', '0', 'D', '0', '6', '0', '9',
        '2', 'A', '8', '6', '4', '8', '8', '6', 'F', '7', '0', 'D', '0', '1',
        '0', '1', '0', '1', '0', '5', '0', '0', '0', '3', '8', '1', '8', 'B',
        '0', '0', '3', '0', '8', '1', '8', '7', '0', '2', '8', '1', '8', '1',
        '0', '0', 'C', '4', '9', 'C', 'A', 'C', '3', '4', 'E', 'D', '1', '3',
        'A', '5', '2', '0', '6', '5', '8', 'F', '6', 'F', '8', 'E', '0', '1',
        '3', '8', 'C', '4', '3', '1', '5', 'B', '4', '3', '1', '5', '2', '7',
        '7', 'E', 'D', '3', 'F', '7', 'D', 'A', 'E', '5', '3', '0', '9', '9',
        'D', 'B', '0', '8', 'E', 'E', '5', '8', '9', 'F', '8', '0', '4', 'D',
        '4', 'B', '9', '8', '1', '3', '2', '6', 'A', '5', '2', 'C', 'C', 'E',
        '4', '3', '8', '2', 'E', '9', 'F', '2', 'B', '4', 'D', '0', '8', '5',
        'E', 'B', '9', '5', '0', 'C', '7', 'A', 'B', '1', '2', 'E', 'D', 'E',
        '2', 'D', '4', '1', '2', '9', '7', '8', '2', '0', 'E', '6', '3', '7',
        '7', 'A', '5', 'F', 'E', 'B', '5', '6', '8', '9', 'D', '4', 'E', '6',
        '0', '3', '2', 'F', '6', '0', 'C', '4', '3', '0', '7', '4', 'A', '0',
        '4', 'C', '2', '6', 'A', 'B', '7', '2', 'F', '5', '4', 'B', '5', '1',
        'B', 'B', '4', '6', '0', '5', '7', '8', '7', '8', '5', 'B', '1', '9',
        '9', '0', '1', '4', '3', '1', '4', 'A', '6', '5', 'F', '0', '9', '0',
        'B', '6', '1', 'F', 'C', '2', '0', '1', '6', '9', '4', '5', '3', 'B',
        '5', '8', 'F', 'C', '8', 'B', 'A', '4', '3', 'E', '6', '7', '7', '6',
        'E', 'B', '7', 'E', 'C', 'D', '3', '1', '7', '8', 'B', '5', '6', 'A',
        'B', '0', 'F', 'A', '0', '6', 'D', 'D', '6', '4', '9', '6', '7', 'C',
        'B', '1', '4', '9', 'E', '5', '0', '2', '0', '1', '1', '1', '\0'};

static const char * MCC_libAVEDClassifierMatlab_matlabpath_data[] = 
    { "libAVEDClassifierMatlab/", "toolbox/compiler/deploy/",
      "$TOOLBOXMATLABDIR/general/", "$TOOLBOXMATLABDIR/ops/",
      "$TOOLBOXMATLABDIR/lang/", "$TOOLBOXMATLABDIR/elmat/",
      "$TOOLBOXMATLABDIR/elfun/", "$TOOLBOXMATLABDIR/specfun/",
      "$TOOLBOXMATLABDIR/matfun/", "$TOOLBOXMATLABDIR/datafun/",
      "$TOOLBOXMATLABDIR/polyfun/", "$TOOLBOXMATLABDIR/funfun/",
      "$TOOLBOXMATLABDIR/sparfun/", "$TOOLBOXMATLABDIR/scribe/",
      "$TOOLBOXMATLABDIR/graph2d/", "$TOOLBOXMATLABDIR/graph3d/",
      "$TOOLBOXMATLABDIR/specgraph/", "$TOOLBOXMATLABDIR/graphics/",
      "$TOOLBOXMATLABDIR/uitools/", "$TOOLBOXMATLABDIR/strfun/",
      "$TOOLBOXMATLABDIR/imagesci/", "$TOOLBOXMATLABDIR/iofun/",
      "$TOOLBOXMATLABDIR/audiovideo/", "$TOOLBOXMATLABDIR/timefun/",
      "$TOOLBOXMATLABDIR/datatypes/", "$TOOLBOXMATLABDIR/verctrl/",
      "$TOOLBOXMATLABDIR/codetools/", "$TOOLBOXMATLABDIR/helptools/",
      "$TOOLBOXMATLABDIR/demos/", "$TOOLBOXMATLABDIR/timeseries/",
      "$TOOLBOXMATLABDIR/hds/", "toolbox/local/", "toolbox/compiler/",
      "toolbox/images/images/", "toolbox/images/iptutils/",
      "toolbox/shared/imageslib/", "toolbox/images/medformats/" };

static const char * MCC_libAVEDClassifierMatlab_classpath_data[] = 
    { "java/jar/toolbox/images.jar" };

static const char * MCC_libAVEDClassifierMatlab_libpath_data[] = 
    { "" };

static const char * MCC_libAVEDClassifierMatlab_app_opts_data[] = 
    { "" };

static const char * MCC_libAVEDClassifierMatlab_run_opts_data[] = 
    { "" };

static const char * MCC_libAVEDClassifierMatlab_warning_state_data[] = 
    { "" };


mclComponentData __MCC_libAVEDClassifierMatlab_component_data = { 

    /* Public key data */
    __MCC_libAVEDClassifierMatlab_public_key,

    /* Component name */
    "libAVEDClassifierMatlab",

    /* Component Root */
    "",

    /* Application key data */
    __MCC_libAVEDClassifierMatlab_session_key,

    /* Component's MATLAB Path */
    MCC_libAVEDClassifierMatlab_matlabpath_data,

    /* Number of directories in the MATLAB Path */
    37,

    /* Component's Java class path */
    MCC_libAVEDClassifierMatlab_classpath_data,
    /* Number of directories in the Java class path */
    1,

    /* Component's load library path (for extra shared libraries) */
    MCC_libAVEDClassifierMatlab_libpath_data,
    /* Number of directories in the load library path */
    0,

    /* MCR instance-specific runtime options */
    MCC_libAVEDClassifierMatlab_app_opts_data,
    /* Number of MCR instance-specific runtime options */
    0,

    /* MCR global runtime options */
    MCC_libAVEDClassifierMatlab_run_opts_data,
    /* Number of MCR global runtime options */
    0,
    
    /* Component preferences directory */
    "libAVEDClassifierMatlab_342C374D606D2DDE775AF6E054AA1644",

    /* MCR warning status data */
    MCC_libAVEDClassifierMatlab_warning_state_data,
    /* Number of MCR warning status modifiers */
    0,

    /* Path to component - evaluated at runtime */
    NULL

};

#ifdef __cplusplus
}
#endif


