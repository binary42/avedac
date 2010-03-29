/*
 * @(#)URLUtils.java
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

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

/**
 *
 * @author dcline
 */
public class URLUtils {
    public static boolean isFile(String urlString) {

        // If this is a true url reference and not a local file
        if (urlString.startsWith("http:") || urlString.startsWith("file:")) {
            return false;
        }

        return true;
    }

    public static boolean isURL(String urlString) {

        // If this is a true url reference and not a local file
        if (urlString.startsWith("http:") || urlString.startsWith("file:")) {
            return true;
        }

        return false;
    }

    public static boolean isValidURL(String urlString) {
        try {
            URL           url        = new URL(urlString);
            URLConnection connection = url.openConnection();

            if (connection instanceof HttpURLConnection) {
                HttpURLConnection httpConnection = (HttpURLConnection) connection;

                httpConnection.connect();

                int response = httpConnection.getResponseCode();

                // System.out.println(
                // "Response: " + response);
                return (response == HttpURLConnection.HTTP_OK);
            }

            return false;
        } catch (Exception e) {
            return false;
        }
    }
}
