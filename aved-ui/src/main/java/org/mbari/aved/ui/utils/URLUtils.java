/*
 * @(#)URLUtils.java
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
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;

/**
 *
 * @author dcline
 */
public class URLUtils {
    public static boolean isFileUrl(String urlString) {

        // If this is a url file reference
        if (urlString.startsWith("file:")) {
            return true;
        }

        return false;
    }

    public static boolean isHttpUrl(String urlString) {

        // If this is a url http reference
        if (urlString.startsWith("http:")) {
            return true;
        }

        return false;
    }

    public static boolean isValidURL(URL url) {
        try {
            
            if (isHttpUrl(url.toString())) { 
                URLConnection connection = url.openConnection();

                if (connection instanceof HttpURLConnection) {
                    HttpURLConnection httpConnection = (HttpURLConnection) connection;

                    httpConnection.setConnectTimeout(1000);
                    httpConnection.connect();

                    int response = httpConnection.getResponseCode();

                    // System.out.println(
                    // "Response: " + response);
                    return (response == HttpURLConnection.HTTP_OK);
                }
            } else if (isFileUrl(url.toString())) { 
                File f = new File( URLDecoder.decode( url.getFile(), "UTF-8" ) ); 
                if (f.exists()) {
                    return true;
                }
            }        

            return false;
        } catch (Exception e) {
            return false;
        }
    }
}
