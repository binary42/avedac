/*
 * @(#)MacOSAppAdapter.java   10/03/17
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

//~--- non-JDK imports --------------------------------------------------------

import com.apple.eawt.Application;
import com.apple.eawt.ApplicationEvent;
import com.apple.eawt.ApplicationListener;

//~--- JDK imports ------------------------------------------------------------

/*
* Copyright (c) Ian F. Darwin, http://www.darwinsys.com/, 1996-2002.
* All rights reserved. Software written by Ian F. Darwin and others.
* $Id: MacOSAppAdapter.java,v 1.4 2009/12/16 17:39:20 dcline Exp $
*
* Redistribution and use in source and binary forms, with or without
* modification, are permitted provided that the following conditions
* are met:
* 1. Redistributions of source code must retain the above copyright
*    notice, this list of conditions and the following disclaimer.
* 2. Redistributions in binary form must reproduce the above copyright
*    notice, this list of conditions and the following disclaimer in the
*    documentation and/or other materials provided with the distribution.
*
* THIS SOFTWARE IS PROVIDED BY THE AUTHOR AND CONTRIBUTORS ``AS IS''
* AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
* TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
* PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE AUTHOR OR CONTRIBUTORS
* BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
* CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
* SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
* INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
* CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
* ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
* POSSIBILITY OF SUCH DAMAGE.
*
* Java, the Duke mascot, and all variants of Sun's Java "steaming coffee
* cup" logo are trademarks of Sun Microsystems. Sun's, and James Gosling's,
* pioneering role in inventing and promulgating (and standardizing) the Java
* language and environment is gratefully acknowledged.
*
* The pioneering role of Dennis Ritchie and Bjarne Stroustrup, of AT&T, for
* inventing predecessor languages C and C++ is also gratefully acknowledged.
 */
import javax.swing.JFrame;

/**
 * The only os-dependant part of com.darwinsys, this is the
 * adapter class to handle MacOS's "different" way of doing About Box,
 * Quit item in App menu, Preferences, and so on.
 */
public class MacOSAppAdapter extends Application {
    AboutBoxHandler     abouter;
    ApplicationListener appListener;
    MainMenu            parent;
    PrefsHandler        prefser;
    PrintHandler        printer;
    ShutdownHandler     shutter;

    /**
     * Construct a MacOSAppAdapter.
     * @param theParent A JFrame, the main application window.
     * @param about A handler for the About box.
     * @param prefs A Preferences handler.
     * @param print A Print handler (bug: does not get invoked now).
     * @param shut A shutdown handler
     */
    public MacOSAppAdapter(MainMenu theParent, AboutBoxHandler about, PrefsHandler prefs, PrintHandler print,
                           ShutdownHandler shut) {
        appListener = new MyAppEventHandler();
        parent      = theParent;

        if (about != null) {
            abouter = about;
            setEnabledAboutMenu(true);
            addAboutMenuItem();
        }

        if (prefs != null) {
            prefser = prefs;
            setEnabledPreferencesMenu(true);
            addPreferencesMenuItem();
        }

        printer = print;
        shutter = shut;
    }

    /**
     * Method to register this handler with Apple's event manager, calling
     * addApplicationListener in parent class.
     */
    public void register() {
        addApplicationListener(appListener);
    }

    /** Inner class to provide ApplicationListener support. */
    class MyAppEventHandler implements ApplicationListener {

        /** This is called when the user does Application->About */
        public void handleAbout(ApplicationEvent event) {
            abouter.showAboutBox();
            event.setHandled(true);
        }

        /** Called when the user does Application->Preferences */
        public void handlePreferences(ApplicationEvent event) {
            if (prefser != null) {
                prefser.showPrefsDialog();
            }

            event.setHandled(true);
        }

        public void handlePrint(ApplicationEvent event) {
            if (printer != null) {
                printer.doPrint();
            }

            event.setHandled(true);
        }

        /** This is called when the user does Application->Quit */
        public void handleQuit(ApplicationEvent event) {
            if (shutter != null) {
                shutter.shutdown(parent);
            }

            // This is reached if the user decided not to shutdown
        }

        /**
         * @see com.apple.eawt.ApplicationListener#handleOpenApplication(com.apple.eawt.ApplicationEvent)
         */
        public void handleOpenApplication(ApplicationEvent arg0) {

            // TODO Auto-generated method stub
        }

        /**
         * @see com.apple.eawt.ApplicationListener#handleOpenFile(com.apple.eawt.ApplicationEvent)
         */
        public void handleOpenFile(ApplicationEvent arg0) {

            // TODO Auto-generated method stub
        }

        /**
         * @see com.apple.eawt.ApplicationListener#handlePrintFile(com.apple.eawt.ApplicationEvent)
         */
        public void handlePrintFile(ApplicationEvent arg0) {

            // TODO Auto-generated method stub
        }

        /**
         * @see com.apple.eawt.ApplicationListener#handleReOpenApplication(com.apple.eawt.ApplicationEvent)
         */
        public void handleReOpenApplication(ApplicationEvent arg0) {

            // TODO Auto-generated method stub
        }
    }
}
