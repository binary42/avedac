/*
 * @(#)PropertiesUtils.java
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

import java.io.IOException;

import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PropertiesUtils {

    /**
     * This method builds a new <code>Properties</code> object, reads in the
     * dss-web.properties file from the classpath, populates the properties and
     * then returns it
     *
     * @return the <code>Properties</code> object with the dss-web.properties
     *         loaded in
     */
    public static Properties getAVEDacProperties() {

        // The properties object to return
        Properties properties = new Properties();

        // Make sure we have the properties read in
        try {
           ClassLoader loader = PropertiesUtils.class.getClassLoader();
           InputStream is = loader.getResourceAsStream("deploy.properties");
            properties.load(is);
        } catch (IOException e) {
            Logger.getLogger(Properties.class.getName()).log(Level.SEVERE, "Could not read deploy.properties from classpath: {0}", e.getMessage());
        } catch (Exception e) {
            Logger.getLogger(Properties.class.getName()).log(Level.SEVERE, "Could not read deploy.properties from classpath: {0}", e.getMessage());
        }

        // Return the object
        return properties;
    }
}
