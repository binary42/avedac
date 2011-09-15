/*
 * @(#)URLUtils.java
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
