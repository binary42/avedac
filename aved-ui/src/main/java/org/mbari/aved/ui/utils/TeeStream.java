/*
 * @(#)TeeStream.java
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



package org.mbari.aved.ui.utils;

//~--- JDK imports ------------------------------------------------------------

import java.io.PrintStream;

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
