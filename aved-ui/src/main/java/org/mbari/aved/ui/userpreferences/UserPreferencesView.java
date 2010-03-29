/*
 * @(#)UserPreferencesView.java
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

import org.mbari.aved.ui.ApplicationInfo;
import org.mbari.aved.ui.appframework.JFrameView;
import org.mbari.aved.ui.appframework.ModelEvent;
import org.mbari.aved.ui.userpreferences.UserPreferencesModel.VideoPlayoutMode;

//~--- JDK imports ------------------------------------------------------------

import java.beans.PropertyChangeListener;

import java.io.File;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

public class UserPreferencesView extends JFrameView {

    /*
     *  Component names in the ApplicationPreferences form
     * If any of the component name are changed in the Abeille form designer, they
     * should be modified here too
     */
    public static final String ID_ASK_BEFORE_DELETE       = "askbeforedelete";    // javax.swing.JCheckBox
    public static final String ID_BROWSE_BUTTON_PLAYER    = "browseplayer";
    public static final String ID_BROWSE_BUTTON_SCRATCH   = "browsescratch";      // javax.swing.JButton
    public static final String ID_CLOSE_BUTTON            = "close";
    public static final String ID_PLAYER_DEFAULT_RADIO    = "playervlc";          // javax.swing.JRadioButton
    public static final String ID_PLAYER_OTHER_RADIO      = "playerother";        // javax.swing.JRadioButton
    public static final String ID_SCRATCH_DIRECTORY_COMBO = "scratchdir";         // javax.swing.JComboBox
    public static final String ID_VIDEO_PLAYER_TEXTFIELD  = "videoplayer";        // javax.swing.JTextField
    private final JCheckBox    askBeforeDeleteCheckBox;
    private final JButton      browsePlayerButton;

    // Frequently accessed view variables
    // private final JFormattedTextField  maxDiskSpaceSliderTextField;
    // private final SpinnerNumberModel           textFieldModel;
    private final JRadioButton   playerDefaultRadio, playerOtherRadio;
    private final JComboBox      scratchDirCombo;
    private final JTextField     videoPlayerTextField;

    public UserPreferencesView(UserPreferencesModel model, UserPreferencesController controller) {
        super("org/mbari/aved/ui/forms/UserPreferences.xml", model, controller);
     
        // Initialize frequently accessed fields
        videoPlayerTextField    = (JTextField) getForm().getComponentByName(ID_VIDEO_PLAYER_TEXTFIELD);
        scratchDirCombo         = getForm().getComboBox(ID_SCRATCH_DIRECTORY_COMBO);
        playerDefaultRadio      = getForm().getRadioButton(ID_PLAYER_DEFAULT_RADIO);
        playerOtherRadio        = getForm().getRadioButton(ID_PLAYER_OTHER_RADIO);
        askBeforeDeleteCheckBox = getForm().getCheckBox(ID_ASK_BEFORE_DELETE);
        browsePlayerButton      = (JButton) getForm().getButton(ID_BROWSE_BUTTON_PLAYER);

        JButton closeButton = (JButton) getForm().getButton(ID_CLOSE_BUTTON);

        // Add handler to buttons and combo boxes
        ActionHandler actionHandler = getActionHandler();

        browsePlayerButton.addActionListener(actionHandler);
        closeButton.addActionListener(actionHandler);
        playerDefaultRadio.addActionListener(actionHandler);
        playerOtherRadio.addActionListener(actionHandler);
        scratchDirCombo.addActionListener(actionHandler);
        videoPlayerTextField.addActionListener(actionHandler);
        askBeforeDeleteCheckBox.addActionListener(actionHandler);
        loadModel(model);

        // Set default size and.getName()
        setTitle(ApplicationInfo.getName() + "-" + "User Preferences");
        this.pack();
        this.setResizable(false);
    }

    /**
     * Loads the model data into the view components
     *
     * @param model the model to load from
     */
    private void loadModel(UserPreferencesModel model) {

        // Initialize components with model defaults
        VideoPlayoutMode m = model.getPlayoutMode();

        videoPlayerTextField.setText(m.command);

        if (model.getPlayoutMode() == VideoPlayoutMode.OTHER) {
            playerOtherRadio.setSelected(true);
            browsePlayerButton.setEnabled(true);
            videoPlayerTextField.setEnabled(true);
            videoPlayerTextField.setText(model.getPlayoutMode().command);
        } else {
            playerDefaultRadio.setSelected(true);
            browsePlayerButton.setEnabled(false);
            videoPlayerTextField.setEnabled(false);
        }

        JComboBox scratchComboBox = getForm().getComboBox(ID_SCRATCH_DIRECTORY_COMBO);

        scratchComboBox.insertItemAt(model.getScratchDirectory(), 0);
        scratchComboBox.setSelectedIndex(0);

        askBeforeDeleteCheckBox.setSelected(model.getAskBeforeDelete());        
    }

    public void modelChanged(ModelEvent e) {

        UserPreferencesModel model = (UserPreferencesModel) getModel();
        
        if (e.getID() == UserPreferencesModel.VIDEO_PLAYOUT_CHANGED) {

            // Some logic to enable/disable associated components in setting
            // video playout, based on mode
            if (model.getPlayoutMode() == VideoPlayoutMode.OTHER) {
                browsePlayerButton.setEnabled(true);
                videoPlayerTextField.setEnabled(true);
                videoPlayerTextField.setText(model.getPlayoutMode().command);
            } else {
                browsePlayerButton.setEnabled(false);
                videoPlayerTextField.setEnabled(false);
            }
        }
        else if (e.getID() == UserPreferencesModel.ASK_BEFORE_DELETE_CHANGED) {
            askBeforeDeleteCheckBox.setSelected(model.getAskBeforeDelete());
        }
    }

    public void updateVideoPlayoutDisplay(File dir) {
        scratchDirCombo.insertItemAt(dir, 0);
        scratchDirCombo.setSelectedIndex(0);
    }

    public void updateScratchComboBox(File dir) {
        scratchDirCombo.insertItemAt(dir, 0);
        scratchDirCombo.setSelectedIndex(0);
    }

    /*
     * public JFormattedTextField getMaxDiskSpaceSliderTextField() {
     *   return maxDiskSpaceSliderTextField;
     * }
     */
    public JTextField getVideoPlayerTextField() {
        return videoPlayerTextField;
    }

    /*
     * public void addSliderTextPropertyChangeListener(PropertyChangeListener listener) {
     *   maxDiskSpaceSliderTextField.addPropertyChangeListener(listener);
     * }
     */
}
