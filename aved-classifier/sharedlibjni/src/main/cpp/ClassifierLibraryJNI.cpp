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
#include "libsharedlib.h"
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
 } 

using namespace std; 


//**************************************************************************
// Returns true if a file exists
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


//*************************************************************************** 
// Converts a comma, tab, new-line delimited jstring to a mxArray
// with each sub-string in 1 row wide by x length of the string tokens
// found
//*************************************************************************** 

mxArray *ClassStringToMxArray(JNIEnv *env, jstring str) {
    char *token = NULL;
    char *ptr = (char *) env->GetStringUTFChars(str, 0);
    int numclasses = 0;
    mxArray *trn = NULL;

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
    trn = mxCreateCellMatrix(1, numclasses);

    /* Now stuff training class strings into array*/
    int i = 0;
    ptr = (char *) env->GetStringUTFChars(str, 0);
    token = strtok(ptr, ",\t\n\r");
    for (i = 0; i < numclasses; i++) {
        mxSetCell(trn, i, mxDuplicateArray(mxCreateString(token)));
        token = strtok(NULL, ",\t\n\r");
    }
    return trn;
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
    const char* options[4];
    options[0] = "-logfile";
    options[1] = matlablog;
    options[2] = "-nojvm";
    options[3] = "-nodisplay";

    try { 
        if (!mclInitializeApplication(options, 4)) {
            ThrowByName(env, "java/lang/RuntimeException", "Could not initialize the MCR properly"); 
            return;
        }
        // Initialize the library of MATLAB functions
        if (!libsharedlibInitialize()) {
             ThrowByName(env, "java/lang/RuntimeException", "Could not initialize the Classifier MATLAB library properly");  
            return;
        }

        printf("MCR initialized : %d\n", mclIsMCRInitialized());
        printf("JVM initialized : %d\n", mclIsJVMEnabled());
        printf("Logfile name : %s\n", mclGetLogFileName());
        printf("nodisplay set : %d\n", mclIsNoDisplaySet());
        fflush(stdout);
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

        libsharedlibTerminate();
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
        jstring jkillfile, 
        jstring jclasses, 
        jstring jtrainingAlias,
        jstring jmatlabdb, 
        jobject jcolorSpace,
        jstring jdescription) {
    
    const char *classdir = env->GetStringUTFChars(jclasses, 0);
    const char *trainingalias = env->GetStringUTFChars(jtrainingAlias, 0);
    const char *matlabdbdir = env->GetStringUTFChars(jmatlabdb, 0);
    const char *description = env->GetStringUTFChars(jdescription, 0);
    const char *killfile = env->GetStringUTFChars(jkillfile, 0);

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

    mxArray *d = mxCreateString(matlabdbdir);
    mxArray *t = mxCreateString(trainingalias);
    mxArray *s = mxCreateString(description);
    mxArray *k = mxCreateString(killfile);
    mxArray *c = ClassStringToMxArray(env, jclasses);
    mxArray *color = NULL;
    
     /* Get the color space */
    jclass cls = env->GetObjectClass(jcolorSpace);
    jmethodID toString_ID = env->GetMethodID(cls, "toString", "()Ljava/lang/String;");
    jstring jjcolorSpace = reinterpret_cast<jstring> (env->CallObjectMethod(jcolorSpace, toString_ID));

    const char *jcolorSpaceString = env->GetStringUTFChars((jstring) jjcolorSpace, NULL);
    if (!strcmp(jcolorSpaceString, "GRAY"))
        color = mxCreateDoubleScalar((double) 1);
    else if (!strcmp(jcolorSpaceString, "RGB"))
        color = mxCreateDoubleScalar((double) 2);
    else if (!strcmp(jcolorSpaceString, "YCBCR"))
        color = mxCreateDoubleScalar((double) 3);
    else
        color = mxCreateDoubleScalar((double) 1);

    if (c == NULL) {
        ThrowByName(env, "java/lang/IllegalArgumentException", "Error creating training class array");
        return;
    }

    try {
        if (mlfTrain_classes_ui(k, d, color, t, c, s) == false)
            ThrowByName(env, "java/lang/RuntimeException", "Train class failed");

    } catch (const mwException &e) {
        ThrowByName(env, "java/lang/RuntimeException", e.what());
    } catch (...) {
        ThrowByName(env, "java/lang/RuntimeException", "Unknown matlab exception");
    }

    removeFile(env, killfile);
    mxDestroyArray(d); 
    mxDestroyArray(color); 
    mxDestroyArray(k);
    mxDestroyArray(t);
    mxDestroyArray(s);
    mxDestroyArray(c);
}

/************************************************************************************/
JNIEXPORT void JNICALL Java_org_mbari_aved_classifier_ClassifierLibraryJNI_collect_1tests
(JNIEnv *env, jobject obj, 
        jstring jkillfile, 
        jstring jtestDir, 
        jstring jmatlabdb, 
        jobject jcolorSpace) {

    const char *matlabdbdir, *testdir, *killfile;
    mxArray *mfile = NULL;

    testdir = env->GetStringUTFChars(jtestDir, 0);
    matlabdbdir = env->GetStringUTFChars(jmatlabdb, 0);
    killfile = env->GetStringUTFChars(jkillfile, 0);
    
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

    mxArray *t = mxCreateString(testdir);
    mxArray *d = mxCreateString(matlabdbdir);
    mxArray *k =  mxCreateString(killfile);
    mxArray *color = NULL;

    /* Get the color space */
    jclass cls = env->GetObjectClass(jcolorSpace);
    jmethodID toString_ID = env->GetMethodID(cls, "toString", "()Ljava/lang/String;");
    jstring jjcolorSpace = reinterpret_cast<jstring> (env->CallObjectMethod(jcolorSpace, toString_ID));

    const char *jcolorSpaceString = env->GetStringUTFChars((jstring) jjcolorSpace, NULL);
    if (!strcmp(jcolorSpaceString, "GRAY"))
        color = mxCreateDoubleScalar((double) 1);
    else if (!strcmp(jcolorSpaceString, "RGB"))
        color = mxCreateDoubleScalar((double) 2);
    else if (!strcmp(jcolorSpaceString, "YCBCR"))
        color = mxCreateDoubleScalar((double) 3);
    else
        color = mxCreateDoubleScalar((double) 1);

    /* Run the collection*/
    try {
        if (mlfCollect_tests(1, &mfile, k, t, d, color) == false)
            ThrowByName(env, "java/lang/RuntimeException", "Collect class failed");
    } catch (const mwException &e) {
        ThrowByName(env, "java/lang/RuntimeException", e.what());
    } catch (...) {
        ThrowByName(env, "java/lang/RuntimeException", "Unknown matlab exception");
    }

    mxDestroyArray(t);
    mxDestroyArray(d);
    mxDestroyArray(k);
    mxDestroyArray(color);
}

/************************************************************************************/
JNIEXPORT void JNICALL Java_org_mbari_aved_classifier_ClassifierLibraryJNI_collect_1class
(JNIEnv *env, jobject obj, 
        jstring jkillfile, 
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
    killfile = env->GetStringUTFChars(jkillfile, 0);
    
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

    mxArray *c = mxCreateString(classname);
    mxArray *d = mxCreateString(matlabdbdir);
    mxArray *s = mxCreateString(varsclassname);
    mxArray *p = mxCreateString(description);
    mxArray *k = mxCreateString(killfile);
    mxArray *raw = mxCreateString(imagerawdir);
    mxArray *square = mxCreateString(imagesqdir);
    mxArray *classmetadata = NULL;
    mxArray *classdata = NULL;
    mxArray *trn = NULL;
    mxArray *color = NULL;

    /* Get the color space */
    jclass cls = env->GetObjectClass(jcolorSpace);
    assert(cls);
    jmethodID toString_ID = env->GetMethodID(cls, "toString", "()Ljava/lang/String;");
    assert(toString_ID);
    jstring jjcolorSpace = reinterpret_cast<jstring> (env->CallObjectMethod(jcolorSpace, toString_ID));
    assert(jjcolorSpace);

    const char *jcolorSpaceString = env->GetStringUTFChars((jstring) jjcolorSpace, NULL);
    if (!strcmp(jcolorSpaceString, "GRAY"))
        color = mxCreateDoubleScalar((double) 1);
    else if (!strcmp(jcolorSpaceString, "RGB"))
        color = mxCreateDoubleScalar((double) 2);
    else if (!strcmp(jcolorSpaceString, "YCBCR"))
        color = mxCreateDoubleScalar((double) 3);
    else
        color = mxCreateDoubleScalar((double) 1);

    /* Run the collection*/
    try {
        if (mlfCollect_ui(2, &classmetadata, &classdata, k, 
                raw, square, c, d, color, s, p) == false)
            ThrowByName(env, "java/lang/RuntimeException", "Collect class failed");
    } catch (const mwException &e) {
        ThrowByName(env, "java/lang/RuntimeException", e.what());
    } catch (...) {
        ThrowByName(env, "java/lang/RuntimeException", "Unknown matlab exception");
    }

    
    removeFile(env, killfile);
    mxDestroyArray(trn);
    mxDestroyArray(c);
    mxDestroyArray(k);
    mxDestroyArray(d);
    mxDestroyArray(s);
    mxDestroyArray(p);
    mxDestroyArray(raw);
    mxDestroyArray(square);
    mxDestroyArray(color);
}

/************************************************************************************/
JNIEXPORT void JNICALL Java_org_mbari_aved_classifier_ClassifierLibraryJNI_run_1test
(JNIEnv * env, jobject obj,  
        jstring jkillfile, 
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
    const char *killfile = env->GetStringUTFChars(jkillfile, 0);

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

    mxArray *majorityclassindex = NULL;
    mxArray *probabilityclassindex = NULL;
    mxArray *eventids = NULL;
    mxArray *storeprob = NULL;
    mxArray *d = mxCreateString(matlabdbdir);
    mxArray *tst = mxCreateString(testclass);
    mxArray *trn = mxCreateString(trainingalias);
    mxArray *threshold = mxCreateDoubleScalar((double) jthreshold);
    mxArray *k = mxCreateString(killfile);
    mxArray *color = NULL;
     
    const char *jcolorSpaceString = env->GetStringUTFChars((jstring) jcolorSpace, NULL);
    if (!strcmp(jcolorSpaceString, "GRAY"))
        color = mxCreateDoubleScalar((double) 1);
    else if (!strcmp(jcolorSpaceString, "RGB"))
        color = mxCreateDoubleScalar((double) 2);
    else if (!strcmp(jcolorSpaceString, "YCBCR"))
        color = mxCreateDoubleScalar((double) 3);
    else
        color = mxCreateDoubleScalar((double) 1);
    
    /* Call the native function that in turn calls the matlab code*/
    try {
        if (mlfRun_tests_ui(4, &eventids, &majorityclassindex,
                &probabilityclassindex, &storeprob, k,
                d, color, tst, trn, threshold) == false)
            ThrowByName(env, "java/lang/RuntimeException", "Run test failed");

    } catch (const mwException &e) {
        ThrowByName(env, "java/lang/RuntimeException", "TESTING123");// e.what());
        return;
    } catch (...) {
        ThrowByName(env, "java/lang/RuntimeException", "Unknown matlab exception");
        return;
    }
    stringstream out; 
    
    /* if java arrays less than size of return array throw exception*/
    jsize len = env->GetArrayLength(jeventIdStringArray);

    // Get number of elements in the return storeprob array
    int rlen = mxGetNumberOfElements(storeprob);

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

        cellptr = mxGetCell(storeprob, i);
        ps = (float) mxGetScalar(cellptr);
        cellptr = mxGetCell(majorityclassindex, i);
        pm = (int) int32_t(mxGetScalar(cellptr));
        cellptr = mxGetCell(probabilityclassindex, i);
        pp = (int) int32_t(mxGetScalar(cellptr));

        cellptr = mxGetCell(eventids, i);

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
    mxDestroyArray(tst);
    mxDestroyArray(d); 
    mxDestroyArray(k);
    mxDestroyArray(trn);
    mxDestroyArray(threshold);
    mxDestroyArray(color);
}

/************************************************************************************/
JNIEXPORT void JNICALL Java_org_mbari_aved_classifier_ClassifierLibraryJNI_set_1kill
(JNIEnv * env, jobject obj, jstring jkillfile, jint jstate) {

    const char *killfile = env->GetStringUTFChars(jkillfile, 0);

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
        jstring jkillfile,
        jobjectArray eventfilenamejstringArray,
        jintArray jclassIndexArray,
        jfloatArray jprobabilityArray,
        jstring jtestClass,
        jstring jtrainingAlias,
        jfloat jthreshold,
        jstring jmatlabdb) { 

    const char *testclass = env->GetStringUTFChars(jtestClass, 0);
    const char *matlabdbdir = env->GetStringUTFChars(jmatlabdb, 0);
    const char *trainingalias = env->GetStringUTFChars(jtrainingAlias, 0);
    const char *killfile = env->GetStringUTFChars(jkillfile, 0);
 
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

    mxArray *eventfilenames = NULL;
    mxArray *classindex = NULL;
    mxArray *storeprob = NULL; 
    mxArray *d = mxCreateString(matlabdbdir);
    mxArray *tst = mxCreateString(testclass);
    mxArray *trn = mxCreateString(trainingalias);
    mxArray *threshold = mxCreateDoubleScalar((double) jthreshold);
    mxArray *k = mxCreateString(killfile);
    
    /* Call the native function that in turn calls the matlab code*/
    try {
        if (mlfTest_class(3, &eventfilenames, &classindex,
                &storeprob, k, d, tst, trn, threshold) == false)
            ThrowByName(env, "java/lang/RuntimeException", "Test class failed");
    } catch (const mwException &e) { 
        ThrowByName(env, "java/lang/RuntimeException", e.what());
        return;
    } catch (...) { 
        ThrowByName(env, "java/lang/RuntimeException", "Unknown matlab exception");
        return;
    }  
    
    /* Get number of elements in the return storeprob array */
    int rlen = mxGetNumberOfElements(classindex); 
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
        cellptr = mxGetCell(storeprob, i);
        ps = (float) mxGetScalar(cellptr);
        cellptr = mxGetCell(classindex, i);
        pi = (int) int32_t(mxGetScalar(cellptr));

        cellptr = mxGetCell(eventfilenames, i);

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
            env->SetObjectArrayElement(eventfilenamejstringArray, i, env->NewStringUTF(buf));
        }

        env->SetFloatArrayRegion(jprobabilityArray, (jsize) i, 1, (jfloat *) & ps);
        env->SetIntArrayRegion(jclassIndexArray, (jsize) i, 1, (jint *) & pi);
    }
    
    removeFile(env, killfile);
    /* release memory */
    mxDestroyArray(tst);
    mxDestroyArray(d);
    mxDestroyArray(k);
    mxDestroyArray(trn);
    mxDestroyArray(threshold);
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

//****************************************************************************
//* Converts a mxArray to its jfieldID selection
//**************************************************************************** 
jfieldID mxArrayToColorSpace(JNIEnv * env, mxArray *jcolorSpace) {
    int color = (int) mxGetScalar(jcolorSpace);
    jfieldID jcolor_space = 0;
    jclass jcolor_class = env->FindClass("org/mbari/aved/classifier/ColorSpace");

    switch (color) {
        case(0) :
        case(1) :
                    jcolor_space = env->GetStaticFieldID(jcolor_class,
                    "GRAY",
                    "Lorg/mbari/aved/classifier/ColorSpace;");
            break;
        case(2) :
                    jcolor_space = env->GetStaticFieldID(jcolor_class,
                    "RGB",
                    "Lorg/mbari/aved/classifier/ColorSpace;");
            break;
        case(3) :
                    jcolor_space = env->GetStaticFieldID(jcolor_class,
                    "YCBCR",
                    "Lorg/mbari/aved/classifier/ColorSpace;");
            break;
    }
    return jcolor_space;
}

//***********************************************************************************
// Searches for collected classes in the matlabdb root directory
// Returns an array of ClassModel objects populated
// with the class metadat
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
    struct dirent **filelist = {0};
    int fcount = -1, numfound = 0;
    int i = 0, j = 0;
    string::size_type pos;
    jobjectArray javedclass_model_array = 0;

    // Check if a valid directory
    if (!exists(features)) {
        tmpstr = string("Directory ") + features + string(" does not exist");
        ThrowByName(env, "java/lang/RuntimeException", tmpstr.c_str());
        return 0;
    }

    // Throw exception when no classes found  
    fcount = scandir(featuresDir, &filelist, 0, alphasort);
    if (fcount < 0) {
        tmpstr = string("No classes found in directory ") + features;
        ThrowByName(env, "java/lang/RuntimeException", tmpstr.c_str());
        return 0;
    }

    DPRINTF("Scanning %s for files matching %s\n", featuresDir, filematch);
    for (i = 0; i < fcount; i++) {
        if (strstr(filelist[i]->d_name, filematch))
            numfound++;
    }

    DPRINTF("found %d matches\n", numfound);
    if (numfound) {
        jclass javedclass_model = env->FindClass("org/mbari/aved/classifier/ClassModel");
        jmethodID javedclass_model_init = env->GetMethodID(javedclass_model, "<init>", "()V");
        jobject jobj = env->NewObject(javedclass_model, javedclass_model_init);

        // allocate array for class model
        javedclass_model_array = (jobjectArray) env->NewObjectArray(numfound, javedclass_model, jobj);

        jmethodID jsetRawImageDirectory = env->GetMethodID(javedclass_model, "setRawImageDirectory", "(Ljava/io/File;)V");
        jmethodID jsetSquareImageDirectory = env->GetMethodID(javedclass_model, "setSquareImageDirectory", "(Ljava/io/File;)V");
        jmethodID jsetColorSpace = env->GetMethodID(javedclass_model, "setColorSpace", "(Lorg/mbari/aved/classifier/ColorSpace;)V");
        jmethodID jsetName = env->GetMethodID(javedclass_model, "setName", "(Ljava/lang/String;)V");
        jmethodID jsetVarsClassName = env->GetMethodID(javedclass_model, "setVarsClassName", "(Ljava/lang/String;)V");
        jmethodID jsetDescription = env->GetMethodID(javedclass_model, "setDescription", "(Ljava/lang/String;)V");
        jmethodID jsetDatabaseRoot = env->GetMethodID(javedclass_model, "setDatabaseRoot", "(Ljava/io/File;)V");

        // In case any of these methods change in the future, check for valid methods
        ASSERT(jsetRawImageDirectory && jsetSquareImageDirectory
                && jsetColorSpace && jsetName && jsetVarsClassName
                && jsetDescription && jsetDatabaseRoot);

        // Find the color space class 
        jclass jcolor_class = env->FindClass("org/mbari/aved/classifier/ColorSpace");

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
                    printf("Error opening file %s\n", file);
                    exit(-1);
                }

                // Get the matlab class_metadata structure
                mxArray *structure_ptr = matGetVariable(pmat, "class_metadata");
                assert(structure_ptr);

                // Get the matlab fields from the matlab structure
                mxArray *classname = mxGetField(structure_ptr, 0, "classname");
                mxArray *dbroot = mxGetField(structure_ptr, 0, "dbroot");
                mxArray *varsclassname = mxGetField(structure_ptr, 0, "varsclassname");
                mxArray *description = mxGetField(structure_ptr, 0, "description");
                mxArray *jcolorSpace = mxGetField(structure_ptr, 0, "color_space");
                mxArray *rawDir = mxGetField(structure_ptr, 0, "raw_directory");
                mxArray *squareDir = mxGetField(structure_ptr, 0, "square_directory");

                if (matClose(pmat) != 0) {
                    printf("Error closing file %s\n", file);
                    exit(-1);
                }

                // Create new ClassModel object                
                jobject jobj = env->NewObject(javedclass_model, javedclass_model_init);

                // get File class
                jclass jnew_file = env->FindClass("java/io/File");

                // get File constructor method id
                jmethodID jmethod = env->GetMethodID(jnew_file, "<init>", "(Ljava/lang/String;)V");

                // Call all methods to initialize the object
                if (dbroot != 0) {
                    // create new java String from the matlab array
                    jstring jnew_string = env->NewStringUTF(getString(dbroot));
                    // create the File object and initialize with the string
                    jobject jdbroot = env->NewObject(jnew_file, jmethod, jnew_string);
                    env->CallObjectMethod(jobj, jsetDatabaseRoot, jdbroot);
                }
                if (rawDir != 0) {
                    // create new java String from the matlab array
                    jstring jnew_string = env->NewStringUTF(getString(rawDir));
                    // create the File object and initialize with the string
                    jobject jrawDir = env->NewObject(jnew_file, jmethod, jnew_string);
                    env->CallObjectMethod(jobj, jsetRawImageDirectory, jrawDir);
                }
                if (squareDir != 0) {
                    // create new java String from the matlab array
                    jstring jnew_string = env->NewStringUTF(getString(squareDir));
                    // create the File object and initialize with the string
                    jobject jsquareDir = env->NewObject(jnew_file, jmethod, jnew_string);
                    env->CallObjectMethod(jobj, jsetSquareImageDirectory, jsquareDir);
                }
                env->CallObjectMethod(jobj, jsetName, env->NewStringUTF(getString(classname)));
                env->CallObjectMethod(jobj, jsetVarsClassName, env->NewStringUTF(getString(varsclassname)));
                env->CallObjectMethod(jobj, jsetDescription, env->NewStringUTF(getString(description)));

                // Create a new ColorSpace object
                jobject jcolor_object = env->GetStaticObjectField(jcolor_class, mxArrayToColorSpace(env, jcolorSpace));

                // Set the ColorSpace  enum
                env->CallObjectMethod(jobj, jsetColorSpace, jcolor_object);

                // Add the ClassModel into the array
                env->SetObjectArrayElement(javedclass_model_array, j++, jobj);

                mxDestroyArray(structure_ptr);
            }
            free(filelist[i]);
        }
    }

    free(filelist);
    return javedclass_model_array;
}

/************************************************************************************/
JNIEXPORT jobjectArray JNICALL Java_org_mbari_aved_classifier_ClassifierLibraryJNI_get_1training_1classes
(JNIEnv * env, jobject obj, jstring jmatlabdb) {

    const char *matlabdbdir = env->GetStringUTFChars(jmatlabdb, 0);

    // Do some checking
    if (matlabdbdir == NULL) {
        ThrowByName(env, "java/lang/IllegalArgumentException", "NULL matlabdb name");
        return 0;
    }

    string features = string(matlabdbdir) + string("/training/class");
    const char *featuresDir = features.c_str();
    const char *filematch = "_metadata.mat";
    string tmpstr, tmpstr2;
    struct dirent **filelist = {0};
    int fcount = -1, numfound = 0;
    int i = 0, j = 0;
    string::size_type pos;
    jobjectArray javedtraining_model_array = 0;

    // Check if a valid directory
    if (!exists(features)) {
        tmpstr = string("Directory ") + features + string(" does not exist");
        ThrowByName(env, "java/lang/RuntimeException", tmpstr.c_str());
        return 0;
    }

    // Throw exception when no classes found - it's  probably cleaner to 
    // simply return an empty array here
    fcount = scandir(featuresDir, &filelist, 0, alphasort);
    if (fcount < 0) {
        tmpstr = string("No classes found in directory ") + features;
        ThrowByName(env, "java/lang/RuntimeException", tmpstr.c_str());
        return 0;
    }

    DPRINTF("Scanning %s for files matching %s\n", featuresDir, filematch);
    for (i = 0; i < fcount; i++) {
        if (strstr(filelist[i]->d_name, filematch))
            numfound++;
    }

    DPRINTF("found %d matches\n", numfound);
    if (numfound) {

        // Get the collected classes
        jobjectArray jcollected_classes = get_collected_classes(env, obj, jmatlabdb);

        jclass javedtraining_model = env->FindClass("org/mbari/aved/classifier/TrainingModel");
        jmethodID javedtraining_model_init = env->GetMethodID(javedtraining_model, "<init>", "()V");
        jobject jobj = env->NewObject(javedtraining_model, javedtraining_model_init);

        // Allocate array for training model
        javedtraining_model_array = (jobjectArray) env->NewObjectArray(numfound, javedtraining_model, jobj);

        // Get all the methods needed
        jmethodID jsetColorSpace = env->GetMethodID(javedtraining_model, "setColorSpace", "(Lorg/mbari/aved/classifier/ColorSpace;)V");
        jmethodID jsetName = env->GetMethodID(javedtraining_model, "setName", "(Ljava/lang/String;)V");
        jmethodID jsetDescription = env->GetMethodID(javedtraining_model, "setDescription", "(Ljava/lang/String;)V");
        jmethodID jsetDatabaseRoot = env->GetMethodID(javedtraining_model, "setDatabaseRoot", "(Ljava/io/File;)V");

        // Find the color space class 
        jclass jcolor_class = env->FindClass("org/mbari/aved/classifier/ColorSpace");

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
                    printf("Error opening file %s\n", file);
                    exit(-1);
                }

                // Get the matlab class_metadata structure
                mxArray *structure_ptr = matGetVariable(pmat, "training_metadata");
                assert(structure_ptr);

                // Get the matlab fields from the matlab structure
                mxArray *classalias = mxGetField(structure_ptr, 0, "classalias");
                mxArray *dbroot = mxGetField(structure_ptr, 0, "dbroot");
                mxArray *description = mxGetField(structure_ptr, 0, "description");
                mxArray *jcolorSpace = mxGetField(structure_ptr, 0, "color_space");
                mxArray *classes = mxGetField(structure_ptr, 0, "classes");

                if (matClose(pmat) != 0) {
                    printf("Error closing file %s\n", file);
                    exit(-1);
                }

                // Create a new ClassModel object                
                jobject jobj = env->NewObject(javedtraining_model, javedtraining_model_init);

                // get File class
                jclass jnew_file = env->FindClass("java/io/File");

                // get File constructor method id
                jmethodID jmethod = env->GetMethodID(jnew_file, "<init>", "(Ljava/lang/String;)V");

                // Call all methods to initialize the object
                if (dbroot != 0) {
                    // create new java String from the matlab array
                    jstring jnew_string = env->NewStringUTF(getString(dbroot));
                    // create the File object and initialize with the string
                    jobject jdbroot = env->NewObject(jnew_file, jmethod, jnew_string);
                    env->CallObjectMethod(jobj, jsetDatabaseRoot, jdbroot);
                }
                env->CallObjectMethod(jobj, jsetName, env->NewStringUTF(getString(classalias)));
                env->CallObjectMethod(jobj, jsetDescription, env->NewStringUTF(getString(description)));

                // Create a new ColorSpace object
                jobject jcolor_object = env->GetStaticObjectField(jcolor_class, mxArrayToColorSpace(env, jcolorSpace));

                // Set the ColorSpace  enum
                env->CallObjectMethod(jobj, jsetColorSpace, jcolor_object);

                // Add the TrainingModel into the array
                env->SetObjectArrayElement(javedtraining_model_array, j++, jobj);

                // Go through all the collected classes and add those defined 
                // by class name to this training set
                if (jcollected_classes != 0) {
                    int size = env->GetArrayLength(jcollected_classes);
                    jclass javedclass_model = env->FindClass("org/mbari/aved/classifier/ClassModel");
                    jmethodID jaddClassModel = env->GetMethodID(javedtraining_model, "addClassModel", "(Lorg/mbari/aved/classifier/ClassModel;)V");
                    jmethodID jgetName = env->GetMethodID(javedclass_model, "getName", "()Ljava/lang/String;");

                    for (int j = 0; j < size; j++) {
                        jobject jmodel = env->GetObjectArrayElement(jcollected_classes, j);
                        jstring jname = (jstring) env->CallObjectMethod(jmodel, jgetName);
                        const char *classname = env->GetStringUTFChars(jname, 0);

                        jsize len = mxGetNumberOfElements(classes);
                        for (int i = 0; i < len; i++) {
                            const mxArray* cellptr = mxGetCell(classes, i);

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

                            // If a class match is found, insert it into the array
                            if (!strcmp(buf, classname)) {
                                env->CallObjectMethod(jobj, jaddClassModel, jmodel);
                            }
                        }
                    }

                }
                mxDestroyArray(structure_ptr);
            }
            free(filelist[i]);
        }
    }

    free(filelist);
    return javedtraining_model_array;
}

/************************************************************************************/
JNIEXPORT jobjectArray JNICALL Java_org_mbari_aved_classifier_ClassifierLibraryJNI_get_1collected_1classes
(JNIEnv * env, jobject obj, jstring jmatlabdb) {
    return get_collected_classes(env, obj, jmatlabdb);
}

