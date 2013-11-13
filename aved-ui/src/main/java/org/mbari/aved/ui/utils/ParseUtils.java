/*
 * @(#)ParseUtils.java
 * 
 * Copyright 2013 MBARI
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

import java.io.File;

import java.util.StringTokenizer;

public class ParseUtils {

    /**
     * Remove all directories from filename and returns remaining string
     * @param filename  the filename, e.g. /foo/foobar/foo.txt
     * @return filename or empty string if filename is a directory, e.g. foo.txt
     */
    public static String parseFileNameRemoveDirectory(String filename) {
        String fileName = filename;

        fileName = fileName.replace('/', File.separatorChar);
        fileName = fileName.replace('\\', File.separatorChar);

        int i = fileName.lastIndexOf(File.separatorChar);

        if (i > 0) {
            fileName = fileName.substring(i + 1);
        }

        return fileName;
    }

    /**
     * Returns everything except the contents of the last . in the input string
     * @param s the filename to parse, e.g. /foo/foobar/foo.txt
     * @return filename without extension, /foo/foobar/foo
     */
    public static String removeFileExtension(String s) {
        String fileName = s;

        while (fileName.lastIndexOf('.') >= 0) {
            fileName = fileName.substring(0, fileName.lastIndexOf('.'));
        }

        return fileName;
    }

    /*
     *  Replaces *all* occurences of @param original string with @param replacement
     *
     * From http://leepoint.net/notes-java/data/strings/96string_examples/example_replaceWord.html
     */
    public static String replaceAllWords2(String original, String find, String replacement) {
        StringBuilder   result     = new StringBuilder(original.length());
        String          delimiters = "+-*/(),. ";
        StringTokenizer st         = new StringTokenizer(original, delimiters, true);

        while (st.hasMoreTokens()) {
            String w = st.nextToken();

            if (w.equals(find)) {
                result.append(replacement);
            } else {
                result.append(w);
            }
        }

        return result.toString();
    }

    /**
     * Removes all white spaces
     * @param s
     * @return
     */
    public static String removeSpaces(String s) {
        StringTokenizer st = new StringTokenizer(s, " ", false);
        String          t  = "";

        while (st.hasMoreElements()) {
            t += st.nextElement();
        }

        return t;
    }
}
