/*
 * @(#)UserPreferences.java
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
