/*
 * @(#)UserPreferencesView.java
 * 
 * Copyright 2013 MBARI
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

import org.mbari.aved.ui.ApplicationInfo;
import org.mbari.aved.ui.appframework.JFrameView;
import org.mbari.aved.ui.appframework.ModelEvent;
import org.mbari.aved.ui.userpreferences.UserPreferencesModel.VideoPlayoutMode;

//~--- JDK imports ------------------------------------------------------------

import java.awt.Component;

import java.io.File;

import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

public class UserPreferencesView extends JFrameView {

    /*
     *  Component names in the ApplicationPreferences form
     * If any of the component name are changed in the Abeille form designer, they
     * should be modified here too
     */
    public static final String ID_ASK_BEFORE_DELETE       = "askbeforedelete";    // javax.swing.JCheckBox
    public static final String ID_BROWSE_BUTTON_PLAYER    = "browseplayer";
    public static final String ID_BROWSE_SCRATCH_DIR      = "browsescratchdir"; 
    public static final String ID_PLAYER_DEFAULT_RADIO    = "playervlc";          // javax.swing.JRadioButton
    public static final String ID_PLAYER_OTHER_RADIO      = "playerother";        // javax.swing.JRadioButton
    public static final String ID_FFMPEG__RADIO           = "convertffmpeg";      // javax.swing.JRadioButton
    public static final String ID_TRANSCODE_RADIO         = "converttranscode";   // javax.swing.JRadioButton
    public static final String ID_SCRATCH_DIRECTORY_COMBO = "scratchdir";         // javax.swing.JComboBox
    public static final String ID_VIDEO_PLAYER_TEXTFIELD  = "videoplayer";        // javax.swing.JTextField
    private final JCheckBox    askBeforeDeleteCheckBox;
    private final JButton      browsePlayerButton;
    private final JButton      browseScratchDirButton;
    private final JRadioButton playerDefaultRadio, playerOtherRadio;
    private final JRadioButton ffmpegRadio, transcodeRadio;
    private final JComboBox    scratchDirComboBox;
    private final JTextField   videoPlayerTextField;

    public UserPreferencesView(UserPreferencesModel model, UserPreferencesController controller) {
        super("org/mbari/aved/ui/forms/UserPreferences.xml", model, controller);

        // Initialize frequently accessed fields
        videoPlayerTextField    = (JTextField) getForm().getComponentByName(ID_VIDEO_PLAYER_TEXTFIELD);
        playerDefaultRadio      = getForm().getRadioButton(ID_PLAYER_DEFAULT_RADIO);
        playerOtherRadio        = getForm().getRadioButton(ID_PLAYER_OTHER_RADIO);
        askBeforeDeleteCheckBox = getForm().getCheckBox(ID_ASK_BEFORE_DELETE);
        browsePlayerButton      = (JButton) getForm().getButton(ID_BROWSE_BUTTON_PLAYER);
        browseScratchDirButton  = (JButton) getForm().getButton(ID_BROWSE_SCRATCH_DIR);
        scratchDirComboBox      = (JComboBox) getForm().getComboBox(ID_SCRATCH_DIRECTORY_COMBO);
        ffmpegRadio             =  getForm().getRadioButton(ID_FFMPEG__RADIO);
        transcodeRadio          =  getForm().getRadioButton(ID_TRANSCODE_RADIO);
         
        // Add handler to buttons and combo boxes
        ActionHandler actionHandler = getActionHandler();

        browsePlayerButton.addActionListener(actionHandler);
        browseScratchDirButton.addActionListener(actionHandler); 
        playerDefaultRadio.addActionListener(actionHandler);
        playerOtherRadio.addActionListener(actionHandler);
        videoPlayerTextField.addActionListener(actionHandler); 
        askBeforeDeleteCheckBox.addActionListener(actionHandler);
        scratchDirComboBox.addActionListener(actionHandler);
        ffmpegRadio.addActionListener(actionHandler);
        transcodeRadio.addActionListener(actionHandler);
        
        loadModel(model);

        // Set default size and name
        setTitle(ApplicationInfo.getName() + "-" + "User Preferences");
        this.pack();
    }

    /**
     * Loads the model data into the view components
     *
     * @param model the model to load from
     */
    public void loadModel(UserPreferencesModel model) {

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

        askBeforeDeleteCheckBox.setSelected(model.getAskBeforeDelete());
        scratchDirComboBox.setRenderer(new CustomListCellRenderer());
        scratchDirComboBox.setEditable(false);

        File[] dirs = { model.getScratchDirectory() };

        initializeScratchDirectories(dirs);
        scratchDirComboBox.setSelectedIndex(0); 
        
        ffmpegRadio.setSelected(model.getEnableFfmpeg());
        transcodeRadio.setSelected(!model.getEnableFfmpeg());
    }

    /**
     * Sets the data model that the image directory <code>JComboBox</code>
     * uses to obtain the list of items.
     *
     * @param directories the list of directories to populte the box with
     *
     */
    void initializeScratchDirectories(File[] directories) {
        scratchDirComboBox.setModel(new DefaultComboBoxModel(directories));
    }

    @Override
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
        } else if (e.getID() == UserPreferencesModel.ASK_BEFORE_DELETE_CHANGED) {
            askBeforeDeleteCheckBox.setSelected(model.getAskBeforeDelete());
        } else if (e.getID() == UserPreferencesModel.SCRATCH_DIR_CHANGED) {
            File f = model.getScratchDirectory();

            addScratchDirectoryToComboBox(f);
        }

        scratchDirComboBox.repaint();
    }

    public JTextField getVideoPlayerTextField() {
        return videoPlayerTextField;
    }

    /**
     * Adds a scratch directory to the scratch directory combo box model
     * @param f
     */
    public void addScratchDirectoryToComboBox(File f) {
        if (f != null) {
            DefaultComboBoxModel scratchDirListModel = (DefaultComboBoxModel) scratchDirComboBox.getModel();

            if (scratchDirListModel.getSize() == 0) {
                scratchDirListModel.addElement(f);
            } else {
                int     num   = scratchDirListModel.getSize();
                boolean found = false;

                for (int i = 0; i < num; i++) {
                    String s = scratchDirListModel.getElementAt(i).toString();

                    if (s.equals(f.toString())) {
                        found = true;

                        break;
                    }
                }

                if (!found) {
                    scratchDirListModel.addElement(f);
                }
            }
        }
    }

    public class CustomListCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
                boolean cellHasFocus) {
            String s = value.toString();

            setText(s);

            if (isSelected) {
                setBackground(list.getSelectionBackground());
                setForeground(list.getSelectionForeground());
            } else {
                setBackground(list.getBackground());
                setForeground(list.getForeground());
            }

            setEnabled(list.isEnabled());
            setFont(list.getFont());
            setOpaque(true);

            return this;
        }
    }
}
