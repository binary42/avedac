/*
 * @(#)ErrorLog.java   10/03/17
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



package org.mbari.aved.ui.appframework;

//~--- non-JDK imports --------------------------------------------------------

import org.mbari.aved.ui.ApplicationInfo;
import org.mbari.aved.ui.message.MessagePrintStream;
import org.mbari.aved.ui.utils.TeeStream;

//~--- JDK imports ------------------------------------------------------------

import java.io.*;

public class ErrorLog {
    private static final ErrorLog INSTANCE = new ErrorLog();

    private ErrorLog() {
        try {
            String logroot;

            if (System.getenv("PWD") != null) {
                logroot = System.getenv("PWD").toString();
            } else if (System.getenv("USERPROFILE") != null) {
                logroot = System.getenv("USERPROFILE").toString();
            } else {
                logroot = "./";
            }

            MessagePrintStream message = MessagePrintStream.getInstance();

            // Tee standard output to system, to file, and to message console display
            PrintStream out = new PrintStream(new FileOutputStream(logroot + "/" + ApplicationInfo.getName() + ".out"));
            PrintStream tee = new TeeStream(System.out, out, message);

            System.setOut(tee);

            // // Tee standard error to system, to file, and to message console display
            PrintStream err = new PrintStream(new FileOutputStream(logroot + "/" + ApplicationInfo.getName() + ".err"));

            tee = new TeeStream(System.err, err, message);
            System.setErr(tee);
        } catch (FileNotFoundException e) {
            e.printStackTrace();

            // TODO: what to do if cannot log ? popup window to confirm with user ?
        }
    }

    public static ErrorLog getInstance() {
        return INSTANCE;
    }
}
