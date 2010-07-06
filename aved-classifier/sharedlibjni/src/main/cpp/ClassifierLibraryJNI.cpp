/*
 * Copyright 2009 MBARI
 *
 * Licensed under the GNU LESSER GENERAL PUBLIC LICENSE, Version 2.1 
 * (the "License"); you may not use this file except in compliance 
 * with the License. You may obtain a copy of the License at
 *
 * http://www.gnu.org/copyleft/lesser.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.   
 */

#include <sstream>
#include <iostream> 
#include <fstream> 
#include <stdio.h>
#include <jni.h>
#include <signal.h>
#include <stdlib.h> 
#include <dirent.h>
#include <sys/stat.h> 

// This is the nar plugin genereated header file that only gets
// genereated in the target directory during a build
#include "org_mbari_aved_classifier_ClassifierLibraryJNI.h" 

// Matlab shared library header files
#include "libavedsharedlib.h"
#include "matrix.h" 

// Debug flag to print out verbose messages. 
// Comment out when everything is working
int debug = 1;
 
#define DPRINTF if(debug) printf

#define ASSERT(x) \
 if (! (x)) \
 { \
     cout << "ERROR!! Assert " << #x << " failed\n"; \
     cout << " on line " << __LINE__  << "\n"; \
     cout << " in file " << __FILE__ << "\n";  \
     exit(-1); \
 } 

using namespace std; 


//**************************************************************************
// Throw new exception
//**************************************************************************
void ThrowByName(JNIEnv * env, const char *name, const char *msg) {
    jclass cls = env->FindClass(name);

    // if cls is NULL, an exception has already been thrown 
    if (cls != NULL) {
        env->ThrowNew(cls, msg);
    }
    /* free the local ref */
    env->DeleteLocalRef(cls);
}

//*************************************************************************** 
// Returns true if a file (or directory) exists
//***************************************************************************  
bool exists(string fileordir) {

    struct stat stFileInfo;
    bool blnReturn;
    int intStat;

    // Attempt to get the file attributes 
    intStat = stat(fileordir.c_str(), &stFileInfo);
    if (intStat == 0) {
        // We were able to get the file attributes 
        // so the file obviously exists. 
        blnReturn = true;
    } else {
        // We were not able to get the file attributes. 
        // This may mean that we don't have permission to 
        // access the folder which contains this file. If you 
        // need to do that level of checking, lookup the 
        // return values of stat which will give you 
        // more details on why stat failed. 
        blnReturn = false;
    }

    return (blnReturn);
}

//*************************************************************************** 
// Returns true if a file was removed
//***************************************************************************  
bool removeFile(JNIEnv *env, const char *filename) {
    if (exists(string(filename))) {
        if (remove(filename) != 0) {
            stringstream out;
            out << "Error deleting file " << string(filename);
            ThrowByName(env, "java/lang/RuntimeException", out.str().c_str());
            return false;
        } else
            return true;
    }
    return false;
}

//****************************************************************************
//* Converts a ColorSpace to its mxArray equivalent. Assumes the caller
//* is reponsible for freeing up the allocated mxArray when done with it.
//**************************************************************************** 
mxArray *colorSpaceToMxArray(JNIEnv * env, jobject jcolorSpace) { 
  
    mxArray *colorSpace = NULL;
    jclass cls = env->GetObjectClass(jcolorSpace);
    jmethodID toStringId = env->GetMethodID(cls, "toString", "()Ljava/lang/String;");
    jstring jjcolorSpace = reinterpret_cast<jstring> (env->CallObjectMethod(jcolorSpace, toStringId));

    ASSERT(jcolorSpace);
    
    const char *colorSpaceString = env->GetStringUTFChars((jstring) jjcolorSpace, NULL);
    
    ASSERT(colorSpaceString);
    
    if (!strcmp(colorSpaceString, "GRAY"))
        colorSpace = mxCreateDoubleScalar((double) 1);
    else if (!strcmp(colorSpaceString, "RGB"))
        colorSpace = mxCreateDoubleScalar((double) 2);
    else if (!strcmp(colorSpaceString, "YCBCR"))
        colorSpace = mxCreateDoubleScalar((double) 3);
    else
        colorSpace = mxCreateDoubleScalar((double) 1);

    return colorSpace;
}
//****************************************************************************
//* Converts a ColorSpace mxArray to its jfieldID selection
//**************************************************************************** 
jfieldID mxArrayToColorSpace(JNIEnv * env, mxArray *jcolorSpace) {
    int color = (int) mxGetScalar(jcolorSpace);
    jfieldID jcolorSpaceId = 0;
    jclass jcolorClass = env->FindClass("org/mbari/aved/classifier/ColorSpace");

    switch (color) {
        case(0) :
        case(1) :
                    jcolorSpaceId = env->GetStaticFieldID(jcolorClass,
                    "GRAY",
                    "Lorg/mbari/aved/classifier/ColorSpace;");
            break;
        case(2) :
                    jcolorSpaceId = env->GetStaticFieldID(jcolorClass,
                    "RGB",
                    "Lorg/mbari/aved/classifier/ColorSpace;");
            break;
        case(3) :
                    jcolorSpaceId = env->GetStaticFieldID(jcolorClass,
                    "YCBCR",
                    "Lorg/mbari/aved/classifier/ColorSpace;");
            break;
    }
    return jcolorSpaceId;
}

//*************************************************************************** 
// Converts a comma, tab, new-line delimited jstring to a mxArray
// with each sub-string in 1 row wide by x length of the string tokens
// found
//*************************************************************************** 

mxArray *ClassStringToMxArray(JNIEnv *env, jstring str) {
    char *token = NULL;
    char *ptr = (char *) env->GetStringUTFChars(str, 0);
    int numclasses = 0;
    mxArray *mxTrn = NULL;

    /* Do some checking */
    if (ptr == NULL)
        return NULL;

    if (env->ExceptionCheck()) /* exception occured */
        return NULL;

    /* Count how many training classes */
    token = strtok(ptr, ",\t\n\r");
    while (token != NULL && ptr != NULL) {
        token = strtok(NULL, ",\t\n\r");
        numclasses++;
    }

    DPRINTF("Found %d classes \n", numclasses);

    if (numclasses == 0)
        return NULL;

    /* Create cell matrix 1 row wide x length of classes */
    mxTrn = mxCreateCellMatrix(1, numclasses);

    /* Now stuff training class strings into array*/
    int i = 0;
    ptr = (char *) env->GetStringUTFChars(str, 0);
    token = strtok(ptr, ",\t\n\r");
    for (i = 0; i < numclasses; i++) {
        mxSetCell(mxTrn, i, mxDuplicateArray(mxCreateString(token)));
        token = strtok(NULL, ",\t\n\r");
    }
    return mxTrn;
} 
//*************************************************************************** 
// Initializes the matlab library.  Calls matlab application and library 
// initialization. Must call this before calling any matlab functions
// in this library
//************************************************************************** 
JNIEXPORT void JNICALL Java_org_mbari_aved_classifier_ClassifierLibraryJNI_initLib
(JNIEnv *env, jobject obj, jstring jmatlablog) {

    const char *matlablog = env->GetStringUTFChars(jmatlablog, 0);
   
    // Do some checking
    if (matlablog == NULL) {
        ThrowByName(env, "java/lang/IllegalArgumentException", "NULL matlablog name");
        return;
    }

    // TODO: check if the parent directory the log file is stored to
    // actually exists
    const char* options[5];
    options[0] = "-logfile";
    options[1] = matlablog;
    options[2] = "-nojvm";
    options[3] = "-nodisplay";
    options[4] = "-singleCompThread";

    try {
        DPRINTF("Initializing mcl\n");
        if (!mclInitializeApplication(options, 5)) {
            ThrowByName(env, "java/lang/RuntimeException", "Could not initialize the MCR properly"); 
            return;
        }
        DPRINTF("Initializing library\n");
        // Initialize the library of MATLAB functions
        if (!libavedsharedlibInitialize()) {
             ThrowByName(env, "java/lang/RuntimeException", "Could not initialize the Classifier MATLAB library properly");  
            return;
        }

        DPRINTF("Library initialized\n");
        DPRINTF("MCR initialized : %d\n", mclIsMCRInitialized());
        DPRINTF("JVM initialized : %d\n", mclIsJVMEnabled());
        DPRINTF("Logfile name : %s\n", mclGetLogFileName());
        DPRINTF("nodisplay set : %d\n", mclIsNoDisplaySet());
    
    } catch (const mwException &e) { 
        ThrowByName(env, "java/lang/RuntimeException", e.what());
        return;
    } catch (...) {
        ThrowByName(env, "java/lang/RuntimeException", "Unknown matlab exception");
        return;
    }
}

//*************************************************************************** 
// Closes the matlab library.  Call this after all matlab calls are completed 
//************************************************************************** 

JNIEXPORT void JNICALL Java_org_mbari_aved_classifier_ClassifierLibraryJNI_closeLib
(JNIEnv *env, jobject obj) {
    try {

        libavedsharedlibTerminate();
        if (!mclTerminateApplication()) {
            DPRINTF("could not terminate the library properly\n");
            return;
        }
    } catch (const mwException &e) {
        ThrowByName(env, "java/lang/RuntimeException", e.what());
        return;
    } catch (...) {
        ThrowByName(env, "java/lang/RuntimeException", "Unknown matlab exception");
        return;
    }
}

/************************************************************************************/
JNIEXPORT void JNICALL Java_org_mbari_aved_classifier_ClassifierLibraryJNI_train_1classes
(JNIEnv *env, jobject obj,  
        jstring jkillFile, 
        jstring jclasses, 
        jstring jtrainingAlias,
        jstring jmatlabdb, 
        jobject jcolorSpace,
        jstring jdescription) {
    
    const char *classdir = env->GetStringUTFChars(jclasses, 0);
    const char *trainingalias = env->GetStringUTFChars(jtrainingAlias, 0);
    const char *matlabdbdir = env->GetStringUTFChars(jmatlabdb, 0);
    const char *description = env->GetStringUTFChars(jdescription, 0);
    const char *killfile = env->GetStringUTFChars(jkillFile, 0);

    /* Do some checking */
    if (classdir == NULL) {
        ThrowByName(env, "java/lang/IllegalArgumentException", "NULL class name");
        return;
    }
    if (description == NULL) {
        ThrowByName(env, "java/lang/IllegalArgumentException", "NULL description ");
        return;
    }
    if (matlabdbdir == NULL) {
        ThrowByName(env, "java/lang/IllegalArgumentException", "NULL matlabdb name");
        return;
    }
   
    if (trainingalias == NULL) {
        ThrowByName(env, "java/lang/IllegalArgumentException", "NULL trainingalias name");
        return;
    }      
    if (killfile == NULL) {
        ThrowByName(env, "java/lang/IllegalArgumentException", "NULL killfile name");
    } 

    removeFile(env, killfile);

    mxArray *mxDbRoot = mxCreateString(matlabdbdir);
    mxArray *mxTrainAlias = mxCreateString(trainingalias);
    mxArray *mxDescription = mxCreateString(description);
    mxArray *mxKillFile= mxCreateString(killfile);
    mxArray *mxClassArray = ClassStringToMxArray(env, jclasses);
    mxArray *mxColorSpace = NULL;
    
     /* Get the color space */
    jclass cls = env->GetObjectClass(jcolorSpace);
    jmethodID toStringId = env->GetMethodID(cls, "toString", "()Ljava/lang/String;");
    jstring jjcolorSpace = reinterpret_cast<jstring> (env->CallObjectMethod(jcolorSpace, toStringId));

    const char *jcolorSpaceString = env->GetStringUTFChars((jstring) jjcolorSpace, NULL);
    if (!strcmp(jcolorSpaceString, "GRAY"))
        mxColorSpace = mxCreateDoubleScalar((double) 1);
    else if (!strcmp(jcolorSpaceString, "RGB"))
        mxColorSpace = mxCreateDoubleScalar((double) 2);
    else if (!strcmp(jcolorSpaceString, "YCBCR"))
        mxColorSpace = mxCreateDoubleScalar((double) 3);
    else
        mxColorSpace = mxCreateDoubleScalar((double) 1);

    if (mxColorSpace == NULL) {
        ThrowByName(env, "java/lang/IllegalArgumentException", "Error creating training class array");
        return;
    }

    try {
        if (mlfTrain_classes_ui(mxKillFile, mxDbRoot, mxColorSpace, mxTrainAlias, mxClassArray, mxDescription) == false)
            ThrowByName(env, "java/lang/RuntimeException", "Train class failed");

    } catch (const mwException &e) {
        ThrowByName(env, "java/lang/RuntimeException", e.what());
    } catch (...) {
        ThrowByName(env, "java/lang/RuntimeException", "Unknown matlab exception");
    }

    removeFile(env, killfile);
    mxDestroyArray(mxDbRoot); 
    mxDestroyArray(mxColorSpace); 
    mxDestroyArray(mxKillFile);
    mxDestroyArray(mxTrainAlias);
    mxDestroyArray(mxDescription);
    mxDestroyArray(mxClassArray);
}

/************************************************************************************/
JNIEXPORT void JNICALL Java_org_mbari_aved_classifier_ClassifierLibraryJNI_collect_1tests
(JNIEnv *env, jobject obj, 
        jstring jkillFile, 
        jstring jtestDir, 
        jstring jmatlabdb, 
        jobject jcolorSpace) {

    const char *matlabdbdir, *testdir, *killfile;
    mxArray *mfile = NULL;

    testdir = env->GetStringUTFChars(jtestDir, 0);
    matlabdbdir = env->GetStringUTFChars(jmatlabdb, 0);
    killfile = env->GetStringUTFChars(jkillFile, 0);
    
    /* Do some checking */
    if (testdir == NULL) {
        ThrowByName(env, "java/lang/IllegalArgumentException", "NULL testing directory");
    }

    if (matlabdbdir == NULL) {
        ThrowByName(env, "java/lang/IllegalArgumentException", "NULL matlabdb name");
    } 
 
    if (killfile == NULL) {
        ThrowByName(env, "java/lang/IllegalArgumentException", "NULL killfile name");
    } 

    DPRINTF("Collecting test data from %s \n", testdir);

    mxArray *mxTestDir = mxCreateString(testdir);
    mxArray *mxDbRoot = mxCreateString(matlabdbdir);
    mxArray *mxKillFile=  mxCreateString(killfile);
    mxArray *mxColorSpace = colorSpaceToMxArray(env,jcolorSpace);
   
    /* Run the collection*/
    try {
        if (mlfCollect_tests(1, &mfile, mxKillFile, mxTestDir, mxDbRoot, mxColorSpace) == false)
            ThrowByName(env, "java/lang/RuntimeException", "Collect class failed");
    } catch (const mwException &e) {
        ThrowByName(env, "java/lang/RuntimeException", e.what());
    } catch (...) {
        ThrowByName(env, "java/lang/RuntimeException", "Unknown matlab exception");
    }

    mxDestroyArray(mxTestDir);
    mxDestroyArray(mxDbRoot);
    mxDestroyArray(mxKillFile);
    mxDestroyArray(mxColorSpace);
}

/************************************************************************************/
JNIEXPORT void JNICALL Java_org_mbari_aved_classifier_ClassifierLibraryJNI_collect_1class
(JNIEnv *env, jobject obj, 
        jstring jkillFile, 
        jstring jimageRawDirectory, 
        jstring jimageSquareDirectory,
        jstring jclassName,  
        jstring jmatlabdb,
        jstring jvarsClassName, 
        jstring jdescription,
        jobject jcolorSpace) {
    
    const char *classname, *varsclassname, *description, *matlabdbdir,
            *imagesqdir, *imagerawdir, *killfile;

    classname = env->GetStringUTFChars(jclassName, 0);
    matlabdbdir = env->GetStringUTFChars(jmatlabdb, 0);
    varsclassname = env->GetStringUTFChars(jvarsClassName, 0);
    description = env->GetStringUTFChars(jdescription, 0);
    imagerawdir = env->GetStringUTFChars(jimageRawDirectory, 0);
    imagesqdir = env->GetStringUTFChars(jimageSquareDirectory, 0);
    killfile = env->GetStringUTFChars(jkillFile, 0);
    
    /* Do some checking */
    if (classname == NULL) {
        ThrowByName(env, "java/lang/IllegalArgumentException", "NULL class name");
    }
    if (description == NULL) {
        ThrowByName(env, "java/lang/IllegalArgumentException", "NULL description");
    }
    if (varsclassname == NULL) {
        ThrowByName(env, "java/lang/IllegalArgumentException", "NULL varsclassname name");
    }
    if (matlabdbdir == NULL) {
        ThrowByName(env, "java/lang/IllegalArgumentException", "NULL matlabdb name");
    }
    if (imagerawdir == NULL) {
        ThrowByName(env, "java/lang/IllegalArgumentException", "NULL matlabdb name");
    }
    if (imagesqdir == NULL) {
        ThrowByName(env, "java/lang/IllegalArgumentException", "NULL matlabdb name");
    }
    if (imagesqdir == NULL) {
        ThrowByName(env, "java/lang/IllegalArgumentException", "NULL matlabdb name");
    }
    if (killfile == NULL) {
        ThrowByName(env, "java/lang/IllegalArgumentException", "NULL killfile name");
    } 

    removeFile(env, killfile);
    
    DPRINTF("Collecting class %s \n", classname);

    mxArray *mxClassName = mxCreateString(classname);
    mxArray *mxDbRoot = mxCreateString(matlabdbdir);
    mxArray *mxVarsClassName = mxCreateString(varsclassname);
    mxArray *mxDescription = mxCreateString(description);
    mxArray *mxKillFile= mxCreateString(killfile);
    mxArray *mxRawDir = mxCreateString(imagerawdir);
    mxArray *mxSquareDir = mxCreateString(imagesqdir);
    mxArray *mxClassMetadata = NULL;
    mxArray *mxClassData = NULL; 
    mxArray *mxColorSpace = colorSpaceToMxArray(env, jcolorSpace); 

    /* Run the collection*/
    try {
        if (mlfCollect_ui(2, &mxClassMetadata, &mxClassData, mxKillFile, 
                mxRawDir, mxSquareDir, mxClassName, mxDbRoot, 
                mxColorSpace, mxVarsClassName,
                mxDescription) == false)
            ThrowByName(env, "java/lang/RuntimeException", "Collect class failed");
    } catch (const mwException &e) {
        ThrowByName(env, "java/lang/RuntimeException", e.what());
    } catch (...) {
        ThrowByName(env, "java/lang/RuntimeException", "Unknown matlab exception");
    }

    
    removeFile(env, killfile);
    mxDestroyArray(mxClassName);
    mxDestroyArray(mxDbRoot);
    mxDestroyArray(mxVarsClassName);
    mxDestroyArray(mxDescription);
    mxDestroyArray(mxKillFile);
    mxDestroyArray(mxRawDir);
    mxDestroyArray(mxSquareDir); 
    mxDestroyArray(mxColorSpace);
}

/************************************************************************************/
JNIEXPORT void JNICALL Java_org_mbari_aved_classifier_ClassifierLibraryJNI_run_1test
(JNIEnv * env, jobject obj,  
        jstring jkillFile, 
        jobjectArray jeventIdStringArray,
        jintArray jmajorityClassIndexArray,
        jintArray jprobabilityClassIndexArray,
        jfloatArray jprobabilityArray,
        jstring jtestClass,
        jstring jtrainingAlias,
        jfloat jthreshold,
        jstring jmatlabdb,
        jobject jcolorSpace) {
     
    const char *testclass = env->GetStringUTFChars(jtestClass, 0);
    const char *matlabdbdir = env->GetStringUTFChars(jmatlabdb, 0);
    const char *trainingalias = env->GetStringUTFChars(jtrainingAlias, 0);
    const char *killfile = env->GetStringUTFChars(jkillFile, 0);

    /* Do some checking */
    if (testclass == NULL) {
        ThrowByName(env, "java/lang/IllegalArgumentException", "NULL test class name");
         return;
    }
    if (matlabdbdir == NULL) {
        ThrowByName(env, "java/lang/IllegalArgumentException", "NULL matlabdb name");
         return;
    }
    if (trainingalias == NULL) {
        ThrowByName(env, "java/lang/IllegalArgumentException", "NULL trainingalias name");
         return;
    }
    if (killfile == NULL) {
        ThrowByName(env, "java/lang/IllegalArgumentException", "NULL killfile name");
    }  
    
    removeFile(env, killfile);
    
    DPRINTF("Testing class %s\n", testclass);

    mxArray *mxMajorityClassIndex = NULL;
    mxArray *mxProbabilityClassIndex = NULL;
    mxArray *mxEventIds = NULL;
    mxArray *mxStoreProb = NULL;
    mxArray *mxDbRoot = mxCreateString(matlabdbdir);
    mxArray *mxTestClass = mxCreateString(testclass);
    mxArray *mxTrainingAlias = mxCreateString(trainingalias);
    mxArray *mxThreshold = mxCreateDoubleScalar((double) jthreshold);
    mxArray *mxKillFile = mxCreateString(killfile);
    mxArray *mxColorSpace = colorSpaceToMxArray(env, jcolorSpace);
      
    /* Call the native function that in turn calls the matlab code*/
    try {
        if (mlfRun_tests_ui(4, &mxEventIds, &mxMajorityClassIndex,
                &mxProbabilityClassIndex, &mxStoreProb, mxKillFile,
                mxDbRoot, mxColorSpace, mxTestClass, 
                mxTrainingAlias, mxThreshold) == false)
            ThrowByName(env, "java/lang/RuntimeException", "Run test failed");

    } catch (const mwException &e) {
        ThrowByName(env, "java/lang/RuntimeException", "Testing class failed");// e.what());
        return;
    } catch (...) {
        ThrowByName(env, "java/lang/RuntimeException", "Unknown matlab exception");
        return;
    }
    stringstream out; 
    
    /* if java arrays less than size of return array throw exception*/
    jsize len = env->GetArrayLength(jeventIdStringArray);

    // Get number of elements in the return storeprob array
    int rlen = mxGetNumberOfElements(mxStoreProb);

    // Do some check to make sure large enough arrays are specified
    if (len != rlen) {
        out << "Event ID index array incorrectly sized initialize to size " << rlen;
        ThrowByName(env, "java/lang/IllegalArgumentException", out.str().c_str());
        return;
    }
    /* if java arrays less than size of return array throw exception*/
    len = env->GetArrayLength(jprobabilityArray);
    if (len != rlen) {
        out << "Class index array incorrectly sized initialize to size " << rlen;
        ThrowByName(env, "java/lang/IllegalArgumentException", out.str().c_str());
        return;
    }
    /* now test the majority class index array */
    len = env->GetArrayLength(jmajorityClassIndexArray);
    if (len != rlen) {
        out << "Class majority index array incorrectly sized initialize to " 
                << rlen << " size";
        ThrowByName(env, "java/lang/IllegalArgumentException", out.str().c_str());
        return;
    }

    /* now test the probability class index array */
    len = env->GetArrayLength(jprobabilityClassIndexArray);
    if (len != rlen) {
        out << "Class probability index array incorrectly sized initialize to" 
                << rlen << " size";
        ThrowByName(env, "java/lang/IllegalArgumentException", out.str().c_str());
        return;
    }

    float ps;
    int pm, pp;
    int buflen;
    char *buf;
    const mxArray* cellptr;

    for (int i = 0; i < rlen; i++) {

        cellptr = mxGetCell(mxStoreProb, i);
        ps = (float) mxGetScalar(cellptr);
        cellptr = mxGetCell(mxMajorityClassIndex, i);
        pm = (int) int32_t(mxGetScalar(cellptr));
        cellptr = mxGetCell(mxProbabilityClassIndex, i);
        pp = (int) int32_t(mxGetScalar(cellptr));

        cellptr = mxGetCell(mxEventIds, i);

        /* Find out how long the input string array is. */
        buflen = (mxGetM(cellptr) * mxGetN(cellptr)) + i + 1;

        /* Allocate enough memory to hold the converted string. */
        buf = (char *) mxCalloc(buflen, sizeof (char));
        if (buf == NULL) {
            ThrowByName(env, "java/lang/RuntimeException", "Not enough heap space to hold string");
            return;
        } else {
            /* Copy the string data into buf. */
            mxGetString(cellptr, buf, buflen);

            /* Store string back into object array */
            env->SetObjectArrayElement(jeventIdStringArray, i, env->NewStringUTF(buf));
        }

        env->SetFloatArrayRegion(jprobabilityArray, (jsize) i, 1, (jfloat *) & ps);
        env->SetIntArrayRegion(jmajorityClassIndexArray, (jsize) i, 1, (jint *) & pm);
        env->SetIntArrayRegion(jprobabilityClassIndexArray, (jsize) i, 1, (jint *) & pp);

    }

    removeFile(env, killfile);
    /* release memory */
    mxDestroyArray(mxTestClass);
    mxDestroyArray(mxDbRoot); 
    mxDestroyArray(mxKillFile);
    mxDestroyArray(mxTrainingAlias);
    mxDestroyArray(mxThreshold);
    mxDestroyArray(mxColorSpace);
}

/************************************************************************************/
JNIEXPORT void JNICALL Java_org_mbari_aved_classifier_ClassifierLibraryJNI_set_1kill
(JNIEnv * env, jobject obj, jstring jkillFile, jint jstate) {

    const char *killfile = env->GetStringUTFChars(jkillFile, 0);

    /* Do some checking */
    if (killfile == NULL) {
        ThrowByName(env, "java/lang/IllegalArgumentException", "NULL killfile name");
        return;
    }
    ofstream file;
    file.open(killfile, ios::out | ios::app | ios::binary);
    if (file.is_open()) {
        file << (unsigned int) jstate;
        file.close();
    } else {        
        ThrowByName(env, "java/lang/RuntimeException", "Exception writing the file");
    }
}
 
/************************************************************************************/
JNIEXPORT void JNICALL Java_org_mbari_aved_classifier_ClassifierLibraryJNI_test_1class
(JNIEnv * env, jobject obj,
        jstring jkillFile,
        jobjectArray jeventFilenamejstringArray,
        jintArray jclassIndexArray,
        jfloatArray jprobabilityArray,
        jstring jtestClass,
        jstring jtrainingAlias,
        jfloat jthreshold,
        jstring jmatlabdb,
        jobject jcolorSpace) { 
            
    const char *testclass = env->GetStringUTFChars(jtestClass, 0);
    const char *matlabdbdir = env->GetStringUTFChars(jmatlabdb, 0);
    const char *trainingalias = env->GetStringUTFChars(jtrainingAlias, 0);
    const char *killfile = env->GetStringUTFChars(jkillFile, 0);
 
    /* Do some checking */
    if (testclass == NULL) {
        ThrowByName(env, "java/lang/IllegalArgumentException", "NULL test class name");
        return;
    }
 
    if (matlabdbdir == NULL) {
        ThrowByName(env, "java/lang/IllegalArgumentException", "NULL matlabdb name");
        return;
    }
    if (trainingalias == NULL) {
        ThrowByName(env, "java/lang/IllegalArgumentException", "NULL trainingalias name");
        return;
    } 
    if (killfile == NULL) {
        ThrowByName(env, "java/lang/IllegalArgumentException", "NULL killfile name");
    }   
    
    removeFile(env, killfile);
    
    DPRINTF("Testing class %s kill\n", testclass);

    mxArray *mxEventFilenames = NULL;
    mxArray *mxClassIndex = NULL;
    mxArray *mxStoreProb = NULL; 
    mxArray *mxDbRoot = mxCreateString(matlabdbdir);
    mxArray *mxTestClass = mxCreateString(testclass);
    mxArray *mxTrainingAlias = mxCreateString(trainingalias);
    mxArray *mxThreshold = mxCreateDoubleScalar((double) jthreshold);
    mxArray *mxKillFile= mxCreateString(killfile);
    mxArray *mxColorSpace = colorSpaceToMxArray(env, jcolorSpace);
    
    /* Call the native function that in turn calls the matlab code*/
    try {
        if (mlfTest_class(3, &mxEventFilenames, &mxClassIndex,
                &mxStoreProb, mxKillFile, mxDbRoot, mxTestClass, mxTrainingAlias,
                mxThreshold, mxColorSpace) == false)
            ThrowByName(env, "java/lang/RuntimeException", "Test class failed");
    } catch (const mwException &e) { 
        ThrowByName(env, "java/lang/RuntimeException", e.what());
        return;
    } catch (...) { 
        ThrowByName(env, "java/lang/RuntimeException", "Unknown matlab exception");
        return;
    }  
    
    /* Get number of elements in the return storeprob array */
    int rlen = mxGetNumberOfElements(mxClassIndex); 
    jsize len = env->GetArrayLength(jprobabilityArray);
    std::stringstream out; 
    
    /* if java arrays less than size of return array throw exception*/
    if (len != rlen) {
        out << "Class index array incorrectly sized, "  << 
                        "initialize to size " << rlen << " size";
        ThrowByName(env, "java/lang/IllegalArgumentException", out.str().c_str());
        return;
    }
    /* now test the class index array */
    len = env->GetArrayLength(jclassIndexArray);
    if (len != rlen) {
        out << "Class probability index array incorrectly sized, " <<
                        "initialize to " << rlen << " size"; 
        ThrowByName(env, "java/lang/IllegalArgumentException", out.str().c_str());
        return;
    }

    const mxArray* cellptr;
    float ps;
    int pi;
    int buflen;
    char *buf;

    for (int i = 0; i < rlen; i++) {
        cellptr = mxGetCell(mxStoreProb, i);
        ps = (float) mxGetScalar(cellptr);
        cellptr = mxGetCell(mxClassIndex, i);
        pi = (int) int32_t(mxGetScalar(cellptr));

        cellptr = mxGetCell(mxEventFilenames, i);

        /* Find out how long the input string array is. */
        buflen = (mxGetM(cellptr) * mxGetN(cellptr)) + i + 1;

        /* Allocate enough memory to hold the converted string. */
        buf = (char *) mxCalloc(buflen, sizeof (char));
        if (buf == NULL) {
            ThrowByName(env, "java/lang/RuntimeException",
                    "Not enough heap space to hold string");
            return;
        } else {
            /* Copy the string data into buf. */
            mxGetString(cellptr, buf, buflen);

            /* Store string back into object array */
            env->SetObjectArrayElement(jeventFilenamejstringArray, i, env->NewStringUTF(buf));
        }

        env->SetFloatArrayRegion(jprobabilityArray, (jsize) i, 1, (jfloat *) & ps);
        env->SetIntArrayRegion(jclassIndexArray, (jsize) i, 1, (jint *) & pi);
    }
    
    removeFile(env, killfile);
    /* release memory */
    mxDestroyArray(mxColorSpace);
    mxDestroyArray(mxTestClass);
    mxDestroyArray(mxDbRoot);
    mxDestroyArray(mxKillFile);
    mxDestroyArray(mxTrainingAlias);
    mxDestroyArray(mxThreshold);
}
//****************************************************************************
// Converts a mxArray to a char pointer
//****************************************************************************

char *getString(mxArray *data) {
    int length;
    char *str;

    length = mxGetN(data) + 1;
    if (length > 1) {
        str = (char *) mxCalloc(length, sizeof (char));
        mxGetString(data, str, length);
    } else {
        str = (char *) mxCalloc(1, sizeof (char));
    }

    return str;
}
//***********************************************************************************
// Searches for collected classes in the matlabdb root directory
// Returns an array of ClassModel objects populated
// with the class metadata
//************************************************************************************

jobjectArray get_collected_classes(JNIEnv * env, jobject obj, jstring jmatlabdb) {
    
    const char *matlabdbdir = env->GetStringUTFChars(jmatlabdb, 0);  
    // Do some checking
    if (matlabdbdir == NULL) {
        ThrowByName(env, "java/lang/IllegalArgumentException", "NULL matlabdb name");
        return 0;
    }
    // Check if a valid directory
    if (!exists(matlabdbdir)) {      
        string tmpstr = string("Directory ") + matlabdbdir + string(" does not exist");
        ThrowByName(env, "java/lang/RuntimeException", tmpstr.c_str());
        return 0;
    }
    string features = string(matlabdbdir) + string("/features/class");
    const char *featuresDir = features.c_str();
    const char *filematch = "_metadata_collection_avljNL3_cl_pcsnew.mat";
    string tmpstr, tmpstr2;
    struct dirent **filelist, **list;
    int fcount = -1, numfound = 0;
    int i = 0, j = 0;
    string::size_type pos;
    jobjectArray jClassModelArray = 0;
    
    // Check if a valid directory
    if (!exists(features)) {
        tmpstr = string("Directory ") + features + string(" does not exist");
        ThrowByName(env, "java/lang/RuntimeException", tmpstr.c_str());
        return 0;
    }
 
    fcount = scandir(featuresDir, &filelist, 0, alphasort);
    if (fcount < 0) {
        return 0;
    }

    DPRINTF("Scanning %s for files matching %s\n", featuresDir, filematch);
    for (i = 0; i < fcount; i++) {
        if (strstr(filelist[i]->d_name, filematch))
            numfound++;
    }

    DPRINTF("found %d matches\n", numfound);
    if (numfound) {
        jclass jClassModel = env->FindClass("org/mbari/aved/classifier/ClassModel");
        jmethodID jClassModelInit = env->GetMethodID(jClassModel, "<init>", "()V");
        jobject jobj = env->NewObject(jClassModel, jClassModelInit);

        // allocate array for class model
        jClassModelArray = (jobjectArray) env->NewObjectArray(numfound, jClassModel, jobj);

        jmethodID jsetRawImageDirectory = env->GetMethodID(jClassModel, "setRawImageDirectory", "(Ljava/io/File;)V");
        jmethodID jsetSquareImageDirectory = env->GetMethodID(jClassModel, "setSquareImageDirectory", "(Ljava/io/File;)V");
        jmethodID jsetColorSpace = env->GetMethodID(jClassModel, "setColorSpace", "(Lorg/mbari/aved/classifier/ColorSpace;)V");
        jmethodID jsetName = env->GetMethodID(jClassModel, "setName", "(Ljava/lang/String;)V");
        jmethodID jsetVarsClassName = env->GetMethodID(jClassModel, "setVarsClassName", "(Ljava/lang/String;)V");
        jmethodID jsetDescription = env->GetMethodID(jClassModel, "setDescription", "(Ljava/lang/String;)V");
        jmethodID jsetDatabaseRoot = env->GetMethodID(jClassModel, "setDatabaseRoot", "(Ljava/io/File;)V");

        // In case any of these methods change in the future, check for valid methods
        ASSERT(jsetRawImageDirectory && jsetSquareImageDirectory
                && jsetColorSpace && jsetName && jsetVarsClassName
                && jsetDescription && jsetDatabaseRoot);

        // Find the color space class 
        jclass jcolorClass = env->FindClass("org/mbari/aved/classifier/ColorSpace");

        for (i = 0; i < fcount; i++) {
            tmpstr = string(filelist[i]->d_name);
            pos = tmpstr.find(filematch);

            if (pos != string::npos) {
                // Remove filematch part of file name for display for simplification   
                DPRINTF("Found: %s%s\n", tmpstr.substr(0, pos).c_str(), filematch);
                tmpstr2 = string(featuresDir) + "/" + tmpstr.substr(0, pos).c_str() + string(filematch);

                // Open the file for read-only
                const char *file = tmpstr2.c_str();
                DPRINTF("Opening %s\n", file);
                MATFile *pmat = matOpen(file, "r");
                if (pmat == NULL) {
                    string s = string("Error opening ") + string(file);                    
                    ThrowByName(env, "java/lang/IllegalArgumentException", s.c_str());
                    return 0;
                }
 
                // Get the matlab class_metadata structure
                mxArray *mxStructurePtr = matGetVariable(pmat, "class_metadata");
                assert(mxStructurePtr);

                // Get the matlab fields from the matlab structure
                mxArray *mxClassName = mxGetField(mxStructurePtr, 0, "classname");
                mxArray *mxDbRoot = mxGetField(mxStructurePtr, 0, "dbroot");
                mxArray *mxVarsClassName = mxGetField(mxStructurePtr, 0, "varsclassname");
                mxArray *mxDescription = mxGetField(mxStructurePtr, 0, "description");
                mxArray *mxColorSpace = mxGetField(mxStructurePtr, 0, "color_space");
                mxArray *mxRawDir = mxGetField(mxStructurePtr, 0, "raw_directory");
                mxArray *mxSquareDir = mxGetField(mxStructurePtr, 0, "square_directory");

                DPRINTF("Closing %s\n", file);
                if (matClose(pmat) != 0) {
                    string s = string("Error closing ") + string(file);    
                    ThrowByName(env, "java/lang/IllegalArgumentException", s.c_str());
                    return 0;
                }

                // Create new ClassModel object                
                jobject jobj = env->NewObject(jClassModel, jClassModelInit);

                // get File class
                jclass jnewFile = env->FindClass("java/io/File");

                // get File constructor method id
                jmethodID jmethod = env->GetMethodID(jnewFile, "<init>", "(Ljava/lang/String;)V");

             
                // Call all methods to initialize the object
                if (mxDbRoot != 0) {
                    // create new java String from the matlab array
                    jstring jnewString = env->NewStringUTF(getString(mxDbRoot));
                    // create the File object and initialize with the string
                    jobject jdbroot = env->NewObject(jnewFile, jmethod, jnewString);
                    env->CallObjectMethod(jobj, jsetDatabaseRoot, jdbroot);
                }
                
                if (mxRawDir != 0) {
                    // create new java String from the matlab array
                    jstring jnewString = env->NewStringUTF(getString(mxRawDir));
                    // create the File object and initialize with the string
                    jobject jrawDir = env->NewObject(jnewFile, jmethod, jnewString);
                    env->CallObjectMethod(jobj, jsetRawImageDirectory, jrawDir);
                }
                
                if (mxSquareDir != 0) {
                    // create new java String from the matlab array
                    jstring jnewString = env->NewStringUTF(getString(mxSquareDir));                    
                
                    // create the File object and initialize with the string
                    jobject jsquareDir = env->NewObject(jnewFile, jmethod, jnewString);                    
                    env->CallObjectMethod(jobj, jsetSquareImageDirectory, jsquareDir);
                }                
                    
                if (mxClassName != 0)
                    env->CallObjectMethod(jobj, jsetName, env->NewStringUTF(getString(mxClassName)));
                
                if (mxVarsClassName != 0)
                    env->CallObjectMethod(jobj, jsetVarsClassName, env->NewStringUTF(getString(mxVarsClassName)));
              
                if (mxDescription != 0)
                    env->CallObjectMethod(jobj, jsetDescription, env->NewStringUTF(getString(mxDescription)));
                             
                
                // Create a new ColorSpace object
                jobject jcolorObject = env->GetStaticObjectField(jcolorClass, mxArrayToColorSpace(env, mxColorSpace));
              
                // Set the ColorSpace  enum
                env->CallObjectMethod(jobj, jsetColorSpace, jcolorObject);
            
                // Add the ClassModel into the array
                env->SetObjectArrayElement(jClassModelArray, j++, jobj);

                mxDestroyArray(mxClassName);                
                mxDestroyArray(mxDbRoot);                
                mxDestroyArray(mxColorSpace);                
                mxDestroyArray(mxRawDir);                
                mxDestroyArray(mxSquareDir);
		mxDestroyArray(mxDescription);
                mxDestroyArray(mxVarsClassName);
            }// end}//  if (pos != string::npos)
        }// end for (i = 0; i < fcount; i++) 
    } 

    // Clean up the allocated memory
     // Clean up the allocated memory
     if (fcount) { printf("Entries are:"); for (i=0, list=filelist; i<fcount; i++) { printf(" %s\n", (*list)->d_name); free(*list); list++; } free(filelist); }
 
    return jClassModelArray;
}

/************************************************************************************/
JNIEXPORT jobjectArray JNICALL Java_org_mbari_aved_classifier_ClassifierLibraryJNI_get_1training_1classes
(JNIEnv * env, jobject obj,
    jstring jmatlabdb) {
 
    const char *matlabdbdir = env->GetStringUTFChars(jmatlabdb, 0);
    
    // Do some checking
    if (matlabdbdir == NULL) {
        ThrowByName(env, "java/lang/IllegalArgumentException", "NULL matlabdb name");
        return 0;
    }
    
    string a = string("Directory ") + matlabdbdir + string(" does not exist");
    string features = string(matlabdbdir) + string("/training/class");
    const char *featuresDir = features.c_str();
    const char *filematch = "_metadata.mat";
    string tmpstr, tmpstr2;
    struct dirent **filelist, **list;
    int fcount = -1, numfound = 0;
    int i = 0, j = 0;
    string::size_type pos;
    jobjectArray jtrainingModelArray = 0; 
   
    // Check if a valid directory
    if (!exists(features)) {
        tmpstr = string("Directory ") + features + string(" does not exist");
        ThrowByName(env, "java/lang/RuntimeException", tmpstr.c_str());
        return 0;
    }
    
    // Throw exception when no classes found - TODO: it's  probably better to 
    // simply return an empty array here
    fcount = scandir(featuresDir, &filelist, 0, alphasort);
  
    if (fcount < 0) {
        //tmpstr = string("No training classes found in directory ") + features;
        //ThrowByName(env, "java/lang/RuntimeException", tmpstr.c_str());
         return 0;
    }

    DPRINTF("Scanning %d files in %s for files matching %s\n", fcount, featuresDir, filematch);
    for (i = 0; i < fcount; i++) {
        if (strstr(filelist[i]->d_name, filematch))
            numfound++;
    }
  
    DPRINTF("found %d matches\n", numfound);
    if (numfound) {

        // Get the collected classes
        jobjectArray jcollectedClasses = get_collected_classes(env, obj, jmatlabdb);
      
        if(jcollectedClasses == 0) {
            ThrowByName(env, "java/lang/RuntimeException", "cannot find classes to populate training library"); 
            return 0;  
        }
        jclass jtrainingModel = env->FindClass("org/mbari/aved/classifier/TrainingModel");
        jmethodID jtrainingModelInit = env->GetMethodID(jtrainingModel, "<init>", "()V");
        jobject jobj = env->NewObject(jtrainingModel, jtrainingModelInit);

        // Allocate array for training model
        jtrainingModelArray = (jobjectArray) env->NewObjectArray(numfound, jtrainingModel, jobj);

        // Get all the methods needed
        jmethodID jsetColorSpace = env->GetMethodID(jtrainingModel, "setColorSpace", "(Lorg/mbari/aved/classifier/ColorSpace;)V");
        jmethodID jsetName = env->GetMethodID(jtrainingModel, "setName", "(Ljava/lang/String;)V");
        jmethodID jsetDescription = env->GetMethodID(jtrainingModel, "setDescription", "(Ljava/lang/String;)V");
        jmethodID jsetDatabaseRoot = env->GetMethodID(jtrainingModel, "setDatabaseRoot", "(Ljava/io/File;)V");

        // Find the color space class
        jclass jcolorClass = env->FindClass("org/mbari/aved/classifier/ColorSpace");

        for (i = 0; i < fcount; i++) {
            tmpstr = string(filelist[i]->d_name);
            pos = tmpstr.find(filematch);

            if (pos != string::npos) {
                // Remove filematch part of file name for display for simplification   
                DPRINTF("Found: %s %s\n", tmpstr.substr(0, pos).c_str(), filematch);
                tmpstr2 = string(featuresDir) + "/" + tmpstr.substr(0, pos).c_str() + string(filematch);

                // Open the file as read only
                const char *file = tmpstr2.c_str();
                DPRINTF("Opening %s\n", file);
                MATFile *pmat = matOpen(file, "r");
                if (pmat == NULL) {
                    DPRINTF("Error opening file %s\n", file);
                    exit(-1);
                }

                // Get the matlab class_metadata structure
                mxArray *mxStructurePtr = matGetVariable(pmat, "training_metadata");
                assert(mxStructurePtr);

                // Get the matlab fields from the matlab structure
                mxArray *mxClassAlias = mxGetField(mxStructurePtr, 0, "classalias");
                mxArray *mxDbRoot = mxGetField(mxStructurePtr, 0, "dbroot");
                mxArray *mxDescription = mxGetField(mxStructurePtr, 0, "description");
                mxArray *mxColorSpace = mxGetField(mxStructurePtr, 0, "color_space");
                mxArray *mxClasses = mxGetField(mxStructurePtr, 0, "classes");

                if (matClose(pmat) != 0) {
                    DPRINTF("Error closing file %s\n", file);
                    exit(-1);
                }

                // Create a new ClassModel object                
                jobject jobj = env->NewObject(jtrainingModel, jtrainingModelInit);

                // get File class
                jclass jnewFile = env->FindClass("java/io/File");

                // get File constructor method id
                jmethodID jmethod = env->GetMethodID(jnewFile, "<init>", "(Ljava/lang/String;)V");

                // Call all methods to initialize the object
                if (mxDbRoot != 0) {
                    // create new java String from the matlab array
                    jstring jnewString = env->NewStringUTF(getString(mxDbRoot));
                    // create the File object and initialize with the string
                    jobject jdbroot = env->NewObject(jnewFile, jmethod, jnewString);
                    env->CallObjectMethod(jobj, jsetDatabaseRoot, jdbroot);
                }
                env->CallObjectMethod(jobj, jsetName, env->NewStringUTF(getString(mxClassAlias)));
                env->CallObjectMethod(jobj, jsetDescription, env->NewStringUTF(getString(mxDescription)));

                // Create a new ColorSpace object
                jobject jcolorObject = env->GetStaticObjectField(jcolorClass, mxArrayToColorSpace(env, mxColorSpace));

                // Set the ColorSpace  enum
                env->CallObjectMethod(jobj, jsetColorSpace, jcolorObject);

                // Add the TrainingModel into the array
                env->SetObjectArrayElement(jtrainingModelArray, j++, jobj);

                 if (jcollectedClasses != 0) {
                     
                     // Get the color space of the library as a string - this is used
                     // later for comparison
                    jclass cls = env->GetObjectClass(jcolorObject);
                    jmethodID toStringId = env->GetMethodID(cls, "toString", "()Ljava/lang/String;");
                    jstring jcolorSpace = reinterpret_cast<jstring> (env->CallObjectMethod(jcolorObject, toStringId));            
                    const char *libColorSpaceString = env->GetStringUTFChars((jstring) jcolorSpace, NULL);
        
                    int size = env->GetArrayLength(jcollectedClasses);
                    jclass jClassModel = env->FindClass("org/mbari/aved/classifier/ClassModel");
                    jmethodID jaddClassModel = env->GetMethodID(jtrainingModel, "addClassModel", "(Lorg/mbari/aved/classifier/ClassModel;)Z");
                    jmethodID jgetName = env->GetMethodID(jClassModel, "getName", "()Ljava/lang/String;");
                    jmethodID jgetColorSpace = env->GetMethodID(jClassModel, "getColorSpace", "()Lorg/mbari/aved/classifier/ColorSpace;");                   
                           
                    // In case any of these methods change in the future, check for valid methods
                    ASSERT(jaddClassModel && jgetName && jgetColorSpace);                    
              
                    // Go through all the collected classes and add those that match 
                    // the class name and color space and to this training set
                    for (int j = 0; j < size; j++) {
                        jobject jmodel = env->GetObjectArrayElement(jcollectedClasses, j);
                        jstring jname = (jstring) env->CallObjectMethod(jmodel, jgetName);
                        const char *classname = env->GetStringUTFChars(jname, 0);
                        jobject jcolor = env->CallObjectMethod(jmodel, jgetColorSpace);
                           
                        // Get the color space as a string
                        jclass cls = env->GetObjectClass(jcolor);
                        jmethodID toStringId = env->GetMethodID(cls, "toString", "()Ljava/lang/String;");
                        jstring jcolorSpace = reinterpret_cast<jstring> (env->CallObjectMethod(jcolor, toStringId));                 
                        const char *colorSpaceString = env->GetStringUTFChars((jstring) jcolorSpace, NULL);
     
                        ASSERT(colorSpaceString && libColorSpaceString);
                        
                        jsize len = mxGetNumberOfElements(mxClasses);
                        for (int i = 0; i < len; i++) {
                            const mxArray* cellptr = mxGetCell(mxClasses, i);

                            // Find out how long the input string array is 
                            int buflen = mxGetN(cellptr) + 1;

                            // Allocate enough memory to hold the converted string
                            char *buf = (char *) mxCalloc(buflen, sizeof (char));

                            if (buf == NULL) {
                                ThrowByName(env, "java/lang/RuntimeException", "Not "
                                        "enough heap space to hold string");
                                exit(-1);
                            } else {
                                // Copy the string data into buf
                                mxGetString(cellptr, buf, buflen);
                            }

                            // If a class match is found in the right color space,
                            // insert it into the array
                            if (!strcmp(buf, classname) &&  !strcmp(libColorSpaceString, colorSpaceString) )  {
                                env->CallObjectMethod(jobj, jaddClassModel, jmodel);
                            }
                        }
                    }

                }//end if (jcollectedClasses != 0)
                mxDestroyArray(mxClassAlias);
                mxDestroyArray(mxDbRoot);
                mxDestroyArray(mxDescription);
                mxDestroyArray(mxColorSpace);
                mxDestroyArray(mxClasses); 
            } //end if (pos != string::npos) 
        }// end for (i = 0; i < fcount; i++)
    }//end if (numfound) 

     // Clean up the allocated memory
     if (fcount) { printf("Entries are:"); for (i=0, list=filelist; i<fcount; i++) { printf(" %s\n", (*list)->d_name); free(*list); list++; } free(filelist); printf("\n"); }

     return jtrainingModelArray;
}

/************************************************************************************/
JNIEXPORT jobjectArray JNICALL Java_org_mbari_aved_classifier_ClassifierLibraryJNI_get_1collected_1classes
(JNIEnv * env, jobject obj,
    jstring jmatlabdb) {
    return get_collected_classes(env, obj, jmatlabdb);
}
 
