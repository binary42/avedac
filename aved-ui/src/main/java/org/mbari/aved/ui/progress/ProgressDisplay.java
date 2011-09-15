/*
 * @(#)ProgressDisplay.java
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



package org.mbari.aved.ui.progress;

//~--- non-JDK imports --------------------------------------------------------

import org.jdesktop.swingworker.SwingWorker;

//~--- JDK imports ------------------------------------------------------------

import java.util.Random;

import javax.swing.JFrame;

public class ProgressDisplay {
    ProgressController controller;

    public ProgressDisplay(SwingWorker worker, String headerdescription) {

        // Create controller
        controller = new ProgressController(headerdescription, worker);
    }

    public JFrame getView() {
        return controller.getView();
    }

    public void display(String string) {
        controller.display(string);
    }

    public static void main(String[] args) {
        class SimulateTask extends SwingWorker<Void, Void> {

            /*
             * Main task. Executed in background thread.
             */
            @Override
            public Void doInBackground() {
                Random random   = new Random();
                int    progress = 0;

                // Initialize progress property.
                setProgress(0);

                // Sleep for at least one second to simulate "startup".
                try {
                    Thread.sleep(1000 + random.nextInt(2000));
                } catch (InterruptedException ignore) {}

                while (progress < 100) {

                    // Sleep for up to one second.
                    try {
                        Thread.sleep(random.nextInt(1000));
                    } catch (InterruptedException ignore) {}

                    // Make random progress.
                    progress += random.nextInt(10);
                    setProgress(Math.min(progress, 100));
                }

                return null;
            }

            /*
             * Executed in event dispatch thread
             */
            public void done() {
                System.out.println("Done!\n");
                System.exit(0);
            }
        }

        SimulateTask    task = new SimulateTask();
        ProgressDisplay m;

        try {
            m = new ProgressDisplay(task, "Progress Display Test");
        } catch (Exception e) {

            // TODO Auto-generated catch block
            e.printStackTrace();

            return;
        }

        // Send a few messages to see if display is working
        m.display("Running simulated task now...");

        // Execute the simulated task
        task.execute();
    }
}
