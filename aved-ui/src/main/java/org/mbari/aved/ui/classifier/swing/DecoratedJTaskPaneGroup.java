/*
 * @(#)DecoratedJTaskPaneGroup.java   10/03/17
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



package org.mbari.aved.ui.classifier.swing;

//~--- non-JDK imports --------------------------------------------------------

import com.l2fprod.common.swing.JTaskPaneGroup;

//~--- JDK imports ------------------------------------------------------------

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Action;
import javax.swing.JButton;

/**
 * Adds an indicator to show which button is currently selected. Only makes
 * sense to use this decorator if buttons within the group are going to be
 * used in a mutually exclusive manner.
 *
 * @author achase
 *
 */
public class DecoratedJTaskPaneGroup extends JTaskPaneGroup {
    private static final String indicator = ">> ";
    private static JButton      selected  = null;

    public Component add(Action action) {
        final Component c = super.add(action);

        ((JButton) c).addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (selected != null) {
                    String text = selected.getText();

                    text = text.substring(indicator.length());
                    selected.setText(text);
                    selected.repaint();
                }

                selected = (JButton) c;

                String text = selected.getText();

                selected.setText(indicator + text);
                selected.repaint();
            }
        });

        return c;
    }
}
