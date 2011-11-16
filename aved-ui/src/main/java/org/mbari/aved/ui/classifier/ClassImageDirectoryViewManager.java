/*
 * @(#)ClassImageDirectoryViewManager.java
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



package org.mbari.aved.ui.classifier;

//~--- non-JDK imports --------------------------------------------------------

import org.mbari.aved.ui.userpreferences.UserPreferences;

//~--- JDK imports ------------------------------------------------------------

import java.awt.event.ActionEvent;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import java.io.File;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

public class ClassImageDirectoryViewManager {
    
    private final ClassifierModel               model;
    private int                                 currentQuickKey = 0;
    private final SortedSet<Integer>            nextKey         = new TreeSet<Integer>();
    private final List<ClassImageDirectoryView> dropViews       = new ArrayList<ClassImageDirectoryView>();

    public ClassImageDirectoryViewManager(ClassifierModel model) {

        this.model = model;
        // for quickKey operations
        setupNextKeyList();
    }
    
    /**
     * Workaround for not having a good directory change listener to 
     * udpate the appropriate views. TODO: add a directory change 
     * listener to the directories associated with each view and 
     * integrate the  ClassImageDirectoryView with the ClassModel
     */
    void notifyChanged() {
        model.notifyClassModelChanged();
    }

    ClassImageDirectoryView getView(int index) {
        return dropViews.get(index);
    }

    void addView(final ClassImageDirectoryView view) {
        dropViews.add(view);
        view.getDockKey().addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {

                // if the events property is dockablestate, then check if the new value is
                // 0. A 0 value indicates a close action so we should remove the dock from
                // our list of "dropviews"
                if (evt.getPropertyName().equalsIgnoreCase("dockablestate")
                        && evt.getNewValue().equals(new Integer(0))) {
                    UserPreferences.getModel().removeClassImageDockingDirectory(
                        new File(view.getModel().getDirectory()));
                    dropViews.remove(view);
                    nextKey.add(view.getQuickKey());
                }
            }
        });
    }

    int getNextQuickKey() {
        int qk = 0;

        if (nextKey.size() != 0) {
            qk = nextKey.first();
            nextKey.remove(qk);
        }

        return qk;
    }

    int getNumViews() {
        return dropViews.size();
    }

    private void setupNextKeyList() {
        nextKey.add(1);
        nextKey.add(2);
        nextKey.add(3);
        nextKey.add(4);
        nextKey.add(5);
        nextKey.add(6);
        nextKey.add(7);
        nextKey.add(8);
        nextKey.add(9);
    }

    public void addQuickKeyMappings(final JPanel panel) {
        for (int i = 1; i < 10; i++) {
            addQuickKeyMap(panel, i);
        }
    }

    private void addQuickKeyMap(final JPanel panel, final int number) {
        final String pressed = "pressed " + number;

        panel.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(pressed), pressed);
        panel.getActionMap().put(pressed, new AbstractAction() {
            public void actionPerformed(ActionEvent ignored) {
                currentQuickKey = number;
            }
        });

        final String released = "released " + number;

        panel.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(released),
                          released);
        panel.getActionMap().put(released, new AbstractAction() {
            public void actionPerformed(ActionEvent ignored) {
                currentQuickKey = 0;
            }
        });
    }

    // achase 20060918
    // it might seem odd to pass quickkey in as an argument, since the argument is likely
    // to be the same as the local variable 'currentQuickKey'. However, I was worried about
    // concurrent access to the 'currentQuickKey' variable, which could allow for the changing
    // of the quickKey search term midway through the method, so for simplicities sake i made
    // it an argument.
    public boolean quickCopy(ImageLabel image, int quickKey) {
        for (Iterator<ClassImageDirectoryView> iter = dropViews.iterator(); iter.hasNext(); ) {
            ClassImageDirectoryView view = iter.next();

            if (view.getQuickKey() == quickKey) {
                return view.moveFileHere(image.getFile());
            }
        }

        return false;
    }

    public int getCurrentQuickKey() {
        return currentQuickKey;
    }
}
