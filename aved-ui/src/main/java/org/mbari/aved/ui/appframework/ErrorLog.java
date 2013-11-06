/*
 * @(#)ErrorLog.java
 * 
 * Copyright 2011 MBARI
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */



package org.mbari.aved.ui.appframework;

//~--- non-JDK imports --------------------------------------------------------

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import org.mbari.aved.ui.ApplicationInfo;
import org.mbari.aved.ui.message.MessagePrintStream;
import org.mbari.aved.ui.utils.TeeStream;

public class ErrorLog {
    private static final ErrorLog INSTANCE = new ErrorLog();
    private static File logroot = new File("/var/tmp");

    private ErrorLog() {
        try {
            File tmp = new File("/var/tmp"); 

            if (tmp.exists() && tmp.canWrite()) {
                logroot = tmp;
            } else if (System.getenv("HOME") != null) {
                logroot = new File(System.getenv("HOME"));
            } else {
                logroot = new File("./");
            }

            MessagePrintStream message = MessagePrintStream.getInstance();

            // Tee standard output to system, to file, and to message console display
            PrintStream out = new PrintStream(new FileOutputStream(logroot + "/" + ApplicationInfo.getName() + ".out"));
            PrintStream teeOut = new TeeStream(System.out, out, message);

            System.setOut(teeOut);
             
            // // Tee standard error to system, to file, and to message console display
            PrintStream err = new PrintStream(new FileOutputStream(logroot + "/" + ApplicationInfo.getName() + ".err"));

            PrintStream teeErr = new TeeStream(System.err, err, message);
            System.setErr(teeErr); 
            
        } catch (FileNotFoundException e) {
            e.printStackTrace(); 
            // TODO: what to do if cannot log ? popup window to confirm with user ?
        }
    }

    public static ErrorLog getInstance() {
        return INSTANCE;
    }
    
    public static File getLogRoot() {
        return logroot;
    }
}
