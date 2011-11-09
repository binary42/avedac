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
// Note - it is normal to get a "cannot find symbol"error in the
// NetBeans IDE here. Don't worry about it.
// The nar-plugin generates a NarSystem class to assist with
// loading the correct version.  This isn't generated until
// build time in the target directory, so this import isn't resolved.
import java.io.File;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.mbari.aved.classifier.NarSystem;

/**
 * A Wrapper for JNI calls to AVED Classifier functions written in Matlab
 * and exposed through a shared library.
 * @author Danelle Cline
 */
public class ClassifierLibraryJNI {

    public ClassifierLibraryJNI(Object classToUse, boolean test) throws Exception {

        try {

	    if (test == true) { 
		System.out.println("Loading library libsharedlib"); 
		NarSystem.loadLibrary(); 
	    }
	    else {
		String lcOSName = System.getProperty("os.name").toLowerCase();
            	// If running from Mac use system loader
            	if (lcOSName.startsWith("mac os x")) {
                	System.out.println("Loading libsharedlib");
                	NarSystem.loadLibrary(); 
            	} else {
                	// If running from Linux, then assume running from an executable
                	// jar, so use the absolute path
                	System.out.println("Loading libsharedlib");
			String path = getPathToJarfileDir(classToUse); 

                  	if (path != null) {
                    	  String loadFile = path + "/lib/" + "libsharedlib-0.4.3-SNAPSHOT.so";
                    	  System.out.println("Loading " + loadFile);
                    	  System.load(loadFile);
                  	}
		  	else {
                    	  NarSystem.loadLibrary(); 
		  	} 
		}
	    }
        } catch (URISyntaxException ex) {
            String message = "ERROR: could not load library sharedlibjni:" + ex.getMessage();
            Logger.getLogger(ClassifierLibraryJNI.class.getName()).log(Level.SEVERE, null, ex);
        } catch (UnsatisfiedLinkError u) {
            String message = "ERROR: could not load library sharedlibjni:" + u.getMessage();
            Logger.getLogger(ClassifierLibraryJNI.class.getName()).log(Level.SEVERE, null, message);
            throw new Exception(message);
        }

    }

    /**
     * Utility method to get the absolute path to the directory containing
     * the jar file for the calling class
     * 
     * @param classToUse
     * @return  the absolute path to the directory containing the jarfile
     * @throws URISyntaxException
     */
    public static String getPathToJarfileDir(Object classToUse) throws URISyntaxException {
        String url = classToUse.getClass().getResource("/" + classToUse.getClass().getName().replaceAll("\\.", "/") + ".class").toString();
        System.out.println("URL: " + url);
        url = url.substring(4).replaceFirst("/[^/]+\\.jar!.*$", "/");
        System.out.println("URL: " + url);
        try {
            File dir = new File(new URL(url).toURI());
            url = dir.getAbsolutePath();
            System.out.println(url);
        } catch (MalformedURLException mue) {
            url = null;
        } catch (URISyntaxException ue) {
            url = null;
        }
        System.out.println("Path to jar file: " + url);
        return url;
    }

    /**
     * Initialize the native libraries - this must always be called first
     * before calling any of the following methods
     *
     * @param matlabLogFile the file to log the matlab output to
     * @param nojvm set to 1 to use a jvm, 0 to not
     */
    public native void initLib(String matlabLogFileName, int nojvm);

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
     * Delete class information. Call this whenever a  class is deleted
     * @param classname the class name
     * @param matlabdbDirName the root directory of the stored classifier matlab (.mat) data
     * @param colorSpace the color space to use for this class
     */
    public native void delete_class(String classname, String matlabdbDirName, ColorSpace colorspace);

     /**
     * Delete training classes information. Call this whenever a  class is deleted
     * @param matlabdbDirName the root directory of the stored classifier matlab (.mat) data
     * @param trainingalias alias to name this training data to 
     */
    public native void delete_train_class(String matlabdbDirName, String trainingalias);

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
            String matlabdbDirName, ColorSpace colorspace);

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
     * @param maxwinnerindex the max rule class assignment
     * @param probability probability of the class match in the probability
     * rule class assignment
     * @param testclassname a subset of the training classes to test
     * @param trainingalias the name of the training classes to use
     * @param minprobthreshold the minimum probability threshold between 0-1.0
     * for a given assignment and is typically 0.8 - 0.9.  The lower this
     * number, the more misclassifications returned.
     * @param matlabdbDirName the name of the root directory of the
     * stored classifier matlab (.mat) data
     * @param colorSpace the color space to use for this class
     */
    public native void run_test(String killfile, String[] eventids, int[] majoritywinnerindex,
            int[] probabilitywinnerindex, int[] maxwinnerindex, float[] probability, String testclassname,
            String trainingalias, float minprobthreshold, String matlabdbDirName,
            ColorSpace colorspace);

    /**
     * This will initialize the classifier with a training library
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
     * @param colorSpace the color space to use for this test
     * @param description the description of this training class
     */
    public native void train_classes(String killfile, String trainingclasses, String trainingalias,
            String matlabdbDirName, ColorSpace colorspace, String description);

    /**
     * Returns training class names.  A training classes name is simply appended
     * class names in alphabetical order, with "_" in between class names
     *
     * @param matlabdbDirName the  name of the root directory of the
     * stored classifier matlab (.mat) data
     * @return string array with training classes names, or null if no classes
     * found
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
