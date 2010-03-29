/*
 * @(#)TextDisplayController.java
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

import org.mbari.aved.ui.appframework.AbstractController;
import org.mbari.aved.ui.appframework.JFrameView;

//~--- JDK imports ------------------------------------------------------------

import java.awt.event.ActionEvent;

public class TextDisplayController extends AbstractController {
    public TextDisplayController() {
        setModel(new TextDisplayModel());
        setView(new TextDisplayView((TextDisplayModel) getModel(), this));
    }

    /* Returns the view associated with this controller */
    public TextDisplayView getView() {
        return (TextDisplayView) super.getView();
    }

    public void display(String line) {
        String lineformatted = line + "\n";

        ((TextDisplayModel) getModel()).setText(lineformatted);
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand().equals("Clear")) {
            ((TextDisplayModel) getModel()).clear();
        } else if (e.getActionCommand().equals("Close")) {
            ((JFrameView) getView()).setVisible(false);
        } else {}
    }
}
