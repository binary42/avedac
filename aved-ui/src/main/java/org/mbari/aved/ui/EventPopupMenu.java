/*
 * @(#)EventPopupMenu.java
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



package org.mbari.aved.ui;

//~--- JDK imports ------------------------------------------------------------

import java.awt.*;

import javax.swing.JPopupMenu;

/**
 * Creates and displays an image icon popup
 * @author D.Cline
 */
public class EventPopupMenu {
    private EditMenu         mainMenu;
    private ApplicationModel model;
    private JPopupMenu       popupMenu;

    public EventPopupMenu(ApplicationModel model) {
        this.model = model;
    }

    /**
     * Shows the popupMenu in the same position as the invoker (e.g. mouse click postiion)
     * @param invoker
     * @param x
     * @param y
     */
    public void show(Component invoker, int x, int y) {

        // Create the popupMenu menu.
        mainMenu  = new EditMenu(model);
        popupMenu = new JPopupMenu(ApplicationInfo.getName() + " - Edit");
        mainMenu.create(popupMenu, invoker, x, y);
        popupMenu.show(invoker, x, y);
    }
}
