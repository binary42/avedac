/*
 * @(#)AppFrameDispatcher.java   10/03/17
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

//~--- non-JDK imports --------------------------------------------------------

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.mbari.vars.annotation.model.VideoArchiveSet;

//~--- JDK imports ------------------------------------------------------------

import java.awt.Frame;

import java.beans.PropertyChangeListener;

import java.util.Collection;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/**
 * <p>A singleton source to retrieve the reference to the currently
 * used application <code>JFrame</code>. This class is very useful for
 * retrieveing the parent frame for dialogs.</p>
 *
 * @author  <a href="http://www.mbari.org">MBARI</a>
 * @version  $Id: AppFrameDispatcher.java,v 1.1 2010/02/03 21:21:53 dcline Exp $
 */
public class AppFrameDispatcher {
    private static final Log log = LogFactory.getLog(AppFrameDispatcher.class);

    /**
     * AppFrameDispatcher delegates most of the work to a regular Dispatcher
     */
    public static final Dispatcher    DISPATCHER = Dispatcher.getDispatcher(AppFrameDispatcher.class);
    private static AppFrameDispatcher cod;
    private static ProgressDialog     progressDialog;
    private static boolean            showDialog;

    /**
     * Singleton
     */
    AppFrameDispatcher() {
        super();
    }

    /**
     * <p><!-- Method description --></p>
     *
     *
     * @param frame
     */
    public static void setFrame(JFrame frame) {
        DISPATCHER.setValueObject(frame);
        progressDialog = null;    // New dialog will be attached to the new frame.

        String     locale    = LocaleFactory.getLocale();
        Collection shipNames = VideoArchiveSet.getShipNames();

        if (shipNames.contains(locale)) {
            showDialog = false;
        } else {
            showDialog = true;
        }
    }

    /**
     * @return  Returns the JFrame uses as the root container.
     */
    public static Frame getFrame() {
        return (Frame) DISPATCHER.getValueObject();
    }

    /**
     * Singleton access. Get an instance of the AppFrameDispatcher here.
     *
     * @return  An IObservable object
     */
    public static AppFrameDispatcher getInstance() {
        if (cod == null) {
            synchronized (AppFrameDispatcher.class) {
                if (cod == null) {
                    cod = new AppFrameDispatcher();
                }
            }
        }

        return cod;
    }

    /**
     * This methods shows an error dialog if and only if a frame has been set. This
     * allows warning dialogs to appear if an action is called in GUI mode. When used
     * in other contexts, such as jython scripts, or command line applications, no
     * dialog appears.
     *
     * @param message
     */
    public static void showErrorDialog(final String message) {
        if (getFrame() != null) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    if (showDialog) {
                        JOptionPane.showMessageDialog(AppFrameDispatcher.getFrame(), wrapMessage(message),
                                                      "VARS - Error", JOptionPane.ERROR_MESSAGE);
                    }

                    if (log.isErrorEnabled()) {
                        log.error("A dialog with this error message was displayed: " + message);
                    }
                }
            });
        }
    }

    /**
     * This methods shows a warning dialog if and only if a frame has been set. This
     * allows warning dialogs to appear if an action is called in GUI mode. When used
     * in other contexts, such as jython scripts, or command line applications, no
     * dialog appears.
     *
     * @param message
     */
    public static void showWarningDialog(final String message) {
        if (getFrame() != null) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    if (showDialog) {
                        JOptionPane.showMessageDialog(AppFrameDispatcher.getFrame(), wrapMessage(message),
                                                      "VARS - Warning", JOptionPane.ERROR_MESSAGE);
                    }

                    if (log.isWarnEnabled()) {
                        log.warn("A dialog with this warning message was displayed: " + message);
                    }
                }
            });
        }
    }

    public static ProgressDialog getProgressDialog() {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(getFrame());
            progressDialog.setLocationRelativeTo(getFrame());
        }

        return progressDialog;
    }

    /**
     * Wraps a message to a line of length 80 characters. This is a handy
     * method for UI programmers so that they don't need to figure out the
     * line wrapping for dialogs.
     *
     * @param message The message to wrap
     * @return The message wrapped to 80 characters.
     */
    private static String wrapMessage(String message) {
        String[]     words      = message.split(" ");
        int          lineLength = 0;
        StringBuffer sb         = new StringBuffer();

        for (int i = 0; i < words.length; i++) {
            String word = words[i];

            if ((lineLength + word.length()) > 80) {
                sb.append("\n");
            }

            sb.append(word).append(" ");
            lineLength += words[i].length();

            if (lineLength > 80) {
                sb.append("\n");
                lineLength = 0;
            }
        }

        return sb.toString();
    }

    /**
     * @see org.mbari.util.Dispatcher#addPropertyChangeListener(java.beans.PropertyChangeListener)
     */
    public static void addPropertyChangeListener(PropertyChangeListener listener) {
        DISPATCHER.addPropertyChangeListener(listener);
    }

    /**
     * @see org.mbari.util.Dispatcher#removePropertyChangeListener(java.beans.PropertyChangeListener)
     */
    public static void removePropertyChangeListener(PropertyChangeListener listener) {
        DISPATCHER.removePropertyChangeListener(listener);
    }
}
