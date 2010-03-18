/*
 * @(#)SwingUtils.java   10/03/17
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

//~--- JDK imports ------------------------------------------------------------

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;

import javax.swing.JComponent;
import javax.swing.JFrame;

/**
 * <p><!--Insert summary here--></p>
 *
 * <h2><u>License</u></h2>
 * <p><font size="-1" color="#336699"><a href="http://www.mbari.org">
 * The Monterey Bay Aquarium Research Institute (MBARI)</a> provides this
 * documentation and code &quot;as is&quot;, with no warranty, express or
 * implied, of its quality or consistency. It is provided without support and
 * without obligation on the part of MBARI to assist in its use, correction,
 * modification, or enhancement. This information should not be published or
 * distributed to third parties without specific written permission from
 * MBARI.</font></p>
 *
 * <p><font size="-1" color="#336699">Copyright 2003 MBARI.
 * MBARI Proprietary Information. All rights reserved.</font></p>
 *
 * @author <a href="http://www.mbari.org">MBARI</a>
 * @version $Id: SwingUtils.java,v 1.1 2010/02/03 21:21:53 dcline Exp $
 */
public class SwingUtils {

    /**
     * Centers the frame on the screen and sets its bounds. THis method should be
     * used after you call frame.pack() or frame.setSize().
     * @param frame
     */
    public static void centerFrame(JFrame frame) {
        GraphicsEnvironment ge     = GraphicsEnvironment.getLocalGraphicsEnvironment();
        Point               center = ge.getCenterPoint();
        Rectangle           bounds = ge.getMaximumWindowBounds();
        int                 w      = Math.max(bounds.width / 2, Math.min(frame.getWidth(), bounds.width));
        int                 h      = Math.max(bounds.height / 2, Math.min(frame.getHeight(), bounds.height));
        int                 x      = center.x - w / 2,
                            y      = center.y - h / 2;

        frame.setBounds(x, y, w, h);

        if ((w == bounds.width) && (h == bounds.height)) {
            frame.setExtendedState(Frame.MAXIMIZED_BOTH);
        }

        frame.validate();
    }

    /**
     * Flash the given component to draw attention to it.
     * @param component The component to flash
     * @param numFlashes The number of times to flash it
     */
    public static void flashJComponent(final JComponent component, final int numFlashes) {
        final int delay    = 300;
        int       duration = delay * numFlashes * 2;

        FlashDecorator.flash(component, delay, duration);

        /*
         * SwingWorker worker = new SwingWorker() {
         *
         *       int timesToFlash = numFlashes * 2 + 1;
         *       int timesFlashed = 0;
         *       boolean isBlue = false;
         *       Color originalBackground =
         *               component.getBackground();
         *       Color originalForeground = component.getForeground();
         *       public Object construct() {
         *               timesFlashed++;
         *               while (timesFlashed < timesToFlash) {
         *                       if (isBlue) {
         *                               component.setBackground(
         *                                       originalBackground);
         *                               component.setForeground(originalForeground);
         *                               component.repaint();
         *                               isBlue = false;
         *                       } else {
         *                               component.setBackground(
         *                                       Color.blue.darker());
         *                               component.setForeground(Color.white);
         *                               component.repaint();
         *
         *                               isBlue = true;
         *                       }
         *                       try {
         *                               Thread.sleep(300);
         *                       } catch (InterruptedException e) {
         *                               //no big deal.
         *                       }
         *                       timesFlashed++;
         *               }
         *               component.setBackground(originalBackground);
         *               return null;
         *       }
         * };
         * worker.start();
         */
    }

    /**
     * <p><!-- Method description --></p>
     *
     *
     * @param frame
     */
    public static void smartSetBounds(JFrame frame) {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int       width      = 500;
        int       height     = 500;

        if (screenSize.height <= 600) {
            height = (int) (screenSize.height * .95);
        } else if (screenSize.height <= 1000) {
            height = (int) (screenSize.height * .90);
        } else if (screenSize.height <= 1200) {
            height = (int) (screenSize.height * .85);
        } else {
            height = (int) (screenSize.height * .80);
        }

        if (screenSize.width <= 1000) {
            width = (int) (screenSize.width * .95);
        } else if (screenSize.width <= 1200) {
            width = (int) (screenSize.width * .90);
        } else if (screenSize.width <= 1600) {
            width = (int) (screenSize.width * .85);
        } else {
            width = (int) (screenSize.width * .80);
        }

        frame.setBounds(0, 0, width, height);
    }
}
