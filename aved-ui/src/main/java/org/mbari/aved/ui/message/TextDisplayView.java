/*
 * @(#)TextDisplayView.java
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



package org.mbari.aved.ui.message;

//~--- non-JDK imports --------------------------------------------------------

import org.mbari.aved.ui.appframework.JFrameView;
import org.mbari.aved.ui.appframework.ModelEvent;

//~--- JDK imports ------------------------------------------------------------

import javax.swing.JEditorPane;
import javax.swing.text.Document;

public class TextDisplayView extends JFrameView {

    /*
     *  Component names in the TextDisplay form
     * If any of the component names are changed in the Abeille form designer, they
     * should be modified here too
     */
    public static final String ID_EDITOR_PANE = "editorpane";    // javax.swing.JEditorPane
    public static final String ID_CLEAR_BUTTON = "clear";    // javax.swing.JButton
    public static final String ID_CLOSE_BUTTON = "close";    // javax.swing.JButton 
    
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
            // If a non-empty message, go ahead and display
            if (s.getText().length() > 0 && !s.getText().equals("\n")) {    
                Document doc = editorPane.getDocument();

                doc.insertString(doc.getLength(), s.getText(), null);
                editorPane.setCaretPosition(doc.getLength());
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
