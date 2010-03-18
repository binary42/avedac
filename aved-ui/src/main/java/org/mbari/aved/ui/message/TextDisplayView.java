/*
 * @(#)TextDisplayView.java   10/03/17
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



package org.mbari.aved.ui.message;

//~--- non-JDK imports --------------------------------------------------------

import org.mbari.aved.ui.appframework.JFrameView;
import org.mbari.aved.ui.appframework.ModelEvent;

//~--- JDK imports ------------------------------------------------------------

import javax.swing.JEditorPane;
import javax.swing.text.Document;

public class TextDisplayView extends JFrameView {
    public static final String ID_CLEAR_BUTTON = "clear";    // javax.swing.JButton
    public static final String ID_CLOSE_BUTTON = "close";    // javax.swing.JButton

    /*
     *  Component names in the TextDisplay form
     * If any of the component names are changed in the Abeille form designer, they
     * should be modified here too
     */
    public static final String ID_EDITOR_PANE = "editorpane";    // javax.swing.JEditorPane
    private JEditorPane        editorPane;

    public TextDisplayView(TextDisplayModel model, TextDisplayController controller) {

        // Initialize the Abeille form
        super("org/mbari/aved/ui/forms/TextDisplay.xml", model, controller);
        editorPane = (JEditorPane) getForm().getComponentByName(ID_EDITOR_PANE);
        editorPane.setEditable(false);

        // Add action handlers for buttons
        ActionHandler handler = getActionHandler();

        getForm().getButton(ID_CLOSE_BUTTON).addActionListener(handler);
        getForm().getButton(ID_CLEAR_BUTTON).addActionListener(handler);
        this.pack();
    }

    public void modelChanged(ModelEvent event) {
        TextDisplayModel s = (TextDisplayModel) event.getSource();

        try {
            if (s.getText().length() > 0) {    // If a non-empty message, go ahead and display
                Document doc = editorPane.getDocument();

                doc.insertString(doc.getLength(), s.getText(), null);
            }

            // Otherwise, if reset, then clear
            if (s.getState().equals(TextDisplayModel.State.RESET)) {
                editorPane.setText("");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
