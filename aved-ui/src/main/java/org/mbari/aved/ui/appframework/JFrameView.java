/*
 * @(#)JFrameView.java   10/03/17
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



package org.mbari.aved.ui.appframework;

//~--- non-JDK imports --------------------------------------------------------

import com.jeta.forms.components.panel.FormPanel;

//~--- JDK imports ------------------------------------------------------------

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;

/**
 * The JFrameView class is the root class of the view class hierarchy for top level
 * (swing) frames. It allows a controller and a model to be registered and can register
 * itself with a model as an observer of that model.
 * <p>
 * It extends the JFrame class, and contains an Abeille FormPanel
 * <p>
 * It requires the implementation of the <code>modelChanged(ModelEvent event);</code>
 * method in order that it can work with the notification mechanism in Java.
 */
abstract public class JFrameView extends JFrame implements View, ModelListener {
    private Controller    controller;
    private FormPanel     form;
    private ActionHandler handler;
    private Model         model;

    public JFrameView(String formpanel, Model model, Controller controller) {
        setModel(model);
        setController(controller);
        form    = new FormPanel(formpanel);
        handler = new ActionHandler();
        this.setContentPane(form);
        getRootPane().setWindowDecorationStyle(JRootPane.FRAME);
    }

    public FormPanel getForm() {
        return form;
    }

    public void registerWithModel() {
        ((AbstractModel) model).addModelListener(this);
    }

    public Controller getController() {
        return controller;
    }

    public void setController(Controller controller) {
        this.controller = controller;
    }

    public Model getModel() {
        return model;
    }

    public void setModel(Model model) {
        this.model = model;
        registerWithModel();
    }

    public ActionHandler getActionHandler() {
        return this.handler;
    }

    public class ActionHandler implements ActionListener {
        public void actionPerformed(final ActionEvent e) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    Thread change = new Thread(new Runnable() {
                        public void run() {
                            getController().actionPerformed(e);
                        }
                    });

                    change.start();
                }
            });
        }
    }
}
