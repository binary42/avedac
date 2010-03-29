/*
 * @(#)LocaleFactory.java
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



package org.mbari.aved.ui.classifier.knowledgebase;

//~--- non-JDK imports --------------------------------------------------------

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.mbari.vars.annotation.model.VideoArchiveSet;

//~--- JDK imports ------------------------------------------------------------

import java.util.Collection;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Acquires the deployment location (Shore, Point Lobos, Western Flyer) from a
 * properties file, vars.propertes. Attempts to set certain behaviors base on
 * location. These behaviors include:
 * <ul>
 * <li>Initializing the VCR on COM1 and calling the play method so that it
 * acquires the time-code when on the ships.</li>
 * </ul>
 *
 * @author brian
 *
 */
public class LocaleFactory {
    private static final Log log = LogFactory.getLog(LocaleFactory.class);
    private static String    cameraPlatform;
    private static boolean   isInitialized;
    private static String    locale;

    /**
     *
     */
    private LocaleFactory() {
        super();

        // No external instantiation allowed
    }

    /**
     *
     * @return The name of the camera platform that VARS is configured for. This
     *         is configured by in the vars.properties file. The platforms are
     *         "Ventana" and "Tiburon"
     */
    public static String getCameraPlatform() {
        initialize();

        return cameraPlatform;
    }

    /**
     *
     * @return The location that vars is installed at. This is configured in the
     *         vars.properties file. This is "Shore", "Western Flyer", "Point
     *         Lobos". If none was specified in the vars.properties file then this
     *         returns null.
     */
    public static String getLocale() {
        initialize();

        return locale;
    }

    /**
     * <p><!-- Method description --></p>
     *
     */
    private static void initialize() {
        if (!isInitialized) {
            if (locale == null) {
                ResourceBundle bundle = ResourceBundle.getBundle("vars");

                try {
                    locale = bundle.getString("deployment.locale");
                } catch (MissingResourceException e) {
                    log.info("The property 'deployment.locale' was not found in vars.properties");
                }

                if (locale != null) {
                    final String s = locale.toLowerCase();
                    Object[]     c = VideoArchiveSet.getCameraPlatforms().toArray();

                    cameraPlatform = c[0].toString();
                }
            }

            isInitialized = true;
        }
    }
}
