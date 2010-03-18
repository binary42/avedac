/*
 * @(#)ClassImageDirectoryFullView.java   10/03/17
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

import com.jgoodies.binding.adapter.BasicComponentFactory;
import com.jgoodies.binding.beans.BeanAdapter;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import com.vlsolutions.swing.docking.DockKey;
import com.vlsolutions.swing.docking.Dockable;

import org.mbari.aved.ui.utils.ParseUtils;

//~--- JDK imports ------------------------------------------------------------

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.HierarchyBoundsAdapter;
import java.awt.event.HierarchyEvent;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import java.io.File;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class ClassImageDirectoryFullView extends JPanel implements Dockable, ClassImageDirectoryView {
    private static final int               BORDER    = 5;
    public static final int                MAX_FILES = 1500;
    private int                            counter   = 1;
    private int                            gridSize  = 100;
    private int                            page      = 1;
    private final List                     labels    = new ArrayList();
    private int                            total     = 0;
    private JPanel                         bottomPanel;
    private DockKey                        dockKey;
    private GridLayout                     imageLayout;
    private final JPanel                   imagePanel;
    private final ClassImageDirectoryModel model;
    private int                            quickKey;
    private JSlider                        sizeSlider;
    private JLabel                         status;

    public ClassImageDirectoryFullView(ClassImageDirectoryModel model, int quickKey) {
        this.model    = model;
        this.quickKey = quickKey;
        setDropTarget(getMyDropTarget());
        dockKey = new DockKey(model.getDirectory());
        dockKey.setCloseEnabled(true);
        dockKey.setResizeWeight(0.5f);
        dockKey.setTooltip("Images dragged here will be moved to this directory");
        this.setLayout(new FormLayout("4dlu, pref:grow", "10dlu, fill:20dlu:grow, bottom:max(20dlu;pref)"));

        CellConstraints cc = new CellConstraints();

        imagePanel = buildImagePanel();

        final JScrollPane scroll = new JScrollPane(imagePanel);

        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(10);
        this.add(scroll, cc.xy(2, 2));
        bottomPanel = buildBottomPanel();
        bottomPanel.setMinimumSize(new Dimension(30, 40));
        this.add(bottomPanel, cc.xywh(1, 3, 2, 1));
        this.addComponentListener(new ComponentAdapter() {
            public void componentShown(ComponentEvent e) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        relayoutImagePanel();
                    }
                });
            }
        });

        Thread t = new Thread() {
            public void run() {
                loadImages();
            }
        };

        t.start();
    }

    private JPanel buildImagePanel() {
        imageLayout = new GridLayout(0, 5, 5, 5);

        JPanel panel = new JPanel(imageLayout);

        return panel;
    }    // placed outside of the loadImages method so they can be accessed in an inner class

    private DropTarget getMyDropTarget() {
        DropTargetListener listener = new DropTargetAdapter() {
            public void dragEnter(DropTargetDragEvent dtde) {

                // if the directories are the same, no drop allowed
                File data = null;

                try {
                    data = getDataFile(dtde.getTransferable());
                } catch (UnsupportedFlavorException e) {

                    // TODO Auto-generated catch block
                    e.printStackTrace();
                    dtde.rejectDrag();

                    return;
                } catch (IOException e) {

                    // TODO Auto-generated catch block
                    e.printStackTrace();
                    dtde.rejectDrag();

                    return;
                }

                if (data.getParent().equals(model.getDirectory())) {
                    dtde.rejectDrag();
                } else {
                    dtde.acceptDrag(DnDConstants.ACTION_MOVE);
                }
            }
            public void drop(DropTargetDropEvent dtde) {
                try {
                    File data = getDataFile(dtde.getTransferable());

                    if (data.getParent().equals(model.getDirectory())) {
                        dtde.rejectDrop();
                    } else {
                        dtde.acceptDrop(DnDConstants.ACTION_MOVE);
                    }

                    if (!moveFileHere(data)) {
                        dtde.dropComplete(false);

                        return;
                    }
                } catch (UnsupportedFlavorException e) {
                    e.printStackTrace();
                    System.err.println("Problem with drag and drop...");
                    dtde.dropComplete(false);
                } catch (IOException e) {
                    e.printStackTrace();
                    System.err.println("Problem with drag and drop...");
                    dtde.dropComplete(false);
                }

                dtde.dropComplete(true);
            }
            private File getDataFile(Transferable source) throws UnsupportedFlavorException, IOException {
                DataFlavor[] flavors = source.getTransferDataFlavors();

                // This is assuming that there will only be one element in the
                // 'flavors' array
                File data = (File) source.getTransferData(flavors[0]);

                return data;
            }
        };
        DropTarget target = new DropTarget(this, DnDConstants.ACTION_COPY_OR_MOVE, listener, true);

        return target;
    }

    private void loadImages() {
        counter = 1;
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                sizeSlider.setEnabled(false);
            }
        });
        imagePanel.removeAll();

        List fileList = model.getFileList();

        // System.out.println("Filelist size: " + fileList.size());
        total = fileList.size();

        if (total == 0) {
            return;
        }

        if (total > MAX_FILES) {
            total = MAX_FILES * page;

            if (total > fileList.size()) {
                total = fileList.size();
            }
        }

        int position = (page - 1) * MAX_FILES;

        counter = position + 1;

        for (int i = position; i < total; i++) {
            String     filename = (String) fileList.get(i);
            ImageLabel label    = new ImageLabel(new File(model.getDirectory(), filename), gridSize, gridSize);

            labels.add(label);
            imagePanel.add(label);
            label.setDropTarget(getMyDropTarget());
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    status.setText("loading " + counter++ + " of " + total);
                }
            });
        }

        // setting the label as the droptarget removes the parent drop target
        // so we have to re-add it.
        setDropTarget(getMyDropTarget());
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                model.setStatus(labels.size() + " images loaded");
                imagePanel.revalidate();
                imagePanel.repaint();
                sizeSlider.setEnabled(true);
            }
        });
    }

    public boolean moveFileHere(File file) {
        if (file.getParentFile().equals(new File(model.getDirectory()))) {
            return false;
        }

        File moved = new File(this.model.getDirectory(), file.getName());

        if (!file.renameTo(moved)) {
            return false;
        }

        model.updateFileList();
        this.refresh(moved);

        return true;
    }

    private JPanel buildBottomPanel() {
        final JPanel panel = new JPanel(new FormLayout("2dlu, pref, 4dlu:grow, pref, 4dlu:grow, pref, 50dlu, 1dlu,",
                                 "pref, 3dlu:grow, pref"));
        CellConstraints cc      = new CellConstraints();
        BeanAdapter     adapter = new BeanAdapter(model);

        status = BasicComponentFactory.createLabel(adapter.getValueModel("status"));
        panel.add(status, cc.xy(2, 1));
        panel.add(new JLabel("QuickKey = " + getQuickKey()), cc.xy(2, 3));

        int           totalPages = (int) (model.getFileList().size() / MAX_FILES) + 1;
        PageComponent pc         = new PageComponent(totalPages);

        pc.addPropertyChangeListener("currentPage", new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                ClassImageDirectoryFullView.this.setPage((Integer) evt.getNewValue());
            }
        });
        panel.add(pc, cc.xy(4, 1));
        sizeSlider = new JSlider(SwingConstants.HORIZONTAL, 20, 400, gridSize);
        sizeSlider.setToolTipText("Slide to change image size");
        sizeSlider.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                final JSlider source = (JSlider) e.getSource();

                if (!source.getValueIsAdjusting()) {
                    source.setEnabled(false);

                    try {
                        foxtrot.Worker.post(new foxtrot.Task() {
                            public void finish() {
                                source.setEnabled(true);
                            }
                            public Object run() throws Exception {
                                setGridSize(source.getValue());

                                return null;
                            }
                        });
                    } catch (Exception ex) {
                        Logger.getLogger(ClassImageDirectoryFullView.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        });
        panel.add(new JLabel("Zoom:"), cc.xy(6, 1));
        panel.add(sizeSlider, cc.xy(7, 1));
        panel.addHierarchyBoundsListener(new HierarchyBoundsAdapter() {
            public void ancestorResized(HierarchyEvent e) {
                relayoutImagePanel();
            }
        });

        return panel;
    }

    protected void setPage(int newPage) {
        page = newPage;

        Thread t = new Thread() {
            public void run() {
                loadImages();
            }
        };

        t.start();
    }

    private void refresh(File moved) {
        counter = 1;

        List fileList = model.getFileList();

        // System.out.println("Filelist size: " + fileList.size());
        total = fileList.size();

        if (total == 0) {
            return;
        }

        if (total > MAX_FILES) {
            total = MAX_FILES * page;

            if (total > fileList.size()) {
                total = fileList.size();
            }
        }

        int position = (page - 1) * MAX_FILES;

        counter = position + 1;

        for (int i = position; i < total; i++) {
            String filename = ParseUtils.parseFileNameRemoveDirectory((String) fileList.get(i));
            String target   = ParseUtils.parseFileNameRemoveDirectory(moved.toString());

            if (target.equals(filename)) {
                ImageLabel label = new ImageLabel(new File(model.getDirectory(), filename), gridSize, gridSize);

                addLabel(i, label);
                status.setText("loading " + counter++ + " of " + total);

                break;
            }
        }
    }

    private void relayoutImagePanel() {
        int width    = imagePanel.getWidth();
        int numCells = (int) (width / (gridSize + BORDER * 2.0));

        if (numCells < 2) {
            numCells = 2;
        }

        imageLayout.setColumns(numCells);
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Test ImageDirectory");

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        ClassImageDirectoryModel model = new ClassImageDirectoryModel();

        model.setDirectory(new File("/Users/dcline/Desktop/ToSort"));
        model.setName("Test");

        ClassImageDirectoryFullView view = new ClassImageDirectoryFullView(model, 1);

        frame.add(view);
        frame.setBounds(300, 300, 600, 1000);
        frame.setVisible(true);

        JFrame frame1 = new JFrame("Test ImageDirectorySynopsis");

        frame1.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        ClassImageDirectoryModel model1 = new ClassImageDirectoryModel();

        model1.setDirectory(new File("/Users/dcline/Desktop/TrainingLibrary/test"));
        model1.setName("Test");

        ClassImageDirectorySynopsisView view1 = new ClassImageDirectorySynopsisView(model1, 2);

        frame1.add(view1);
        frame1.pack();
        frame1.setVisible(true);
    }

    public ClassImageDirectoryModel getModel() {
        return model;
    }

    public int getGridSize() {
        return gridSize;
    }

    public void setGridSize(int gridSize) {
        this.gridSize = gridSize;
        relayoutImagePanel();

        for (Iterator iter = labels.iterator(); iter.hasNext(); ) {
            ImageLabel label = (ImageLabel) iter.next();

            label.updateImageIcon(getGridSize(), getGridSize());
        }
    }

    public void removeLabel(ImageLabel label) {
        if (labels.remove(label)) {
            imagePanel.remove(label);
            imagePanel.revalidate();
            imagePanel.repaint();
            counter = 1;
            model.updateFileList();

            List fileList = model.getFileList();

            total = fileList.size();

            if (total == 0) {
                return;
            }

            if (total > MAX_FILES) {
                total = MAX_FILES * page;

                if (total > fileList.size()) {
                    total = fileList.size();
                }
            }

            int position = (page - 1) * MAX_FILES;

            counter = position + 1;
            status.setText("loading " + counter++ + " of " + total);
        }
    }

    public void addLabel(int index, ImageLabel label) {
        labels.add(index, label);
        imagePanel.add(label, null, index);

        // setting the label as the droptarget removes the parent drop target
        // so we have to re-add it.
        setDropTarget(getMyDropTarget());
        label.setDropTarget(getMyDropTarget());
    }

    public Component getComponent() {
        return this;
    }

    public DockKey getDockKey() {
        return dockKey;
    }

    public int getQuickKey() {
        return quickKey;
    }
}
