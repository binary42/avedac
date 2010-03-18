/*
 * @(#)UserPreferences.java   10/03/17
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



package org.mbari.aved.ui.userpreferences;

/**
 * Contains all the user preferences for this application. This includes, e.g.
 * preferences for what external video player to play video clips in
 * and what directory to use as scratch space. }
 *
 * @author dcline
 */
public class UserPreferences {
    private static final UserPreferences    INSTANCE = new UserPreferences();
    private final UserPreferencesController controller;
    private final UserPreferencesModel      model;

    /**
     * Constructor. Creates the associated model and controller
     */
    private UserPreferences() {

        // Create controller and model
        model      = new UserPreferencesModel();
        controller = new UserPreferencesController(model);
    }

    /**
     * Gets the singleton view associated with this object
     * @return the view
     */
    public UserPreferencesView getView() {
        return (UserPreferencesView) controller.getView();
    }

    /**
     * Get the singleton for this object.
     * @return
     */
    public static synchronized UserPreferences getInstance() {

        // Prevent two threads from calling this method
        // at the same time
        return INSTANCE;
    }

    /**
     * Get the singleton model associated with this object.
     * @return
     */
    public static synchronized UserPreferencesModel getModel() {
        return INSTANCE.model;
    }
}
