/*
 * @(#)TestJAIViewer.java
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



package org.mbari.aved.ui.player;

//~--- JDK imports ------------------------------------------------------------

import java.awt.Frame;
import java.awt.Rectangle;
import java.awt.image.RenderedImage;

import java.io.IOException;

/*import com.imagero.reader.*;
import com.imagero.reader.j2.JaiViewer;
import com.jeta.forms.components.panel.FormPanel;
import com.sun.media.jai.widget.DisplayJAI;
import javax.media.jai.PlanarImage;
import javax.swing.JFrame;
import javax.swing.JPanel;

/**
 * This class demonstrates the JAIViewer class
 * It will create an instance of JAIViewer and display it
 * with an image. That's it. Nothing more.
 */
public class TestJAIViewer {
    public static void main(String[] args) {

/*
        FormPanel form = new FormPanel("org/mbari/aved/ui/forms/EventPlayer.xml");

        DisplayJAI myImageJAI = null;

        // Initialize frequently accessed components for getting/setting
        JPanel myImagePanel = form.getPanel("thumbnailDisplayPanel");

        String filename = "video/processedresults/2344_00_32_40_2_evt0017_000369.jpg";

        ImageReader reader;
        try {
            reader = ReaderFactory.createReader(filename);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return;
        }

        int imageIndex = 0;
        int tileWidth = 300; //required tile width
        int tileHeight = 300; //required tile height

        Rectangle roi = null; //image region to read (may be null)
        //really only hint - used only with heavy compressed images (currently JPEG only)
        int cacheHint = com.imagero.reader.MetadataUtils.CACHE_FILE;
        RenderedImage image = (RenderedImage) MetadataUtils.getAsRenderedImageNoEx(reader, imageIndex, tileWidth, tileHeight, roi, cacheHint);

        PlanarImage planarImage = PlanarImage.wrapRenderedImage(image);

        JaiViewer jaiViewer = new JaiViewer(planarImage);

        JFrame window = new JFrame("JAI Sample Program");
        window.add(jaiViewer);
        window.pack();
        window.setVisible(true);
*/
    }
}
