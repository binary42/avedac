/* Created by JReleaseInfo AntTask from Open Source Competence Group */
/* Creation date Mon Mar 29 15:15:48 PDT 2010 */
package org.mbari.aved.ui;

import java.util.Date;

/**
 * This class provides information gathered from the build environment.
 * 
 * @author JReleaseInfo AntTask
 */
public class VersionInfo {


   /** buildDate (set during build process to 1269900948218L). */
   private static Date buildDate = new Date(1269900948218L);

   /**
    * Get buildDate (set during build process to Mon Mar 29 15:15:48 PDT 2010).
    * @return Date buildDate
    */
   public static final Date getBuildDate() { return buildDate; }


   /**
    * Get buildNumber (set during build process to 14).
    * @return int buildNumber
    */
   public static final int getBuildNumber() { return 14; }


   /** version (set during build process to "0.4.3-SNAPSHOT"). */
   private static String version = "0.4.3-SNAPSHOT";

   /**
    * Get version (set during build process to "0.4.3-SNAPSHOT").
    * @return String version
    */
   public static final String getVersion() { return version; }

}
