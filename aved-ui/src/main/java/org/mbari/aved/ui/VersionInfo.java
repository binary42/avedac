/*
 * @(#)VersionInfo.java
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



/* Created by JReleaseInfo AntTask from Open Source Competence Group */
/* Creation date Mon Mar 29 15:15:48 PDT 2010 */
package org.mbari.aved.ui;

//~--- JDK imports ------------------------------------------------------------

import java.util.Date;

/**
 * This class provides information gathered from the build environment.
 *
 * @author JReleaseInfo AntTask
 */
public class VersionInfo {

    /** buildDate (set during build process to 1269900948218L). */
    private static Date buildDate = new Date(1269900948218L);

    /** version (set during build process to "0.4.3-SNAPSHOT"). */
    private static String version = "0.4.3-SNAPSHOT";

    /**
     * Get buildDate (set during build process to Mon Mar 29 15:15:48 PDT 2010).
     * @return Date buildDate
     */
    public static final Date getBuildDate() {
        return buildDate;
    }

    /**
     * Get buildNumber (set during build process to 14).
     * @return int buildNumber
     */
    public static final int getBuildNumber() {
        return 14;
    }

    /**
     * Get version (set during build process to "0.4.3-SNAPSHOT").
     * @return String version
     */
    public static final String getVersion() {
        return version;
    }
}
