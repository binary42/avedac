/*
 * @(#)View.java   10/03/17
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

/**
 * This interface must be implemented by all classes that wish to take the role
 * of the View within the MVC framework.
 * The role of a View is the display of information and the capture of
 * data entered.
 */
public interface View {
    Controller getController();

    void setController(Controller controller);

    Model getModel();

    void setModel(Model model);
}
