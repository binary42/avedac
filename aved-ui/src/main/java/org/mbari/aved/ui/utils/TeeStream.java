/*
 * @(#)TeeStream.java
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



package org.mbari.aved.ui.utils;

//~--- JDK imports ------------------------------------------------------------

import java.io.*;

//All writes to this print stream are copied to two print streams
//From http://www.exampledepot.com/egs/java.lang/Redirect.html
public class TeeStream extends java.io.PrintStream {
    java.io.PrintStream out2;
    java.io.PrintStream out3;

    public TeeStream(PrintStream out1, PrintStream out2, PrintStream out3) {
        super(out1);
        this.out2 = out2;
        this.out3 = out3;
    }

    public void write(byte buf[], int off, int len) {
        try {
            super.write(buf, off, len);
            out2.write(buf, off, len);
            out3.write(buf, off, len);
        } catch (Exception e) {}
    }

    public void flush() {
        super.flush();
        out2.flush();
        out3.flush();
    }
}
