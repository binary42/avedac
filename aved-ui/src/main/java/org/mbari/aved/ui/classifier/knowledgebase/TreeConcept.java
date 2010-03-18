/*
 * @(#)TreeConcept.java   10/03/17
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

//~--- non-JDK imports --------------------------------------------------------

import org.mbari.vars.knowledgebase.model.Concept;
import org.mbari.vars.knowledgebase.model.ConceptName;

import vars.knowledgebase.IConceptName;

//~--- JDK imports ------------------------------------------------------------

import java.util.Collection;
import java.util.Iterator;

import javax.swing.tree.DefaultMutableTreeNode;

/**
 * <p><!-- Class description --></p>
 *
 * @version    $Id: TreeConcept.java,v 1.1 2010/02/03 21:21:53 dcline Exp $
 * @author     <a href="http://www.mbari.org">Monterey Bay Aquarium Research Institute</a>
 */
public class TreeConcept implements Comparable {
    private final Concept concept;
    private boolean       details;
    private String        name;
    private boolean       primaryImage;
    private String[]      secondaryNames;

    /**
     * Constructs ...
     *
     *
     * @param concept
     */
    TreeConcept(Concept concept) {
        this.name    = concept.getPrimaryConceptNameAsString();
        this.concept = concept;

        IConceptName[] secondaryConceptNames = concept.getSecondaryConceptNames();

        secondaryNames = new String[secondaryConceptNames.length];

        for (int i = 0; i < secondaryConceptNames.length; ++i) {
            secondaryNames[i] = secondaryConceptNames[i].getName();
        }

        // TODO 20040402 brian: This call tries to load the conceptDelegate.
        // Which slows things up. Will need to optimize this at some point.
        details      = concept.hasDetails();
        primaryImage = concept.hasPrimaryImage();
    }

    /*
     *  (non-Javadoc)
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */

    /**
     * <p><!-- Method description --></p>
     *
     *
     * @param o
     *
     * @return
     */
    public int compareTo(Object o) {
        return getName().compareTo(((TreeConcept) o).getName());
    }

    /**
     * <p><!-- Method description --></p>
     *
     *
     * @return
     */
    public Concept getConcept() {
        return concept;
    }

    /**
     * <p><!-- Method description --></p>
     *
     *
     * @return
     */
    public String getName() {
        return name;
    }

    /**
     * <p><!-- Method description --></p>
     *
     *
     * @return
     */
    String[] getSecondaryNames() {

        // RxNOTE wcpr No defensive copy since package protected method
        return secondaryNames;
    }

    /**
     * <p><!-- Method description --></p>
     *
     *
     * @return
     */
    boolean hasDetails() {
        return details;
    }

    /**
     * <p><!-- Method description --></p>
     *
     *
     * @return
     */
    boolean hasPrimaryImage() {
        return primaryImage;
    }

    /**
     * Add this concepts children to the node passed to the method. For example,
     * <pre>
     * TreeConcept treeConcept = new TreeConcept(rootConcept);
     * DefaultMutableTreeNode rootNode =
     *     new DefaultMutableTreeNode(treeConcept);
     * treeConcept.lazyExpand(rootNode);
     *
     * </pre>
     *
     * @param parent
     * @return
     *
     */
    public synchronized boolean lazyExpand(DefaultMutableTreeNode parent) {

        // Return false if attempt made to expand a node with no children.
        if (parent.isLeaf()) {
            return false;
        }

        // check if this node needs to be updated
        DefaultMutableTreeNode flag = (DefaultMutableTreeNode) parent.getFirstChild();

        if (flag == null) {
            return false;    // no flag set, this node doesn't need any action performed
        }

        Object obj = flag.getUserObject();

        if (!(obj instanceof Boolean)) {
            return false;    // the first object is not a flag, no need to expand
        }

        // remove the flag
        parent.removeAllChildren();

        Collection concepts = getConcept().getChildConceptColl();

        for (Iterator iter = concepts.iterator(); iter.hasNext(); ) {
            Concept                childConcept = (Concept) iter.next();
            DefaultMutableTreeNode node         = new SortedTreeNode(new TreeConcept(childConcept));

            parent.add(node);

            if (childConcept.hasChildConcepts()) {
                node.add(new SortedTreeNode(Boolean.TRUE));
            }
        }

        return true;
    }

    /**
     * <p><!-- Method description --></p>
     *
     *
     * @return
     */
    public String toString() {
        StringBuffer buf = new StringBuffer("TreeConcept( ");

        buf.append(name);

        if (0 < secondaryNames.length) {
            buf.append(": ");

            for (int i = 0; i < secondaryNames.length - 1; ++i) {
                buf.append(secondaryNames[i]);
                buf.append(", ");
            }

            buf.append(secondaryNames[secondaryNames.length - 1]);
        }

        buf.append(" )");

        return buf.toString();
    }
}
