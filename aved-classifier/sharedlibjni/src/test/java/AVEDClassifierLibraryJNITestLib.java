/****************************************************************************/
/* Copyright © 2005 MBARI.                                                  */
/* MBARI Proprietary Information. All rights reserved.                      */
/****************************************************************************/
import junit.framework.*;
import org.mbari.aved.classifier.ClassifierLibraryJNI; 
import java.io.*;


public class AVEDClassifierLibraryJNITestLib extends TestCase
{
    public AVEDClassifierLibraryJNITestLib(String name) {
        super(name);
    }
    protected void setUp() throws Exception {
        super.setUp();
    }
    protected void tearDown() throws Exception {
        super.tearDown();
    }
    
    public void testAVEDClassifierLibraryJNI() throws Exception {
	int i, iclass;
        String logfile = System.getProperty("user.home") + "/matlablog.txt";
	ClassifierLibraryJNI app = new ClassifierLibraryJNI();
        
        System.out.println("initialize library");
        app.initLib(logfile);
        
        System.out.println("close library");
        app.closeLib();
        
        System.exit(0);
    }    
}
