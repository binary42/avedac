/*
 * @(#)NewClass.java
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



package org.mbari.aved.classifier;

//~--- JDK imports ------------------------------------------------------------

import java.io.File;
import java.io.UnsupportedEncodingException;

import java.net.MalformedURLException;
import java.net.URLEncoder;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.ImageIcon;

/**
 *
 * @author dcline
 */
public class NewClass {

    /** Returns an ImageIcon, or null if the path was invalid. */
    public static ImageIcon createImageIcon(File file) {
        try {
            if (file != null) {
                java.net.URL url = file.toURL();
                String       encodeTargetUrl;

                encodeTargetUrl = URLEncoder.encode(url.toString(), "UTF-8");

                return new ImageIcon(encodeTargetUrl);
            } else {
                System.err.println("Couldn't find file: " + file.getName());
            }
        } catch (MalformedURLException ex) {
            Logger.getLogger(NewClass.class.getName()).log(Level.SEVERE, null, ex);
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(NewClass.class.getName()).log(Level.SEVERE, null, ex);
        }

        return null;
    }

    /**
     * Test main. This build a classifier for testing ouside
     * of the main application.
     * @param args
     */
    public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                try {
                    ImageIcon i =
                        NewClass.createImageIcon(
                            new File(
                                "/Users/dcline/Desktop/TrainingLibrary/Rathbunaster Californicus/f000195evt12.jpg"));

                    System.out.println(i.toString());
                } catch (Exception ex) {
                    Logger.getLogger(NewClass.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });
    }
}
