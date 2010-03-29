/*
 * @(#)TestJAIViewer.java
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
