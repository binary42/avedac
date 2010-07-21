/*
 * @(#)TextEntryPanel.java
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



package org.mbari.aved.ui;

//~--- non-JDK imports --------------------------------------------------------

import org.mbari.aved.ui.userpreferences.UserPreferences;

//~--- JDK imports ------------------------------------------------------------

import java.awt.*;
import java.awt.event.*;

import java.util.ArrayList;

import javax.swing.*;

public class TextEntryPanel extends JPanel implements ActionListener {
    private JComboBox comboBox;
    String            currentText;
    ActionListener    listener;
    JLabel            result;

    /**
     *
     * @param listener an optional listener to register with the changes
     * in the combobox in the TextEntryPanel
     * @param textId a text identifier used in the formatting of the JLabel
     * for this JPanel
     * @param comboBoxItems optional items to place in the initial comboBox
     * @param selectedComboBoxItem optional item to assign to the text field as the
     * last selected item. This must exist in the comboBoxItem list
     */
    public TextEntryPanel(ActionListener listener, String textId, ArrayList<String> comboBoxItems,
                          String selectedComboBoxItem) {
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        this.listener = listener;

        JLabel tagDescriptionLabel = new JLabel("Enter new " + textId + " or select one from the list");

        // Create the UI for displaying result.
        JLabel lastUsedTagLabel = new JLabel("Last entered " + textId, JLabel.LEADING);    // == LEFT

        result = new JLabel(" ");
        result.setForeground(Color.black);
        result.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.black),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));

        // Populate the combo box
        if (comboBoxItems.size() > 0) {
            comboBox = new JComboBox(comboBoxItems.toArray());
        } else {
            comboBox = new JComboBox();
        }

        comboBox.setEditable(true);
        comboBox.addActionListener(this);

        if ((comboBoxItems.size() > 0) && (selectedComboBoxItem.length() > 0)
                && comboBoxItems.contains(selectedComboBoxItem)) {
            currentText = selectedComboBoxItem;
            comboBox.setSelectedItem(currentText);
        } else if (comboBoxItems.size() > 0) {
            currentText = comboBoxItems.get(0);
            comboBox.setSelectedIndex(0);
        } else {
            currentText = "";
            comboBox.setSelectedIndex(-1);
        }

        // Lay out everything.
        JPanel patternPanel = new JPanel();

        patternPanel.setLayout(new BoxLayout(patternPanel, BoxLayout.PAGE_AXIS));
        patternPanel.add(tagDescriptionLabel);
        comboBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        patternPanel.add(comboBox);

        JPanel resultPanel = new JPanel(new GridLayout(0, 1));

        resultPanel.add(lastUsedTagLabel);
        resultPanel.add(result);
        patternPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        resultPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(patternPanel);
        add(Box.createRigidArea(new Dimension(0, 10)));
        add(resultPanel);
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        updateResult();
    }

    /**
     * Returns the combo box in this panel
     * @return the combo box
     */
    public JComboBox getComboBox() {
        return comboBox;
    }

    /**
     * Returns the text last entered/selected in this panels ComboBox
     * @return the text last entered/selected
     */
    public String getText() {
        return currentText;
    }

    /**
     * Invoked when an action occurs. This handles
     * combobox selections and use
     */
    public void actionPerformed(ActionEvent e) {
        JComboBox cb = (JComboBox) e.getSource();

        if (e.getActionCommand().equals("comboBoxEdited")) {
            Object newItem = cb.getSelectedItem();

            currentText = newItem.toString();
        }

        if (e.getActionCommand().equals("comboBoxChanged")) {
            Object newSelection = cb.getSelectedItem();

            if (newSelection != null) {
                currentText = newSelection.toString();
            }
        }

        updateResult();

        if (listener != null) {
            listener.actionPerformed(e);
        }
    }

    /** Updates the last entered tag. */
    private void updateResult() {
        try {
            result.setForeground(Color.black);
            result.setText(currentText);
        } catch (IllegalArgumentException iae) {
            result.setForeground(Color.red);
            result.setText("Error: " + iae.getMessage());
        }
    }

    /**
     * Create the GUI and show it.  For thread safety,
     * this method should be invoked from the
     * event dispatch thread.
     */
    private static void createAndShowGUI() {

        // Create and set up the window.
        JFrame frame = new JFrame("ComboBoxDemo2");

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        ArrayList<String> comboBoxItems = UserPreferences.getModel().getTagList();

        // Create and set up the content pane.
        TextEntryPanel newContentPane = new TextEntryPanel(null, "tag", comboBoxItems, comboBoxItems.get(0));

        newContentPane.setOpaque(true);    // content panes must be opaque
        frame.setContentPane(newContentPane);

        // Display the window.
        frame.pack();
        frame.setVisible(true);
    }

    public static void main(String[] args) {

        // Schedule a job for the event dispatch thread:
        // creating and showing this application's GUI.
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                createAndShowGUI();
            }
        });
    }
}
