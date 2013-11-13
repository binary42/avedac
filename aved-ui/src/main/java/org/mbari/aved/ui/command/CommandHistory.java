/*
 * @(#)CommandHistory.java
 * 
 * Copyright 2013 MBARI
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



package org.mbari.aved.ui.command;

//~--- JDK imports ------------------------------------------------------------

import java.util.LinkedList;
import java.util.NoSuchElementException;

public class CommandHistory {
    private static final CommandHistory INSTANCE            = new CommandHistory();
    private static final int            MAX_COMMAND_HISTORY = 5;
    private LinkedList<AbstractCommand> commands            = new LinkedList<AbstractCommand>();
    private int                         lastCommandIndex    = 0;
    private UndoRedoState               state               = UndoRedoState.INIT_STATE;

    private CommandHistory() {}

    public enum UndoRedoState {
        INIT_STATE(0), UNDO_STATE(1), REDO_STATE(2), UNDO_REDO_STATE(3);

        public final int state;

        UndoRedoState(int index) {
            this.state = index;
        }
    }

    /**
     * Singleton helper function
     * @return the singleton instance
     */
    public static CommandHistory getInstance() {
        return INSTANCE;
    }

    /**
     * Adds a command to the head of the list
     * @param command
     */
    public void addCommand(AbstractCommand command) {
        if (commands.size() > MAX_COMMAND_HISTORY) {
            commands.removeFirst();
        }

        commands.addFirst(command);
    }

    /**
     * Retrieves, but does not remove, the next command
     *
     * @return the next command in the list
     * @throws NoSuchElementException if this list is empty.
     */
    public AbstractCommand getNextCommand() throws NoSuchElementException {
        AbstractCommand command = commands.get(lastCommandIndex++);

        // If this is the last command
        if (lastCommandIndex >= commands.size()) {
            lastCommandIndex = commands.size() - 1;
        }

        if (isLast(command)) {
            setState(UndoRedoState.REDO_STATE);
        } else {
            setState(UndoRedoState.UNDO_REDO_STATE);
        }

        return command;
    }

    /**
     * Retrieves, but does not remove, the previous command
     *
     * @return the previous command in the list
     * @throws NoSuchElementException if this list is empty.
     */
    public AbstractCommand getPrevCommand() throws NoSuchElementException {
        AbstractCommand command = commands.get(lastCommandIndex--);

        // If this is the last command, start over
        if (lastCommandIndex < 0) {
            lastCommandIndex = 0;
        }

        if (isFirst(command)) {
            setState(UndoRedoState.UNDO_STATE);
        } else {
            setState(UndoRedoState.UNDO_REDO_STATE);
        }

        return command;
    }

    /**
     * Helper function to return the state of the Undo/Redo
     * @return
     */
    public UndoRedoState getState() {
        return state;
    }

    /**
     * Helper function to set the state of the Undo/Redo
     * @return
     */
    private void setState(UndoRedoState state) {
        this.state = state;
    }

    /**
     * Helper function to get the size of the command history
     * @return the size of the history
     */
    public int getSize() {
        return commands.size();
    }

    /**
     * Return true is this is the head (first element) of this list.
     * @return true is this is the head (first element) of this list, otherwise false
     * @throws NoSuchElementException if this queue is empty.
     * @since 1.5
     */
    public boolean isFirst(AbstractCommand command) {
        AbstractCommand head = commands.getFirst();

        if (head.equals(command)) {
            return true;
        }

        return false;
    }

    /**
     * Return true is this is the tail (last element) of this list.
     * @return true is this is the tail (last element) of this list, otherwise false
     * @throws NoSuchElementException if this queue is empty.
     * @since 1.5
     */
    public boolean isLast(AbstractCommand command) {
        AbstractCommand tail = commands.getLast();

        if (tail.equals(command)) {
            return true;
        }

        return false;
    }
}
