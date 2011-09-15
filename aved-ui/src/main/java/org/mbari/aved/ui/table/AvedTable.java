/*
 * @(#)AvedTable.java
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



package org.mbari.aved.ui.table;

//~--- non-JDK imports --------------------------------------------------------

import org.mbari.aved.ui.Application;
import org.mbari.aved.ui.model.EventAbstractTableModel;

//~--- JDK imports ------------------------------------------------------------

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;

import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.ListSelectionModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn; 

public class AvedTable extends JTable {
    private static final Color VERTICAL_LINE_COLOR = new Color(0xd9d9d9);

    public AvedTable() {

        // Although it's the JTable default, most systems' tables don't draw a grid by default.
        // Worse, it's not easy (or possible?) for us to take over grid painting
        // ourselves for those LAFs (Metal, for example) that do paint grids.
        setShowGrid(false);

        // Tighten the cells up, and enable the manual painting of the vertical grid lines.
        setIntercellSpacing(new Dimension());
        getTableHeader().setReorderingAllowed(false);

        // Set the table to allow for one or more contiguous ranges of indices at a time.
        getSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
    }

    /**
     * Custom override to implement table header tool tips.
     *
     * @return column tool tip text
     */
    @Override
    protected JTableHeader createDefaultTableHeader() {
        return new JTableHeader(columnModel) {
            @Override
            public String getToolTipText(MouseEvent e) {
                java.awt.Point p         = e.getPoint();
                int            index     = columnModel.getColumnIndexAtX(p.x);
                int            realIndex = columnModel.getColumn(index).getModelIndex();

                return EventAbstractTableModel.columnToolTips[realIndex];
            }
        };
    }

    /**
     * Paints empty rows too, after letting the UI delegate do
     * its painting.
     */
    @Override
    public void paint(Graphics g) {
        if (this.getModel() != null) {
            if (this.getModel().getRowCount() > 0) {
                super.paint(g);
            } else {
                paintEmptyRows(g);
            }
        }
    }

    /**
     * Paints the backgrounds of the implied empty rows when the
     * table model is insufficient to fill all the visible area
     * available to us. We don't involve cell renderers, because
     * we have no data.
     */
    protected void paintEmptyRows(Graphics g) {
        final int       rowCount = getRowCount();
        final Rectangle clip     = g.getClipBounds();
        final int       height   = clip.y + clip.height;

        if (rowCount * rowHeight < height) {
            for (int i = rowCount; i <= height / rowHeight; ++i) {
                g.setColor((i % 2 == 0)
                           ? Color.LIGHT_GRAY
                           : Color.WHITE);
                g.fillRect(clip.x, i * rowHeight, clip.width, rowHeight);
            }

            g.setColor(VERTICAL_LINE_COLOR);
            columnModel = getColumnModel();

            int x = 0;

            for (int i = 0; i < columnModel.getColumnCount(); ++i) {
                TableColumn column = columnModel.getColumn(i);

                x += column.getWidth();
                g.drawLine(x - 1, rowCount * rowHeight, x - 1, height);
            }
        }
    }

    /**
     * Changes the behavior of a table in a JScrollPane to be more like
     * the behavior of JList, which expands to fill the available space.
     * JTable normally restricts its size to just what's needed by its
     * model.
     */
    @Override
    public boolean getScrollableTracksViewportHeight() {
        if (getParent() instanceof JViewport) {
            JViewport parent = (JViewport) getParent();

            return (parent.getHeight() > getPreferredSize().height);
        }

        return false;
    }

    /**
     * Shades alternate rows in different colors.
     */
    public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
        Component c        = super.prepareRenderer(renderer, row, column);
        boolean   selected = isCellSelected(row, column);

        if (selected) {
            c.setBackground(Application.lookAndFeelSettings.getSelectedColor());
        } else {

            // Outside of selected rows, we want to alternate the background color.
            c.setBackground((row % 2 == 0)
                            ? Color.LIGHT_GRAY
                            : Color.WHITE);
        }

        if (c instanceof JComponent) {
            JComponent jc = (JComponent) c;

            if ((getCellSelectionEnabled() == false) && (isEditing() == false)) {
                jc.setBorder(null);
            }

            initToolTip(jc, row, column);
            c.setEnabled(isEnabled());
        }

        return c;
    }

    /**
     * Sets the component's tool tip if the component is being rendered smaller than its preferred size.
     * This means that all users automatically get tool tips on truncated text fields that show them the full value.
     */
    private void initToolTip(JComponent c, int row, int column) {
        String toolTipText = null;

        if (c.getPreferredSize().width > getCellRect(row, column, false).width) {
            toolTipText = getValueAt(row, column).toString();
        }

        c.setToolTipText(toolTipText);
    }

    /**
     * Places tool tips over the cell they correspond to. MS Outlook does this, and it works rather well.
     * Swing will automatically override our suggested location if it would cause the tool tip to go off the display.
     */
    @Override
    public Point getToolTipLocation(MouseEvent e) {

        // After a tool tip has been displayed for a cell that has a tool tip,
        // cells without tool tips will show an empty tool tip until the tool tip
        // mode times out (or the table has a global default tool tip).
        // (ToolTipManager.checkForTipChange considers a non-null result
        // from getToolTipText *or* a non-null result from getToolTipLocation
        // as implying that the tool tip should be displayed. This seems like
        // a bug, but that's the way it is.)
        if (getToolTipText(e) == null) {
            return null;
        }

        final int row    = rowAtPoint(e.getPoint());
        final int column = columnAtPoint(e.getPoint());

        if ((row == -1) || (column == -1)) {
            return null;
        }

        return getCellRect(row, column, false).getLocation();
    }
}
