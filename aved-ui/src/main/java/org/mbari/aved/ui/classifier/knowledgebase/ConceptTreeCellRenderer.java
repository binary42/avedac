/*
 * @(#)ConceptTreeCellRenderer.java
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



package org.mbari.aved.ui.classifier.knowledgebase;

//~--- JDK imports ------------------------------------------------------------

import java.awt.Component;

import java.util.Arrays;

import javax.swing.ImageIcon;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;

//RxTBD: wcpr - Displayed font is different when traversing up and down the
//tree?

/**
 * <p><!-- Class description --></p>
 *
 * @version    $Id: ConceptTreeCellRenderer.java,v 1.1 2010/02/03 21:21:53 dcline Exp $
 * @author     <a href="http://www.mbari.org">Monterey Bay Aquarium Research Institute</a>
 */
public class ConceptTreeCellRenderer extends DefaultTreeCellRenderer {
    private static final String DEFAULT = "/org/mbari/aved/ui/images/blueball.gif";
    private static final String DETAILS = "/org/mbari/aved/ui/images/greenball.gif";
    private static final String IMAGE   = "/org/mbari/aved/ui/images/showmovie.gif";
    private ImageIcon           defaultIcon;
    private ImageIcon           hasDetailsIcon;
    private ImageIcon           hasImageIcon;

    // Buffer for text label
    private StringBuffer textBuf;

    /**
     * Constructs ...
     *
     */
    public ConceptTreeCellRenderer() {
        super();
        hasDetailsIcon = new ImageIcon(getClass().getResource(DETAILS));
        defaultIcon    = new ImageIcon(getClass().getResource(DEFAULT));
        hasImageIcon   = new ImageIcon(getClass().getResource(IMAGE));

        // Utility buffer for stringing together Concept names and aliases
        textBuf = new StringBuffer();
    }

    // Override

    /**
     * <p><!-- Method description --></p>
     *
     *
     * @param tree
     * @param value
     * @param isSelected
     * @param isExpanded
     * @param isLeaf
     * @param row
     * @param hasFocus
     *
     * @return
     */
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean isSelected, boolean isExpanded,
            boolean isLeaf, int row, boolean hasFocus) {

        // RxNOTE wcpr After great consternation I finally discovered this line in
        // the implementation of this in DefaultTreeCellRenderer. Without it the
        // background of selected cells does not properly set.
        selected = isSelected;

        // Toggle cell background color
        if (isSelected) {
            setForeground(getTextSelectionColor());
        } else {
            setForeground(getTextNonSelectionColor());
        }

        // Get the name from the Object contained in this node
        DefaultMutableTreeNode node       = (DefaultMutableTreeNode) value;
        Object                 userObject = node.getUserObject();

        // if the user object is a boolean value, then that means that
        // the concept nodes at this level are currently be retrieved.
        if (userObject instanceof Boolean) {
            setText("Retrieving Concepts...");

            return this;
        }

        TreeConcept treeConcept = (TreeConcept) userObject;

        // Put the capatilized primary ConceptName in first
        textBuf.replace(0, textBuf.length(), treeConcept.getName());
        textBuf.setCharAt(0, Character.toUpperCase(textBuf.charAt(0)));

        // now add the aliases
        String[] secondaryNames = treeConcept.getSecondaryNames();

        Arrays.sort(secondaryNames);

        if (0 < secondaryNames.length) {
            textBuf.append(" (");

            for (int i = 0; i < secondaryNames.length; i++) {
                textBuf.append(secondaryNames[i]);

                if (i != secondaryNames.length - 1) {
                    textBuf.append(", ");
                }
            }

            textBuf.append(")");
        }

        setText(textBuf.toString());

        // set the Icon based on Concept information
        if (treeConcept.hasDetails()) {
            setIcon(hasDetailsIcon);
        } else if (treeConcept.hasPrimaryImage()) {
            setIcon(hasImageIcon);
        } else {
            setIcon(defaultIcon);
        }

        return this;
    }
}
