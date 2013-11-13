/*
 * @(#)ImageLabel.java
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



package org.mbari.aved.ui.classifier;

//~--- JDK imports ------------------------------------------------------------

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.datatransfer.Transferable;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;

import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;

public class ImageLabel extends JLabel {

    // we need to maintain a reference to dockingContainer in order to perform
    // quickKey operations that allow for quick transfer of images to directories
    // without drag and drop.
    // I don't want to pollute the whole object chain with references to DockingContainer
    // so, for now, we'll just use a single static reference.
    static DockingContainer dockingContainer = null;
    private BufferedImage   grayedImage      = null;

    // the image, with a gray mask applied to it (to indicate mouse selection)
    private ImageIcon grayedImageIcon = null;

    // the image, as read from disk
    private BufferedImage image = null;

    // the image, after being scaled to the users chosen resolution
    private ImageIcon scaledImageIcon = null;
    private File      file;

    public ImageLabel(File file) {
        this(file, 100, 100);
    }

    public ImageLabel(File file, int x, int y) {
        this.file = file;
        updateImageIcon(x, y);

        TransferHandler myHandler = new TransferHandler("file") {
            protected void exportDone(JComponent source, Transferable data, int action) {
                super.exportDone(source, data, action);

                if (action == TransferHandler.MOVE) {
                    removeSelfFromFullView();
                }

                removeGrayOverlay();
            }
            public int getSourceActions(JComponent c) {
                return TransferHandler.MOVE;
            }
        };

        setTransferHandler(myHandler);

        MouseListener listener = new DragMouseAdapter();

        addMouseListener(listener);
        setToolTipText(file.getPath());
        setBorder(BorderFactory.createEmptyBorder());
    }

    private void removeSelfFromFullView() {

        // achase 20060914: yeah, this is really ugly.
        // I did it this way because I'm trying to avoid a direct reference to
        // the instance of ImageDirectoryFullView
        Container container = this.getParent();

        while ((container != null) &&!(container instanceof ClassImageDirectoryView)) {
            container = container.getParent();
        }

        if (container instanceof ClassImageDirectoryView) {
            ((ClassImageDirectoryView) container).removeLabel(this);
        }
    }

    public void updateImageIcon(int width, int height) {
        Image img = getImage();

        // to scale images while retaining appropriate proportions, use a BufferedImage object
        // as described here: http://java.sun.com/j2se/1.4.2/docs/guide/imageio/spec/apps.fm1.html
        scaledImageIcon = new ImageIcon(img.getScaledInstance(width, height, Image.SCALE_SMOOTH));
        grayedImageIcon = null;
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                ImageLabel.this.setText("");
                ImageLabel.this.setIcon(scaledImageIcon);
            }
        });
    }

    private BufferedImage getImage() {
        if (this.image != null) {
            return this.image;
        }

        image = getUncachedImage();

        return image;
    }

    private BufferedImage getUncachedImage() {
        try {
            BufferedImage bi = ImageIO.read(file);

            if (bi == null) {
                bi = new BufferedImage(100, 100, BufferedImage.TYPE_4BYTE_ABGR);

                Graphics2D g         = bi.createGraphics();
                String     extension = file.getName().substring(file.getName().lastIndexOf('.'));

                g.setPaint(Color.red);
                g.fillRect(0, 0, 100, 100);
                g.setPaint(Color.black);
                g.drawString(extension + " reader", 0, 40);
                g.drawString("required", 0, 60);
                g.dispose();
            }

            return bi;
        } catch (IOException e) {
            System.err.println("Unable to open " + file);
            e.printStackTrace();
        }

        return null;
    }

    private void addGrayOverlayToImage() {
        if (grayedImageIcon == null) {
            if (grayedImage == null) {
                grayedImage = getUncachedImage();

                Graphics2D g = (Graphics2D) grayedImage.createGraphics();

                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
                g.setPaint(Color.GRAY);
                g.fillRect(0, 0, grayedImage.getWidth(), grayedImage.getHeight());
                g.dispose();
            }

            // use the getIcon method so we don't have to track what size the image should be
            Icon icon = this.getIcon();

            grayedImageIcon = new ImageIcon(grayedImage.getScaledInstance(icon.getIconWidth(), icon.getIconHeight(),
                    Image.SCALE_SMOOTH));
        }

        this.setIcon(grayedImageIcon);
    }

    private void removeGrayOverlay() {
        this.setIcon(scaledImageIcon);
    }

    public static void main(String[] args) {
        final JFrame frame = new JFrame("Test ImageDirectorySynopsis");

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        final ImageLabel label =
            new ImageLabel(new File("c:/Documents and Settings/achase/My Documents/My Pictures/auv.gif"));

        frame.add(label);
        frame.pack();
        frame.addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent e) {
                label.updateImageIcon(frame.getWidth(), frame.getHeight());
            }
        });
        frame.setVisible(true);

        JFrame frame1 = new JFrame("Test ImageDirectorySynopsis");

        frame1.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        ClassImageDirectoryModel model = new ClassImageDirectoryModel();

        model.setDirectory(new File("c:/Documents and Settings/achase/My Documents/My Pictures"));
        model.setName("Test");

        ClassImageDirectorySynopsisView view = new ClassImageDirectorySynopsisView(model, 0);

        frame1.add(view);
        frame1.pack();
        frame1.setVisible(true);
    }

    public File getFile() {
        return file;
    }

    public DockingContainer getDockingContainer() {
        return dockingContainer;
    }

    public void setDockingContainer(DockingContainer dockingContainer) {
        this.dockingContainer = dockingContainer;
    }

    private class DragMouseAdapter extends MouseAdapter {
        public void mousePressed(final MouseEvent e) {

            // if we're in quickKey mode, short circuit.
            if ((getDockingContainer() != null) && (getDockingContainer().getCurrentQuickKey() != 0)) {
                if (getDockingContainer().quickCopy(ImageLabel.this, getDockingContainer().getCurrentQuickKey())) {
                    removeSelfFromFullView();
                }

                return;
            }

            final JComponent c = (JComponent) e.getSource();

            addGrayOverlayToImage();

            TransferHandler handler = c.getTransferHandler();

            handler.exportAsDrag(c, e, TransferHandler.MOVE);
        }
    }
}
