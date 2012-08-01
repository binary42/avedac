/*
 * @(#)FullViewContainer.java
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
import org.mbari.aved.classifier.ClassModel;

public class FullViewContainer extends JPanel implements Dockable {
    private final JFileChooser          chooser = new JFileChooser();
    private final DockKey               dockKey;
    private ClassImageDirectoryFullView fullView;

    public FullViewContainer(int key) {
        super(new BorderLayout());

        File dir = UserPreferences.getModel().getLastClassImageImportDirectory();
        ClassifierModel classifierModel = Classifier.getController().getModel();

        ClassModel classModel = classifierModel.getClassModel(dir.getName());

        ClassImageDirectoryModel model = new ClassImageDirectoryModel(classModel);

        model.setDirectory(dir);
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
                File dir = UserPreferences.getModel().getLastClassImageImportDirectory();
                chooser.setCurrentDirectory(dir);
                
                int choice = chooser.showDialog(FullViewContainer.this, "Choose Dir");

                if (choice == JFileChooser.APPROVE_OPTION) {
                    button.setEnabled(false);

                    File file = chooser.getSelectedFile();

                    dirName.setText(file.getAbsolutePath());

                    ClassifierModel classifierModel = Classifier.getController().getModel();

                    ClassModel classModel = classifierModel.getClassModel(dir.getName());
                    final ClassImageDirectoryModel model = new ClassImageDirectoryModel(classModel);

                    model.setDirectory(file);
                    UserPreferences.getModel().setClassImportDirectory(file);
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
