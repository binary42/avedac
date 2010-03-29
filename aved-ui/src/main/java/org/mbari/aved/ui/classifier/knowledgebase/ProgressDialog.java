/*
 * @(#)ProgressDialog.java
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

import java.awt.BorderLayout;
import java.awt.Frame;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

public class ProgressDialog extends JDialog {
    private JLabel       descriptionLabel = null;
    private JPanel       jContentPane     = null;
    private JProgressBar progressBar      = null;

    /**
     * This is the default constructor
     */
    public ProgressDialog() {
        this(null);
    }

    public ProgressDialog(Frame owner) {
        super(owner);
        initialize();
    }

    public void setVisible(boolean visible) {
        if (!visible) {
            setLabel("");
            getProgressBar().setString("");
            getProgressBar().setValue(0);
        }

        super.setVisible(visible);
    }

    /**
     * This method initializes this
     *
     * @return void
     */
    private void initialize() {
        this.setContentPane(getJContentPane());
        this.setSize(250, 150);
    }

    public void setLabel(String label) {
        descriptionLabel.setText(label);
    }

    /**
     * This method initializes jContentPane
     *
     * @return javax.swing.JPanel
     */
    private JPanel getJContentPane() {
        if (jContentPane == null) {
            descriptionLabel = new JLabel();
            descriptionLabel.setText("");
            jContentPane = new JPanel();
            jContentPane.setLayout(new BorderLayout());
            jContentPane.add(descriptionLabel, java.awt.BorderLayout.NORTH);
            jContentPane.add(getProgressBar(), java.awt.BorderLayout.CENTER);
        }

        return jContentPane;
    }

    /**
     * This method initializes progressBar
     *
     * @return javax.swing.JProgressBar
     */
    public JProgressBar getProgressBar() {
        if (progressBar == null) {
            progressBar = new JProgressBar();
        }

        return progressBar;
    }
}
