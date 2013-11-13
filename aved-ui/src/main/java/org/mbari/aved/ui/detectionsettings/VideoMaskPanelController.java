/*
 * @(#)VideoMaskPanelController.java
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



package org.mbari.aved.ui.detectionsettings;

//~--- non-JDK imports --------------------------------------------------------

import org.mbari.aved.ui.userpreferences.UserPreferences;
import org.mbari.aved.ui.userpreferences.UserPreferencesModel;

//~--- JDK imports ------------------------------------------------------------

import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;

import java.io.File;

import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;

public class VideoMaskPanelController extends DetectionAbstractController implements MouseMotionListener {
    private static final String EMPTY_MASK_DESCRIPTION = "-----------------";
    private JFrame              myParentFrame          = null;

    // Flag used to disable combo box handler
    private Boolean myDisableComboBoxHandler = false;

    public VideoMaskPanelController(JFrame parent, DetectionSettingsModel model) {
        myParentFrame = parent;
        setModel(model);
        setView(new VideoMaskPanelView(model, this));
    }

    public VideoMaskPanelView getView() {
        return (VideoMaskPanelView) super.getView();
    }

    public DetectionSettingsModel getModel() {
        return (DetectionSettingsModel) super.getModel();
    }

    private File browse() {
        JFileChooser chooser = new JFileChooser();

        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

        // Initialize the chooser with the model directory
        chooser.setCurrentDirectory(UserPreferences.getModel().getVideoMaskDirectory());
        chooser.setDialogTitle("Choose mask file");

        if (chooser.showOpenDialog(getView()) == JFileChooser.APPROVE_OPTION) {

            // TODO: test that this writes preferences accordingly
            UserPreferences.getModel().setVideoMaskDirectory(chooser.getCurrentDirectory());

            return chooser.getSelectedFile();
        } else {

            // TODO print error message box
            System.out.println("No Selection ");
        }

        return null;
    }

    public void addFileComboBox(FileWrapper f) {
        JComboBox cb = getView().getFileComboBox();

        if (cb.getItemCount() == 0) {
            cb.addItem(f);
        } else {
            int num = cb.getItemCount();

            for (int i = 0; i < num; i++) {
                String s = cb.getItemAt(i).toString();

                if (s.equals(f.toString())) {
                    cb.removeItemAt(i);

                    break;
                }
            }

            cb.insertItemAt(f, 0);
            cb.setSelectedIndex(0);
        }
    }

    private void updateMask(FileWrapper f, Boolean disablecombobox) {
        try {
            if (f.toString().equals(EMPTY_MASK_DESCRIPTION)) {
                getView().disableCursor();
            }

            getView().displayMask(f.getContent());
        } catch (Exception e) {

            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        getModel().setVideoMask(f.getContent());

        // Disable combo box update. Just want to add the file to the box
        // and ignore the "FileComboBoxChanged" action as a result of adding the
        // file name, so use the myDisableComboBoxHandler flag for this.
        myDisableComboBoxHandler = true;
        addFileComboBox(f);
        myDisableComboBoxHandler = false;
    }

    /**
     * This method will be called when we drag the mouse over the file mask.
     */
    public void mouseDragged(MouseEvent e) {}

    /**
     * This method will be called when the mouse is moved. It simply displays the "real" image
     * coordinates.
     */
    public void mouseMoved(MouseEvent e) {
        int       x = e.getX();
        int       y = e.getY();
        Rectangle r = getView().getMaskThumbnail().getImageBounds(x, y);

        getView().displayCursorLabel("" + r.x + "," + r.y + "");
    }

    public void actionPerformed(ActionEvent e) {
        String actionCommand = e.getActionCommand();

        System.out.println(actionCommand);

        if (actionCommand.equals("Close")) {
            myParentFrame.setVisible(false);
        } else if (actionCommand.equals("Browse")) {
            File f = browse();

            updateMask(new FileWrapper(f), true);
        } else if (actionCommand.equals("ResetDefaults") && (getView() != null)) {
            DetectionSettingsModel snapshot = getSnapshot();

            updateMask(new FileWrapper(snapshot.getVideoMaskFile()), true);
        } else if (actionCommand.equals("FileComboBoxChanged") && (getView() != null)
                   && (myDisableComboBoxHandler == false)) {
            FileWrapper f = (FileWrapper) ((JComboBox) e.getSource()).getSelectedItem();

            updateMask(f, false);
        }

        getView().repaint();
    }

    // A generic wrapper for File. Used to wrap a null File object, and still return
    // a valid toString() to display in a combobox
    private class FileWrapper {
        private File f;

        public FileWrapper(File f) {
            this.f = f;
        }

        public File getContent() {    // return the file
            return f;
        }

        public String toString() {
            if (f != null) {
                return f.toString();
            }

            return EMPTY_MASK_DESCRIPTION;
        }
    }
}
