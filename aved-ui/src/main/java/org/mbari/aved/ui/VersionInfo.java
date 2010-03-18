/*
 * @(#)VersionInfo.java   10/03/17
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



/* Created by JReleaseInfo AntTask from Open Source Competence Group */
/* Creation date Mon Mar 15 17:03:28 PDT 2010 */
package org.mbari.aved.ui;

//~--- JDK imports ------------------------------------------------------------

import java.util.Date;

/**
 * This class provides information gathered from the build environment.
 *
 * @author JReleaseInfo AntTask
 */
public class VersionInfo {

    /** buildDate (set during build process to 1268697808814L). */
    private static Date buildDate = new Date(1268697808814L);

    /** version (set during build process to "0.4.1-SNAPSHOT"). */
    private static String version = "0.4.1-SNAPSHOT";

    /**
     * Get buildDate (set during build process to Mon Mar 15 17:03:28 PDT 2010).
     * @return Date buildDate
     */
    public static final Date getBuildDate() {
        return buildDate;
    }

    /**
     * Get buildNumber (set during build process to 13).
     * @return int buildNumber
     */
    public static final int getBuildNumber() {
        return 13;
    }

    /**
     * Get version (set during build process to "0.4.1-SNAPSHOT").
     * @return String version
     */
    public static final String getVersion() {
        return version;
    }
}
