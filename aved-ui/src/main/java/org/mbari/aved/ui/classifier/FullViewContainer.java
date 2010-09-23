/*
 * @(#)FullViewContainer.java
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

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import com.vlsolutions.swing.docking.DockKey;
import com.vlsolutions.swing.docking.Dockable;

import org.mbari.aved.ui.userpreferences.UserPreferences;

//~--- JDK imports ------------------------------------------------------------

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.io.File;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.filechooser.FileFilter;

public class FullViewContainer extends JPanel implements Dockable {
    private final JFileChooser          chooser = new JFileChooser();
    private final DockKey               dockKey;
    private ClassImageDirectoryFullView fullView;

    public FullViewContainer(int key) {
        super(new BorderLayout());

        ClassImageDirectoryModel model = new ClassImageDirectoryModel();

        model.setDirectory(UserPreferences.getModel().getLastClassImageImportDirectory());
        fullView = new ClassImageDirectoryFullView(model, key);
        build();
        chooser.setAcceptAllFileFilterUsed(false);
        chooser.addChoosableFileFilter(new FileFilter() {
            public boolean accept(File f) {
                if (f.isDirectory()) {
                    return true;
                }

                return false;
            }
            public String getDescription() {
                return "directory chooser";
            }
        });
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        dockKey = new DockKey("Images to be organized");
        dockKey.setCloseEnabled(false);
        dockKey.setAutoHideEnabled(false);
        dockKey.setTooltip("Images in this directory can be dragged to other directories");
    }

    private void build() {
        FormLayout      layout   = new FormLayout("4dlu, right:pref, 4dlu, pref:grow, 2dlu, left:pref, 2dlu", "pref");
        CellConstraints cc       = new CellConstraints();
        JPanel          topPanel = new JPanel(layout);

        topPanel.add(new JLabel("Dir:"), cc.xy(2, 1));

        final JTextField dirName = new JTextField(fullView.getModel().getDirectory());

        dirName.setEditable(false);
        topPanel.add(dirName, cc.xy(4, 1));

        final JButton button = new JButton("Change");

        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int choice = chooser.showDialog(FullViewContainer.this, "Choose Dir");

                if (choice == JFileChooser.APPROVE_OPTION) {
                    button.setEnabled(false);

                    File file = chooser.getSelectedFile();

                    dirName.setText(file.getAbsolutePath());

                    final ClassImageDirectoryModel model = new ClassImageDirectoryModel();

                    model.setDirectory(file);
                    UserPreferences.getModel().setClassTrainingDirectory(file);
                    FullViewContainer.this.remove(fullView);
                    fullView = new ClassImageDirectoryFullView(model, 0);
                    FullViewContainer.this.add(fullView);
                    FullViewContainer.this.revalidate();
                    FullViewContainer.this.repaint();
                    button.setEnabled(true);
                }
            }
        });
        topPanel.add(button, cc.xy(6, 1));
        this.add(topPanel, BorderLayout.NORTH);
        this.add(fullView, BorderLayout.CENTER);
    }

    public Component getComponent() {
        return this;
    }

    public ClassImageDirectoryFullView getView() {
        return fullView;
    }

    public DockKey getDockKey() {
        return dockKey;
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Test ImageDirectorySynopsis");

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(new FullViewContainer(1));
        frame.pack();
        frame.setVisible(true);
    }
}
