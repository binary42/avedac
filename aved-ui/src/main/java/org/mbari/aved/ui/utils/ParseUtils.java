/*
 * @(#)ParseUtils.java
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
