/*
 * @(#)EventLabelerController.java
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



package org.mbari.aved.ui;

//~--- non-JDK imports --------------------------------------------------------

import org.mbari.aved.ui.appframework.AbstractController;
import org.mbari.aved.ui.appframework.ModelEvent;
import org.mbari.aved.ui.appframework.ModelListener;
import org.mbari.aved.ui.command.Execute;
import org.mbari.aved.ui.command.LabelCommand;
import org.mbari.aved.ui.model.EventListModel;

//~--- JDK imports ------------------------------------------------------------

import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JComboBox;

/**
 *
 * @author dcline
 */
class EventLabelerController extends AbstractController implements ModelListener, MouseListener {
    private String className   = "";
    private String id          = "";
    private String speciesName = "";
    private String tag         = "";

    EventLabelerController(EventListModel model) {
        setModel(model);
        setView(new EventLabelerView(model, this));

        // Register as listener to the models
        model.addModelListener(this);
    }

    @Override
    public EventLabelerView getView() {
        return (EventLabelerView) super.getView();
    }

    /**
     * Operation handler for handling actions initiated in the view
     *
     * @param actionCommand A semantic event which indicates that a
     * component-defined action occurred.
     */
    public void actionPerformed(ActionEvent e) {
        String actionCommand = e.getActionCommand();

        if (actionCommand.equals("BrowseVARSKnowledgeBase")) {
            getView().displayConceptTree();
        } else if (actionCommand.equals("Apply")) {
            Execute.run(new LabelCommand(speciesName, className, tag, id));
            this.getView().dispose();
        } else if (actionCommand.equals("Close")) {
            this.getView().dispose();
        } else if (actionCommand.equals("classComboBoxChanged")) {
            JComboBox cb  = (JComboBox) e.getSource();
            Object    obj = cb.getSelectedItem();

            if (obj != null) {
                className = obj.toString();
            }
        } else if (actionCommand.equals("speciesComboBoxChanged")) {
            JComboBox cb  = (JComboBox) e.getSource();
            Object    obj = cb.getSelectedItem();

            if (obj != null) {
                speciesName = obj.toString();
            }
        } else if (actionCommand.equals("tagComboBoxChanged")) {
            JComboBox cb  = (JComboBox) e.getSource();
            Object    obj = cb.getSelectedItem();

            if (obj != null) {
                tag = obj.toString();
            }
        } else if (actionCommand.equals("idComboBoxChanged")) {
            JComboBox cb  = (JComboBox) e.getSource();
            Object    obj = cb.getSelectedItem();

            if (obj != null) {
                id = obj.toString();
            }
        }
    }

    /**
     * Model listener. Reacts to changes in the
     *  {@link org.mbari.aved.ui.model.EventListModel}
     */
    public void modelChanged(ModelEvent event) {}

    public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() == 2) {
            speciesName = getView().getSelectedConceptName();
            getView().setSpeciesCombo(speciesName);
            getView().setClassCombo(speciesName);
        }
    }

    public void mousePressed(MouseEvent e) {}

    public void mouseReleased(MouseEvent e) {}

    public void mouseEntered(MouseEvent e) {}

    public void mouseExited(MouseEvent e) {}
}
