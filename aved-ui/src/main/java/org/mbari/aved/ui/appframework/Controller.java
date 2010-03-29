/*
 * @(#)Controller.java
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

//~--- JDK imports ------------------------------------------------------------

import java.awt.event.ActionEvent;

/**
 * The Controller interface is the interface which must be implemented by
 * all classes which wish to take the role of a Controller.
 * All controllers must be able to reference a model and a view object.
 * <p>
 * The primary role of a Controller within the MVC is to determine what
 * should happen in response to user input.
 */
public interface Controller {
    public void setModel(Model model);

    public Model getModel();

    public View getView();

    public void setView(View view);

    public void actionPerformed(ActionEvent e);
}
