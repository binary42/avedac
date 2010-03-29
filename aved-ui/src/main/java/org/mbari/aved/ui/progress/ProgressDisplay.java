/*
 * @(#)ProgressDisplay.java
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
