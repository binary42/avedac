/*
 * @(#)ApplicationView.java
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



package org.mbari.aved.ui;

//~--- non-JDK imports --------------------------------------------------------

import com.jeta.forms.components.image.ImageComponent;
import com.jeta.forms.components.panel.FormPanel;
import com.jeta.forms.gui.form.FormAccessor;

import org.mbari.aved.ui.appframework.JFrameView;
import org.mbari.aved.ui.appframework.ModelEvent;
import org.mbari.aved.ui.model.SummaryModel;
import org.mbari.aved.ui.utils.URLUtils;

//~--- JDK imports ------------------------------------------------------------

import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.MouseListener;

import java.io.File;

import java.net.URL;

import java.util.Iterator;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import org.mbari.aved.ui.process.ProcessDisplay;

/**
 *
 */
public class ApplicationView extends JFrameView {
    private static final long  serialVersionUID = 1L;
    public final static Cursor defaultCursor    = Cursor.getDefaultCursor();

    /** Process display for transcoding */
    private ProcessDisplay display;
    
    /** Busy and wait cursor */
    public final static Cursor busyCursor = Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR);
    private SummaryView        summaryView;

    /**
     * ApplicationView.
     */
    public ApplicationView(ApplicationModel model, ApplicationController controller) throws Exception {
        super("org/mbari/aved/ui/forms/Main.xml", model, controller);
        super.setFocusable(true);

        // Create Jerome's pretty header and title
        ImageIcon icon = new ImageIcon(getClass().getResource("/org/mbari/aved/ui/images/logo.jpg"));

        if (icon != null) {
            JPanel      p      = new JPanel(new BorderLayout());
            HeaderPanel header = new HeaderPanel("Automated Visual Event Detection and Classification   ", icon, 0.25f);

            p.add(BorderLayout.NORTH, header);
            p.setBorder(new EmptyBorder(0, 0, 0, 0));

            // Replace header with pretty one
            replaceHeaderPanel(p);
        } else {
            throw new Exception("Invalid logo.jpg");    // Create a new instance of the summary view
        }

        summaryView = new SummaryView(model, controller);

        // Replace embedded summary panel with the summary form
        replaceSummaryPanel(summaryView.getForm());

        // Set the application title
        setTitle(ApplicationInfo.getName());

        // Don't do anything; require the program to handle the operation in the
        // windowClosing  method of a registered WindowListener object.
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

        Toolkit   kit        = this.getToolkit();
        Dimension screenSize = kit.getScreenSize();

        // Set window size to full display
        setSize((int) (screenSize.width), (int) (screenSize.height));

        // Display window in center of screen
        int x = (screenSize.width - getWidth()) / 2;
        int y = (screenSize.height - getHeight()) / 2;

        setLocation(x, y);
    }

    public void replaceThumbnailPanel(FormPanel form) {
        JTabbedPane pane = getTabbedPane();

        if (pane != null) {
            pane.setComponentAt(1, form);
        }
    }

    public void replaceTablePanel(JTable table) {
        JTabbedPane pane = getTabbedPane();

        if (pane != null) {

            // Put table in scroll pane  this allows for display of
            // column headings
            JScrollPane s = new JScrollPane(table, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                            JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

            pane.setComponentAt(0, s);

            // Calculate the height remaining in the view
            // after the summary view
            Dimension pd = this.getPreferredSize();
            Dimension sd = summaryView.getSize();
            Dimension vd = getSize();

            sd.height = vd.height - sd.height;
            pane.setPreferredSize(sd);
        }
    }

    private void replaceSummaryPanel(FormPanel panel) {
        FormAccessor accessor = getForm().getFormAccessor("main");

        // TODO: put exception handling code around this
        accessor.replaceBean("editorSummary", panel);
    }

    private void replaceHeaderPanel(JPanel p) {
        FormAccessor        accessor = getForm().getFormAccessor("header");
        Iterator<Component> i        = accessor.beanIterator();

        while (i.hasNext()) {
            Component c = i.next();

            if (c instanceof FormAccessor) {

                // found nested form
            } else {

                // if this is the image component, then replace it
                if (c.getClass().equals((ImageComponent.class))) {
                    accessor.replaceBean(c, p);
                }
            }
        }
    }

    public JTabbedPane getTabbedPane() {
        FormPanel   panel = getForm();
        JTabbedPane c     = panel.getTabbedPane("tabbedPane");

        return c;
    }

    /** Handles the model changes from the ApplicationModel */
    public void modelChanged(ModelEvent event) {}

    public void actionPerformed(ActionEvent e) {}

    public SummaryView getSummaryView() {
        return summaryView;
    }

    /** Helper function to return a reference to the MpegLabel in the EditorSummary form panel */
    public JLabel getMpegLabel() {
        if (summaryView != null) {
            return summaryView.getMpegLabel();
        }

        return null;
    }

    /** Helper function to return a reference to the MasterLabel in the EditorSummary form panel */
    public JLabel getMasterLabel() {
        if (summaryView != null) {
            return summaryView.getMasterLabel();
        }

        return null;
    }

    /** Sets the busy cursor for this view. Use this to disable the user mouse during long operations */
    public void setBusyCursor() {
        this.requestFocusInWindow();
        this.setCursor(busyCursor);
    }

    /** Sets the default cursor for this view. Use this when a long operation is done */
    public void setDefaultCursor() {
        this.requestFocusInWindow();
        this.setCursor(defaultCursor);
    }

    public void display(JFrame v) {
        if (v.getState() == ICONIFIED) {
            if (getToolkit().isFrameStateSupported(MAXIMIZED_BOTH)) {
                v.setExtendedState(MAXIMIZED_BOTH);
                v.setVisible(true);
                v.toFront();
            } else {

                /*
                 * String err = v.getTitle() + " window is already open but " +
                 * "minimized. Please open window manually.  Unminimize " +
                 * "is not supported on this platform ";
                 * try {
                 * NonModalMessageDialog dialog = new NonModalMessageDialog(this, err);
                 * } catch (Exception ex) {
                 * ex.printStackTrace();
                 * }
                 */
            }
        } else {
            v.setVisible(true);
            v.toFront();
        }
    }

    /**
     * Ce panneau représente l'en-tête d'un formulaire. Un en-tête affiche un logo (dont l'opacité peut
     * être modifiée) et un titre.
     *
     * @author Roguy
     */
    public static class HeaderPanel extends JPanel {
        private static AlphaComposite composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER);
        private Color                 blender;
        private ImageIcon             icon;

        /**
         * Crée un nouvel en-tête affichant l'image sélectionnée, avec l'opacité choisie et le titre
         * spécifié.
         *
         * @param title Le titre de l'en-tête
         * @param icon L'image d'en-tête
         * @param alpha L'opacité de l'image
         */
        public HeaderPanel(String title, ImageIcon icon, float alpha) {
            super(new BorderLayout());
            this.icon    = icon;
            this.blender = new Color(255, 255, 255, (int) (255 * alpha));

            JLabel headerTitle = new JLabel(title);
            Font   font        = new Font("Papyrus", Font.PLAIN, 20);

            // Font font = headerTitle.getFont().deriveFont(Font.PLAIN, 20.0f);
            headerTitle.setFont(font);
            headerTitle.setBorder(new EmptyBorder(0, 0, 0, 4));
            headerTitle.setForeground(new Color(0x04549a));
            add(BorderLayout.EAST, headerTitle);
            setPreferredSize(new Dimension(this.icon.getIconWidth(), this.icon.getIconHeight()));
        }

        /**
         * Récupère la couleur de l'en-tête en fonction du thème choisi.
         *
         * @return Le couleur de fond de l'en-tête
         */
        protected Color getHeaderBackground() {
            Color c = UIManager.getColor("SimpleInternalFrame.activeTitleBackground");

            if (c != null) {
                return c;
            }

            return (c != null)
                   ? c
                   : UIManager.getColor("InternalFrame.activeTitleBackground");
        }

        /**
         * Dessine un dégradé dans le composant.
         *
         * @param g L'objet graphique sur lequel peindre
         */
        public void paintComponent(Graphics g) {
            super.paintComponent(g);

            if (!isOpaque()) {
                return;
            }

            Color      control = new Color(178, 213, 255);
            int        width   = getWidth();
            int        height  = getHeight();
            Graphics2D g2      = (Graphics2D) g;

            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);

            Paint storedPaint = g2.getPaint();

            g2.setPaint(new GradientPaint(this.icon.getIconWidth(), 0, Color.white, width, 0, control));
            g2.fillRect(0, 0, width, height);
            g2.setPaint(storedPaint);
            g2.drawImage(this.icon.getImage(), 0, 0, this);
            g2.setColor(blender);
            g2.setComposite(composite);
            g2.fillRect(0, 0, this.icon.getIconWidth(), this.icon.getIconHeight());
        }
    }


    public static class SummaryView extends JFrameView {
        public static final String ID_IMPORT_DIRECTORY_LABEL = "importDir";         // javax.swing.JTable
        public static final String ID_INPUTSOURCE_URL_LABEL  = "videoSourceURL";    // javax.swing.JLabel
        public static final String ID_MASTER_BROWSE_BUTTON   = "browseMaster";      // javax.swing.JButton
        public static final String ID_MPEG_BROWSE_BUTTON     = "browseMpeg";        // javax.swing.JButton
        public static final String ID_MPEG_FILE_LABEL        = "mpegFile";          // javax.swing.JLabel

        /*
         *  Component names in the EditorSummary.xml form
         * If any of the component name are changed in the Abeille form designer, they
         * should be modified here too
         */
        public static final String ID_XML_FILE_LABEL = "xmlFile";    // javax.swing.JLabel

        /**
         *
         */
        private static final long serialVersionUID = 1L;

        /** frequently accessed components */
        JLabel mpegFileLabel, xmlFileLabel, inputSourceUrlLabel, importDirectoryLabel;

        public SummaryView(ApplicationModel model, ApplicationController controller) {

            // Constructor
            super("org/mbari/aved/ui/forms/Summary.xml", model, controller);

            // Add action listeners for hte button, but disable them from
            // view until there is data to edit
            ActionHandler l = getActionHandler();

            getForm().getButton(ID_MPEG_BROWSE_BUTTON).addActionListener(l);
            getForm().getButton(ID_MASTER_BROWSE_BUTTON).addActionListener(l);
            mpegFileLabel        = getForm().getLabel(ID_MPEG_FILE_LABEL);
            xmlFileLabel         = getForm().getLabel(ID_XML_FILE_LABEL);
            inputSourceUrlLabel  = getForm().getLabel(ID_INPUTSOURCE_URL_LABEL);
            importDirectoryLabel = getForm().getLabel(ID_IMPORT_DIRECTORY_LABEL);
        }

        protected void paintComponent(Graphics graphics) {
            Graphics g = graphics.create();

            g.dispose();
        }

        /** Returns a reference to the Mpeg Label object */
        public JLabel getMpegLabel() {
            return mpegFileLabel;
        }

        /** Returns a reference to the Master Label object */
        public JLabel getMasterLabel() {
            return inputSourceUrlLabel;
        }

        /** Adds mouse listener to file clicks */
        @Override
        public void addMouseListener(MouseListener l) {
            if (mpegFileLabel != null) {
                mpegFileLabel.addMouseListener(l);
            }

            if (inputSourceUrlLabel != null) {
                inputSourceUrlLabel.addMouseListener(l);
            }
        }

        /**
         * Updates the display logic.
         *  Changes the labels and associated buttons
         *  If all files are in the same directory, sets the import directory label and strips off
         *  the full path names of the results XML and video clips to avoid clutter in the UI.
         *  Otherwise, if files are in different directories, displays the full path names
         */
        private void runDisplayLogic() {
            SummaryModel e           = ((ApplicationModel) getModel()).getSummaryModel();
            File         xml         = e.getXmlFile();
            URL          mpeg        = e.getMpegUrl();
            URL          inputsource = e.getInputSourceURL();

            // If the xml is defined then show all related files/directories
            // relative to it
            if (xml != null) {
                if (xml.getParent() != null) {
                    importDirectoryLabel.setText(xml.getParent().toString());
                } else {
                    importDirectoryLabel.setText(xml.toString());
                }

                xmlFileLabel.setText(xml.getName()); 
                       
                if (mpeg != null ) {
                    if (URLUtils.isValidURL(mpeg)) {
                        File f = new File(mpeg.toString());

                        mpegFileLabel.setText("<html><a href>" + f.getName() + "</a></html>");
                    } else {

                        // This is a URL. Since it can be long, so only display the name
                        // not the fullpath
                        File f = new File(mpeg.getPath());

                        mpegFileLabel.setText("<html><a href>" + f.getName() + "</a></html>");
                    }
                } else {
                    mpegFileLabel.setText("");
                }

                if (inputsource != null) {
                    if (URLUtils.isValidURL(inputsource)) {
                        File f = new File(inputsource.toString());

                        inputSourceUrlLabel.setText("<html><a href>" + f.getName() + "</a></html>");
                    } else {

                        /**
                         * This is a URL. Since it can be long, so only display the name
                         * not the fullpath.  Only display the URL if it doesn't end with
                         * a tar tgz, or tar.gz
                         */
                        File f = new File(inputsource.getPath());

                        inputSourceUrlLabel.setText("<html><a href>" + f.getName() + "</a></html>");
                    }
                } else {
                    inputSourceUrlLabel.setText("");
                }
            } else {

                // If the xml isn't defined, then show all full path names
                // and html reference if valid, otherwise indicate missing
                // data with "-"
                importDirectoryLabel.setText("-");
                xmlFileLabel.setText("-");

                if (mpeg != null) {
                    mpegFileLabel.setText("<html><a href>" + mpeg.toString() + "</a></html>");
                } else {
                    mpegFileLabel.setText("-");
                }

                if (inputsource != null) {
                    inputSourceUrlLabel.setText("<html><a href>" + inputsource.toString() + "</a></html>");
                } else {
                    inputSourceUrlLabel.setText("-");
                }
            }
        }

        /** Handles the model changes from the ApplicationModel */
        public void modelChanged(ModelEvent event) {
            if (event instanceof SummaryModel.SummaryModelEvent) {
                runDisplayLogic();
            }
        }
    }
}
