/*
 * @(#)UserPreferencesController.java
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



package org.mbari.aved.ui.userpreferences;

//~--- non-JDK imports --------------------------------------------------------

import org.mbari.aved.ui.appframework.AbstractController;
import org.mbari.aved.ui.userpreferences.UserPreferencesModel.VideoPlayoutMode;

//~--- JDK imports ------------------------------------------------------------

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import java.io.File;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JTextField;
import javax.swing.KeyStroke;

public class UserPreferencesController extends AbstractController {
    public UserPreferencesController(UserPreferencesModel model) {
        setModel(model);
        setView(new UserPreferencesView(getModel(), this));

        // Implement key enter check on text field
        Action checkplayertext = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                String           s    = (String) ((JTextField) e.getSource()).getText();
                VideoPlayoutMode mode = getModel().getPlayoutMode();

                mode.command = s;
                getModel().setPlayoutMode(mode);
            }
        };

        // Add action handler for enter to slider text field
        getView().getVideoPlayerTextField().getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "check");
        getView().getVideoPlayerTextField().getActionMap().put("check", checkplayertext);
    }

    @Override
    public UserPreferencesView getView() {
        return (UserPreferencesView) super.getView();
    }

    @Override
    public UserPreferencesModel getModel() {
        return (UserPreferencesModel) super.getModel();
    }

    /**
     * Creates a browser
     * @param dir sets the current directory to start the browser in
     * @param choosermode sets the mode the chooser should be in
     * @param dialogTitle sets the title of the chooser dialog
     * @return
     */
    private File browse(File dir, int choosermode, String dialogTitle) {
        JFileChooser chooser = new JFileChooser();

        chooser.setFileSelectionMode(choosermode);

        // Initialize the chooser with the model directory
        chooser.setCurrentDirectory(dir);
        chooser.setDialogTitle(dialogTitle);

        if (chooser.showOpenDialog(getView()) == JFileChooser.APPROVE_OPTION) {
            return chooser.getSelectedFile();
        } else {

            // TODO print error message box
            System.out.println("No Selection ");
        }

        return null;
    }

    public void actionPerformed(ActionEvent e) {
        String op = e.getActionCommand();

        if (op.equals("BrowsePlayer")) {
            VideoPlayoutMode m = getModel().getPlayoutMode();
            File             f = browse(new File(m.command), JFileChooser.FILES_AND_DIRECTORIES, "Choose video player");

            if (f != null) {
                m         = VideoPlayoutMode.OTHER;
                m.command = f.toString();
                getModel().setPlayoutMode(m);
            }
        } else if (op.equals("PlayoutOther")) {
            getModel().setPlayoutMode(VideoPlayoutMode.OTHER);
        } else if (op.equals("PlayoutDefault")) {
            getModel().setPlayoutMode(VideoPlayoutMode.DEFAULT);
        } else if (op.equals("AskBeforeDelete")) {
            boolean state = ((JCheckBox) e.getSource()).isSelected();

            getModel().setAskBeforeDelete(state);
        } else if (op.equals("Close")) {
            getView().setVisible(false);
        } else if (op.equals("BrowseScratchDir")) {
            File scratch = getModel().getScratchDirectory();
            File f       = browse(scratch, JFileChooser.DIRECTORIES_ONLY, "Choose scratch directory");

            if (f != null) {
                getModel().setScratchDirectory(f);
            }
        } else if (op.equals("ScratchDirComboBoxChanged")) {
            JComboBox box    = ((JComboBox) e.getSource());
            File      newDir = (File) box.getSelectedItem();

            if (newDir != null) {
                getModel().setScratchDirectory(newDir);
            }
        } else {}
    }
}
