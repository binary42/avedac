/*
 * @(#)JFrameView.java
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



package org.mbari.aved.ui.appframework;

//~--- non-JDK imports --------------------------------------------------------

import com.jeta.forms.components.panel.FormPanel;

//~--- JDK imports ------------------------------------------------------------
 
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.util.logging.Level;
import java.util.logging.Logger;

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
                    try {
                        getController().actionPerformed(e);
                    } catch (Exception ex) {
                        Logger.getLogger(JFrameView.class.getName()).log(Level.SEVERE, null, ex);
                        System.out.println(ex.getCause());
                    }
                }
            });
        }
    }
}
