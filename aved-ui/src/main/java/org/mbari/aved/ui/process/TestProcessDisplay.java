/*
 * @(#)TestProcessDisplay.java
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

//~--- JDK imports ------------------------------------------------------------

import javax.swing.JFrame;

public class TestProcessDisplay {
    public static void main(String[] args) {
        ProcessDisplay m     = new ProcessDisplay("Process Display Test");
        JFrame         frame = m.getView();

        frame.setTitle("Message Display Test");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }
}
