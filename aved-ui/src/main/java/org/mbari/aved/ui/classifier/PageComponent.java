/*
 * @(#)PageComponent.java
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

//~--- non-JDK imports --------------------------------------------------------

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

//~--- JDK imports ------------------------------------------------------------

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class PageComponent extends JComponent {
    private int              currentPage = 1;
    private final JButton    nextButton  = new JButton("Next");
    private final JButton    prevButton  = new JButton("Prev");
    private final JTextField pageDisplay;
    private final int        totalPages;

    public PageComponent(int totalPages) {
        this.totalPages = totalPages;

        // set the jtextfield to the total pages so it's initial size will
        // be big enough to show the right number of digits.
        pageDisplay = new JTextField(" " + totalPages + " ");
        pageDisplay.setHorizontalAlignment(JTextField.RIGHT);
        pageDisplay.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String input  = pageDisplay.getText();
                int    number = currentPage;

                try {
                    number = Integer.parseInt(input);
                } catch (NumberFormatException nfe) {
                    number = currentPage;
                    System.out.println("Unable to parse number provided");
                }

                setCurrentPage(number);
            }
        });
        nextButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setCurrentPage(getCurrentPage() + 1);
            }
        });
        prevButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setCurrentPage(getCurrentPage() - 1);
            }
        });

        FormLayout      layout = new FormLayout("pref, 2dlu, max(22dlu;pref), pref, 2dlu, pref", "20dlu");
        CellConstraints cc     = new CellConstraints();

        this.setLayout(layout);
        this.add(prevButton, cc.xy(1, 1));
        this.add(pageDisplay, cc.xy(3, 1));
        this.add(new JLabel("of " + totalPages), cc.xy(4, 1));
        this.add(nextButton, cc.xy(6, 1));
        setCurrentPage(1);
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(int currentPage) {
        if (currentPage > totalPages) {
            pageDisplay.setText(this.currentPage + "");

            return;
        }

        if (currentPage <= 0) {
            pageDisplay.setText(this.currentPage + "");

            return;
        }

        int oldCurrent = this.currentPage;

        this.currentPage = currentPage;
        pageDisplay.setText(currentPage + "");
        firePropertyChange("currentPage", oldCurrent, currentPage);
    }

    public static void main(String[] args) {
        JPanel       panel = new JPanel();
        final JLabel label = new JLabel("1");

        panel.add(label, BorderLayout.CENTER);

        PageComponent pc = new PageComponent(500);

        pc.addPropertyChangeListener("currentPage", new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                label.setText(evt.getNewValue() + "");
            }
        });
        panel.add(pc, BorderLayout.SOUTH);

        JFrame frame = new JFrame("Test Page Component");

        frame.getContentPane().add(panel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setBounds(300, 300, 600, 600);
        frame.pack();
        frame.setVisible(true);
    }
}
