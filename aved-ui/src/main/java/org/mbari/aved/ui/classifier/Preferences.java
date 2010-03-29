/*
 * @(#)Preferences.java
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



package org.mbari.aved.ui.classifier;

//~--- non-JDK imports --------------------------------------------------------

import org.mbari.aved.ui.classifier.ClassifierModel;

//~--- JDK imports ------------------------------------------------------------

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JFrame;

/**
 *
 * @author dcline
 */
public class Preferences {
    private final PreferencesController controller;

    public Preferences(ClassifierModel model) {
        controller = new PreferencesController(model);
    }

    JFrame getView() {
        return (JFrame) controller.getView();
    }

    /**
     * Test  main
     * @param args
     */
    public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                try {
                    Preferences c = new Preferences(new ClassifierModel());

                    c.getView().setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                    c.getView().setVisible(true);
                } catch (Exception ex) {
                    Logger.getLogger(Preferences.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });
    }
}
