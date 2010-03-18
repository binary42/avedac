/*
 * @(#)FlashDecorator.java   10/03/17
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

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JComponent;
import javax.swing.Timer;

/**
 *
 * <p>A decorator that can be used to flash a component. Use it like so:</p>
 * <pre>
 * FlashDecorator flash = new FlashDecorator(new JButton());
 * flash.setFlashEnabled(true); // button will flash
 * flash.setFlashEnabled(false); //button will stop flashing
 *
 *  // or if you just want a component to flash for a short duration
 * flash.flash(1500); // flashes component for 1.5 seconds.
 * </pre>
 *
 * @author Brian Schlining
 * @version $Id: FlashDecorator.java,v 1.1 2010/02/03 21:21:53 dcline Exp $
 */
public class FlashDecorator {

    /**
     * This is the timer that actually flashes a component by switching
     * foreground and background colors
     */
    private final Timer flashTimer = new Timer(300, new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            final Color fg = component.getForeground();
            final Color bg = component.getBackground();

            component.setForeground(bg);
            component.setBackground(fg);
        }
    });

    /**
     * This timer stops the flashing after a specified duration. It's used by
     * the flash() method.
     */
    private final Timer durationTimer = new Timer(1500, new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            setFlashEnabled(false);
        }
    });
    private Color            backGroundColor;
    private final JComponent component;
    private boolean          flashEnabled;
    private Color            foregroundColor;

    /**
     * Constructs ...
     *
     *
     * @param component
     */
    public FlashDecorator(final JComponent component) {
        this.component = component;
        durationTimer.setRepeats(false);
    }

    /**
     * Causes the JComponent to flash for a specified time.
     * @param duration The millisecs to flash the component
     */
    public void flash(int duration) {
        durationTimer.setDelay(duration);
        setFlashEnabled(true);
        durationTimer.start();
    }

    /**
     * Convience method for flashing a component. For example to flash a button
     * 3 times a seconds for 2 seconds do the following:
     * <pre>
     * FlashDecorator.flash(myJButton, 333, 2000);
     * </pre>
     * @param component The component to be flashed
     * @param interval The interval between flashes in millisecs
     * @param duration The duration in millisecs that the component should be flashed.
     */
    public static final void flash(JComponent component, int interval, int duration) {
        FlashDecorator flashDecorator = new FlashDecorator(component);

        flashDecorator.setFlashInterval(interval);
        flashDecorator.flash(duration);
    }

    /**
     * <p><!-- Method description --></p>
     *
     *
     * @return
     */
    public JComponent getComponent() {
        return component;
    }

    /**
     * <p><!-- Method description --></p>
     *
     *
     * @return
     */
    public int getFlashInterval() {
        return flashTimer.getDelay();
    }

    /**
     *
     * @return <strong>true></strong> if flashing is currently enabled.
     */
    public boolean isFlashEnabled() {
        return flashEnabled;
    }

    /**
     * Setting this to true
     * @param flashEnabled
     */
    public void setFlashEnabled(final boolean flashEnabled) {
        if (flashEnabled) {
            if (!flashTimer.isRunning()) {
                foregroundColor = component.getForeground();
                backGroundColor = component.getBackground();
                flashTimer.start();
            }
        } else {
            if (flashTimer.isRunning()) {
                component.setForeground(foregroundColor);
                component.setBackground(backGroundColor);
                flashTimer.stop();
            }
        }
    }

    /**
     * Sets the flashInterval in milliseconds. The default is 300.
     * @param millisec
     */
    public void setFlashInterval(int millisec) {
        flashTimer.setDelay(millisec);
    }
}
