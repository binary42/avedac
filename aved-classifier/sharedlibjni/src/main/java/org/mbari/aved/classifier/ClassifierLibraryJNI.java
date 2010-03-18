/*
 * @(#)ClassifierLibraryJNI.java
 * 
 * Copyright 2010 MBARI
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



package org.mbari.aved.classifier;

//~--- non-JDK imports --------------------------------------------------------

import org.mbari.aved.classifier.NarSystem;

/**
 * A Wrapper for JNI calls to AVED Classifier functions written in Matlab
 * and exposed through a shared library.
 * @author Danelle Cline
 */
public class ClassifierLibraryJNI {

    /* For debugging without the JNI layer, set to true */
    private static final boolean debug = false;

    static {
        if (!debug) {
            try {
                System.out.println("Loading sharedlibjni");
                NarSystem.loadLibrary();
            } catch (UnsatisfiedLinkError u) {
                System.err.println("ERROR: could not load library sharedlibjni:" + u.getMessage());
                System.exit(1);
            }
        } else {
            System.out.println("Debug mode - not loading sharedlib");
        }
    }

    /**
     * Initialize the native libraries - this must always be called first
     * before calling any of the following methods
     *
     * @param matlabLogFile the file to log the matlab output to
     */
    public native void initLib(String matlabLogFileName);

    /* Close the library - this must be called when done with this library */
    public native void closeLib();

    /**
     * Sets a kill state. This is the mechanism used to kill a running
     * Matlab function.
     * @param killFileName file name of the kill handle
     * @param state set 1 to kill associated Matlab function; 0 to not kill.
     */
    public native void set_kill(String killFileName, int state);

    /**
     * Collect class information. Call this whenever a new class is created,
     * or images are added to an existing class
     * @param killfile a file used to signal a kill to this matlab function.
     * @param rawDirectoryName name of the directory  raw ppm,jpeg, or
     * jpeg images the squared images were collected from
     * @param squaredDirectoryName directory of ppm,jpeg, or jpeg images to collect
     * @param classname the class name
     * @param matlabdbDirName the root directory of the stored classifier matlab (.mat) data
     * @param colorSpace the color space to use for this class
     * @param varsclassname the VARS classname this class represents
     * @param description the description of this class
     */
    public native void collect_class(String killfile, String rawDirectoryName, String squaredDirectoryName,
                                     String classname, String matlabdbDirName, String varsclassname,
                                     String description, ColorSpace colorspace);

    /**
     * Collect testing images information. Call this before running run_test
     * to gather information about the images to test against training classes
     * This only needs to be called once to initialize the directory of images,
     * or whenever images are added to an existing directory.
     * @param killfile a file used to signal a kill to this matlab function.
     * @param testDirName the name of the directory of test images to collect
     * @param matlabdbDirName the root directory of the stored classifier matlab (.mat) data
     * @param colorSpace the color space to use for this test
     */
    public native void collect_tests(String killfile, String testDirName, String matlabdbDirName,
                                     ColorSpace colorspace);

    /**
     * Test class against set of training classes in <code>trainingclasses
     * </code>. This tests how good a particular training classes discriminates
     * against a test class. This takes a 10% randomly selected subset of the
     * training classes images and tests it against the training classes.
     * This returns two arrays  <code>classindex</code> is the index of the
     * classifier assignment that maps to the <code>trainingclasses</code>,
     * and <code>probability</code> the probability of the mapping
     *
     * @param killfile a file used to signal a kill to this running matlab function.
     * @param eventfilenames event filenames that were randomly selected and tested
     * @param classindex the class assignment
     * @param probability probability of the class match in the probability
     * rule class assignment
     * @param testclassname a subset of the training classes to test
     * @param trainingclasses the name of the trainig classes to use
     * @param minprobthreshold the minimum probability threshold between 0-1.0
     * for a given assignment and is typically 0.8 - 0.9.  The lower this
     * number, the more misclassifications returned.
     * @param matlabdbDirName the root directory of the stored classifier matlab (.mat) data
     */
    public native void test_class(String killfile, String[] eventfilenames, int[] classindex, float[] probability,
                                  String testclassname, String trainingclasses, float minprobthreshold,
                                  String matlabdbDirName);

    /**
     * Test data against set of training classes in <code>trainingclasses
     * </code>. This runs the classifier. This returns three arrays
     * <code>majoritywinnerindex</code> is the majority rule winning
     * index, <code>majoritywinnerindex</code> is the probability
     * winner rule that assigns a class winner to the index of highest
     * probability in at least 30% of the total frames.
     * % Both of these maps to the <code>trainingclasses</code>
     * index. The third array <code>storeprob</code> is the probability of
     * the mapping
     * @param killfile a file used to signal a kill to this matlab function.
     * @param eventids event identifiers - this corresponds to the filename with the "evt" stem
     * @param majoritywinnerindex the majority class assignment
     * @param probabilitywinnerindex the probability rule class assignment
     * @param probability probability of the class match in the probability
     * rule class assignment
     * @param testclassname a subset of the training classes to test
     * @param trainingclasses the name of the trainig classes to use
     * @param minprobthreshold the minimum probability threshold between 0-1.0
     * for a given assignment and is typically 0.8 - 0.9.  The lower this
     * number, the more misclassifications returned.
     * @param matlabdbDirName the name of the root directory of the
     * stored classifier matlab (.mat) data
     */
    public native void run_test(String killfile, String[] eventids, int[] majoritywinnerindex,
                                int[] probabilitywinnerindex, float[] probability, String testclassname,
                                String trainingalias, float minprobthreshold, String matlabdbDirName);

    /**
     * This will initialialize the classifier with a training classes
     * which is a collection of two or more collected classes.
     * All the classes in the training class need to collected before
     * running this. The training class string - which is comma,  space,
     * or new line delimited list of class names.
     *
     * @param killfile a file used to signal a kill to this matlab function.
     * @param trainingclasses a comma, tab, or new-line delimited string
     * of classes to use
     * @param trainingalias alias to name this training data to
     * @param matlabdbDirName the name of the root directory of the stored
     * classifier matlab (.mat) data
     * @param description the description of this training class
     */
    public native void train_classes(String killfile, String trainingclasses, String trainingalias,
                                     String matlabdbDirName, String description);

    /**
     * Assign class for single file. Same as test_class, but for assignment of
     * individual files not necessarily from the  same test class. This would
     * be used to test a single file, versus a collection of files,
     * against a trainingclasses.
     *
     * @param killfile a file used to signal a kill to this matlab function.
     * @param classindex an array of class indexes of the assigned classes
     * @param storeprob a float array that returns the probability a class is assigned
     * @param file the image file to test
     * @param minprobthreshold  the minimum probability threshold between 0-1.0
     * for a given assignment and is typically 0.8 - 0.9.  The lower this
     * number, the more misclassifications returned.
     * @param trainingclasses
     */
    public native void assign_class(String killfile, int[] classindex, float[] storeprob, String file,
                                    float minprobthreshold, String trainingclasses);

    /**
     * Returns training class names.  A training classes name is simply appended
     * class names in alphabetical order, with "_" in between class names
     *
     * @param matlabdbDirName the  name of the root directory of the
     * stored classifier matlab (.mat) data
     * @return string array with training classes names.
     */
    public native TrainingModel[] get_training_classes(String matlabdbDirName);

    /**
     * Returns an @{link org.mbari.aved.classifier.ClassModel}
     * array filled with the collected classes that exist
     *
     * @param matlabdbDirName the name of the root directory of the stored classifier
     * matlab (.mat) data
     * @return ClassModel array with collected classes found, or null
     * if none found
     */
    public native ClassModel[] get_collected_classes(String matlabdbDirName);
}
