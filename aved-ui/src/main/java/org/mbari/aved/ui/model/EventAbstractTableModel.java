/*
 * @(#)EventAbstractTableModel.java
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



package org.mbari.aved.ui.model;

//~--- non-JDK imports --------------------------------------------------------

import java.util.logging.Level;
import java.util.logging.Logger;
import org.mbari.aved.ui.appframework.AbstractModel;

//~--- JDK imports ------------------------------------------------------------

import javax.swing.ImageIcon;
import javax.swing.table.AbstractTableModel;

public class EventAbstractTableModel extends AbstractModel {
    public static final int      NUM_COLUMNS = 13;
    public static final int      COL_THUMBNAIL = 6;
    
    public static final String[] columnNames = {
        "Object ID", "Predicted Class", "Probability in Class", "Class", "Tag", "Identity Reference", "Thumbnail", "Max Size",
        "Start TimeCode", "End Timecode", "Start Frame", "End Frame", "Frame Duration", 
    };
    public static final String[] columnToolTips = {
        "The  numerical index assigned by mbarivision to this event. "
        + "This is simply a numerically increasing assignment" + " in the order the events were automatically detected",
        "The predicted class name this event was assigned to be. This can either be a VARS "
        + "concept, or a user-defined class name. This corresponds to the " + "AVEDac classifier prediction",
        "The probability of the predicted class. A number between 0 and 1.0 "
        + "The higher this number, the more accurate the predicted value.",
        "The class name this event was assigned to be. If using VARS " + "this is equivalent to a VARS concept",
        "A user defined tag. This can be anything you define, e.g. swimming, "
        + "ignore, junk. Useful to help sort and combine events",
        "A user defined identity reference.  This can be anything you define," 
        + "The frame duration of this event", "A thumbnail picture of event,"
        + " e.g. benthocodon1, benthocodon2. Use e.g. to label whether same " + "animal or new animal",
        "The maximum size (square pixels) of any instance of this event " + "over its lifetime.",
        "The first time the event was detected. An optional field and is emtpy "
        + "if the video source was not ISO860 formatted.",
        "The last time the event was detected. An optional field that's emtpy if "
        + "the video source was not ISO860 formatted.",
        "The first frame the event was detected", "The last frame the event was detected"
    };
    private static final long serialVersionUID = 1L;

    /**
     * Abstract table model class that contains a custom
     * table model for handling table event notifications
     * for AVED metadata
     */
    private CustomTableModel customTableModel = null;

    /**
     * Constructor. Creates custom table model with the eventlistmodel
     * @param eventListModel
     */
    public EventAbstractTableModel(EventListModel eventListModel) throws Exception {
        customTableModel = new CustomTableModel();
        customTableModel.replace(eventListModel);
    }

    /**  */
    public CustomTableModel getTableModel() {
        return customTableModel;
    }

    public class CustomTableModel extends AbstractTableModel {

        /**
         *  Custom table model that displays AVED xml metadata in a column
         *  format.
         */
        private static final long serialVersionUID = 1L;
        private EventListModel    eventListModel;
        private ImageIcon         missingFrameImageIcon;

        /** Creates a new instance of TableModel */
        public CustomTableModel() throws Exception {

            // If imageIcon is missing, insert a dummy imageIcon
            missingFrameImageIcon =
                new ImageIcon(getClass().getResource("/org/mbari/aved/ui/images/missingframeexception.jpg"));

            // If imageIcon is missing, insert a dummy imageIcon
            if (missingFrameImageIcon == null) {
                throw new Exception("Cannot find missingframeexception.jpg");
            }
        }

        /**
         * Returns the number of columns in the model. A
         * <code>JTable</code> uses this method to determine how many columns it
         * should create and display by default.
         *
         * @return the number of columns in the model
         * @see #getRowCount
         *
         */
        public int getColumnCount() {
            return columnNames.length;
        }

        public String getColumnName(int c) {
            return columnNames[c];
        }

        /**
         * Returns the number of rows in the model. A
         * <code>JTable</code> uses this method to determine how many rows it
         * should display.  This method should be quick, as it
         * is called frequently during rendering.
         *
         * @return the number of rows in the model
         * @see #getColumnCount
         *
         */
        public int getRowCount() {
            return ((eventListModel != null)
                    ? eventListModel.getSize()
                    : 0);
        }

        public Object getEntry(int rowIndex) {
            return ((eventListModel != null)
                    ? eventListModel.getElementAt(rowIndex)
                    : 0);
        }

        /**
         * Returns the most specific superclass for all the cell values
         * in the column.  This is used by the <code>JTable</code> to set up a
         * default renderer and editor for the column.
         *
         * @param columnIndex  the index of the column
         * @return the common ancestor class of the object values in the model.
         */
        @Override
        public Class getColumnClass(int columnIndex) {
            return getValueAt(0, columnIndex).getClass();
        }

        /**
         * Returns the value for the cell at <code>columnIndex</code> and
         * <code>rowIndex</code>.
         *
         * @param       rowIndex        the row whose value is to be queried
         * @param       columnIndex     the column whose value is to be queried
         * @return      the value Object at the specified cell
         *
         */
        public Object getValueAt(int rowIndex, int columnIndex) {
            if ((eventListModel != null) && (eventListModel.getSize() > 0)) {
                try {
                    EventObjectContainer e = eventListModel.getElementAt(rowIndex);

                    if (e == null) {
                        return String.valueOf(-1);
                    }

                    switch (columnIndex) {
                    case 0 :
                        return e.getObjectId();

                    case 1 :
                        return e.getPredictedClassName();

                    case 2 :
                        return e.getPredictedClassProbability();

                    case 3 :
                        return e.getClassName();

                    case 4 :
                        return e.getTag();

                    case 5 :
                        return e.getIdentityReference();
    
                    case COL_THUMBNAIL :
                        ImageIcon imageIcon = e.getBestImage();

                        if (imageIcon != null) {
                            return imageIcon;
                        }

                        // If imageIcon is missing, insert a dummy imageIcon
                        return missingFrameImageIcon;

                    case 7 :
                        return e.getMaxSize();

                    case 8 :
                        return e.getStartTimecode();

                    case 9 :
                        return e.getEndTimecode();

                    case 10 :
                        return e.getStartFrame();

                    case 11 :
                        return e.getEndFrame();

                    case 12 :
                        return e.getTtlFrames();

                  
                    default :
                        return String.valueOf(-1);
                    }
                } catch (Exception ex) {
                    Logger.getLogger(EventAbstractTableModel.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

            return String.valueOf(-1);
        }

        /**
         * Replaces the list model
         * @param listmodel
         */
        public void replace(EventListModel listmodel) {
            eventListModel = listmodel;
            fireTableDataChanged();
        }

        public void clear() {
            if (eventListModel != null) {
                eventListModel = null;
            }

            fireTableDataChanged();
        }
    }
}
