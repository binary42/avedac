/*
 * @(#)TextDisplayController.java
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

import java.awt.event.ActionEvent;
import org.mbari.aved.ui.appframework.AbstractController;
import org.mbari.aved.ui.appframework.JFrameView;

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
        if (line.contains("\n")) {
            ((TextDisplayModel) getModel()).setText(line);
        } else {
            String lineformatted = line + "\n";
            ((TextDisplayModel) getModel()).setText(lineformatted);
        }
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand().equals("Clear")) {
            ((TextDisplayModel) getModel()).clear();
        } else if (e.getActionCommand().equals("Close")) {
            ((JFrameView) getView()).setVisible(false);
        } else {}
    }
}
