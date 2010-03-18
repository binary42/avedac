/*
 * @(#)PlayerController.java   10/03/17
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



package org.mbari.aved.ui.player;

//~--- non-JDK imports --------------------------------------------------------

import aved.model.EventObject;

import org.mbari.aved.ui.Application;
import org.mbari.aved.ui.ApplicationModel;
import org.mbari.aved.ui.appframework.AbstractController;
import org.mbari.aved.ui.command.DeleteCommand;
import org.mbari.aved.ui.command.Execute;
import org.mbari.aved.ui.exceptions.FrameOutRangeException;
import org.mbari.aved.ui.message.ModalYesNoNeverDialog;
import org.mbari.aved.ui.message.NonModalMessageDialog;
import org.mbari.aved.ui.model.EventListModel;
import org.mbari.aved.ui.model.EventObjectContainer;
import org.mbari.aved.ui.model.TableSorter;
import org.mbari.aved.ui.userpreferences.UserPreferences;
import org.mbari.aved.ui.userpreferences.UserPreferencesModel;

//~--- JDK imports ------------------------------------------------------------

import java.awt.event.*;

import java.io.File;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.*;

public class PlayerController extends AbstractController implements ActionListener, KeyListener {

    /** counter that maintains the current frame to display */
    private int                  frameNo   = 0;
    private int                  nImages   = 0;    // number of images to animate
    private Direction            direction = Direction.Forward;
    private State                state     = State.Stop;
    private Mode                 mode      = Mode.Continuous;
    private EventObjectContainer event;
    private EventListModel       eventListModel;

    /** the icon displayed with pause function is enabled */
    private ImageIcon pauseIcon;

    /** the icon displayed with play function is enabled */
    private ImageIcon   playIcon;
    private TableSorter tableSorter;
    private Timer       timer;    // the timer animating the images

    /** Defines the direction of playout */
    private enum Direction { Forward, Reverse }

    ;

    /**
     * Defines the way to play - either keep playing continuously until the end,
     * or single step and stop
     */
    private enum Mode { Continuous, SingleStep }

    /** Defines the state of the player */
    private enum State { Stop, Play }

    ;
    ;
    public PlayerController(EventObjectContainer event, ApplicationModel model) {
        setModel(model);
        setView(new PlayerView(event, model, this));
        tableSorter    = model.getSorter();
        eventListModel = model.getEventListModel();
        this.event     = event;

        // Set up timer to drive animation events.
        // Set to 50 msec delay between frames
        timer = new Timer(50, this);
        timer.setInitialDelay(0);
        nImages   = this.event.getEndFrame() - this.event.getStartFrame();
        frameNo   = 0;
        playIcon  = new ImageIcon(getClass().getResource("/org/mbari/aved/ui/images/play.jpg"));
        pauseIcon = new ImageIcon(getClass().getResource("/org/mbari/aved/ui/images/pause.jpg"));
        ((PlayerView) getView()).setPlayStopButtonIcon(playIcon);
        updateButtonStates();
    }

    /**
     * Controls telling the view what image to display from the
     * offset from the beginning of the event
     * @param offset
     */
    private void displayImage(int offset) {
        EventObject eventObj = null;
        PlayerView  view     = ((PlayerView) getView());

        // Calculate the actual frame number
        int num = (event.getStartFrame() + offset);

        // Get the image sequence, and display the image
        try {
            File src = null;

            eventObj = event.getEventObject(num);

            // Get the frame source and catch exception
            // in case it is missing
            try {
                src = event.getFrameSource(num);
            } catch (Exception e) {

                // TODO Auto-generated catch block
                src = null;
                e.printStackTrace();

                return;
            }

            // TODO: push this logic down into the PlayerView
            // it knows how to best display the timecode
            if (eventObj != null) {
                view.displayEventImage(eventObj, src);
                view.displayTimecodeFrameString(eventObj.getFrameEventSet().getTimecode(),
                                                eventObj.getFrameEventSet().getFrameNumber());
            }
        } catch (FrameOutRangeException e) {

            // If image is missing display message
            // Create the yes/no dialog
            String                message = new String("Error: " + e.getMessage());
            NonModalMessageDialog dialog;

            try {
                dialog = new NonModalMessageDialog((PlayerView) getView(), message);
                dialog.setVisible(true);
            } catch (Exception ex) {
                Logger.getLogger(PlayerController.class.getName()).log(Level.SEVERE, null, ex);
            }

            // dialog.answer();
            timer.stop();
        } catch (Exception e) {

            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void updateButtonStates() {
        PlayerView view = ((PlayerView) getView());

        // Disable the next button if this is the last event in the sequence
        if (eventListModel.isLastEvent(event)) {
            view.disableNextButton();
        } else {
            view.enableNextButton();

            // Disable the next button if this is the last event in the sequence
        }

        if (eventListModel.isFirstEvent(event)) {
            view.disablePrevButton();
        } else {
            view.enablePrevButton();
        }
    }

    /*
     *  Stop the timer, sets the play icon and sets the internal state variable.
     * Call this anytime the play sequence is stopped
     */
    private void stop() {
        PlayerView view = ((PlayerView) getView());

        state = State.Stop;
        view.setPlayStopButtonIcon(playIcon);
        timer.stop();
    }

    /*
     *  Starts the timer, sets the pause icon and starts the timer
     * Call this anytime the play button is hit
     */
    private void play(Mode mode, Direction direction) {
        PlayerView view = ((PlayerView) getView());

        this.mode      = mode;
        this.direction = direction;
        view.setPlayStopButtonIcon(pauseIcon);

        if (nImages > 0) {

            // kick start the timer is it isn't running
            if (!timer.isRunning()) {
                timer.start();
            }
        }
    }

    /*
     *  timer action listener. Updates the image sequence counter and implements stop logic
     * (non-Javadoc)
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     * If it's the last frame, restart the timer to get a long pause between loops
     */
    public void actionPerformed(ActionEvent e) {
        PlayerView view = ((PlayerView) getView());

        updateButtonStates();

        // Translated the row index into the real eventListModel index
        // through the tableSorter, since the table may be sorted
        int              row           = tableSorter.viewIndex((eventListModel.getIndexOf(event)));
        int              index         = 0;
        ApplicationModel m             = (ApplicationModel) getModel();
        String           actionCommand = e.getActionCommand();

        if (actionCommand != null) {
            if (actionCommand.equals("Next")) {
                index = tableSorter.modelIndex(++row);
                event = eventListModel.getElementAt(index);
                PlayerManager.getInstance().openView(event, m, false);
                PlayerManager.getInstance().remove(view);

                ApplicationModel   model = Application.getModel();
                ListSelectionModel lsm   = model.getListSelectionModel();

                lsm.setSelectionInterval(index, index);
            } else if (actionCommand.equals("Prev")) {
                index = tableSorter.modelIndex(--row);
                event = eventListModel.getElementAt(index);
                PlayerManager.getInstance().openView(event, m, false);
                PlayerManager.getInstance().remove(view);

                ApplicationModel   model = Application.getModel();
                ListSelectionModel lsm   = model.getListSelectionModel();

                lsm.setSelectionInterval(index, index);
            } else if (actionCommand.equals("Close")) {
                view.setVisible(false);
            } else if (actionCommand.equals("Delete")) {
                UserPreferencesModel prefs = UserPreferences.getModel();

                if (prefs.getDeleteWithoutWarning() == false) {
                    String question = new String("Are you sure you want to delete" + Execute.getObjectIdDescription()
                                                 + " ?");
                    ModalYesNoNeverDialog dialog;

                    try {
                        dialog = new ModalYesNoNeverDialog(Application.getView(), question);
                        dialog.setVisible(true);

                        if (dialog.isNever() == true) {
                            prefs.setDeleteWithoutWarning();
                        }

                        if (dialog.answer() == true) {
                            Execute.run(new DeleteCommand(event, eventListModel));
                        }
                    } catch (Exception ex) {
                        Logger.getLogger(PlayerController.class.getName()).log(Level.SEVERE, null, ex);
                    }
                } else {
                    Execute.run(new DeleteCommand(event, eventListModel));
                }
            } else if (actionCommand.equals("PlayStop")) {
                if (state != State.Stop)    // is playing, so toggle function to stop
                {
                    stop();
                } else {
                    play(Mode.Continuous, Direction.Forward);
                }
            } else if (actionCommand.equals("ForwardStep")) {
                play(Mode.SingleStep, Direction.Forward);
            } else if (actionCommand.equals("ReverseStep")) {
                play(Mode.SingleStep, Direction.Reverse);
            } else if (actionCommand.equals("ForwardToEnd")) {
                frameNo = nImages;          // Let the display show the last image
                play(Mode.SingleStep, Direction.Forward);
            } else if (actionCommand.equals("RewindToEnd")) {
                frameNo = 0;
                play(Mode.SingleStep, Direction.Reverse);
            } else {
                play(Mode.Continuous, Direction.Forward);
            }
        }

        if (frameNo == nImages) {
            stop();
        }

        state = State.Play;
        displayImage(frameNo);

        if (mode == Mode.SingleStep) {
            stop();
        }

        if (direction == Direction.Forward) {
            frameNo++;
        } else if (direction == Direction.Reverse) {
            frameNo--;
        } else {
            frameNo++;
        }

        if ((frameNo > nImages) || (frameNo < 0)) {
            frameNo = 0;
            stop();
        }
    }

    public void keyTyped(KeyEvent e) {}

    public void keyPressed(KeyEvent e) {

        // If right arrow key
        if ((e.getKeyCode() == 39) && e.isActionKey()) {
            play(Mode.SingleStep, Direction.Forward);
        }

        // If up arrow key
        if ((e.getKeyCode() == 38) && e.isActionKey()) {
            play(Mode.Continuous, Direction.Forward);
        }

        // If down arrow key
        if ((e.getKeyCode() == 40) && e.isActionKey()) {
            stop();
        }

        // If left arrow key
        if ((e.getKeyCode() == 37) && e.isActionKey()) {
            play(Mode.SingleStep, Direction.Reverse);
        }

        String s = System.getProperty("os.name").toLowerCase();

        if (((s.indexOf("linux") != -1) || (s.indexOf("windows") != -1)) && (e.getKeyCode() == KeyEvent.VK_C)) {
            PlayerView view = ((PlayerView) getView());

            view.setVisible(false);
        } else if ((s.indexOf("mac") != -1) && (e.getKeyCode() == KeyEvent.VK_W)) {
            PlayerView view = ((PlayerView) getView());

            view.setVisible(false);
        }
    }

    public void keyReleased(KeyEvent e) {}
}
