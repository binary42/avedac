/*
 * @(#)ClassImageDirectorySynopsisView.java
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



package org.mbari.aved.ui.classifier;

//~--- non-JDK imports --------------------------------------------------------

import com.jgoodies.binding.adapter.BasicComponentFactory;
import com.jgoodies.binding.beans.BeanAdapter;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import com.vlsolutions.swing.docking.DockKey;
import com.vlsolutions.swing.docking.Dockable;

import org.mbari.aved.ui.classifier.ImageLabel;

//~--- JDK imports ------------------------------------------------------------

import java.awt.Component;
import java.awt.Font;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.HierarchyBoundsAdapter;
import java.awt.event.HierarchyEvent;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import java.io.File;
import java.io.IOException;

import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public class ClassImageDirectorySynopsisView extends JPanel implements Dockable, ClassImageDirectoryView {
    private int              quickKey   = 0;
    private final List<File> imageStack = new ArrayList<File>();

    // we need to save a reference to this panel so that we can calculate the space
    // available for images.
    private JPanel                         bottomPanel;
    private final DockKey                  dockKey;
    private JPanel                         labelPanel;
    private final ClassImageDirectoryModel model;

    public ClassImageDirectorySynopsisView(ClassImageDirectoryModel model, int quickKey) {
        this.model    = model;
        this.quickKey = quickKey;
        init();
        setDropTarget(getMyDropTarget());
        dockKey = new DockKey(model.getDirectory());
        dockKey.setCloseEnabled(true);
        dockKey.setResizeWeight(0.5f);
        dockKey.setTooltip("Images dragged here will be moved to this directory");

        List l = model.getFileList();

        // Add an image to the file listing on startup if this directory is non-empty
        if (l.size() > 0) {
            updateLabel(new File(model.getDirectory() + "/" + l.get(0)));
        }

        this.addHierarchyBoundsListener(new HierarchyBoundsAdapter() {
            public void ancestorResized(HierarchyEvent e) {
                if (imageStack.size() > 0) {
                    updateLabel(imageStack.get(imageStack.size() - 1));
                    System.out.println("resizing...");
                }
            }
        });
    }

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

    public boolean moveFileHere(File file) {
        if (file.getParentFile().equals(new File(model.getDirectory()))) {
            return false;
        }

        File moved = new File(this.model.getDirectory(), file.getName());

        if (!file.renameTo(moved)) {
            return false;
        }

        model.updateFileList();
        updateLabel(moved);
        imageStack.add(moved);

        return true;
    }

    private void updateLabel(File file) {
        int size = 0;

        // we have to subtract an additional 10 to compensate for borders... i think
        int height = getHeight() - bottomPanel.getHeight() - 10;

        if (getWidth() < height) {
            size = getWidth() - 10;
        } else {
            size = height;
        }

        ImageLabel label = new ImageLabel(file, size, size);

        label.setDropTarget(getMyDropTarget());

        // setting the label as the droptarget removes the parent drop target
        // so we have to re-add it.
        setDropTarget(getMyDropTarget());
        labelPanel.removeAll();
        labelPanel.add(label);
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                ClassImageDirectorySynopsisView.this.repaint();
            }
        });
    }

    private void init() {
        this.setLayout(new FormLayout("center:pref:grow", "center:pref:grow, max(20dlu;pref)"));

        CellConstraints ccMain       = new CellConstraints();
        BeanAdapter     modelAdapter = new BeanAdapter(model);
        JLabel          mainLabel    = new JLabel(model.getName());

        mainLabel.setFont(new Font("Serif", Font.BOLD, 24));
        mainLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        mainLabel.setBorder(BorderFactory.createEmptyBorder());
        labelPanel = new JPanel();
        labelPanel.add(mainLabel);
        this.add(labelPanel, ccMain.xy(1, 1));

        JLabel dir = BasicComponentFactory.createLabel(modelAdapter.getValueModel("name"));

        bottomPanel = new JPanel(new FormLayout("2dlu, left:pref, fill:2dlu:grow, right:pref, 2dlu", "pref, pref"));

        CellConstraints cc = new CellConstraints();

        bottomPanel.add(dir, cc.xy(2, 2));

        final JLabel fileCount = new JLabel("Files: " + model.getFileList().size());

        model.addPropertyChangeListener("fileList", new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        fileCount.setText("Files: " + model.getFileList().size());
                    }
                });
            }
        });
        bottomPanel.add(fileCount, cc.xy(4, 2));

        if (getQuickKey() != 0) {
            bottomPanel.add(new JLabel("QuickKey = " + getQuickKey()), cc.xywh(2, 1, 3, 1));
        }

        this.add(bottomPanel, ccMain.xy(1, 2, CellConstraints.FILL, CellConstraints.BOTTOM));
    }

    public Component getComponent() {
        return this;
    }

    public DockKey getDockKey() {
        return dockKey;
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Test ImageDirectorySynopsis");

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        ClassImageDirectoryModel model = new ClassImageDirectoryModel();

        model.setDirectory(new File("/Users/dcline/Desktop/TrainingLibrary/test"));
        model.setName("Test");

        ClassImageDirectorySynopsisView view = new ClassImageDirectorySynopsisView(model, 0);

        frame.add(view);
        frame.pack();
        frame.setVisible(true);
    }

    public int getQuickKey() {
        return quickKey;
    }

    public void setQuickKey(int quickKey) {
        this.quickKey = quickKey;
    }

    public void removeLabel(ImageLabel label) {
        imageStack.remove(label.getFile());

        if (imageStack.size() > 0) {
            updateLabel(imageStack.get(imageStack.size() - 1));
        } else {
            labelPanel.removeAll();
            labelPanel.add(new JLabel(model.getName()));
        }

        labelPanel.repaint();
        model.updateFileList();
    }

    public ClassImageDirectoryModel getModel() {
        return model;
    }
}
