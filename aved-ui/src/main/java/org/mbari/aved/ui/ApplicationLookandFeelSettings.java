/*
 * @(#)ApplicationLookandFeelSettings.java
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



package org.mbari.aved.ui;

//~--- non-JDK imports --------------------------------------------------------


//~--- JDK imports ------------------------------------------------------------

import java.awt.Color;
import javax.swing.UIManager;


/**
 * A few simple settings of the JGoodies Looks. 
 * 
 */
public final class ApplicationLookandFeelSettings {  
    private Color         highlightColor;  
    private Color         selectedColor; 
    
    // Instance Creation ******************************************************
    private ApplicationLookandFeelSettings() {

        // Override default constructor; prevents instantiability.
    }

    public static ApplicationLookandFeelSettings createDefault() {
        
        UIManager.put("window", Color.white);
        UIManager.put("Panel.background", Color.white);
        ApplicationLookandFeelSettings settings = new ApplicationLookandFeelSettings(); 
        settings.setHighlightColor(new Color(108, 207, 255));
        settings.setSelectedColor(new Color(0, 128, 192));

        return settings;
    }

    // Accessors **************************************************************
    private void setHighlightColor(Color color) {
        highlightColor = color;
    }

    public Color getHighlightColor() {
        return highlightColor;
    }

    private void setSelectedColor(Color color) {
        selectedColor = color;
    }

    public Color getSelectedColor() {
        return selectedColor;
    } 
}
