/*
 * @(#)UserPreferencesController.java
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

    public UserPreferencesView getView() {
        return (UserPreferencesView) super.getView();
    }

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
        } else {}
    }
}
