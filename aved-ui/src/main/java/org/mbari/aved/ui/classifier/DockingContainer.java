/*
 * @(#)DockingContainer.java
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



package org.mbari.aved.ui.classifier;

//~--- non-JDK imports --------------------------------------------------------

import com.vlsolutions.swing.docking.DockKey;
import com.vlsolutions.swing.docking.Dockable;
import com.vlsolutions.swing.docking.DockingConstants;
import com.vlsolutions.swing.docking.DockingDesktop;
import com.vlsolutions.swing.docking.DockingPreferences;

import org.mbari.aved.ui.ApplicationView.HeaderPanel;
import org.mbari.aved.ui.userpreferences.UserPreferences;

//~--- JDK imports ------------------------------------------------------------

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.io.File;

import java.util.ArrayList;
import java.util.Iterator;

import javax.imageio.ImageIO;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.filechooser.FileFilter;

public class DockingContainer extends JPanel {
    private DockingDesktop                 desk = new DockingDesktop();
    private final ButtonPanel              buttonPanel;
    private ClassImageDirectoryViewManager viewManager;

    public DockingContainer(ClassImageDirectoryViewManager viewManager) throws Exception {
        super(new BorderLayout());
        this.viewManager = viewManager;

        // Create Jerome's pretty header and title
        ImageIcon icon = new ImageIcon(getClass().getResource("/org/mbari/aved/ui/images/logo.jpg"));

        if (icon == null) {
            throw new Exception("Invalid logo.jpg");
        }

        JPanel      p      = new JPanel(new BorderLayout());
        HeaderPanel header = new HeaderPanel("Automated Visual Event Detection and Classification - "
                                 + " Class Image Organizer  ", icon, 0.25f);

        p.add(BorderLayout.NORTH, header);

        // for quickKey operations
        int qk = viewManager.getNextQuickKey();

        // Create the rest of the layout
        FullViewContainer fullView = new FullViewContainer(qk);

        viewManager.addQuickKeyMappings(fullView);
        desk.addDockable(fullView);
        viewManager.addView(fullView.getView());
        buttonPanel = new ButtonPanel(viewManager);
        desk.split(fullView, buttonPanel, DockingConstants.SPLIT_RIGHT);
        desk.setDockableWidth(fullView, .8);
        desk.setDockableHeight(buttonPanel, .1);

        // Add the components to this panel
        this.add(p, BorderLayout.NORTH);
        this.add(desk, BorderLayout.CENTER);
        DockingPreferences.setFlatDesktopStyle();
        DockingPreferences.setShadowDesktopStyle();

        if (!checkForJpgReader()) {
            JOptionPane
                .showMessageDialog(
                    this, "You are missing the Java Advanced Imaging Library needed to "
                    + "view .ppm images.\nPlease go to "
                    + "https://jai.dev.java.net/binary-builds.html\nand download "
                    + "and install the appropriate package for your platform\n(version 1.1.3 or better)", "Missing Require Libraries", JOptionPane
                        .ERROR_MESSAGE);
        }
  
        viewManager.addQuickKeyMappings(this);
        ImageLabel.dockingContainer = this;

        // If there are any previously defined docking containers, recreate them
        ArrayList<File> dirList = UserPreferences.getModel().getLastTrainingImageDockingDirectories();

        // Add a synopsis views for each
        if (dirList.size() > 0) {
            for (Iterator<File> iter = dirList.iterator(); iter.hasNext(); ) {
                File dir = iter.next();

                buttonPanel.addSynopsisView(dir);
            }
        }
    }

    public DockingDesktop getDesktop() {
        return this.desk;
    }

    int getCurrentQuickKey() {
        return viewManager.getCurrentQuickKey();
    }

    boolean quickCopy(ImageLabel aThis, int currentQuickKey) {
        return viewManager.quickCopy(aThis, currentQuickKey);
    }
 
    private boolean checkForJpgReader() {
        ImageIO.scanForPlugins();

        String[] formats = ImageIO.getReaderFormatNames();

        for (int i = 0; i < formats.length; i++) {
            if (formats[i].equalsIgnoreCase("jpg")) {
                return true;
            }
        }

        return false;
    }
    private class ButtonPanel extends JPanel implements Dockable {
        private final JFileChooser             chooser = new JFileChooser();
        private final DockKey                  dockKey;
        private ClassImageDirectoryViewManager viewManager;

        public ButtonPanel(ClassImageDirectoryViewManager vManager) {
            viewManager = vManager;
            chooser.setAcceptAllFileFilterUsed(false);
            chooser.addChoosableFileFilter(new FileFilter() {
                public boolean accept(File f) {
                    if (f.isDirectory()) {
                        return true;
                    }

                    return false;
                }
                public String getDescription() {
                    return "directory chooser";
                }
            });
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

            JButton button = new JButton("Add Directory");

            button.setToolTipText("Select directories that will be available to drag images to");
            button.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    int action = chooser.showDialog(ButtonPanel.this, "Choose Dir");

                    if (action == JFileChooser.APPROVE_OPTION) {
                        File dir = chooser.getSelectedFile();

                        UserPreferences.getModel().setLastDockingImageDirectory(dir);
                        addSynopsisView(dir);
                    }
                }
            });
            this.add(button);
            dockKey = new DockKey("Add More Folders");
            dockKey.setCloseEnabled(false);
            dockKey.setMaximizeEnabled(false);
            dockKey.setTooltip("Select directories that will be available to drag images to and from");
            dockKey.setResizeWeight(.1f);
        }

        public void addSynopsisView(File dir) {
            ClassImageDirectoryModel model = new ClassImageDirectoryModel();

            model.setDirectory(dir);
            model.setName(dir.getName());

            int qk = 0;

            qk = viewManager.getNextQuickKey();

            final ClassImageDirectorySynopsisView synopsis = new ClassImageDirectorySynopsisView(model, qk);
            int                                   size     = viewManager.getNumViews();

            if (size > 1) {
                desk.split((Dockable) viewManager.getView(size - 1), synopsis, DockingConstants.SPLIT_BOTTOM);
            } else {
                desk.split(buttonPanel, synopsis, DockingConstants.SPLIT_BOTTOM);
            }

            viewManager.addView(synopsis);
        }

        public Component getComponent() {
            return this;
        }

        public DockKey getDockKey() {
            return dockKey;
        }
    }
}
