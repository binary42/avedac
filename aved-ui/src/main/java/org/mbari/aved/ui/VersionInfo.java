/* Created by JReleaseInfo AntTask from Open Source Competence Group */
/* Creation date Wed Nov 09 17:13:44 PST 2011 */
package org.mbari.aved.ui;

import java.util.Date;

/**
 * This class provides information gathered from the build environment.
 * 
 * @author JReleaseInfo AntTask
 */
public class VersionInfo {


   /** buildDate (set during build process to 1320887624846L). */
   private static Date buildDate = new Date(1320887624846L);

   /**
    * Get buildDate (set during build process to Wed Nov 09 17:13:44 PST 2011).
    * @return Date buildDate
    */
   public static final Date getBuildDate() { return buildDate; }


   /**
    * Get buildNumber (set during build process to 16).
    * @return int buildNumber
    */
   public static final int getBuildNumber() { return 16; }


   /** version (set during build process to "0.4.3-SNAPSHOT"). */
   private static String version = "0.4.3-SNAPSHOT";

   /**
    * Get version (set during build process to "0.4.3-SNAPSHOT").
    * @return String version
    */
   public static final String getVersion() { return version; }

}
