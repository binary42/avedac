/*
 * @(#)EventPopupMenu.java
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

//~--- JDK imports ------------------------------------------------------------

import java.awt.*;

import javax.swing.JPopupMenu;

/**
 * Creates and displays popupMenu menu
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
