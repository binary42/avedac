/*
 * @(#)EventLabelerController.java   10/03/17
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
    private String className   = new String("");
    private String id          = new String("");
    private String speciesName = new String("");
    private String tag         = new String("");

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

            className = obj.toString();
        } else if (actionCommand.equals("speciesComboBoxChanged")) {
            JComboBox cb  = (JComboBox) e.getSource();
            Object    obj = cb.getSelectedItem();

            speciesName = obj.toString();
        } else if (actionCommand.equals("tagComboBoxChanged")) {
            JComboBox cb  = (JComboBox) e.getSource();
            Object    obj = cb.getSelectedItem();

            tag = obj.toString();
        } else if (actionCommand.equals("idComboBoxChanged")) {
            JComboBox cb  = (JComboBox) e.getSource();
            Object    obj = cb.getSelectedItem();

            id = obj.toString();
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
