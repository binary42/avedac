/*
 * @(#)ProcessDisplay.java
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



package org.mbari.aved.ui.process;

//~--- non-JDK imports --------------------------------------------------------

import org.mbari.aved.ui.message.MessageController;

//~--- JDK imports ------------------------------------------------------------

import java.awt.Dimension;
import java.awt.Toolkit;

import java.io.*;

import javax.swing.JFrame;

public class ProcessDisplay extends OutputStream {
    private MessageController controller = null;

    public ProcessDisplay(String headerdescription) {

        // Create message controller and view
        controller = new MessageController(headerdescription);

        // Bring the display to the front right
        Toolkit   kit        = controller.getView().getToolkit();
        Dimension screenSize = kit.getScreenSize();

        // Display window in right of screen
        int x = (int) (screenSize.width - getView().getWidth());
        int y = (int) (screenSize.height - getView().getHeight());

        getView().setLocation(x, y);
        getView().setVisible(false);
    }

    /** Helper function that returns view for this display */
    public JFrame getView() {
        return controller.getView();
    }

    @Override
    public void write(int b) throws IOException {

        // TODO Auto-generated method stub
    }

    public void write(byte b[]) throws IOException {
        String s = "";

        for (int i = 0; i < b.length; i++) {
            s += (char) b[i];
        }

        controller.display(s);
    }
}
