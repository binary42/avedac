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
import java.io.File;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

public class UserPreferencesView extends JFrameView {

    /*
     *  Component names in the ApplicationPreferences form
     * If any of the component name are changed in the Abeille form designer, they
     * should be modified here too
     */
    public static final String ID_ASK_BEFORE_DELETE       = "askbeforedelete";    // javax.swing.JCheckBox
    public static final String ID_BROWSE_BUTTON_PLAYER = "browseplayer";
    public static final String ID_BROWSE_SCRATCH_DIR = "browsescratchdir";
    public static final String ID_CLOSE_BUTTON            = "close";
    public static final String ID_PLAYER_DEFAULT_RADIO    = "playervlc";          // javax.swing.JRadioButton
    public static final String ID_PLAYER_OTHER_RADIO      = "playerother";        // javax.swing.JRadioButton
    public static final String ID_SCRATCH_DIRECTORY_COMBO = "scratchdir";         // javax.swing.JComboBox
    public static final String ID_VIDEO_PLAYER_TEXTFIELD  = "videoplayer";        // javax.swing.JTextField

    private final JCheckBox    askBeforeDeleteCheckBox;
    private final JButton      browsePlayerButton;
private final JButton browseScratchDirButton;
    private final JComboBox scratchDirComboBox;
    
    // Frequently accessed view variables
    private final JRadioButton   playerDefaultRadio, playerOtherRadio;
    private final JTextField     videoPlayerTextField;

    public UserPreferencesView(UserPreferencesModel model, UserPreferencesController controller) {
        super("org/mbari/aved/ui/forms/UserPreferences.xml", model, controller);
     
        // Initialize frequently accessed fields
        videoPlayerTextField    = (JTextField) getForm().getComponentByName(ID_VIDEO_PLAYER_TEXTFIELD);
        playerDefaultRadio      = getForm().getRadioButton(ID_PLAYER_DEFAULT_RADIO);
        playerOtherRadio        = getForm().getRadioButton(ID_PLAYER_OTHER_RADIO);
        askBeforeDeleteCheckBox = getForm().getCheckBox(ID_ASK_BEFORE_DELETE);
        browsePlayerButton      = (JButton) getForm().getButton(ID_BROWSE_BUTTON_PLAYER);
 browseScratchDirButton = (JButton) getForm().getButton(ID_BROWSE_SCRATCH_DIR);
        scratchDirComboBox = (JComboBox) getForm().getComboBox(ID_SCRATCH_DIRECTORY_COMBO);


        JButton closeButton = (JButton) getForm().getButton(ID_CLOSE_BUTTON);

        // Add handler to buttons and combo boxes
        ActionHandler actionHandler = getActionHandler();

        browsePlayerButton.addActionListener(actionHandler);
 browseScratchDirButton.addActionListener(actionHandler);
       
        closeButton.addActionListener(actionHandler);
        playerDefaultRadio.addActionListener(actionHandler);
        playerOtherRadio.addActionListener(actionHandler);
        videoPlayerTextField.addActionListener(actionHandler);
        askBeforeDeleteCheckBox.addActionListener(actionHandler);
  scratchDirComboBox.addActionListener(actionHandler);
      
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
  scratchDirComboBox.addItem(model.getLastScratchDirectory());

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
        
 } else if (e.getID() == UserPreferencesModel.SCRATCH_DIR_CHANGED) {
            File f = model.getLastScratchDirectory();
            addScratchDirectoryToComboBox(f);
        }
    }  
    public JTextField getVideoPlayerTextField() {
        return videoPlayerTextField;
    }
 /**
 *      * Adds a scratch directory to the scratch directory combo box
 *           * @param f
 *                */
    public void addScratchDirectoryToComboBox(File f) {

        if (f != null) {
            JComboBox cb = scratchDirComboBox;

            if (cb.getItemCount() == 0) {
                cb.addItem(f);
            } else {
                int num = cb.getItemCount();
                boolean found = false;

                for (int i = 0; i < num; i++) {
                    String s = ((File) cb.getItemAt(i)).toString();

                    if (s.equals(f.toString())) {
                        found = true;

                        break;
                    }
                }

                if (!found) {
                    cb.insertItemAt(f, 0);
                    cb.setSelectedIndex(0);
                }
            }
        }
    }
}
